package com.goreecloud.launcher.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle
import com.goreecloud.launcher.core.launcher.LauncherAppFreshness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads only package timestamps available through the current user's PackageManager.
 *
 * Other Android profiles deliberately fail closed until Launcher has a profile-correct timestamp
 * authority. This avoids applying primary-user package timestamps to an identically named Work or
 * managed-profile package.
 */
internal suspend fun loadLauncherDrawerFreshness(
    packageManager: PackageManager,
    packageNames: Collection<String>,
): Map<String, LauncherAppFreshness> = withContext(Dispatchers.IO) {
    packageNames
        .asSequence()
        .filter(String::isNotBlank)
        .distinct()
        .mapNotNull { packageName ->
            launcherDrawerFreshness(packageManager, packageName)?.let { packageName to it }
        }
        .toMap()
}

@Suppress("DEPRECATION")
private fun launcherDrawerFreshness(
    packageManager: PackageManager,
    packageName: String,
): LauncherAppFreshness? = runCatching {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()),
        )
    } else {
        packageManager.getPackageInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
    }
    LauncherAppFreshness(
        firstInstallTimeMillis = packageInfo.firstInstallTime,
        lastUpdateTimeMillis = packageInfo.lastUpdateTime,
    )
}.getOrNull()

internal fun launcherDrawerFreshnessForApp(
    appUser: UserHandle,
    primaryUser: UserHandle,
    packageName: String,
    freshnessByPackage: Map<String, LauncherAppFreshness>,
): LauncherAppFreshness? =
    if (appUser == primaryUser) freshnessByPackage[packageName] else null
