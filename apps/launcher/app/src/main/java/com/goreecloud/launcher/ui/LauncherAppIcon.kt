package com.goreecloud.launcher.ui

import android.content.pm.LauncherActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.goreecloud.launcher.core.launcher.LauncherAppIconCache
import com.goreecloud.launcher.core.launcher.LauncherIconFillMode
import com.goreecloud.launcher.core.launcher.LauncherIconPackRepository
import com.goreecloud.launcher.core.launcher.LauncherIconShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal data class LauncherIconAppearance(
    val shape: LauncherIconShape = LauncherIconShape.ROUNDED_SQUARE,
    val iconPackPackage: String? = null,
    val fillMode: LauncherIconFillMode = LauncherIconFillMode.BALANCED,
)

internal val LocalLauncherIconAppearance = compositionLocalOf {
    LauncherIconAppearance()
}

/**
 * Reads cached Android icon state synchronously and replaces it with the selected icon-pack asset
 * when one exists. Missing pack mappings fail soft to the original Android-provided icon.
 */
@Composable
internal fun rememberLauncherAppIcon(app: LauncherActivityInfo): ImageBitmap? {
    val appearance = LocalLauncherIconAppearance.current
    val appContext = LocalContext.current.applicationContext
    val cacheStamp = LauncherAppIconCache.stamp(app)
    var icon by remember(
        app.componentName,
        app.user,
        cacheStamp,
        appearance.iconPackPackage,
    ) {
        mutableStateOf(LauncherAppIconCache.peek(app)?.asImageBitmap())
    }

    LaunchedEffect(
        app.componentName,
        app.user,
        cacheStamp,
        appearance.iconPackPackage,
    ) {
        val iconPackPackage = appearance.iconPackPackage
        val themed = if (iconPackPackage.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                LauncherIconPackRepository(appContext)
                    .loadIcon(iconPackPackage, app.componentName)
            }
        }

        if (themed != null) {
            icon = themed.asImageBitmap()
        } else {
            // Package/profile callbacks can invalidate artwork while a decode is in flight. OEM
            // launchers also expose short windows where an updated activity resource is not yet
            // readable. Re-read the process cache first, then retry the authoritative Android icon
            // path with a small bounded backoff. Never replace already-rendered artwork with null.
            var loaded = LauncherAppIconCache.peek(app)
            var attempt = 0
            while (loaded == null && attempt < 4) {
                loaded = LauncherAppIconCache.load(app, appContext.packageManager)
                if (loaded == null) {
                    delay(75L * (attempt + 1))
                }
                attempt += 1
            }
            if (loaded != null) {
                icon = loaded.asImageBitmap()
            }
        }
    }

    return icon
}

/**
 * Applies only the selected geometric mask.
 *
 * The previous implementation painted a generic surface-colored plane behind Android artwork.
 * That made already-masked icons look like a small icon sitting inside a second gray mask. Adaptive
 * icon backgrounds are now reconstructed by [LauncherAppIconCache], so this layer stays neutral.
 */
@Composable
internal fun Modifier.launcherIconMask(): Modifier {
    val shape = LocalLauncherIconAppearance.current.shape
    if (shape == LauncherIconShape.ORIGINAL) return this
    return clip(shape.toLauncherComposeShape())
}

/**
 * Shared app-artwork renderer for Home, Dock, Apps, Search and folder/provider surfaces.
 *
 * Masked icons get bounded optical expansion so app artwork reaches the selected mask without
 * aggressively cropping legacy logo-only artwork. Users can choose Fit, Balanced or Fill. Original
 * shape bypasses both clipping and optical expansion.
 */
@Composable
internal fun LauncherAppIconImage(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val appearance = LocalLauncherIconAppearance.current
    val masked = appearance.shape != LauncherIconShape.ORIGINAL
    val artworkScale = if (masked) appearance.fillMode.artworkScale else 1f
    val containerModifier = if (masked) {
        modifier.clip(appearance.shape.toLauncherComposeShape())
    } else {
        modifier
    }

    Box(modifier = containerModifier) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = artworkScale
                    scaleY = artworkScale
                },
        )
    }
}

internal fun LauncherIconShape.toLauncherComposeShape(): Shape = when (this) {
    LauncherIconShape.ROUNDED_SQUARE -> RoundedCornerShape(24)
    LauncherIconShape.ORIGINAL -> RoundedCornerShape(0)
    LauncherIconShape.SQUIRCLE -> RoundedCornerShape(38)
    LauncherIconShape.CIRCLE -> CircleShape
    LauncherIconShape.TEARDROP -> RoundedCornerShape(
        topStart = CornerSize(50),
        topEnd = CornerSize(50),
        bottomEnd = CornerSize(12),
        bottomStart = CornerSize(50),
    )
}
