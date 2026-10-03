package com.goreecloud.launcher

import android.os.Process
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.launcher.core.launcher.LauncherAppsRepository
import com.goreecloud.launcher.core.launcher.LauncherHomeAppMode
import com.goreecloud.launcher.core.launcher.LauncherPreferencesRepository
import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import com.goreecloud.launcher.core.workspace.db.LauncherDatabaseProvider
import com.goreecloud.launcher.core.workspace.db.WorkspaceLegacyImportMapper
import com.goreecloud.launcher.core.workspace.db.WorkspacePagedHomeState
import com.goreecloud.launcher.core.workspace.db.WorkspaceProductionRuntimeCoordinator
import com.goreecloud.launcher.core.workspace.workspaceKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinnedAppsManagerRuntimeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var preferencesRepository: LauncherPreferencesRepository
    private var previousHomeAppMode = LauncherHomeAppMode.NONE
    private var previousPinnedKeys: Set<String> = emptySet()
    private var previousPinnedOrder: List<String> = emptyList()
    private var testPinnedKey: String? = null
    private var testPinnedLabel: String? = null

    @Before
    fun prepareEstablishedLauncher(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        preferencesRepository = LauncherPreferencesRepository(context)
        previousHomeAppMode = preferencesRepository.experiencePreferences.first().homeAppMode
        val previousPinnedState = preferencesRepository.drawerPinnedState.first()
        previousPinnedKeys = previousPinnedState.keys
        previousPinnedOrder = previousPinnedState.order

        previousPinnedKeys.forEach { key ->
            preferencesRepository.setDrawerAppPinned(key, false).join()
        }
        preferencesRepository.setHomeAppMode(LauncherHomeAppMode.NONE).join()
        preferencesRepository.setHomeHintsDismissed(true).join()
        preferencesRepository.markStartupWizardCompleted().join()

        val candidate = withTimeout(10_000) {
            LauncherAppsRepository(context).apps.first { apps ->
                apps.any { app -> app.user == Process.myUserHandle() }
            }.first { app -> app.user == Process.myUserHandle() }
        }
        testPinnedKey = candidate.workspaceKey()
        testPinnedLabel = candidate.label.toString()
        preferencesRepository.setDrawerAppPinned(candidate.workspaceKey(), true).join()
        withTimeout(5_000) {
            preferencesRepository.drawerPinnedState.first { state ->
                candidate.workspaceKey() in state.keys
            }
        }

        val workspaceRepository = WorkspaceRepository(context)
        workspaceRepository.ensureDefaults(
            favoriteKeys = emptyList(),
            dockKeys = emptyList(),
        )
        val runtime = WorkspaceProductionRuntimeCoordinator(
            authorityRepository = workspaceRepository,
            workspaceDaoProvider = {
                LauncherDatabaseProvider.get(context).workspaceDao()
            },
        )
        runtime.reconcileAndActivate()
        withTimeout(10_000) {
            workspaceRepository.state.first {
                it.initialized && it.authority == WorkspaceAuthority.ROOM
            }
        }
        withTimeout(10_000) {
            runtime.observeHomePages().first { state ->
                state is WorkspacePagedHomeState.Ready &&
                    state.pages.any { page ->
                        page.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
                    }
            }
        }

        preferencesRepository.markStarterLayoutApplied().join()
        withTimeout(5_000) {
            preferencesRepository.experiencePreferences.first {
                it.homeAppMode == LauncherHomeAppMode.NONE &&
                    it.startupWizardCompleted &&
                    it.starterLayoutApplied
            }
        }
        Unit
    }

    @After
    fun restorePreferences() = runBlocking {
        if (!::preferencesRepository.isInitialized) return@runBlocking
        val currentPinnedState = preferencesRepository.drawerPinnedState.first()
        currentPinnedState.keys.forEach { key ->
            preferencesRepository.setDrawerAppPinned(key, false).join()
        }
        previousPinnedKeys.forEach { key ->
            preferencesRepository.setDrawerAppPinned(key, true).join()
        }
        preferencesRepository.setDrawerPinnedAppOrder(previousPinnedOrder).join()
        preferencesRepository.setHomeAppMode(previousHomeAppMode).join()
    }

    @Test
    fun settingsPinnedAppsManagerCanUnpinWithoutChangingWorkspace() {
        val pinnedKey = requireNotNull(testPinnedKey)
        val pinnedLabel = requireNotNull(testPinnedLabel)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule
                    .onAllNodesWithTag(
                        "launcher-home-empty-space-actions",
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithTag(
                    "launcher-home-empty-space-actions",
                    useUnmergedTree = true,
                )
                .performSemanticsAction(SemanticsActions.OnLongClick)

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule
                    .onAllNodesWithTag(
                        "launcher-home-editor-fullscreen",
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onAllNodesWithText("Settings", useUnmergedTree = true)[0]
                .performClick()
            composeRule
                .onNodeWithText("Launcher settings", useUnmergedTree = true)
                .assertIsDisplayed()
            composeRule
                .onNodeWithText("App drawer", useUnmergedTree = true)
                .performClick()

            composeRule
                .onNodeWithTag("launcher-settings-pinned-apps", useUnmergedTree = true)
                .performScrollTo()
                .performClick()

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule
                    .onAllNodesWithTag(
                        "launcher-pinned-apps-manager",
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule
                .onNodeWithTag("launcher-pinned-apps-manager", useUnmergedTree = true)
                .assertIsDisplayed()
            composeRule
                .onNodeWithText(pinnedLabel, useUnmergedTree = true)
                .assertIsDisplayed()

            composeRule
                .onNodeWithTag(
                    "launcher-unpin-pinned-app-" + pinnedKey,
                    useUnmergedTree = true,
                )
                .performClick()

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule
                    .onAllNodesWithText("No pinned apps.", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule
                .onNodeWithText("No pinned apps.", useUnmergedTree = true)
                .assertIsDisplayed()

            runBlocking {
                withTimeout(5_000) {
                    preferencesRepository.drawerPinnedState.first { state ->
                        pinnedKey !in state.keys
                    }
                }
            }
        } finally {
            scenario.close()
        }
    }
}
