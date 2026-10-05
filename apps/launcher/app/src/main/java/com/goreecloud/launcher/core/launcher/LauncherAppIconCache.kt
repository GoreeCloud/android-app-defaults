package com.goreecloud.launcher.core.launcher

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.max
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

internal const val LAUNCHER_ICON_DECODE_SIZE_PX = 144
internal const val LAUNCHER_ICON_CACHE_MAX_KIB = 12 * 1024
internal const val LAUNCHER_ICON_STALE_CACHE_MAX_KIB = 4 * 1024
internal const val LAUNCHER_ICON_PRELOAD_COUNT = 128
internal const val LAUNCHER_ICON_PRELOAD_PARALLELISM = 3
internal const val LAUNCHER_ADAPTIVE_FOREGROUND_OVERSCAN_FRACTION = 0.14f
internal const val LAUNCHER_LEGACY_ICON_NORMALIZE_THRESHOLD = 0.84f
internal const val LAUNCHER_LEGACY_ICON_TARGET_FRACTION = 0.90f
internal const val LAUNCHER_LEGACY_ICON_MAX_SCALE = 1.24f

internal data class LauncherIconCacheKey(
    val user: UserHandle,
    val componentName: ComponentName,
)

private data class LauncherIconPackageKey(
    val user: UserHandle,
    val packageName: String,
)

internal data class LauncherIconCacheStamp(
    val generation: Long,
    val packageGeneration: Long,
)

private data class LauncherIconLoadKey(
    val cacheKey: LauncherIconCacheKey,
    val stamp: LauncherIconCacheStamp,
)

internal fun <T> firstSuccessfulIconLoad(vararg loaders: () -> T?): T? {
    loaders.forEach { loader ->
        val value = runCatching(loader).getOrNull()
        if (value != null) return value
    }
    return null
}

internal fun launcherLegacyIconNormalizationScale(
    contentWidth: Int,
    contentHeight: Int,
    canvasSize: Int,
): Float {
    if (contentWidth <= 0 || contentHeight <= 0 || canvasSize <= 0) return 1f
    val occupiedFraction = max(contentWidth, contentHeight).toFloat() / canvasSize.toFloat()
    if (occupiedFraction >= LAUNCHER_LEGACY_ICON_NORMALIZE_THRESHOLD) return 1f
    return (LAUNCHER_LEGACY_ICON_TARGET_FRACTION / occupiedFraction)
        .coerceIn(1f, LAUNCHER_LEGACY_ICON_MAX_SCALE)
}

private fun renderDrawableSquare(
    drawable: Drawable,
    sizePx: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val copy = drawable.mutate()
    val previous = Rect(copy.bounds)
    copy.setBounds(0, 0, sizePx, sizePx)
    copy.draw(canvas)
    copy.bounds = previous
    return bitmap
}

private fun renderAdaptiveIconSquare(
    drawable: AdaptiveIconDrawable,
    sizePx: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    drawable.background?.mutate()?.let { background ->
        val previous = Rect(background.bounds)
        background.setBounds(0, 0, sizePx, sizePx)
        background.draw(canvas)
        background.bounds = previous
    }

    drawable.foreground?.mutate()?.let { foreground ->
        // Android adaptive foreground layers intentionally include a large safe zone because the
        // platform may apply many masks. Launcher owns the selected mask, so render the layer with
        // bounded overscan and let Compose perform the final user-selected clip.
        val overscan = (sizePx * LAUNCHER_ADAPTIVE_FOREGROUND_OVERSCAN_FRACTION).toInt()
        val previous = Rect(foreground.bounds)
        foreground.setBounds(-overscan, -overscan, sizePx + overscan, sizePx + overscan)
        foreground.draw(canvas)
        foreground.bounds = previous
    }

    return bitmap
}

private fun alphaBounds(bitmap: Bitmap): Rect? {
    var left = bitmap.width
    var top = bitmap.height
    var right = -1
    var bottom = -1

    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            if ((bitmap.getPixel(x, y) ushr 24) > 8) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }

    return if (right < left || bottom < top) null else Rect(left, top, right + 1, bottom + 1)
}

