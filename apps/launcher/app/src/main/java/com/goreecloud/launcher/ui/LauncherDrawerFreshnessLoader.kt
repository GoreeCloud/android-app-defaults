package com.goreecloud.launcher.ui

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import com.goreecloud.launcher.core.launcher.LauncherAppFreshness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class LauncherProfilePackageKey(
    val profileId: Int,
    val packageName: String,
)

/**
 * Loads package freshness without borrowing primary-user metadata for a different Android profile.
 *
 * LauncherActivityInfo remains the profile-qualified source for first-install time. Update time is
 * read from a profile-scoped Context when Android permits that access; otherwise it fails closed to
 * the first-install timestamp so "New" remains truthful while "Updated" does not fabricate a hit.
 */
internal suspend fun loadLauncherDrawerFreshness(
    context: Context,
    apps: Collection<LauncherActivityInfo>,
): Map<String, LauncherAppFreshness> = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    val primaryUser = Process.myUserHandle()
    val packageCache = mutableMapOf<LauncherProfilePackageKey, Pair<Long, Long>?>()

    buildMap {
        apps.forEach { app ->
            val packageName = app.componentName.packageName
            val firstInstall = app.firstInstallTime.takeIf { it > 0L } ?: 0L
            val cacheKey = LauncherProfilePackageKey(
                profileId = app.user.hashCode(),
                packageName = packageName,
            )

            val packageTimes = packageCache.getOrPut(cacheKey) {
                val profileContext = when {
                    app.user == primaryUser -> appContext
                    else -> runCatching {
                        appContext.createContextAsUser(app.user, 0)
                    }.getOrNull()
                }
                profileContext?.let { scopedContext ->
                    launcherPackageTimes(
                        packageManager = scopedContext.packageManager,
                        packageName = packageName,
                    )
                }
            }

            val installedAt = firstInstall.takeIf { it > 0L }
                ?: packageTimes?.first?.takeIf { it > 0L }
                ?: return@forEach
            val authoritativeUpdate = packageTimes?.second?.takeIf { it > 0L }
            val updatedAt = authoritativeUpdate ?: installedAt

            put(
                app.workspaceKey(),
                LauncherAppFreshness(
                    firstInstallTimeMillis = installedAt,
                    lastUpdateTimeMillis = updatedAt,
                    updateMetadataAvailable = authoritativeUpdate != null,
                ),
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun launcherPackageTimes(
    packageManager: PackageManager,
    packageName: String,
): Pair<Long, Long>? = runCatching {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0L),
        )
    } else {
        packageManager.getPackageInfo(packageName, 0)
    }
    packageInfo.firstInstallTime to packageInfo.lastUpdateTime
}.getOrNull()