private fun normalizeLegacyIconBitmap(bitmap: Bitmap): Bitmap {
    val bounds = alphaBounds(bitmap) ?: return bitmap
    val scale = launcherLegacyIconNormalizationScale(
        contentWidth = bounds.width(),
        contentHeight = bounds.height(),
        canvasSize = bitmap.width.coerceAtMost(bitmap.height),
    )
    if (scale <= 1.001f) return bitmap

    val destinationWidth = (bounds.width() * scale).coerceAtMost(bitmap.width.toFloat())
    val destinationHeight = (bounds.height() * scale).coerceAtMost(bitmap.height.toFloat())
    val left = (bitmap.width - destinationWidth) / 2f
    val top = (bitmap.height - destinationHeight) / 2f
    val normalized = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    Canvas(normalized).drawBitmap(
        bitmap,
        bounds,
        RectF(left, top, left + destinationWidth, top + destinationHeight),
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
    )
    return normalized
}

private fun renderLauncherIconBitmap(
    drawable: Drawable,
    sizePx: Int,
): Bitmap =
    if (drawable is AdaptiveIconDrawable) {
        renderAdaptiveIconSquare(drawable, sizePx)
    } else {
        normalizeLegacyIconBitmap(renderDrawableSquare(drawable, sizePx))
    }

private fun renderLauncherIconBitmap(
    drawable: Drawable,
    packageManager: PackageManager?,
    user: UserHandle,
    sizePx: Int,
): Bitmap {
    val normalized = renderLauncherIconBitmap(drawable, sizePx)
    if (packageManager == null) return normalized

    val badged = packageManager.getUserBadgedIcon(
        BitmapDrawable(Resources.getSystem(), normalized),
        user,
    )
    // Badging is applied only after normalization so Android cannot bake its default adaptive mask
    // into the base app artwork before Launcher applies the selected shape.
    return renderDrawableSquare(badged, sizePx)
}

internal class LauncherIconSingleFlightLoader<K : Any, V>(
    private val scope: CoroutineScope,
) {
    private val inFlight = ConcurrentHashMap<K, CompletableDeferred<V>>()

    suspend fun load(key: K, block: suspend () -> V): V {
        val candidate = CompletableDeferred<V>()
        val existing = inFlight.putIfAbsent(key, candidate)
        if (existing != null) return existing.await()

        scope.launch {
            try {
                candidate.complete(block())
            } catch (failure: Throwable) {
                candidate.completeExceptionally(failure)
            } finally {
                inFlight.remove(key, candidate)
            }
        }
        return candidate.await()
    }
}

/**
 * Owns the single replaceable background icon-preload job.
 *
 * Inventory snapshots can change again while an older warm pass is still walking its bounded tail.
 * Replacing that pass prevents stale background work from continuing to schedule additional icon
 * loads. Already-started single-flight decodes remain safe: callers for the newest snapshot can join
 * them, while cache stamps still reject results made stale by package/profile invalidation.
 */
internal class LauncherLatestPreloadRunner(
    private val scope: CoroutineScope,
) {
    private val stateLock = Any()
    private var generation = 0L
    private var currentJob: Job? = null

    fun replace(block: suspend CoroutineScope.() -> Unit) {
        val (ticket, previousJob) = synchronized(stateLock) {
            generation += 1L
            val previous = currentJob
            currentJob = null
            generation to previous
        }
        previousJob?.cancel()

        val candidate = scope.launch(start = CoroutineStart.LAZY, block = block)
        val accepted = synchronized(stateLock) {
            if (ticket != generation) {
                false
            } else {
                currentJob = candidate
                true
            }
        }
        if (!accepted) {
            candidate.cancel()
            return
        }

        candidate.invokeOnCompletion {
            synchronized(stateLock) {
                if (currentJob === candidate) {
                    currentJob = null
                }
            }
        }
        candidate.start()
    }

    fun cancel() {
        val job = synchronized(stateLock) {
            generation += 1L
            currentJob.also { currentJob = null }
        }
        job?.cancel()
    }
}

/**
 * Bounded process-local cache for Android-provided badged launcher icons.
 *
 * LauncherApps remains inventory authority. This cache owns presentation bitmaps only; nothing is
 * persisted and package invalidation makes stale decode work unable to re-enter the cache.
 */
internal object LauncherAppIconCache {
    private val stateLock = Any()
    private var generation = 0L
    private val packageGenerations = mutableMapOf<LauncherIconPackageKey, Long>()
    private val loadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val singleFlight = LauncherIconSingleFlightLoader<LauncherIconLoadKey, Bitmap?>(loadScope)
    private val preloadRunner = LauncherLatestPreloadRunner(loadScope)

    private val cache = object : LruCache<LauncherIconCacheKey, Bitmap>(LAUNCHER_ICON_CACHE_MAX_KIB) {
        override fun sizeOf(key: LauncherIconCacheKey, value: Bitmap): Int =
            bitmapSizeKib(value)
    }

    /**
     * Short-lived stale-while-revalidate fallback.
     *
     * Package/profile refreshes should not flash a generic placeholder while Android's replacement
     * icon is being decoded. Stale entries are process-local, bounded, and removed as soon as a
     * fresh authoritative icon is available.
     */
    private val staleCache =
        object : LruCache<LauncherIconCacheKey, Bitmap>(LAUNCHER_ICON_STALE_CACHE_MAX_KIB) {
            override fun sizeOf(key: LauncherIconCacheKey, value: Bitmap): Int =
                bitmapSizeKib(value)
        }

    fun peek(app: LauncherActivityInfo): Bitmap? = synchronized(stateLock) {
        val key = app.cacheKey()
        cache.get(key) ?: staleCache.get(key)
    }

    /**
     * Stable presentation identity for Compose icon state.
     *
     * The stamp changes only when the complete icon cache or this app's package scope is invalidated,
     * so ordinary LauncherApps snapshot object churn does not discard an already-warm icon.
     */
    fun stamp(app: LauncherActivityInfo): LauncherIconCacheStamp = synchronized(stateLock) {
        stampFor(app)
    }

    fun preload(
        apps: List<LauncherActivityInfo>,
        packageManager: PackageManager? = null,
        maxCount: Int = LAUNCHER_ICON_PRELOAD_COUNT,
    ) {
        if (maxCount <= 0) {
            preloadRunner.cancel()
            return
        }

        // Every new authoritative inventory snapshot replaces the older bounded warm pass. Candidate
        // selection stays off the UI thread, and the existing single-flight loader still coalesces any
        // decode already in progress when a newer snapshot requests the same icon.
        preloadRunner.replace {
            val candidates = apps.asSequence()
                .distinctBy { app -> app.cacheKey() }
                .take(maxCount)
                .filter { app -> peek(app) == null }
                .toList()
            if (candidates.isEmpty()) return@replace

            candidates.chunked(LAUNCHER_ICON_PRELOAD_PARALLELISM).forEach { batch ->
                batch.map { app ->
                    async { load(app, packageManager) }
                }.awaitAll()
            }
        }
    }

    suspend fun load(
        app: LauncherActivityInfo,
        packageManager: PackageManager? = null,
    ): Bitmap? {
        val key = app.cacheKey()
        val requestedStamp = synchronized(stateLock) {
            cache.get(key)?.let { return it }
            stampFor(app)
        }
        val loadKey = LauncherIconLoadKey(key, requestedStamp)

        return singleFlight.load(loadKey) {
            val cachedAfterClaim = synchronized(stateLock) {
                if (stampFor(app) == requestedStamp) cache.get(key) else null
            }
            if (cachedAfterClaim != null) {
                cachedAfterClaim
            } else if (!isCurrentStamp(app, requestedStamp)) {
                null
            } else {
                val decoded = firstSuccessfulIconLoad(
                    {
                        // Prefer the unbadged source so adaptive foreground/background layers can
                        // be flattened without Android's system mask. Launcher then applies its
                        // selected mask consistently across Home, Dock, Apps, Search, and folders.
                        renderLauncherIconBitmap(
                            drawable = app.getIcon(0),
                            packageManager = packageManager,
                            user = app.user,
                            sizePx = LAUNCHER_ICON_DECODE_SIZE_PX,
                        )
                    },
                    {
                        // PackageManager can still expose the activity resource when the direct
                        // LauncherActivityInfo resource path is temporarily unavailable.
                        packageManager?.let { manager ->
                            renderLauncherIconBitmap(
                                drawable = manager.getActivityIcon(app.componentName),
                                packageManager = manager,
                                user = app.user,
                                sizePx = LAUNCHER_ICON_DECODE_SIZE_PX,
                            )
                        }
                    },
                    {
                        // A small number of vendor launch activities expose a broken activity icon
                        // while their application icon remains valid.
                        packageManager?.let { manager ->
                            renderLauncherIconBitmap(
                                drawable = app.applicationInfo.loadIcon(manager),
                                packageManager = manager,
                                user = app.user,
                                sizePx = LAUNCHER_ICON_DECODE_SIZE_PX,
                            )
                        }
                    },
                    {
                        // Retain Android's already-badged bitmap only as a late compatibility
                        // fallback. It may already contain an OEM/system mask and therefore should
                        // never outrank the mask-neutral paths above.
                        app.getBadgedIcon(0).toBitmap(
                            width = LAUNCHER_ICON_DECODE_SIZE_PX,
                            height = LAUNCHER_ICON_DECODE_SIZE_PX,
                        )
                    },
                    {
                        // Final Android-owned fail-soft presentation fallback. This prevents a
                        // blank drawer slot when every app-owned icon resource is unreadable.
                        packageManager?.let { manager ->
                            renderLauncherIconBitmap(
                                drawable = manager.defaultActivityIcon,
                                packageManager = manager,
                                user = app.user,
                                sizePx = LAUNCHER_ICON_DECODE_SIZE_PX,
                            )
                        }
                    },
                )

                synchronized(stateLock) {
                    if (stampFor(app) != requestedStamp) {
                        staleCache.get(key)
                    } else if (decoded == null) {
                        staleCache.get(key)
                    } else {
                        cache.get(key) ?: decoded.also {
                            cache.put(key, it)
                            staleCache.remove(key)
                        }
                    }
                }
            }
        }
    }

    fun invalidatePackage(packageName: String, user: UserHandle) = synchronized(stateLock) {
        val packageKey = LauncherIconPackageKey(user, packageName)
        packageGenerations[packageKey] = (packageGenerations[packageKey] ?: 0L) + 1L
        cache.snapshot().keys
            .filter { key -> key.user == user && key.componentName.packageName == packageName }
            .forEach { key ->
                cache.get(key)?.let { staleCache.put(key, it) }
                cache.remove(key)
            }
    }

    fun clear() {
        preloadRunner.cancel()
        synchronized(stateLock) {
            generation += 1L
            packageGenerations.clear()
            cache.evictAll()
            staleCache.evictAll()
        }
    }

    private fun isCurrentStamp(
        app: LauncherActivityInfo,
        expected: LauncherIconCacheStamp,
    ): Boolean = synchronized(stateLock) {
        stampFor(app) == expected
    }

    private fun stampFor(app: LauncherActivityInfo): LauncherIconCacheStamp =
        LauncherIconCacheStamp(
            generation = generation,
            packageGeneration = packageGenerations[
                LauncherIconPackageKey(app.user, app.componentName.packageName)
            ] ?: 0L,
        )

    private fun LauncherActivityInfo.cacheKey(): LauncherIconCacheKey =
        LauncherIconCacheKey(user = user, componentName = componentName)

    private fun bitmapSizeKib(value: Bitmap): Int =
        ((value.allocationByteCount.toLong() + 1023L) / 1024L)
            .coerceAtLeast(1L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
}
