package com.goreecloud.gallery

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.LruCache
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import com.goreecloud.gallery.android.AndroidMediaCopyRequests
import com.goreecloud.gallery.android.AndroidMediaCopySource
import com.goreecloud.gallery.android.AndroidMediaMovePendingState
import com.goreecloud.gallery.android.AndroidMediaMoveRequests
import com.goreecloud.gallery.android.AndroidMediaMutationMode
import com.goreecloud.gallery.android.AndroidMediaMutationPendingState
import com.goreecloud.gallery.android.AndroidMediaMutationPendingStates
import com.goreecloud.gallery.android.AndroidMediaMutationRequests
import com.goreecloud.gallery.android.AndroidMediaStoreReader
import com.goreecloud.gallery.core.GalleryBulkActionPolicy
import com.goreecloud.gallery.core.AuthorizedMediaSearch
import com.goreecloud.gallery.core.GalleryDragSelectionPolicy
import com.goreecloud.gallery.core.GalleryDragSelectionSession
import com.goreecloud.gallery.core.GalleryCopyDestination
import com.goreecloud.gallery.core.GalleryCopyDestinationPolicy
import com.goreecloud.gallery.core.GalleryCopyNamePolicy
import com.goreecloud.gallery.core.GalleryFavoriteBulkAction
import com.goreecloud.gallery.core.GalleryMoveDestination
import com.goreecloud.gallery.core.GalleryMoveDestinationPolicy
import com.goreecloud.gallery.core.GalleryNewFolderCopyPolicy
import com.goreecloud.gallery.core.GalleryNewFolderMovePolicy
import com.goreecloud.gallery.core.GallerySelectionPolicy
import com.goreecloud.gallery.core.MediaItem
import com.goreecloud.gallery.core.MediaSortOrder
import com.goreecloud.gallery.core.buildAlbumCatalog
import com.goreecloud.gallery.core.sort
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class GalleryActivity : Activity() {
    private lateinit var rootFrame: FrameLayout
    private lateinit var content: LinearLayout
    private lateinit var headerTitle: TextView
    private lateinit var headerSubtitle: TextView
    private lateinit var brandMark: ImageView
    private lateinit var backControl: ImageView
    private lateinit var searchControl: ImageView
    private lateinit var sortControl: ImageView
    private lateinit var searchContainer: LinearLayout
    private lateinit var searchField: EditText
    private var videoFilterStripView: View? = null
    private lateinit var accessPanel: LinearLayout
    private lateinit var status: TextView
    private lateinit var action: TextView
    private lateinit var library: LinearLayout
    private lateinit var libraryScroll: ScrollView
    private lateinit var navigationCapsule: LinearLayout
    private lateinit var selectionActionCapsule: LinearLayout

    private var thumbnailWorkerCount = GalleryFileLoadingPriority.FAST.thumbnailWorkerCount
    private var thumbnailExecutor: ExecutorService = Executors.newFixedThreadPool(thumbnailWorkerCount)
    private val thumbnailCache = object : LruCache<String, Bitmap>(THUMBNAIL_CACHE_KIB) {
        override fun sizeOf(key: String, value: Bitmap): Int = maxOf(1, value.allocationByteCount / 1024)
    }

    private val favoriteUris = linkedSetOf<String>()
    private val selectedUris = linkedSetOf<String>()
    private val renderedMediaTiles = linkedMapOf<String, FrameLayout>()
    private val navigationItems = linkedMapOf<GalleryDestination, TextView>()
    private var selectionScopeItems: List<MediaItem> = emptyList()
    private var dragSelectionSession: GalleryDragSelectionSession? = null
    private var dragSelectionScope: List<MediaItem> = emptyList()
    private var dragSelectionRawX = 0f
    private var dragSelectionRawY = 0f
    private var dragSelectionAutoScrollPosted = false
    private var loadGeneration = 0
    private var authorizedItems: List<MediaItem> = emptyList()
    private var selectedSort = MediaSortOrder.NEWEST
    private var destination = GalleryDestination.PHOTOS
    private var openAlbumId: String? = null
    private var showingFavorites = false
    private var searchQuery = ""
    private var videoFilter = GalleryVideoFilter.ALL
    private var suppressSearchRender = false
    private var viewerOverlay: View? = null
    private var viewerVideoSurface: GalleryVideoPlayerSurface? = null
    private var viewerSlideshowStop: (() -> Unit)? = null
    private var pendingMediaMutation: AndroidMediaMutationPendingState? = null
    private var pendingMediaMove: AndroidMediaMovePendingState? = null
    private var mediaMoveExecutionInProgress = false
    private var mediaCopyExecutionInProgress = false
    private var setupDialog: AlertDialog? = null
    private val mediaRefreshHandler = Handler(Looper.getMainLooper())
    private var mediaStoreObserverRegistered = false
    private var mediaRefreshPending = false
    private val mediaRefreshRunnable = Runnable { refreshObservedMediaIfReady() }
    private val mediaStoreObserver = object : ContentObserver(mediaRefreshHandler) {
        override fun onChange(selfChange: Boolean) {
            scheduleObservedMediaRefresh()
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            scheduleObservedMediaRefresh()
        }
    }

    private val inSelectionMode: Boolean
        get() = selectedUris.isNotEmpty() || dragSelectionSession != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoriteUris += galleryPreferences()
            .getStringSet(FAVORITES_KEY, emptySet())
            .orEmpty()
        pendingMediaMutation = restorePendingMediaMutation(savedInstanceState)
        pendingMediaMove = restorePendingMediaMove(savedInstanceState)
        if (pendingMediaMutation != null && pendingMediaMove != null) {
            pendingMediaMutation = null
            pendingMediaMove = null
        }
        requestedDestination(intent)?.let { destination = it }
        selectedSort = currentUserSettings().sortPreference.mediaSortOrder
        reconfigureThumbnailExecutor(currentUserSettings().fileLoadingPriority)
        buildSurface()

        val setupPreferences = GallerySetupPreferences(this)
        if (setupPreferences.shouldShowSetup()) {
            rootFrame.post {
                if (!isFinishing && !isDestroyed) {
                    showSetupWizard(replay = false)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        setIntent(intent)
        val requested = requestedDestination(intent) ?: return

        clearSelection(render = false)
        destination = requested
        openAlbumId = null
        showingFavorites = false
        searchQuery = ""
        if (::searchField.isInitialized) {
            suppressSearchRender = true
            searchField.setText("")
            suppressSearchRender = false
            closeSearch(clearQuery = false)
        }

        if (destination == GalleryDestination.SETTINGS) {
            renderCurrentDestination()
        } else {
            renderPermissionState()
        }
    }

    private fun requestedDestination(intent: Intent?): GalleryDestination? = when (
        intent?.getStringExtra(GalleryNavigationContract.EXTRA_DESTINATION)
    ) {
        GalleryNavigationContract.PHOTOS -> GalleryDestination.PHOTOS
        GalleryNavigationContract.ALBUMS -> GalleryDestination.ALBUMS
        GalleryNavigationContract.VIDEOS -> GalleryDestination.VIDEOS
        GalleryNavigationContract.SETTINGS -> GalleryDestination.SETTINGS
        else -> null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingMediaMutation?.let { mutation ->
            outState.putString(
                STATE_PENDING_MEDIA_MUTATION_MODE,
                AndroidMediaMutationPendingStates.modeName(mutation),
            )
            outState.putStringArray(
                STATE_PENDING_MEDIA_MUTATION_URIS,
                AndroidMediaMutationPendingStates.contentUriValues(mutation),
            )
        }
        pendingMediaMove?.let { move ->
            outState.putStringArray(
                STATE_PENDING_MEDIA_MOVE_URIS,
                AndroidMediaMoveRequests.contentUriValues(move),
            )
            outState.putString(
                STATE_PENDING_MEDIA_MOVE_DESTINATION,
                move.destinationRelativePath,
            )
        }
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        registerMediaStoreObserver()
    }

    override fun onStop() {
        mediaRefreshHandler.removeCallbacks(mediaRefreshRunnable)
        mediaRefreshPending = false
        unregisterMediaStoreObserver()
        super.onStop()
    }

    override fun onPause() {
        viewerSlideshowStop?.invoke()
        viewerVideoSurface?.pauseForHost()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (viewerOverlay != null) {
            viewerVideoSurface?.resumeForHost()
            return
        }
        if (
            pendingMediaMutation != null ||
            pendingMediaMove != null ||
            mediaMoveExecutionInProgress ||
            mediaCopyExecutionInProgress
        ) return
        if (destination == GalleryDestination.SETTINGS) {
            renderCurrentDestination()
        } else {
            renderPermissionState()
        }
    }

    @Deprecated("The Development Gallery uses Android activity results for media mutation confirmation and document portability.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MEDIA_MUTATION_REQUEST) {
            val mutation = pendingMediaMutation
            pendingMediaMutation = null
            if (resultCode == RESULT_OK && mutation != null) {
                completeConfirmedMediaMutation(mutation)
            } else if (mutation != null) {
                Toast.makeText(
                    this,
                    if (mutation.mode == AndroidMediaMutationMode.TRASH) "Move to Recycle Bin canceled" else "Delete canceled",
                    Toast.LENGTH_SHORT,
                ).show()
            }
            return
        }

        if (requestCode == MEDIA_MOVE_REQUEST) {
            val move = pendingMediaMove
            pendingMediaMove = null
            if (resultCode == RESULT_OK && move != null) {
                completeConfirmedMediaMove(move)
            } else if (move != null) {
                Toast.makeText(this, "Move canceled", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            EXPORT_FAVORITES_REQUEST -> writeJsonDocument(
                uri = uri,
                json = buildFavoritesExportJson(),
                successMessage = "Favorites exported",
            )
            IMPORT_FAVORITES_REQUEST -> readJsonDocument(uri) { importFavorites(it) }
            EXPORT_SETTINGS_REQUEST -> writeJsonDocument(
                uri = uri,
                json = buildSettingsExportJson(),
                successMessage = "Gallery settings exported",
            )
            IMPORT_SETTINGS_REQUEST -> readJsonDocument(uri) { importSettings(it) }
        }
    }

    private fun restorePendingMediaMutation(savedInstanceState: Bundle?): AndroidMediaMutationPendingState? =
        GalleryMediaMutationPendingPolicy.restore(
            modeName = savedInstanceState?.getString(STATE_PENDING_MEDIA_MUTATION_MODE),
            contentUris = savedInstanceState?.getStringArray(STATE_PENDING_MEDIA_MUTATION_URIS)?.asList(),
        )

    private fun restorePendingMediaMove(savedInstanceState: Bundle?): AndroidMediaMovePendingState? =
        AndroidMediaMoveRequests.restore(
            contentUris = savedInstanceState?.getStringArray(STATE_PENDING_MEDIA_MOVE_URIS)?.asList(),
            destinationRelativePath = savedInstanceState?.getString(STATE_PENDING_MEDIA_MOVE_DESTINATION),
        )

    override fun onDestroy() {
        setupDialog?.dismiss()
        setupDialog = null
        viewerSlideshowStop?.invoke()
        viewerSlideshowStop = null
        viewerVideoSurface?.apply {
            onPlaybackError = null
            stop()
        }
        viewerVideoSurface = null
        mediaRefreshHandler.removeCallbacks(mediaRefreshRunnable)
        unregisterMediaStoreObserver()
        thumbnailExecutor.shutdownNow()
        thumbnailCache.evictAll()
        super.onDestroy()
    }

    @Deprecated("Activity back navigation is intentionally handled for the native Development shell.")
    override fun onBackPressed() {
        if (viewerOverlay != null) {
            closeAuthorizedViewer()
            return
        }
        if (inSelectionMode) {
            clearSelection()
            return
        }
        if (destination == GalleryDestination.ALBUMS && (openAlbumId != null || showingFavorites)) {
            openAlbumId = null
            showingFavorites = false
            renderCurrentDestination()
            return
        }
        super.onBackPressed()
    }

    private fun buildSurface() {
        rootFrame = FrameLayout(this).apply {
            setBackgroundColor(canvasColor())
        }

        val contentHorizontalGutter = dp(
            GalleryGlazeContract.horizontalGutterDp(resources.configuration.screenWidthDp),
        )
        val contentTopPadding = dp(12)
        val contentBottomPadding = dp(GalleryGlazeContract.CONTENT_BOTTOM_INSET_DP)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(GalleryGlazeContract.horizontalGutterDp(resources.configuration.screenWidthDp)),
                dp(16),
                dp(GalleryGlazeContract.horizontalGutterDp(resources.configuration.screenWidthDp)),
                dp(GalleryGlazeContract.CONTENT_BOTTOM_INSET_DP),
            )
            setBackgroundColor(canvasColor())
        }

        content.addView(buildHeader())
        content.addView(buildSearchSurface())
        content.addView(buildAccessPanel())

        library = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        content.addView(
            library,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )

        libraryScroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(
                content,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }
        rootFrame.addView(
            libraryScroll,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                bottomMargin = dp(GalleryGlazeContract.NAVIGATION_RESERVED_SPACE_DP)
            },
        )

        navigationCapsule = buildNavigationCapsule()
        rootFrame.addView(
            navigationCapsule,
            bottomCapsuleLayoutParams(),
        )

        selectionActionCapsule = buildSelectionActionCapsule().apply {
            visibility = View.GONE
        }
        rootFrame.addView(
            selectionActionCapsule,
            bottomCapsuleLayoutParams(),
        )

        rootFrame.setOnApplyWindowInsetsListener { _, insets ->
            val systemBars = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.systemBars())
            } else {
                null
            }
            val topInset = if (systemBars != null) {
                systemBars.top
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetTop
            }
            val bottomInset = if (systemBars != null) {
                systemBars.bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }

            if (
                rootFrame.paddingLeft != 0 ||
                rootFrame.paddingTop != topInset ||
                rootFrame.paddingRight != 0 ||
                rootFrame.paddingBottom != bottomInset
            ) {
                rootFrame.setPadding(0, topInset, 0, bottomInset)
            }
            if (
                content.paddingLeft != contentHorizontalGutter ||
                content.paddingTop != contentTopPadding ||
                content.paddingRight != contentHorizontalGutter ||
                content.paddingBottom != contentBottomPadding
            ) {
                content.setPadding(
                    contentHorizontalGutter,
                    contentTopPadding,
                    contentHorizontalGutter,
                    contentBottomPadding,
                )
            }
            insets
        }

        setContentView(rootFrame)
        rootFrame.requestApplyInsets()
        applySystemChrome()
        renderNavigation()
        updateHeader()
    }

    private fun bottomCapsuleLayoutParams(): FrameLayout.LayoutParams =
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(GalleryGlazeContract.NAVIGATION_HEIGHT_DP),
        ).apply {
            gravity = Gravity.BOTTOM
            marginStart = dp(GalleryGlazeContract.NAVIGATION_SIDE_MARGIN_DP)
            marginEnd = dp(GalleryGlazeContract.NAVIGATION_SIDE_MARGIN_DP)
            bottomMargin = dp(GalleryGlazeContract.NAVIGATION_BOTTOM_MARGIN_DP)
        }

    private fun buildHeader(): View {
        val primaryTextColor = primaryTextColor()
        val secondaryTextColor = secondaryTextColor()

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(3), 0, 0)
        }

        brandMark = ImageView(this).apply {
            setImageResource(R.mipmap.ic_gallery_launcher)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        row.addView(
            brandMark,
            LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                marginEnd = dp(8)
            },
        )

        backControl = iconHeaderAction(R.drawable.ic_gallery_back, "Back to Albums") {
            if (inSelectionMode) {
                clearSelection()
            } else {
                openAlbumId = null
                showingFavorites = false
                renderCurrentDestination()
            }
        }.apply {
            visibility = View.GONE
        }
        row.addView(
            backControl,
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ),
        )

        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerTitle = TextView(this).apply {
            setTextColor(primaryTextColor)
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                if (resources.configuration.screenWidthDp < 360) 26f else 29f,
            )
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        headerSubtitle = TextView(this).apply {
            setTextColor(secondaryTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(1), 0, 0)
        }
        titles.addView(headerTitle)
        titles.addView(headerSubtitle)
        row.addView(
            titles,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(2)
            },
        )

        searchControl = iconHeaderAction(
            R.drawable.ic_gallery_search,
            "Search the current Gallery destination",
        ) {
            toggleSearch()
        }
        row.addView(
            searchControl,
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ),
        )

        sortControl = iconHeaderAction(
            sortIconResource(),
            "Change Gallery sort order",
        ) {
            val preference = if (selectedSort == MediaSortOrder.NEWEST) {
                GallerySortPreference.OLDEST
            } else {
                GallerySortPreference.NEWEST
            }
            selectedSort = preference.mediaSortOrder
            galleryPreferences().edit()
                .putString(SORT_PREFERENCE_KEY, preference.storedValue)
                .apply()
            renderCurrentDestination()
            announceForAccessibility("Sorted " + preference.label.lowercase())
        }
        row.addView(
            sortControl,
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ).apply {
                marginStart = dp(4)
            },
        )

        return row
    }

    private fun buildSearchSurface(): View {
        searchContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(dp(14), dp(2), dp(4), dp(2))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.CONTROL,
                GalleryGlazeContract.SHAPE_CONTAINER_DP,
            )
        }

        searchField = EditText(this).apply {
            hint = "Search photos, videos, and albums"
            setSingleLine(true)
            setTextColor(primaryTextColor())
            setHintTextColor(withAlpha(secondaryTextColor(), 0.88f))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = null
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString()?.trim().orEmpty()
                    if (!suppressSearchRender) {
                        renderCurrentDestination()
                    }
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        searchContainer.addView(
            searchField,
            LinearLayout.LayoutParams(0, dp(GalleryGlazeContract.GENERAL_TARGET_DP), 1f),
        )

        searchContainer.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.ic_gallery_close)
                setColorFilter(primaryTextColor())
                setPadding(dp(13), dp(13), dp(13), dp(13))
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                isClickable = true
                isFocusable = true
                contentDescription = "Close search"
                tooltipText = "Close search"
                background = roundedSurface(Color.TRANSPARENT, 16)
                GalleryInteractionFeedback.applyBoundedRipple(
                    this,
                    primaryTextColor(),
                    16,
                )
                setOnClickListener { closeSearch() }
            },
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ),
        )

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
            addView(
                searchContainer,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }
    }

    private fun buildAccessPanel(): View {
        accessPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(dp(12), dp(8), dp(6), dp(8))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.RAISED,
                GalleryGlazeContract.SHAPE_CONTAINER_DP,
            )
        }

        status = TextView(this).apply {
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setLineSpacing(0f, 1.06f)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        accessPanel.addView(
            status,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            },
        )

        action = TextView(this).apply {
            minHeight = dp(GalleryGlazeContract.GENERAL_TARGET_DP)
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
            setTextColor(accentColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedSurface(withAlpha(accentColor(), 0.12f), 15)
            isClickable = true
            isFocusable = true
            setAccessActionPresentation(
                this,
                label = "Choose media",
                iconResource = R.drawable.ic_gallery_nav_photos,
            )
            GalleryInteractionFeedback.applyBoundedRipple(
                this,
                accentColor(),
                15,
            )
        }
        accessPanel.addView(action)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
            addView(
                accessPanel,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }
    }

    private fun setAccessActionPresentation(
        control: TextView,
        label: String,
        iconResource: Int,
    ) {
        control.text = label
        control.setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0)
        control.compoundDrawableTintList = ColorStateList.valueOf(accentColor())
        control.compoundDrawablePadding = dp(6)
        control.contentDescription = "Gallery media access action. $label"
        control.tooltipText = label
    }

    private fun buildNavigationCapsule(): LinearLayout = bottomCapsuleSurface().apply {
        val navigationMode = currentUserSettings().navigationDisplayMode
        GalleryDestination.entries.forEach { item ->
            val label = navigationLabel(item)
            val selected = destination == item
            val view = TextView(this@GalleryActivity).apply {
                GalleryNavigationStyling.apply(
                    activity = this@GalleryActivity,
                    item = this,
                    label = label,
                    iconRes = navigationIcon(item),
                    mode = navigationMode,
                    selected = selected,
                    foreground = if (selected) accentColor() else primaryTextColor(),
                )
                isSelected = selected
                contentDescription = "$label${if (selected) ", selected" else ""}"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    stateDescription = if (selected) "Selected" else null
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (item == GalleryDestination.TRASH) {
                        clearSelection(render = false)
                        openAlbumId = null
                        showingFavorites = false
                        searchQuery = ""
                        if (::searchField.isInitialized) searchField.setText("")
                        closeSearch(clearQuery = false)
                        startActivity(Intent(this@GalleryActivity, RecycleBinActivity::class.java))
                        return@setOnClickListener
                    }
                    if (destination == item && openAlbumId == null && !showingFavorites) {
                        libraryScroll.smoothScrollTo(0, 0)
                        return@setOnClickListener
                    }
                    clearSelection(render = false)
                    destination = item
                    openAlbumId = null
                    showingFavorites = false
                    searchQuery = ""
                    if (::searchField.isInitialized) searchField.setText("")
                    closeSearch(clearQuery = false)
                    renderCurrentDestination()
                }
            }
            navigationItems[item] = view
            addView(
                view,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
            )
        }
    }

    private fun navigationLabel(item: GalleryDestination): String = when (item) {
        GalleryDestination.PHOTOS -> "Photos"
        GalleryDestination.ALBUMS -> "Albums"
        GalleryDestination.VIDEOS -> "Videos"
        GalleryDestination.TRASH -> "Trash"
        GalleryDestination.SETTINGS -> "Settings"
    }

    private fun navigationIcon(item: GalleryDestination): Int = when (item) {
        GalleryDestination.PHOTOS -> R.drawable.ic_gallery_nav_photos
        GalleryDestination.ALBUMS -> R.drawable.ic_gallery_nav_albums
        GalleryDestination.VIDEOS -> R.drawable.ic_gallery_nav_videos
        GalleryDestination.TRASH -> R.drawable.ic_gallery_nav_trash
        GalleryDestination.SETTINGS -> R.drawable.ic_gallery_nav_settings
    }

    private fun buildSelectionActionCapsule(): LinearLayout = bottomCapsuleSurface()

    private fun bottomCapsuleSurface(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(3), dp(3), dp(3), dp(3))
        background = GalleryGlazeSurfaces.drawable(
            context,
            GalleryGlazeSurfaces.Role.CHROME,
            GalleryGlazeContract.NAVIGATION_RADIUS_DP,
        )
        elevation = dp(GalleryGlazeContract.NAVIGATION_ELEVATION_DP).toFloat()
    }

    private fun renderNavigation() {
        if (!::navigationCapsule.isInitialized || !::selectionActionCapsule.isInitialized) return
        if (viewerOverlay != null) {
            navigationCapsule.visibility = View.GONE
            selectionActionCapsule.visibility = View.GONE
            return
        }
        if (inSelectionMode) {
            navigationCapsule.visibility = View.GONE
            selectionActionCapsule.visibility = View.VISIBLE
            renderSelectionActions()
            return
        }

        selectionActionCapsule.visibility = View.GONE
        navigationCapsule.visibility = View.VISIBLE
        val navigationMode = currentUserSettings().navigationDisplayMode
        GalleryDestination.entries.forEach { item ->
            val view = navigationItems[item] ?: return@forEach
            val selected = destination == item
            val label = navigationLabel(item)
            val foreground = if (selected) accentColor() else primaryTextColor()
            GalleryNavigationStyling.apply(
                activity = this,
                item = view,
                label = label,
                iconRes = navigationIcon(item),
                mode = navigationMode,
                selected = selected,
                foreground = foreground,
            )
            view.isSelected = selected
            view.contentDescription = "$label${if (selected) ", selected" else ""}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.stateDescription = if (selected) "Selected" else null
            }
        }
    }

    private fun renderSelectionActions() {
        selectionActionCapsule.removeAllViews()
        val selectedItems = currentSelectedItems()
        val favoriteAction = GalleryBulkActionPolicy.favoriteAction(
            selectionScopeItems,
            selectedUris,
            favoriteUris,
        )
        val favoriteLabel = if (favoriteAction == GalleryFavoriteBulkAction.REMOVE) "Unfavorite" else "Favorite"
        val deleteSupported = AndroidMediaMutationRequests.isSupported()
        val deletionDescription = when {
            !deleteSupported -> "Delete requires Android 11 or newer in this Development build"
            currentUserSettings().moveDeletedItemsToRecycleBin -> "Move selected media to the Android Recycle Bin"
            else -> "Permanently delete selected media after Android confirmation"
        }
        val moveSupported = AndroidMediaMoveRequests.isSupported()
        val copySupported = AndroidMediaCopyRequests.isSupported()
        val currentScope = visibleAuthorizedItems()
        val moveDestinations = if (selectedItems.isEmpty()) {
            emptyList()
        } else {
            GalleryMoveDestinationPolicy.existingDestinations(
                currentScope = currentScope,
                selectedContentUris = selectedUris,
            )
        }
        val newFolderParent = if (selectedItems.isEmpty()) null else {
            GalleryNewFolderMovePolicy.parentForSelection(
                currentScope = currentScope,
                selectedContentUris = selectedUris,
            )
        }
        val copyDestinations = if (selectedItems.isEmpty()) {
            emptyList()
        } else {
            GalleryCopyDestinationPolicy.existingDestinations(
                currentScope = currentScope,
                selectedContentUris = selectedUris,
            )
        }
        val newCopyFolderParent = if (selectedItems.isEmpty()) null else {
            GalleryNewFolderCopyPolicy.parentForSelection(
                currentScope = currentScope,
                selectedContentUris = selectedUris,
            )
        }
        val moveDescription = when {
            !moveSupported -> "Move requires Android 11 or newer in this Development build"
            moveDestinations.isNotEmpty() && newFolderParent != null ->
                "Move selected media to an existing authorized folder or create a new folder inside ${newFolderParent.displayName}"
            moveDestinations.isNotEmpty() -> "Move selected media to an existing authorized folder"
            newFolderParent != null -> "Create a new folder inside ${newFolderParent.displayName} and move selected media there"
            else -> "No eligible move destination is available for this selection"
        }
        val copyDescription = when {
            !copySupported -> "Copy requires Android 11 or newer in this Development build"
            selectedItems.size > AndroidMediaCopyRequests.MAX_COPY_ITEMS ->
                "Copy is limited to ${AndroidMediaCopyRequests.MAX_COPY_ITEMS} items at a time"
            copyDestinations.isNotEmpty() && newCopyFolderParent != null ->
                "Copy selected media while preserving the originals, or create a new folder inside $newCopyFolderParent"
            copyDestinations.isNotEmpty() -> "Copy selected media to another authorized local folder"
            newCopyFolderParent != null -> "Create a new folder inside $newCopyFolderParent and copy selected media there"
            else -> "No eligible copy destination is available for this selection"
        }

        val actions = listOf(
            selectionAction(
                R.drawable.ic_gallery_share,
                selectedItems.isNotEmpty(),
                "Share selected media",
            ) {
                shareSelectedItems()
            },
            selectionAction(
                if (favoriteAction == GalleryFavoriteBulkAction.REMOVE) {
                    R.drawable.ic_gallery_favorite
                } else {
                    R.drawable.ic_gallery_favorite_outline
                },
                selectedItems.isNotEmpty(),
                "$favoriteLabel selected media",
                selected = favoriteAction == GalleryFavoriteBulkAction.REMOVE,
            ) {
                applySelectedFavoriteAction()
            },
            selectionAction(
                R.drawable.ic_gallery_move,
                selectedItems.isNotEmpty() &&
                    moveSupported &&
                    (moveDestinations.isNotEmpty() || newFolderParent != null) &&
                    pendingMediaMove == null &&
                    !mediaCopyExecutionInProgress,
                moveDescription,
            ) {
                showMoveDestinationDialog()
            },
            selectionAction(
                R.drawable.ic_gallery_copy,
                selectedItems.isNotEmpty() &&
                    selectedItems.size <= AndroidMediaCopyRequests.MAX_COPY_ITEMS &&
                    copySupported &&
                    (copyDestinations.isNotEmpty() || newCopyFolderParent != null) &&
                    !mediaCopyExecutionInProgress &&
                    pendingMediaMove == null &&
                    pendingMediaMutation == null,
                copyDescription,
            ) {
                showCopyDestinationDialog()
            },
            selectionAction(
                R.drawable.ic_gallery_delete,
                selectedItems.isNotEmpty() && deleteSupported && !mediaCopyExecutionInProgress,
                deletionDescription,
                destructive = true,
            ) {
                requestMediaDeletion(selectedItems)
            },
            selectionAction(
                R.drawable.ic_gallery_more,
                currentScope.isNotEmpty(),
                "More selection actions",
            ) {},
        )
        actions.lastOrNull()?.let { moreAction ->
            moreAction.setOnClickListener {
                PopupMenu(this, moreAction).apply {
                    val menuState = GallerySelectionMenuPolicy.state(
                        visibleContentUris = currentScope.map { it.contentUri },
                        selectedContentUris = selectedUris,
                    )
                    menu.add(0, 1, 0, menuState.primaryActionLabel)
                    if (menuState.showDetails) {
                        menu.add(0, 2, 1, "Details")
                    }
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            1 -> {
                                if (menuState.primaryActionLabel == "Clear selection") {
                                    clearSelection()
                                } else {
                                    selectedUris.clear()
                                    selectedUris.addAll(
                                        GallerySelectionPolicy.selectAll(currentScope),
                                    )
                                    refreshRenderedSelectionState()
                                    updateHeader()
                                    renderNavigation()
                                    announceSelectionCount()
                                }
                                true
                            }
                            2 -> {
                                selectedItems.singleOrNull()?.let(::showItemDetails)
                                true
                            }
                            else -> false
                        }
                    }
                    show()
                }
            }
        }
        actions.forEachIndexed { index, view ->
            selectionActionCapsule.addView(
                view,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (index > 0) marginStart = dp(2)
                },
            )
        }
    }

    private fun selectionAction(
        iconResource: Int,
        enabled: Boolean,
        description: String,
        selected: Boolean = false,
        destructive: Boolean = false,
        onClick: () -> Unit,
    ): TextView = TextView(this).apply {
        text = ""
        gravity = Gravity.CENTER
        minHeight = dp(GalleryGlazeContract.GENERAL_TARGET_DP)
        minWidth = dp(GalleryGlazeContract.GENERAL_TARGET_DP)
        setPadding(dp(11), dp(11), dp(11), dp(11))
        val foreground = when {
            !enabled -> secondaryTextColor()
            destructive -> 0xffc62828.toInt()
            selected -> accentColor()
            else -> accentColor()
        }
        setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0)
        compoundDrawableTintList = ColorStateList.valueOf(foreground)
        background = roundedSurface(
            when {
                !enabled -> Color.TRANSPARENT
                destructive -> withAlpha(0xffc62828.toInt(), 0.11f)
                selected -> withAlpha(accentColor(), 0.18f)
                else -> withAlpha(accentColor(), 0.10f)
            },
            18,
        )
        isEnabled = enabled
        isClickable = enabled
        isFocusable = enabled
        isSelected = selected
        alpha = if (enabled) 1f else 0.42f
        contentDescription = description
        tooltipText = description
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            stateDescription = when {
                !enabled -> "Unavailable"
                destructive -> "Destructive action"
                selected -> "Selected"
                else -> null
            }
        }
        GalleryInteractionFeedback.applyBoundedRipple(this, foreground, 18)
        if (enabled) setOnClickListener { onClick() }
    }

    private fun updateHeader() {
        if (!::headerTitle.isInitialized) return

        if (inSelectionMode) {
            brandMark.visibility = View.GONE
            videoFilterStripView?.visibility = View.GONE
            headerTitle.text = if (selectedUris.size == 1) "1 selected" else "${selectedUris.size} selected"
            headerSubtitle.text = if (dragSelectionSession != null) {
                "Drag across photos and videos to select quickly"
            } else {
                "Tap items to add or remove"
            }
            backControl.visibility = View.VISIBLE
            backControl.contentDescription = "Exit selection"
            sortControl.visibility = View.GONE
            searchControl.visibility = View.GONE
            return
        }

        val visibleItems = visibleAuthorizedItems()
        val hasMediaAccess = GalleryMediaAccessPolicy.canRead(currentMediaAccessScope())

        val collectionItems = when {
            destination == GalleryDestination.ALBUMS && showingFavorites ->
                visibleItems.filter { it.contentUri in favoriteUris }
            destination == GalleryDestination.ALBUMS && openAlbumId != null ->
                visibleItems.filter { it.albumId == openAlbumId }
            else -> emptyList()
        }

        val title = when {
            destination == GalleryDestination.ALBUMS && showingFavorites -> "Favorites"
            destination == GalleryDestination.ALBUMS && openAlbumId != null ->
                visibleItems.firstOrNull { it.albumId == openAlbumId }?.albumName
                    ?: authorizedItems.firstOrNull { it.albumId == openAlbumId }?.albumName
                    ?: "Album"
            destination == GalleryDestination.PHOTOS -> "Photos"
            destination == GalleryDestination.ALBUMS -> "Albums"
            destination == GalleryDestination.VIDEOS -> "Videos"
            destination == GalleryDestination.TRASH -> "Trash"
            destination == GalleryDestination.SETTINGS -> "Settings"
            else -> "Gallery"
        }

        val baseSubtitle = when {
            destination == GalleryDestination.SETTINGS -> "Local Gallery preferences"
            destination == GalleryDestination.TRASH -> "Recently deleted media"
            destination == GalleryDestination.ALBUMS && (showingFavorites || openAlbumId != null) ->
                itemCountLabel(collectionItems.size)
            destination == GalleryDestination.PHOTOS ->
                itemCountLabel(visibleItems.count { it.mimeType.startsWith("image/") })
            destination == GalleryDestination.VIDEOS ->
                videoCountLabel(visibleItems.count { it.mimeType.startsWith("video/") })
            destination == GalleryDestination.ALBUMS -> {
                val albumCatalog = visibleItems.buildAlbumCatalog()
                val hasFavorites =
                    favoriteUris.any { uri -> visibleItems.any { it.contentUri == uri } }
                val collectionCount = albumCatalog.size + if (hasFavorites) 1 else 0
                val albumsLabel =
                    if (collectionCount == 1) "1 album" else "$collectionCount albums"
                val hasSmartCollections =
                    hasFavorites ||
                        visibleItems.any { it.mimeType.startsWith("video/") } ||
                        albumCatalog.any { album ->
                            GalleryAlbumQuickAccessPolicy.priority(album.displayName, isFavorites = false) != null
                        }
                if (hasSmartCollections) albumsLabel + " · Smart collections" else albumsLabel
            }
            else -> ""
        }

        headerTitle.text = title
        val presentationOrderLabel = if (destination == GalleryDestination.VIDEOS) {
            GalleryVideoPresentationPolicy.filterAndOrderLabel(videoFilter, selectedSort)
        } else {
            sortOrderLabel()
        }
        headerSubtitle.text = when {
            destination == GalleryDestination.SETTINGS -> baseSubtitle
            !hasMediaAccess -> "Media access required"
            visibleItems.isEmpty() -> baseSubtitle
            else -> baseSubtitle + " · " + presentationOrderLabel
        }
        val showBack =
            destination == GalleryDestination.ALBUMS && (openAlbumId != null || showingFavorites)
        backControl.visibility = if (showBack) View.VISIBLE else View.GONE
        brandMark.visibility = if (showBack) View.GONE else View.VISIBLE
        backControl.contentDescription = "Back to Albums"
        backControl.tooltipText = "Back to Albums"
        val sortLabel = sortOrderLabel()
        sortControl.setImageResource(sortIconResource())
        sortControl.contentDescription = "Sort order: $sortLabel. Double tap to change."
        sortControl.tooltipText = "Sort: $sortLabel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            sortControl.stateDescription = sortLabel
        }
        searchField.hint = when {
            destination == GalleryDestination.PHOTOS -> "Search photos"
            destination == GalleryDestination.VIDEOS -> "Search videos"
            destination == GalleryDestination.ALBUMS && showingFavorites -> "Search Favorites"
            destination == GalleryDestination.ALBUMS && openAlbumId != null -> "Search this album"
            destination == GalleryDestination.ALBUMS -> "Search albums and favorites"
            else -> "Search Gallery"
        }
        val showMediaControls =
            destination != GalleryDestination.SETTINGS &&
                destination != GalleryDestination.TRASH &&
                visibleItems.isNotEmpty()
        sortControl.visibility = if (showMediaControls) View.VISIBLE else View.GONE
        searchControl.visibility =
            if (showMediaControls && searchContainer.visibility != View.VISIBLE) View.VISIBLE else View.GONE
        videoFilterStripView?.visibility =
            if (destination == GalleryDestination.VIDEOS) View.VISIBLE else View.GONE
    }

    private fun registerMediaStoreObserver() {
        if (mediaStoreObserverRegistered) return
        val mediaUris = listOf(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        )
        var registered = false
        mediaUris.forEach { uri ->
            val success = runCatching {
                contentResolver.registerContentObserver(uri, true, mediaStoreObserver)
            }.isSuccess
            registered = registered || success
        }
        mediaStoreObserverRegistered = registered
    }

    private fun unregisterMediaStoreObserver() {
        if (!mediaStoreObserverRegistered) return
        runCatching { contentResolver.unregisterContentObserver(mediaStoreObserver) }
        mediaStoreObserverRegistered = false
    }

    private fun scheduleObservedMediaRefresh() {
        mediaRefreshPending = true
        mediaRefreshHandler.removeCallbacks(mediaRefreshRunnable)
        mediaRefreshHandler.postDelayed(mediaRefreshRunnable, MEDIA_REFRESH_DEBOUNCE_MS)
    }

    private fun refreshObservedMediaIfReady() {
        if (!mediaRefreshPending) return
        if (
            GalleryLiveRefreshPolicy.shouldDefer(
                viewerOpen = viewerOverlay != null,
                mediaMutationPending = pendingMediaMutation != null,
                mediaMovePending = pendingMediaMove != null,
                mediaMoveExecutionInProgress = mediaMoveExecutionInProgress,
                mediaCopyExecutionInProgress = mediaCopyExecutionInProgress,
            )
        ) {
            return
        }

        val accessScope = currentMediaAccessScope()
        if (!GalleryMediaAccessPolicy.canRead(accessScope)) {
            mediaRefreshPending = false
            renderPermissionState()
            return
        }

        mediaRefreshPending = false
        loadLocalLibrary(
            accessScope = accessScope,
            showLoading = false,
            preserveSelection = true,
        )
    }

    private fun renderPermissionState() {
        if (destination == GalleryDestination.SETTINGS) {
            clearSelection(render = false)
            accessPanel.visibility = View.GONE
            renderCurrentDestination()
            return
        }

        val accessScope = currentMediaAccessScope()
        if (!GalleryMediaAccessPolicy.canRead(accessScope)) {
            clearSelection(render = false)
            loadGeneration += 1
            authorizedItems = emptyList()
            openAlbumId = null
            showingFavorites = false
            accessPanel.visibility = View.VISIBLE
            status.text = "Choose which photos and videos Gallery can see. Your local media stays on this device."
            action.isEnabled = true
            action.alpha = 1f
            setAccessActionPresentation(
                action,
                label = "Choose media",
                iconResource = R.drawable.ic_gallery_nav_photos,
            )
            action.setOnClickListener { requestReadableMediaAccess() }
            library.removeAllViews()
            library.addView(
                emptyState(
                    title = "No media selected",
                    message = "Choose media above to show only the photos and videos you approve. You can change access later.",
                ),
            )
            updateHeader()
            renderNavigation()
            return
        }

        action.isEnabled = true
        action.alpha = 1f
        if (GalleryMediaAccessPolicy.isPartial(accessScope)) {
            accessPanel.visibility = View.VISIBLE
            status.text = "${accessScopeLabel(accessScope)}. Gallery only shows the media Android currently authorizes."
            setAccessActionPresentation(
                action,
                label = "Change access",
                iconResource = R.drawable.ic_gallery_nav_settings,
            )
            action.setOnClickListener { requestReadableMediaAccess() }
        } else {
            accessPanel.visibility = View.GONE
            setAccessActionPresentation(
                action,
                label = "Refresh",
                iconResource = R.drawable.ic_gallery_refresh,
            )
            action.setOnClickListener { loadLocalLibrary(accessScope) }
        }
        loadLocalLibrary(accessScope)
    }

    private fun loadLocalLibrary(
        accessScope: GalleryMediaAccessScope,
        showLoading: Boolean = true,
        preserveSelection: Boolean = false,
    ) {
        val generation = ++loadGeneration
        if (showLoading) mediaRefreshPending = false
        val previousScrollY = if (::libraryScroll.isInitialized) libraryScroll.scrollY else 0
        val previousItems = authorizedItems
        if (showLoading) {
            accessPanel.visibility = View.VISIBLE
            action.isEnabled = false
            action.alpha = 0.45f
            status.text = "${accessScopeLabel(accessScope)} · Loading…"
            library.removeAllViews()
            library.addView(messageRow("Loading your library", "Reading the local media Android has authorized."))
        }

        thread(name = "goreecloud-gallery-mediastore") {
            try {
                val result = AndroidMediaStoreReader(contentResolver).readLatest(GalleryGlazeContract.MAX_RENDERED_MEDIA_ROWS)
                runOnUiThread {
                    if (generation != loadGeneration) return@runOnUiThread
                    val libraryChanged = previousItems != result.items
                    if (preserveSelection) {
                        val availableUris = result.items.mapTo(hashSetOf()) { it.contentUri }
                        selectedUris.retainAll(availableUris)
                    } else {
                        clearSelection(render = false)
                    }
                    authorizedItems = result.items
                    action.isEnabled = true
                    action.alpha = 1f

                    if (GalleryMediaAccessPolicy.isPartial(accessScope)) {
                        accessPanel.visibility = View.VISIBLE
                        status.text = buildString {
                            append(accessScopeLabel(accessScope))
                            append(" · ${result.items.size} item")
                            if (result.items.size != 1) append('s')
                            if (result.rejectedRowCount > 0) append(" · ${result.rejectedRowCount} skipped")
                        }
                        setAccessActionPresentation(
                            action,
                            label = "Change access",
                            iconResource = R.drawable.ic_gallery_nav_settings,
                        )
                        action.setOnClickListener { requestReadableMediaAccess() }
                    } else {
                        accessPanel.visibility = View.GONE
                    }

                    if (!showLoading && destination == GalleryDestination.SETTINGS) {
                        updateHeader()
                        return@runOnUiThread
                    }
                    if (!showLoading && !libraryChanged) return@runOnUiThread
                    renderCurrentDestination(generation)
                    if (!showLoading && previousScrollY > 0 && ::libraryScroll.isInitialized) {
                        libraryScroll.post {
                            libraryScroll.scrollTo(0, previousScrollY)
                        }
                    }
                }
            } catch (_: SecurityException) {
                renderLoadFailure(generation, "Android denied the current local media read.")
            } catch (_: RuntimeException) {
                renderLoadFailure(generation, "The local media provider is unavailable right now.")
            }
        }
    }

    private fun renderLoadFailure(generation: Int, message: String) {
        runOnUiThread {
            if (generation != loadGeneration) return@runOnUiThread
            clearSelection(render = false)
            authorizedItems = emptyList()
            openAlbumId = null
            showingFavorites = false
            accessPanel.visibility = View.VISIBLE
            action.isEnabled = true
            action.alpha = 1f
            setAccessActionPresentation(
                action,
                label = "Try again",
                iconResource = R.drawable.ic_gallery_refresh,
            )
            action.setOnClickListener {
                val scope = currentMediaAccessScope()
                if (GalleryMediaAccessPolicy.canRead(scope)) loadLocalLibrary(scope) else requestReadableMediaAccess()
            }
            status.text = message
            library.removeAllViews()
            library.addView(
                messageRow(
                    "Library unavailable",
                    "Gallery is not treating a failed provider read as an empty library. Try again or review media access.",
                ),
            )
            updateHeader()
            renderNavigation()
        }
    }

    private fun renderCurrentDestination(generation: Int = loadGeneration) {
        if (generation != loadGeneration) return

        updateHeader()
        renderNavigation()
        renderedMediaTiles.clear()
        library.removeAllViews()

        if (destination == GalleryDestination.SETTINGS) {
            clearSelection(render = false)
            accessPanel.visibility = View.GONE
            if (searchContainer.visibility == View.VISIBLE) closeSearch(clearQuery = false)
            renderSettings()
            updateHeader()
            return
        }

        if (!GalleryMediaAccessPolicy.canRead(currentMediaAccessScope())) {
            renderPermissionState()
            return
        }
        val visibleItems = visibleAuthorizedItems()

        val setupPreferences = GallerySetupPreferences(this)
        val contextualHint = when (destination) {
            GalleryDestination.PHOTOS -> GallerySetupPreferences.HINT_PHOTOS to
                "Long-press a photo to begin multi-select. Search and sort stay local to the Android-authorized library."
            GalleryDestination.VIDEOS -> GallerySetupPreferences.HINT_VIDEOS to
                "Open a video for local playback, or long-press to begin multi-select."
            GalleryDestination.ALBUMS -> GallerySetupPreferences.HINT_ALBUMS to
                "Albums are built only from the media Android currently authorizes Gallery to read."
            GalleryDestination.TRASH,
            GalleryDestination.SETTINGS -> null
        }
        if (
            currentUserSettings().contextualHintsEnabled &&
            contextualHint != null &&
            !setupPreferences.isContextualHintDismissed(contextualHint.first)
        ) {
            library.addView(
                messageRow(
                    title = "Gallery hint",
                    message = contextualHint.second,
                    actionLabel = "Dismiss",
                    actionIcon = R.drawable.ic_gallery_close,
                    onAction = {
                        setupPreferences.dismissContextualHint(contextualHint.first)
                        renderCurrentDestination(generation)
                    },
                ),
            )
        }

        when (destination) {
            GalleryDestination.PHOTOS -> {
                val items = selectedSort.sort(
                    AuthorizedMediaSearch.search(
                        visibleItems.filter { it.mimeType.startsWith("image/") },
                        searchQuery,
                    ),
                )
                renderChronologicalLibrary(
                    items = items,
                    generation = generation,
                    emptyTitle = if (searchQuery.isBlank()) "No photos yet" else "No photo results",
                    emptyMessage = if (searchQuery.isBlank()) {
                        if (authorizedItems.any { it.mimeType.startsWith("image/") }) {
                            "No photos match the current folder or hidden-item visibility settings."
                        } else {
                            "No authorized photos are available in this library."
                        }
                    } else {
                        "No visible authorized photos match “$searchQuery”."
                    },
                )
            }
            GalleryDestination.VIDEOS -> renderVideos(
                generation = generation,
                sourceItems = visibleItems,
            )
            GalleryDestination.ALBUMS -> when {
                showingFavorites -> {
                    val items = selectedSort.sort(
                        AuthorizedMediaSearch.search(
                            visibleItems.filter { it.contentUri in favoriteUris },
                            searchQuery,
                        ),
                    )
                    renderChronologicalLibrary(
                        items = items,
                        generation = generation,
                        emptyTitle = if (searchQuery.isBlank()) "No favorites yet" else "No favorite results",
                        emptyMessage = if (searchQuery.isBlank()) {
                            "Mark a visible photo or video as a favorite from the viewer or selection mode to keep it here."
                        } else {
                            "No visible authorized favorites match “$searchQuery”."
                        },
                    )
                }
                openAlbumId != null -> {
                    val albumId = openAlbumId
                    val items = selectedSort.sort(
                        AuthorizedMediaSearch.search(
                            visibleItems.filter { it.albumId == albumId },
                            searchQuery,
                        ),
                    )
                    renderChronologicalLibrary(
                        items = items,
                        generation = generation,
                        emptyTitle = if (searchQuery.isBlank()) "Album is empty" else "No album results",
                        emptyMessage = if (searchQuery.isBlank()) {
                            "No visible authorized media is currently available in this album."
                        } else {
                            "No visible authorized items in this album match “$searchQuery”."
                        },
                    )
                }
                else -> renderAlbums(generation, visibleItems)
            }
            GalleryDestination.TRASH,
            GalleryDestination.SETTINGS -> Unit
        }
    }

    private fun renderChronologicalLibrary(
        items: List<MediaItem>,
        generation: Int,
        emptyTitle: String,
        emptyMessage: String,
    ) {
        syncSelectionScope(items)
        updateHeader()
        renderNavigation()

        if (items.isEmpty()) {
            library.addView(emptyState(emptyTitle, emptyMessage))
            return
        }

        val groupingMode = currentUserSettings().groupingMode
        when (groupingMode) {
            GalleryGroupingMode.NONE -> {
                renderMediaGrid(items, items, generation, library)
            }
            GalleryGroupingMode.DAY,
            GalleryGroupingMode.MONTH,
            GalleryGroupingMode.YEAR -> {
                val groups = linkedMapOf<String, MutableList<MediaItem>>()
                items.forEach { item ->
                    val label = when (groupingMode) {
                        GalleryGroupingMode.DAY -> dateGroupLabel(item)
                        GalleryGroupingMode.MONTH -> monthGroupLabel(item)
                        GalleryGroupingMode.YEAR -> yearGroupLabel(item)
                        GalleryGroupingMode.NONE -> error("handled above")
                    }
                    groups.getOrPut(label) { mutableListOf() }.add(item)
                }

                groups.forEach { (label, groupItems) ->
                    library.addView(timelineSectionHeader(label, groupItems.size))
                    renderMediaGrid(groupItems, items, generation, library)
                }
            }
        }
    }

    private fun renderVideos(generation: Int, sourceItems: List<MediaItem>) {
        videoFilterStripView = null
        val allVideos = selectedSort.sort(
            sourceItems.filter { it.mimeType.startsWith("video/") },
        )
        val availableFilters = GalleryVideoFilterPolicy.available(allVideos, favoriteUris)
        if (videoFilter !in availableFilters) {
            videoFilter = GalleryVideoFilter.ALL
        }

        val filteredVideos = GalleryVideoFilterPolicy.filter(
            items = allVideos,
            selected = videoFilter,
            favoriteContentUris = favoriteUris,
        )
        val items = AuthorizedMediaSearch.search(filteredVideos, searchQuery)

        syncSelectionScope(items)
        updateHeader()
        renderNavigation()

        if (allVideos.isNotEmpty()) {
            library.addView(videoFilterStrip(availableFilters))
        }

        if (items.isEmpty()) {
            val emptyTitle = if (searchQuery.isBlank()) {
                if (videoFilter == GalleryVideoFilter.ALL) "No videos yet"
                else "No " + videoFilter.label.lowercase() + " videos"
            } else {
                "No video results"
            }
            val emptyMessage = when {
                searchQuery.isNotBlank() ->
                    "No visible authorized videos match “$searchQuery” in " + videoFilter.label.lowercase() + "."
                allVideos.isEmpty() && authorizedItems.any { it.mimeType.startsWith("video/") } ->
                    "No videos match the current folder or hidden-item visibility settings."
                allVideos.isEmpty() ->
                    "No authorized videos are available in this library."
                else ->
                    "No currently authorized videos match the " + videoFilter.label.lowercase() + " filter."
            }
            library.addView(emptyState(emptyTitle, emptyMessage))
            return
        }

        renderVideoCards(items, generation)
    }

    private fun videoFilterStrip(filters: List<GalleryVideoFilter>): HorizontalScrollView =
        HorizontalScrollView(this).apply {
            videoFilterStripView = this
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(0, dp(4), 0, dp(8))

            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL

                    filters.forEachIndexed { index, filter ->
                        addView(
                            videoFilterChip(filter),
                            LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                            ).apply {
                                if (index > 0) marginStart = dp(8)
                            },
                        )
                    }
                },
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

    private fun videoFilterChip(filter: GalleryVideoFilter): TextView {
        val selected = filter == videoFilter
        return TextView(this).apply {
            text = filter.label
            gravity = Gravity.CENTER
            minWidth = dp(72)
            setPadding(dp(14), 0, dp(14), 0)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTypeface(typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
            val foregroundColor = if (selected) accentColor() else primaryTextColor()
            setTextColor(foregroundColor)
            setCompoundDrawablesWithIntrinsicBounds(videoFilterIcon(filter), 0, 0, 0)
            compoundDrawableTintList = ColorStateList.valueOf(foregroundColor)
            compoundDrawablePadding = dp(6)
            background = if (selected) {
                GalleryGlazeSurfaces.drawable(
                    context,
                    GalleryGlazeSurfaces.Role.CONTROL,
                    GalleryGlazeContract.SHAPE_CAPSULE_DP,
                )
            } else {
                GalleryGlazeSurfaces.drawable(
                    context,
                    GalleryGlazeSurfaces.Role.RAISED,
                    GalleryGlazeContract.SHAPE_CAPSULE_DP,
                )
            }
            isClickable = true
            isFocusable = true
            GalleryInteractionFeedback.applyBoundedRipple(
                this,
                foregroundColor,
                GalleryGlazeContract.SHAPE_CAPSULE_DP,
            )
            isSelected = selected
            tooltipText = filter.label + " videos"
            contentDescription = filter.label + " videos" + if (selected) ", selected" else ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                stateDescription = if (selected) "Selected" else null
            }
            setOnClickListener {
                if (videoFilter == filter) return@setOnClickListener
                clearSelection(render = false)
                videoFilter = filter
                renderCurrentDestination()
                announceForAccessibility(filter.label + " videos")
            }
        }
    }

    private fun renderVideoCards(items: List<MediaItem>, generation: Int) {
        val widthDp = resources.configuration.screenWidthDp
        val gutterPx = dp(GalleryGlazeContract.horizontalGutterDp(widthDp))
        val availableWidth = (
            resources.displayMetrics.widthPixels - (gutterPx * 2)
        ).coerceAtLeast(dp(240))
        val useFeaturedCard =
            GalleryGlazeContract.videoUsesFeaturedCard(widthDp) && items.size >= 3

        var firstGridIndex = 0
        if (useFeaturedCard) {
            val featured = items.first()
            val featuredWidth = availableWidth.coerceAtMost(
                dp(GalleryGlazeContract.MAX_FEATURED_VIDEO_WIDTH_DP),
            )
            library.addView(
                videoCard(
                    item = featured,
                    collectionItems = items,
                    collectionIndex = 0,
                    generation = generation,
                    thumbnailHeight = ((featuredWidth * 9f) / 16f).toInt().coerceAtLeast(dp(160)),
                    featured = true,
                ),
                LinearLayout.LayoutParams(
                    featuredWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                },
            )
            firstGridIndex = 1
        }

        if (firstGridIndex >= items.size) return

        val columns = GalleryGlazeContract.videoGridColumns(widthDp)
        val gaps = dp(VIDEO_CARD_GAP_DP) * (columns - 1)
        val cardWidth = ((availableWidth - gaps) / columns).coerceAtLeast(dp(136))
        val thumbnailHeight = ((cardWidth * 9f) / 16f).toInt().coerceAtLeast(dp(88))

        items.drop(firstGridIndex).chunked(columns).forEachIndexed { rowIndex, rowItems ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
            }
            rowItems.forEachIndexed { columnIndex, item ->
                val collectionIndex = items.indexOf(item)
                row.addView(
                    videoCard(
                        item = item,
                        collectionItems = items,
                        collectionIndex = collectionIndex,
                        generation = generation,
                        thumbnailHeight = thumbnailHeight,
                        featured = false,
                    ),
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (columnIndex > 0) marginStart = dp(VIDEO_CARD_GAP_DP)
                    },
                )
            }
            repeat(columns - rowItems.size) { spacerIndex ->
                row.addView(
                    Space(this),
                    LinearLayout.LayoutParams(0, 1, 1f).apply {
                        if (rowItems.isNotEmpty() || spacerIndex > 0) {
                            marginStart = dp(VIDEO_CARD_GAP_DP)
                        }
                    },
                )
            }
            library.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = dp(
                        if (rowIndex == 0 && useFeaturedCard) 12
                        else if (rowIndex == 0) 4
                        else VIDEO_CARD_GAP_DP,
                    )
                },
            )
        }
    }

    private fun videoCard(
        item: MediaItem,
        collectionItems: List<MediaItem>,
        collectionIndex: Int,
        generation: Int,
        thumbnailHeight: Int,
        featured: Boolean,
    ): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = GalleryGlazeSurfaces.drawable(
            context,
            GalleryGlazeSurfaces.Role.RAISED,
            GalleryGlazeContract.SHAPE_CONTAINER_DP,
        )
        clipToOutline = true
        elevation = dp(1).toFloat()
        setPadding(dp(2), dp(2), dp(2), dp(if (featured) 10 else 8))

        addView(
            mediaTile(item, collectionItems, collectionIndex, generation),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                thumbnailHeight,
            ),
        )
        addView(videoCardFooter(item, featured))
    }

    private fun videoCardFooter(item: MediaItem, featured: Boolean): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(9), dp(5), dp(1), 0)

            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(
                        TextView(context).apply {
                            text = mediaDisplayTitle(item)
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            setTextColor(primaryTextColor())
                            setTextSize(
                                TypedValue.COMPLEX_UNIT_SP,
                                if (featured) 16f else 13.5f,
                            )
                            setTypeface(typeface, Typeface.BOLD)
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        },
                    )
                    addView(
                        TextView(context).apply {
                            text = mediaDateLabel(item)
                            maxLines = 1
                            setTextColor(secondaryTextColor())
                            setTextSize(
                                TypedValue.COMPLEX_UNIT_SP,
                                if (featured) 12f else 11f,
                            )
                            setPadding(0, dp(1), 0, 0)
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        },
                    )
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )

            addView(
                cardOverflowButton("More actions for " + mediaDisplayTitle(item)) { anchor ->
                    showVideoOverflowMenu(anchor, item)
                },
                LinearLayout.LayoutParams(
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                ),
            )
        }

    private fun albumCardFooter(album: AlbumPresentation): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(2), 0)

            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(
                        TextView(context).apply {
                            text = album.name
                            setTextColor(primaryTextColor())
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                            setTypeface(typeface, Typeface.BOLD)
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        },
                    )
                    addView(
                        TextView(context).apply {
                            text = itemCountLabel(album.count)
                            setTextColor(secondaryTextColor())
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                            maxLines = 1
                            setPadding(0, dp(2), 0, 0)
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        },
                    )
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )

            addView(
                cardOverflowButton("More actions for " + album.name) { anchor ->
                    showAlbumOverflowMenu(anchor, album)
                },
                LinearLayout.LayoutParams(
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                ),
            )
        }

    private fun cardOverflowButton(
        description: String,
        onClick: (View) -> Unit,
    ): ImageView = ImageView(this).apply {
        setImageResource(R.drawable.ic_gallery_more)
        setColorFilter(secondaryTextColor())
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setPadding(dp(13), dp(13), dp(13), dp(13))
        background = roundedSurface(Color.TRANSPARENT, GalleryGlazeContract.SHAPE_CONTROL_DP)
        isClickable = true
        isFocusable = true
        contentDescription = description
        tooltipText = description
        GalleryInteractionFeedback.applyBoundedRipple(
            this,
            secondaryTextColor(),
            GalleryGlazeContract.SHAPE_CONTROL_DP,
        )
        setOnClickListener { onClick(this) }
    }

    private fun showVideoOverflowMenu(anchor: View, item: MediaItem) {
        val actions = GalleryCardOverflowPolicy.videoActions(item.contentUri in favoriteUris)
        lateinit var popup: PopupWindow
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.OVERLAY,
                GalleryGlazeContract.SHAPE_CONTAINER_DP,
            )
            clipToOutline = true
            elevation = dp(8).toFloat()
        }

        actions.forEach { action ->
            val iconResource = when (action) {
                GalleryCardOverflowAction.SHARE -> R.drawable.ic_gallery_share
                GalleryCardOverflowAction.ADD_FAVORITE -> R.drawable.ic_gallery_favorite_outline
                GalleryCardOverflowAction.REMOVE_FAVORITE -> R.drawable.ic_gallery_favorite
                GalleryCardOverflowAction.DETAILS -> R.drawable.ic_gallery_info
                else -> R.drawable.ic_gallery_more
            }
            panel.addView(
                videoOverflowActionRow(
                    iconResource = iconResource,
                    label = action.label,
                ) {
                    popup.dismiss()
                    when (action) {
                        GalleryCardOverflowAction.SHARE -> shareSingleItem(item)
                        GalleryCardOverflowAction.ADD_FAVORITE,
                        GalleryCardOverflowAction.REMOVE_FAVORITE -> {
                            toggleFavorite(item)
                            renderCurrentDestination()
                        }
                        GalleryCardOverflowAction.DETAILS -> showItemDetails(item)
                        GalleryCardOverflowAction.OPEN,
                        GalleryCardOverflowAction.PIN_TO_TOP,
                        GalleryCardOverflowAction.UNPIN_FROM_TOP,
                        GalleryCardOverflowAction.MOVE_EARLIER,
                        GalleryCardOverflowAction.MOVE_LATER -> Unit
                    }
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                ),
            )
        }

        val popupWidth = dp(216)
        val estimatedHeight = dp(actions.size * GalleryGlazeContract.GENERAL_TARGET_DP + 16)
        popup = PopupWindow(
            panel,
            popupWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            elevation = dp(8).toFloat()
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        }

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val popupX = (location[0] + anchor.width - popupWidth)
            .coerceIn(dp(8), (screenWidth - popupWidth - dp(8)).coerceAtLeast(dp(8)))
        val maximumY = (screenHeight - estimatedHeight - dp(12)).coerceAtLeast(dp(8))
        val popupY = (location[1] + anchor.height - estimatedHeight)
            .coerceIn(dp(8), maximumY)
        popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, popupX, popupY)
    }

    private fun videoOverflowActionRow(
        iconResource: Int,
        label: String,
        onClick: () -> Unit,
    ): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minHeight = dp(GalleryGlazeContract.GENERAL_TARGET_DP)
        setPadding(dp(10), 0, dp(12), 0)
        isClickable = true
        isFocusable = true
        contentDescription = label
        tooltipText = label
        GalleryInteractionFeedback.applyBoundedRipple(
            this,
            accentColor(),
            GalleryGlazeContract.SHAPE_CONTROL_DP,
        )
        addView(
            ImageView(context).apply {
                setImageResource(iconResource)
                setColorFilter(accentColor())
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                marginEnd = dp(12)
            },
        )
        addView(
            TextView(context).apply {
                text = label
                setTextColor(primaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        setOnClickListener { onClick() }
    }

    private fun showAlbumOverflowMenu(anchor: View, album: AlbumPresentation) {
        val albumId = album.id
        val settings = currentUserSettings()
        val isPinned = albumId != null && albumId in settings.pinnedAlbumIds
        val availability = if (albumId == null) {
            GalleryAlbumMoveAvailability(canMoveEarlier = false, canMoveLater = false)
        } else {
            GalleryAlbumOrderPolicy.availability(
                availableAlbumIds = currentAlbumBaseOrderIds(),
                pinnedAlbumIds = settings.pinnedAlbumIds,
                preferredAlbumOrderIds = settings.albumOrderIds,
                albumId = albumId,
            )
        }
        val actions = GalleryCardOverflowPolicy.albumActions(
            isPinned = isPinned,
            canPin = albumId != null,
            canMoveEarlier = availability.canMoveEarlier,
            canMoveLater = availability.canMoveLater,
        )
        val byId = actions.associateBy { action -> action.ordinal + 1 }
        PopupMenu(this, anchor).apply {
            actions.forEach { action ->
                menu.add(0, action.ordinal + 1, action.ordinal, action.label)
            }
            setOnMenuItemClickListener { menuItem ->
                when (byId[menuItem.itemId]) {
                    GalleryCardOverflowAction.OPEN -> openAlbumPresentation(album)
                    GalleryCardOverflowAction.PIN_TO_TOP -> setAlbumPinned(album, pinned = true)
                    GalleryCardOverflowAction.UNPIN_FROM_TOP -> setAlbumPinned(album, pinned = false)
                    GalleryCardOverflowAction.MOVE_EARLIER ->
                        moveAlbumPresentation(album, GalleryAlbumMoveDirection.EARLIER)
                    GalleryCardOverflowAction.MOVE_LATER ->
                        moveAlbumPresentation(album, GalleryAlbumMoveDirection.LATER)
                    GalleryCardOverflowAction.DETAILS -> showAlbumDetails(album)
                    GalleryCardOverflowAction.SHARE,
                    GalleryCardOverflowAction.ADD_FAVORITE,
                    GalleryCardOverflowAction.REMOVE_FAVORITE,
                    null -> return@setOnMenuItemClickListener false
                }
                true
            }
            show()
        }
    }

    private fun setAlbumPinned(album: AlbumPresentation, pinned: Boolean) {
        val albumId = album.id ?: return
        val availableAlbumIds = visibleAuthorizedItems().buildAlbumCatalog().map { it.id }.toSet()
        if (albumId !in availableAlbumIds) {
            Toast.makeText(this, "This album is no longer available.", Toast.LENGTH_SHORT).show()
            return
        }

        val updated = GalleryAlbumPinPolicy.toggled(
            pinnedAlbumIds = currentUserSettings().pinnedAlbumIds,
            albumId = albumId,
            pinned = pinned,
        )
        galleryPreferences().edit()
            .putStringSet(PINNED_ALBUM_IDS_KEY, updated)
            .apply()
        Toast.makeText(
            this,
            if (pinned) "${album.name} pinned to top" else "${album.name} unpinned",
            Toast.LENGTH_SHORT,
        ).show()
        renderCurrentDestination()
    }

    private fun currentAlbumBaseOrderIds(): List<String> =
        visibleAuthorizedItems()
            .buildAlbumCatalog()
            .let { albums ->
                if (selectedSort == MediaSortOrder.NEWEST) albums else albums.sortedBy { it.newestAt }
            }
            .map { it.id }

    private fun moveAlbumPresentation(
        album: AlbumPresentation,
        direction: GalleryAlbumMoveDirection,
    ) {
        val albumId = album.id ?: return
        val settings = currentUserSettings()
        val availableAlbumIds = currentAlbumBaseOrderIds()
        if (albumId !in availableAlbumIds) {
            Toast.makeText(this, "This album is no longer available.", Toast.LENGTH_SHORT).show()
            return
        }

        val updated = GalleryAlbumOrderPolicy.moved(
            availableAlbumIds = availableAlbumIds,
            pinnedAlbumIds = settings.pinnedAlbumIds,
            preferredAlbumOrderIds = settings.albumOrderIds,
            albumId = albumId,
            direction = direction,
        )
        galleryPreferences().edit()
            .putString(ALBUM_ORDER_IDS_KEY, stringListJson(updated).toString())
            .apply()
        Toast.makeText(
            this,
            if (direction == GalleryAlbumMoveDirection.EARLIER) {
                "${album.name} moved earlier"
            } else {
                "${album.name} moved later"
            },
            Toast.LENGTH_SHORT,
        ).show()
        renderCurrentDestination()
    }

    private fun renderAlbums(generation: Int, sourceItems: List<MediaItem>) {
        clearSelection(render = false)
        selectionScopeItems = emptyList()
        val query = searchQuery.trim().lowercase()
        val searchedItems = AuthorizedMediaSearch.search(sourceItems, searchQuery)
        val sortedCatalog = searchedItems.buildAlbumCatalog()
            .let { albums ->
                if (selectedSort == MediaSortOrder.NEWEST) albums else albums.sortedBy { it.newestAt }
            }
        val catalogById = sortedCatalog.associateBy { it.id }
        val settings = currentUserSettings()
        val catalog = GalleryAlbumOrderPolicy.orderedIds(
            availableAlbumIds = sortedCatalog.map { it.id },
            pinnedAlbumIds = settings.pinnedAlbumIds,
            preferredAlbumOrderIds = settings.albumOrderIds,
        ).mapNotNull(catalogById::get)

        val allFavoriteItems = sourceItems.filter { it.contentUri in favoriteUris }
        val favoriteItems = if (query.isBlank() || "favorites".contains(query)) {
            selectedSort.sort(allFavoriteItems)
        } else {
            selectedSort.sort(AuthorizedMediaSearch.search(allFavoriteItems, searchQuery))
        }

        val showFavoritesTile = favoriteItems.isNotEmpty()

        if (catalog.isEmpty() && !showFavoritesTile) {
            library.addView(
                emptyState(
                    if (searchQuery.isBlank()) "No albums yet" else "No album results",
                    if (searchQuery.isBlank()) {
                        "No visible authorized albums are available with the current Gallery settings."
                    } else {
                        "No visible authorized albums match “$searchQuery”."
                    },
                ),
            )
            updateHeader()
            renderNavigation()
            return
        }

        if (catalog.isNotEmpty() || showFavoritesTile) {
            val tiles = mutableListOf<AlbumPresentation>()
            if (showFavoritesTile) {
                tiles += AlbumPresentation(
                    id = null,
                    name = "Favorites",
                    count = favoriteItems.size,
                    cover = favoriteItems.first(),
                    isFavorites = true,
                )
            }
            catalog.forEach { album ->
                val cover = searchedItems.firstOrNull { it.id == album.coverItemId } ?: return@forEach
                tiles += AlbumPresentation(
                    id = album.id,
                    name = album.displayName,
                    count = album.itemCount,
                    cover = cover,
                    isFavorites = false,
                )
            }

            val quickAccessTiles = tiles
                .mapNotNull { album ->
                    GalleryAlbumQuickAccessPolicy.priority(album.name, album.isFavorites)
                        ?.let { priority -> priority to album }
                }
                .sortedBy { it.first }
                .map { it.second }
                .take(ALBUM_QUICK_ACCESS_LIMIT)

            val videoCount = searchedItems.count { it.mimeType.startsWith("video/") }
            if (quickAccessTiles.isNotEmpty() || videoCount > 0) {
                renderAlbumQuickAccess(quickAccessTiles, videoCount)
            }

            library.addView(sectionHeader("Collections"))
            renderAlbumGrid(tiles, generation)
        }
        updateHeader()
        renderNavigation()
    }

    private fun renderAlbumQuickAccess(
        albums: List<AlbumPresentation>,
        videoCount: Int,
    ) {
        library.addView(
            HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                isFillViewport = false
                overScrollMode = View.OVER_SCROLL_NEVER
                setPadding(0, dp(4), 0, dp(12))

                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL

                        albums.forEachIndexed { index, album ->
                            addView(
                                albumQuickAccessChip(
                                    label = album.name,
                                    count = album.count,
                                    iconRes = albumQuickAccessIcon(album),
                                ) { openAlbumPresentation(album) },
                                LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                                ).apply {
                                    if (index > 0) marginStart = dp(8)
                                },
                            )
                        }

                        if (videoCount > 0) {
                            addView(
                                albumQuickAccessChip(
                                    label = "Videos",
                                    count = videoCount,
                                    iconRes = R.drawable.ic_gallery_nav_videos,
                                ) {
                                    clearSelection(render = false)
                                    destination = GalleryDestination.VIDEOS
                                    openAlbumId = null
                                    showingFavorites = false
                                    searchQuery = ""
                                    searchField.setText("")
                                    closeSearch(clearQuery = false)
                                    renderCurrentDestination()
                                },
                                LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                                ).apply {
                                    if (albums.isNotEmpty()) marginStart = dp(8)
                                },
                            )
                        }
                    },
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            },
        )
    }

    private fun videoFilterIcon(filter: GalleryVideoFilter): Int = when (filter) {
        GalleryVideoFilter.ALL -> R.drawable.ic_gallery_nav_videos
        GalleryVideoFilter.SCREEN_RECORDINGS -> R.drawable.ic_gallery_screen_recording
        GalleryVideoFilter.CAMERA -> R.drawable.ic_gallery_camera
        GalleryVideoFilter.FAVORITES -> R.drawable.ic_gallery_favorite
    }

    private fun recognizedAlbumIcon(album: AlbumPresentation): Int? =
        when (GalleryAlbumQuickAccessPolicy.kind(album.name, album.isFavorites)) {
            GalleryAlbumQuickAccessKind.FAVORITES -> R.drawable.ic_gallery_favorite
            GalleryAlbumQuickAccessKind.CAMERA -> R.drawable.ic_gallery_camera
            GalleryAlbumQuickAccessKind.SCREENSHOTS -> R.drawable.ic_gallery_nav_photos
            GalleryAlbumQuickAccessKind.DOWNLOADS -> R.drawable.ic_gallery_download
            GalleryAlbumQuickAccessKind.SCREEN_RECORDINGS -> R.drawable.ic_gallery_screen_recording
            null -> null
        }

    private fun albumQuickAccessIcon(album: AlbumPresentation): Int =
        recognizedAlbumIcon(album) ?: R.drawable.ic_gallery_nav_albums

    private fun albumBadgeIcon(album: AlbumPresentation): Int =
        recognizedAlbumIcon(album)
            ?: if (album.cover.mimeType.startsWith("video/")) {
                R.drawable.ic_gallery_nav_videos
            } else {
                R.drawable.ic_gallery_nav_albums
            }

    private fun albumQuickAccessChip(
        label: String,
        count: Int,
        iconRes: Int,
        onClick: () -> Unit,
    ): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        minWidth = dp(76)
        setPadding(dp(14), 0, dp(14), 0)
        setTextColor(primaryTextColor())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        setTypeface(typeface, Typeface.BOLD)
        background = GalleryGlazeSurfaces.drawable(
            context,
            GalleryGlazeSurfaces.Role.RAISED,
            GalleryGlazeContract.SHAPE_CAPSULE_DP,
        )
        setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
        compoundDrawableTintList = ColorStateList.valueOf(primaryTextColor())
        compoundDrawablePadding = dp(6)
        isClickable = true
        isFocusable = true
        contentDescription = "$label, ${itemCountLabel(count)}"
        tooltipText = label
        GalleryInteractionFeedback.applyBoundedRipple(
            this,
            primaryTextColor(),
            GalleryGlazeContract.SHAPE_CAPSULE_DP,
        )
        setOnClickListener { onClick() }
    }

    private fun openAlbumPresentation(album: AlbumPresentation) {
        clearSelection(render = false)
        searchQuery = ""
        searchField.setText("")
        closeSearch(clearQuery = false)
        if (album.isFavorites) {
            showingFavorites = true
            openAlbumId = null
        } else {
            showingFavorites = false
            openAlbumId = album.id
        }
        renderCurrentDestination()
    }

    private fun renderAlbumGrid(albums: List<AlbumPresentation>, generation: Int) {
        val columns = GalleryGlazeContract.albumGridColumns(resources.configuration.screenWidthDp)
        val gutterPx = dp(GalleryGlazeContract.horizontalGutterDp(resources.configuration.screenWidthDp))
        val gaps = dp(ALBUM_GAP_DP) * (columns - 1)
        val tileWidth = (
            (resources.displayMetrics.widthPixels - (gutterPx * 2) - gaps) / columns
        ).coerceAtLeast(dp(GalleryGlazeContract.MIN_ALBUM_TILE_DP))

        albums.chunked(columns).forEachIndexed { rowIndex, rowAlbums ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
            }

            rowAlbums.forEachIndexed { columnIndex, album ->
                row.addView(
                    albumTile(album, generation, tileWidth),
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (columnIndex > 0) marginStart = dp(ALBUM_GAP_DP)
                    },
                )
            }
            repeat(columns - rowAlbums.size) { spacerIndex ->
                row.addView(
                    Space(this),
                    LinearLayout.LayoutParams(0, 1, 1f).apply {
                        if (rowAlbums.isNotEmpty() || spacerIndex > 0) marginStart = dp(ALBUM_GAP_DP)
                    },
                )
            }
            library.addView(
                row,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    if (rowIndex > 0) topMargin = dp(16)
                },
            )
        }
    }

    private fun albumTile(album: AlbumPresentation, generation: Int, tileWidth: Int): LinearLayout {
        val cornerDp = thumbnailCornerDp(ALBUM_CORNER_DP)
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = roundedSurface(withAlpha(primaryTextColor(), 0.08f), cornerDp)
            clipToOutline = true
            tag = thumbnailCacheKey(ALBUM_THUMBNAIL_NAMESPACE, album.cover.contentUri)
        }
        loadLocalThumbnail(album.cover, image, generation, ALBUM_THUMBNAIL_DP, ALBUM_THUMBNAIL_NAMESPACE)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.RAISED,
                GalleryGlazeContract.SHAPE_CONTAINER_DP,
            )
            clipToOutline = true
            elevation = dp(1).toFloat()
            setPadding(dp(2), dp(2), dp(2), dp(8))
            isClickable = true
            isFocusable = true
            contentDescription = "${album.name}, ${itemCountLabel(album.count)}"
            GalleryInteractionFeedback.applyBoundedRipple(
                this,
                primaryTextColor(),
                GalleryGlazeContract.SHAPE_CONTAINER_DP,
            )
            setOnClickListener { openAlbumPresentation(album) }
            addView(
                FrameLayout(context).apply {
                    addView(
                        image,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                    addView(
                        ImageView(context).apply {
                            setImageResource(albumBadgeIcon(album))
                            imageTintList = ColorStateList.valueOf(primaryTextColor())
                            scaleType = ImageView.ScaleType.CENTER_INSIDE
                            setPadding(dp(10), dp(10), dp(10), dp(10))
                            background = GalleryGlazeSurfaces.drawable(
                                context,
                                GalleryGlazeSurfaces.Role.CHROME,
                                ALBUM_BADGE_DP / 2,
                            )
                            elevation = dp(2).toFloat()
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        },
                        FrameLayout.LayoutParams(
                            dp(ALBUM_BADGE_DP),
                            dp(ALBUM_BADGE_DP),
                        ).apply {
                            gravity = Gravity.BOTTOM or Gravity.START
                            marginStart = dp(10)
                            bottomMargin = dp(10)
                        },
                    )
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ((tileWidth * ALBUM_COVER_ASPECT_HEIGHT).toInt()).coerceAtLeast(dp(92)),
                ),
            )
            addView(albumCardFooter(album))
        }
    }

    private fun renderMediaGrid(
        groupItems: List<MediaItem>,
        collectionItems: List<MediaItem>,
        generation: Int,
        parent: LinearLayout,
    ) {
        val columns = currentUserSettings().viewDensity.mediaGridColumnsForGroup(
            resources.configuration.screenWidthDp,
            groupItems.size,
        )
        val gutterPx = dp(GalleryGlazeContract.horizontalGutterDp(resources.configuration.screenWidthDp))
        val gaps = dp(GRID_GAP_DP) * (columns - 1)
        val tileSize = (
            (resources.displayMetrics.widthPixels - (gutterPx * 2) - gaps) / columns
        ).coerceAtLeast(dp(GalleryGlazeContract.MIN_GRID_TILE_DP))
        val collectionIndexes = collectionItems.withIndex().associate { it.value.contentUri to it.index }

        groupItems.chunked(columns).forEachIndexed { rowIndex, rowItems ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
            }
            rowItems.forEachIndexed { columnIndex, item ->
                val collectionIndex = collectionIndexes[item.contentUri] ?: return@forEachIndexed
                row.addView(
                    mediaTile(item, collectionItems, collectionIndex, generation),
                    LinearLayout.LayoutParams(0, tileSize, 1f).apply {
                        if (columnIndex > 0) marginStart = dp(GRID_GAP_DP)
                    },
                )
            }
            repeat(columns - rowItems.size) { spacerIndex ->
                row.addView(
                    Space(this),
                    LinearLayout.LayoutParams(0, tileSize, 1f).apply {
                        if (rowItems.isNotEmpty() || spacerIndex > 0) marginStart = dp(GRID_GAP_DP)
                    },
                )
            }
            parent.addView(
                row,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tileSize).apply {
                    if (rowIndex > 0) topMargin = dp(GRID_GAP_DP)
                },
            )
        }
    }

    private fun mediaTile(
        item: MediaItem,
        items: List<MediaItem>,
        index: Int,
        generation: Int,
    ): FrameLayout {
        val placeholder = withAlpha(primaryTextColor(), 0.08f)
        val cacheKey = thumbnailCacheKey(GRID_THUMBNAIL_NAMESPACE, item.contentUri)
        val cornerDp = thumbnailCornerDp(GRID_CORNER_DP)
        val selected = item.contentUri in selectedUris
        val thumbnail = ImageView(this).apply {
            tag = cacheKey
            contentDescription = null
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = roundedSurface(placeholder, cornerDp)
        }
        loadLocalThumbnail(item, thumbnail, generation, GRID_THUMBNAIL_DP, GRID_THUMBNAIL_NAMESPACE)

        return FrameLayout(this).apply {
            background = roundedSurface(placeholder, cornerDp)
            clipToOutline = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            isClickable = true
            isLongClickable = true
            isFocusable = true
            GalleryInteractionFeedback.applyBoundedRipple(this, Color.WHITE, cornerDp)
            isSelected = selected
            contentDescription = mediaTileContentDescription(item, selected)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                stateDescription = if (selected) "Selected" else null
            }
            setOnClickListener {
                if (inSelectionMode) toggleSelection(item, items)
                else showAuthorizedViewer(items, index, generation)
            }
            setOnLongClickListener {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                beginDragSelection(item, items)
            }
            setOnTouchListener { _, event ->
                if (dragSelectionSession == null) return@setOnTouchListener false
                when (event.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        updateDragSelectionPointer(event.rawX, event.rawY)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        finishDragSelection()
                        true
                    }
                    else -> true
                }
            }
            addView(
                thumbnail,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            addView(
                View(context).apply {
                    tag = SELECTION_OVERLAY_TAG
                    visibility = if (selected) View.VISIBLE else View.GONE
                    background = selectedTileSurface(cornerDp)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            if (item.mimeType.startsWith("video/")) {
                addView(
                    ImageView(context).apply {
                        tag = VIDEO_PLAY_TAG
                        visibility = if (inSelectionMode) View.GONE else View.VISIBLE
                        setImageResource(R.drawable.ic_gallery_play)
                        setColorFilter(Color.WHITE)
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        setPadding(dp(14), dp(14), dp(14), dp(14))
                        background = roundedSurface(0xb3000000.toInt(), 24)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    },
                    FrameLayout.LayoutParams(dp(48), dp(48)).apply {
                        gravity = Gravity.CENTER
                    },
                )
                item.durationMillis?.let { durationMillis ->
                    addView(
                        TextView(context).apply {
                            text = formatDuration(durationMillis)
                            setTextColor(Color.WHITE)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                            setTypeface(typeface, Typeface.BOLD)
                            gravity = Gravity.CENTER
                            setPadding(dp(7), dp(3), dp(7), dp(3))
                            background = roundedSurface(0xb3000000.toInt(), 9)
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        },
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            gravity = Gravity.END or Gravity.BOTTOM
                            marginEnd = dp(5)
                            bottomMargin = dp(5)
                        },
                    )
                }
            }
            addView(
                ImageView(context).apply {
                    tag = SELECTION_CHECK_TAG
                    visibility = if (selected) View.VISIBLE else View.GONE
                    setImageResource(R.drawable.ic_gallery_check_white)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setPadding(dp(5), dp(5), dp(5), dp(5))
                    background = roundedSurface(accentColor(), 12)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                FrameLayout.LayoutParams(dp(24), dp(24)).apply {
                    gravity = Gravity.END or Gravity.TOP
                    marginEnd = dp(6)
                    topMargin = dp(6)
                },
            )
        }.also { tile ->
            renderedMediaTiles[item.contentUri] = tile
        }
    }

    private fun mediaTileContentDescription(item: MediaItem, selected: Boolean): String =
        if (inSelectionMode) {
            "${item.displayName}. ${if (selected) "Selected" else "Not selected"}. Double tap to toggle selection."
        } else {
            "${item.displayName}. ${mediaMetadata(item)}. Double tap to open viewer. Long press then drag to select multiple items."
        }

    private fun refreshRenderedSelectionState() {
        val itemsByUri = selectionScopeItems.associateBy { it.contentUri }
        renderedMediaTiles.forEach { (contentUri, tile) ->
            val item = itemsByUri[contentUri] ?: return@forEach
            val selected = contentUri in selectedUris
            val selectionVisibility = if (selected) View.VISIBLE else View.GONE
            tile.isSelected = selected
            tile.contentDescription = mediaTileContentDescription(item, selected)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                tile.stateDescription = if (selected) "Selected" else null
            }
            tile.findViewWithTag<View>(SELECTION_OVERLAY_TAG)?.visibility = selectionVisibility
            tile.findViewWithTag<View>(SELECTION_CHECK_TAG)?.visibility = selectionVisibility
            tile.findViewWithTag<View>(VIDEO_PLAY_TAG)?.visibility =
                if (inSelectionMode) View.GONE else View.VISIBLE
        }
    }

    private fun syncSelectionScope(items: List<MediaItem>) {
        selectionScopeItems = items
        if (!inSelectionMode) return
        val pruned = GallerySelectionPolicy.prune(selectedUris, items)
        selectedUris.clear()
        selectedUris.addAll(pruned)
        if (selectedUris.isEmpty()) selectionScopeItems = items
    }

    private fun toggleSelection(item: MediaItem, currentScope: List<MediaItem>) {
        selectionScopeItems = currentScope
        val updated = GallerySelectionPolicy.toggle(selectedUris, item, currentScope)
        selectedUris.clear()
        selectedUris.addAll(updated)
        if (selectedUris.isEmpty()) {
            announceForAccessibility("Selection cleared")
        } else {
            announceForAccessibility(
                if (selectedUris.size == 1) "1 item selected" else "${selectedUris.size} items selected",
            )
        }
        refreshRenderedSelectionState()
        updateHeader()
        renderNavigation()
    }

    private fun beginDragSelection(item: MediaItem, currentScope: List<MediaItem>): Boolean {
        val result = GalleryDragSelectionPolicy.begin(
            selectedContentUris = selectedUris,
            item = item,
            currentScope = currentScope,
        ) ?: return false

        selectionScopeItems = currentScope
        dragSelectionScope = currentScope
        dragSelectionSession = result.session
        selectedUris.clear()
        selectedUris.addAll(result.selectedContentUris)
        if (::libraryScroll.isInitialized) libraryScroll.requestDisallowInterceptTouchEvent(true)
        refreshRenderedSelectionState()
        updateHeader()
        renderNavigation()
        announceSelectionCount()
        return true
    }

    private fun updateDragSelectionPointer(rawX: Float, rawY: Float) {
        if (dragSelectionSession == null) return
        dragSelectionRawX = rawX
        dragSelectionRawY = rawY
        applyDragSelectionAtRawPoint(rawX, rawY)
        scheduleDragSelectionAutoScroll()
    }

    private fun applyDragSelectionAtRawPoint(rawX: Float, rawY: Float) {
        val session = dragSelectionSession ?: return
        val hitUri = renderedMediaTiles.entries.firstOrNull { (_, tile) ->
            val bounds = Rect()
            tile.getGlobalVisibleRect(bounds) && bounds.contains(rawX.toInt(), rawY.toInt())
        }?.key ?: return
        val item = dragSelectionScope.firstOrNull { it.contentUri == hitUri } ?: return
        val result = GalleryDragSelectionPolicy.apply(
            selectedContentUris = selectedUris,
            session = session,
            item = item,
            currentScope = dragSelectionScope,
        )
        dragSelectionSession = result.session
        selectedUris.clear()
        selectedUris.addAll(result.selectedContentUris)
        refreshRenderedSelectionState()
        updateHeader()
        renderNavigation()
    }

    private fun scheduleDragSelectionAutoScroll() {
        if (dragSelectionSession == null || dragSelectionAutoScrollPosted || !::libraryScroll.isInitialized) return
        dragSelectionAutoScrollPosted = true
        libraryScroll.postOnAnimation(object : Runnable {
            override fun run() {
                if (dragSelectionSession == null) {
                    dragSelectionAutoScrollPosted = false
                    return
                }
                val viewport = Rect()
                if (!libraryScroll.getGlobalVisibleRect(viewport)) {
                    dragSelectionAutoScrollPosted = false
                    return
                }
                val edgePx = dp(DRAG_SELECTION_EDGE_DP)
                val delta = when {
                    dragSelectionRawY < viewport.top + edgePx && libraryScroll.canScrollVertically(-1) ->
                        -dp(DRAG_SELECTION_SCROLL_STEP_DP)
                    dragSelectionRawY > viewport.bottom - edgePx && libraryScroll.canScrollVertically(1) ->
                        dp(DRAG_SELECTION_SCROLL_STEP_DP)
                    else -> 0
                }
                if (delta == 0) {
                    dragSelectionAutoScrollPosted = false
                    return
                }
                libraryScroll.scrollBy(0, delta)
                applyDragSelectionAtRawPoint(dragSelectionRawX, dragSelectionRawY)
                libraryScroll.postOnAnimation(this)
            }
        })
    }

    private fun finishDragSelection() {
        if (dragSelectionSession == null) return
        dragSelectionSession = null
        dragSelectionScope = emptyList()
        dragSelectionAutoScrollPosted = false
        if (::libraryScroll.isInitialized) libraryScroll.requestDisallowInterceptTouchEvent(false)
        if (selectedUris.isEmpty()) selectionScopeItems = emptyList()
        refreshRenderedSelectionState()
        updateHeader()
        renderNavigation()
        announceSelectionCount()
    }

    private fun announceSelectionCount() {
        announceForAccessibility(
            when (selectedUris.size) {
                0 -> "Selection cleared"
                1 -> "1 item selected"
                else -> "${selectedUris.size} items selected"
            },
        )
    }

    private fun clearSelection(render: Boolean = true) {
        val hadSelection = selectedUris.isNotEmpty() || dragSelectionSession != null
        dragSelectionSession = null
        dragSelectionScope = emptyList()
        dragSelectionAutoScrollPosted = false
        if (::libraryScroll.isInitialized) libraryScroll.requestDisallowInterceptTouchEvent(false)
        selectedUris.clear()
        selectionScopeItems = emptyList()
        if (!::navigationCapsule.isInitialized) return
        if (render) {
            renderCurrentDestination()
        } else if (hadSelection) {
            updateHeader()
            renderNavigation()
        }
    }

    private fun currentSelectedItems(): List<MediaItem> =
        GallerySelectionPolicy.resolve(selectionScopeItems, selectedUris)

    private fun shareSelectedItems() {
        val plan = GalleryBulkActionPolicy.sharePlan(selectionScopeItems, selectedUris) ?: return
        val uris = ArrayList(plan.contentUris.map(Uri::parse))
        val shareIntent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = plan.mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = plan.mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }.apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(shareIntent, "Share selected media"))
        } catch (_: RuntimeException) {
            Toast.makeText(this, "No compatible share destination is available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareSingleItem(item: MediaItem) {
        if (authorizedItems.none { authorized -> authorized.contentUri == item.contentUri }) {
            Toast.makeText(this, "This media item is no longer authorized.", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse(item.contentUri)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(intent, "Share media"))
        } catch (_: RuntimeException) {
            Toast.makeText(this, "No compatible share destination is available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applySelectedFavoriteAction() {
        val selectedItems = currentSelectedItems()
        val plannedAction = GalleryBulkActionPolicy.favoriteAction(
            selectionScopeItems,
            selectedUris,
            favoriteUris,
        ) ?: return

        when (plannedAction) {
            GalleryFavoriteBulkAction.ADD -> selectedItems.forEach { favoriteUris.add(it.contentUri) }
            GalleryFavoriteBulkAction.REMOVE -> selectedItems.forEach { favoriteUris.remove(it.contentUri) }
        }
        persistFavorites()
        val count = selectedItems.size
        val message = when (plannedAction) {
            GalleryFavoriteBulkAction.ADD -> if (count == 1) "Added to Favorites" else "Added $count items to Favorites"
            GalleryFavoriteBulkAction.REMOVE -> if (count == 1) "Removed from Favorites" else "Removed $count items from Favorites"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        clearSelection()
    }

    private fun showMoveDestinationDialog() {
        if (!AndroidMediaMoveRequests.isSupported()) {
            Toast.makeText(this, "Move requires Android 11 or newer in this Development build.", Toast.LENGTH_SHORT).show()
            return
        }
        if (pendingMediaMove != null || pendingMediaMutation != null || mediaMoveExecutionInProgress || mediaCopyExecutionInProgress) return

        val selectedItems = currentSelectedItems()
        val currentScope = visibleAuthorizedItems()
        val destinations = GalleryMoveDestinationPolicy.existingDestinations(
            currentScope = currentScope,
            selectedContentUris = selectedUris,
        )
        val newFolderParent = GalleryNewFolderMovePolicy.parentForSelection(
            currentScope = currentScope,
            selectedContentUris = selectedUris,
        )
        if (selectedItems.isEmpty() || (destinations.isEmpty() && newFolderParent == null)) {
            Toast.makeText(this, "No eligible move destination is available for this selection.", Toast.LENGTH_SHORT).show()
            return
        }

        var dialog: AlertDialog? = null
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(14))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.OVERLAY,
                GalleryGlazeContract.SHAPE_OVERLAY_DP,
            )
        }
        panel.addView(TextView(this).apply {
            text = "Move"
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
        })
        panel.addView(TextView(this).apply {
            text = if (newFolderParent != null) {
                "Choose an existing local folder, or create a new folder inside ${newFolderParent.displayName}."
            } else {
                "Choose an existing local folder from media Android currently authorizes."
            }
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setLineSpacing(0f, 1.06f)
            setPadding(0, dp(4), 0, dp(14))
        })

        if (newFolderParent != null) {
            panel.addView(
                glazeDialogActionRow(
                    title = "New folder",
                    subtitle = "Create inside ${newFolderParent.displayName}",
                ) {
                    dialog?.dismiss()
                    showNewFolderDialog(selectedItems, newFolderParent.displayName)
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(10)
                },
            )
        } else {
            panel.addView(TextView(this).apply {
                text = "New folder requires selected items from one current folder."
                setTextColor(secondaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setPadding(dp(4), dp(2), dp(4), dp(10))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            })
        }

        destinations.forEach { moveDestination ->
            panel.addView(
                glazeDialogActionRow(
                    title = moveDestination.displayName,
                    subtitle = "${itemCountLabel(moveDestination.itemCount)} · Existing local folder",
                ) {
                    requestMediaMove(selectedItems, moveDestination)
                    dialog?.dismiss()
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(7)
                },
            )
        }

        panel.addView(
            dialogDismissAction("Cancel move") { dialog?.dismiss() },
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ).apply {
                gravity = Gravity.END
            },
        )

        dialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()
        dialog?.setOnShowListener {
            dialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.setDimAmount(0.42f)
            dialog?.window?.setLayout(
                resources.displayMetrics.widthPixels - dp(32),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        dialog?.show()
        dialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setDimAmount(0.42f)
        dialog?.window?.setLayout(
            resources.displayMetrics.widthPixels - dp(32),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun showNewFolderDialog(items: List<MediaItem>, parentDisplayName: String) {
        if (items.isEmpty() || pendingMediaMove != null || pendingMediaMutation != null || mediaMoveExecutionInProgress || mediaCopyExecutionInProgress) return

        var dialog: AlertDialog? = null
        val folderNameField = EditText(this).apply {
            hint = "Folder name"
            setSingleLine(true)
            minHeight = dp(GalleryGlazeContract.GENERAL_TARGET_DP)
            setPadding(dp(14), 0, dp(14), 0)
            setTextColor(primaryTextColor())
            setHintTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.CONTROL,
                GalleryGlazeContract.SHAPE_CONTROL_DP,
            )
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = "New folder name"
        }
        val createAction = TextView(this).apply {
            text = "Create & move"
            gravity = Gravity.CENTER
            minHeight = dp(GalleryGlazeContract.GENERAL_TARGET_DP)
            setPadding(dp(14), 0, dp(14), 0)
            setTextColor(accentColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gallery_move, 0, 0, 0)
            compoundDrawableTintList = ColorStateList.valueOf(accentColor())
            compoundDrawablePadding = dp(7)
            background = roundedSurface(withAlpha(accentColor(), 0.12f), GalleryGlazeContract.SHAPE_CONTROL_DP)
            isClickable = true
            isFocusable = true
            contentDescription = "Create new folder and move selected media"
            GalleryInteractionFeedback.applyBoundedRipple(
                this,
                accentColor(),
                GalleryGlazeContract.SHAPE_CONTROL_DP,
            )
        }
        val cancelAction = dialogDismissAction("Cancel new folder") {
            dialog?.dismiss()
        }

        createAction.setOnClickListener {
            val destination = try {
                GalleryNewFolderMovePolicy.destinationForSelection(
                    currentScope = visibleAuthorizedItems(),
                    selectedContentUris = selectedUris,
                    rawFolderName = folderNameField.text?.toString().orEmpty(),
                )
            } catch (error: IllegalArgumentException) {
                folderNameField.error = error.message ?: "Choose a valid folder name"
                folderNameField.requestFocus()
                return@setOnClickListener
            }
            dialog?.dismiss()
            requestMediaMove(items, destination.relativePath)
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, 0)
            addView(
                cancelAction,
                LinearLayout.LayoutParams(
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                ).apply {
                    marginEnd = dp(6)
                },
            )
            addView(createAction, LinearLayout.LayoutParams(0, dp(GalleryGlazeContract.GENERAL_TARGET_DP), 1f))
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(18))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.OVERLAY,
                GalleryGlazeContract.SHAPE_OVERLAY_DP,
            )
            addView(TextView(context).apply {
                text = "New folder"
                setTextColor(primaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = "Create inside $parentDisplayName and move ${itemCountLabel(items.size)} there after Android authorizes the selected media."
                setTextColor(secondaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setLineSpacing(0f, 1.06f)
                setPadding(0, dp(4), 0, dp(14))
            })
            addView(folderNameField, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)))
            addView(actions)
        }

        dialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()
        dialog?.setOnShowListener {
            dialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.setDimAmount(0.42f)
            dialog?.window?.setLayout(
                resources.displayMetrics.widthPixels - dp(32),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            folderNameField.requestFocus()
            folderNameField.post {
                (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInput(folderNameField, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        dialog?.show()
        dialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setDimAmount(0.42f)
        dialog?.window?.setLayout(
            resources.displayMetrics.widthPixels - dp(32),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun showCopyDestinationDialog() {
        if (!AndroidMediaCopyRequests.isSupported()) {
            Toast.makeText(this, "Copy requires Android 11 or newer in this Development build.", Toast.LENGTH_SHORT).show()
            return
        }
        if (pendingMediaMove != null || pendingMediaMutation != null || mediaMoveExecutionInProgress || mediaCopyExecutionInProgress) return

        val selectedItems = currentSelectedItems()
        if (selectedItems.size > AndroidMediaCopyRequests.MAX_COPY_ITEMS) {
            Toast.makeText(
                this,
                "Copy is limited to ${AndroidMediaCopyRequests.MAX_COPY_ITEMS} items at a time.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        val currentScope = visibleAuthorizedItems()
        val destinations = GalleryCopyDestinationPolicy.existingDestinations(
            currentScope = currentScope,
            selectedContentUris = selectedUris,
        )
        val newFolderParent = GalleryNewFolderCopyPolicy.parentForSelection(
            currentScope = currentScope,
            selectedContentUris = selectedUris,
        )
        if (selectedItems.isEmpty() || (destinations.isEmpty() && newFolderParent == null)) {
            Toast.makeText(this, "No eligible copy destination is available for this selection.", Toast.LENGTH_SHORT).show()
            return
        }

        var dialog: AlertDialog? = null
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(14))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.OVERLAY,
                GalleryGlazeContract.SHAPE_OVERLAY_DP,
            )
        }
        panel.addView(TextView(this).apply {
            text = "Copy"
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
        })
        panel.addView(TextView(this).apply {
            text = if (newFolderParent != null) {
                "Choose another local folder, or create a new folder inside $newFolderParent. Originals remain unchanged."
            } else {
                "Choose another local folder from media Android currently authorizes. Originals remain unchanged."
            }
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setLineSpacing(0f, 1.06f)
            setPadding(0, dp(4), 0, dp(14))
        })

        if (newFolderParent != null) {
            panel.addView(
                glazeDialogActionRow(
                    title = "New folder",
                    subtitle = "Create inside $newFolderParent",
                    actionDescription = "Create a new folder and copy selected media",
                ) {
                    dialog?.dismiss()
                    showNewCopyFolderDialog(selectedItems, newFolderParent)
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(10)
                },
            )
        }

        destinations.forEach { destination ->
            panel.addView(
                glazeDialogActionRow(
                    title = destination.displayName,
                    subtitle = "${itemCountLabel(destination.itemCount)} · Existing local folder",
                    actionDescription = "Copy selected media here and preserve originals",
                ) {
                    requestMediaCopy(selectedItems, destination)
                    dialog?.dismiss()
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(7)
                },
            )
        }

        panel.addView(
            dialogDismissAction("Cancel copy") { dialog?.dismiss() },
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ).apply {
                gravity = Gravity.END
            },
        )

        dialog = AlertDialog.Builder(this).setView(panel).create()
        dialog?.setOnShowListener {
            dialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.setDimAmount(0.42f)
            dialog?.window?.setLayout(
                resources.displayMetrics.widthPixels - dp(32),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        dialog?.show()
        dialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setDimAmount(0.42f)
        dialog?.window?.setLayout(
            resources.displayMetrics.widthPixels - dp(32),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun showNewCopyFolderDialog(items: List<MediaItem>, parentDisplayName: String) {
        if (items.isEmpty() || pendingMediaMove != null || pendingMediaMutation != null || mediaMoveExecutionInProgress || mediaCopyExecutionInProgress) return

        var dialog: AlertDialog? = null
        val folderNameField = EditText(this).apply {
            hint = "Folder name"
            setSingleLine(true)
            minHeight = dp(GalleryGlazeContract.GENERAL_TARGET_DP)
            setPadding(dp(14), 0, dp(14), 0)
            setTextColor(primaryTextColor())
            setHintTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.CONTROL,
                GalleryGlazeContract.SHAPE_CONTROL_DP,
            )
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = "New copy folder name"
        }
        val createAction = TextView(this).apply {
            text = "Create & copy"
            gravity = Gravity.CENTER
            minHeight = dp(GalleryGlazeContract.GENERAL_TARGET_DP)
            setPadding(dp(14), 0, dp(14), 0)
            setTextColor(accentColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gallery_copy, 0, 0, 0)
            compoundDrawableTintList = ColorStateList.valueOf(accentColor())
            compoundDrawablePadding = dp(7)
            background = roundedSurface(withAlpha(accentColor(), 0.12f), GalleryGlazeContract.SHAPE_CONTROL_DP)
            isClickable = true
            isFocusable = true
            contentDescription = "Create new folder and copy selected media"
            GalleryInteractionFeedback.applyBoundedRipple(
                this,
                accentColor(),
                GalleryGlazeContract.SHAPE_CONTROL_DP,
            )
        }
        val cancelAction = dialogDismissAction("Cancel new copy folder") {
            dialog?.dismiss()
        }

        createAction.setOnClickListener {
            val destination = try {
                GalleryNewFolderCopyPolicy.destinationForSelection(
                    currentScope = visibleAuthorizedItems(),
                    selectedContentUris = selectedUris,
                    rawFolderName = folderNameField.text?.toString().orEmpty(),
                )
            } catch (error: IllegalArgumentException) {
                folderNameField.error = error.message ?: "Choose a valid folder name"
                folderNameField.requestFocus()
                return@setOnClickListener
            }
            dialog?.dismiss()
            requestMediaCopy(items, destination.relativePath)
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, 0)
            addView(
                cancelAction,
                LinearLayout.LayoutParams(
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                ).apply {
                    marginEnd = dp(6)
                },
            )
            addView(createAction, LinearLayout.LayoutParams(0, dp(GalleryGlazeContract.GENERAL_TARGET_DP), 1f))
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(18))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.OVERLAY,
                GalleryGlazeContract.SHAPE_OVERLAY_DP,
            )
            addView(TextView(context).apply {
                text = "New folder"
                setTextColor(primaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = "Create inside $parentDisplayName and copy ${itemCountLabel(items.size)} there. The originals remain unchanged."
                setTextColor(secondaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setLineSpacing(0f, 1.06f)
                setPadding(0, dp(4), 0, dp(14))
            })
            addView(folderNameField, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)))
            addView(actions)
        }

        dialog = AlertDialog.Builder(this).setView(panel).create()
        dialog?.setOnShowListener {
            dialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.setDimAmount(0.42f)
            dialog?.window?.setLayout(
                resources.displayMetrics.widthPixels - dp(32),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            folderNameField.requestFocus()
            folderNameField.post {
                (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInput(folderNameField, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        dialog?.show()
        dialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setDimAmount(0.42f)
        dialog?.window?.setLayout(
            resources.displayMetrics.widthPixels - dp(32),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun requestMediaCopy(items: List<MediaItem>, destination: GalleryCopyDestination) =
        requestMediaCopy(items, destination.relativePath)

    private fun requestMediaCopy(items: List<MediaItem>, destinationRelativePath: String) {
        if (items.isEmpty() || pendingMediaMove != null || pendingMediaMutation != null || mediaMoveExecutionInProgress || mediaCopyExecutionInProgress) return
        if (items.size > AndroidMediaCopyRequests.MAX_COPY_ITEMS) {
            Toast.makeText(
                this,
                "Copy is limited to ${AndroidMediaCopyRequests.MAX_COPY_ITEMS} items at a time.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        if (!AndroidMediaCopyRequests.isSupported()) {
            Toast.makeText(this, "Copy requires Android 11 or newer in this Development build.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentScope = visibleAuthorizedItems()
        val currentByUri = currentScope.associateBy { it.contentUri }
        if (items.any { currentByUri[it.contentUri] != it }) {
            Toast.makeText(this, "The selected media changed before Copy could start.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedVolume = items
            .mapNotNull { it.volumeName?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .singleOrNull()
        if (selectedVolume == null || items.any { it.volumeName?.trim() != selectedVolume }) {
            Toast.makeText(this, "Copy requires selected media from one current storage volume.", Toast.LENGTH_SHORT).show()
            return
        }

        val occupiedNames = currentScope
            .filter {
                it.volumeName?.trim() == selectedVolume &&
                    it.relativePath == destinationRelativePath
            }
            .map { it.displayName }
            .toMutableSet()
        val sources = try {
            items.map { item ->
                val outputName = GalleryCopyNamePolicy.nextAvailable(item.displayName, occupiedNames)
                occupiedNames += outputName
                AndroidMediaCopySource(
                    contentUri = item.contentUri,
                    displayName = item.displayName,
                    outputDisplayName = outputName,
                    mimeType = item.mimeType,
                    capturedAtMillis = item.capturedAt?.toEpochMilli(),
                )
            }
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, "Gallery refused an invalid Copy source.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: IllegalStateException) {
            Toast.makeText(this, "Gallery could not allocate a safe Copy name.", Toast.LENGTH_SHORT).show()
            return
        }

        mediaCopyExecutionInProgress = true
        renderNavigation()
        thread(name = "goreecloud-gallery-mediastore-copy") {
            val result = try {
                AndroidMediaCopyRequests.execute(
                    contentResolver = contentResolver,
                    sources = sources,
                    destinationRelativePath = destinationRelativePath,
                )
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: IllegalStateException) {
                null
            } catch (_: IOException) {
                null
            } catch (_: RuntimeException) {
                null
            }

            runOnUiThread {
                mediaCopyExecutionInProgress = false
                if (isFinishing || isDestroyed) return@runOnUiThread

                clearSelection(render = false)
                thumbnailCache.evictAll()
                val message = when {
                    result == null -> "Copy could not be completed"
                    result.failedCount == 0 && result.copiedCount == 1 -> "Copied 1 item"
                    result.failedCount == 0 -> "Copied ${result.copiedCount} items"
                    result.copiedCount == 0 && result.failedCount == 1 -> "Copy failed for 1 item"
                    result.copiedCount == 0 -> "Copy failed for ${result.failedCount} items"
                    else -> "Copied ${result.copiedCount} items · ${result.failedCount} failed"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                val accessScope = currentMediaAccessScope()
                if (GalleryMediaAccessPolicy.canRead(accessScope)) {
                    loadLocalLibrary(accessScope)
                } else {
                    renderPermissionState()
                }
            }
        }
    }

    private fun glazeDialogActionRow(
        title: String,
        subtitle: String,
        actionDescription: String = "Move selected media here",
        onClick: () -> Unit,
    ): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(64)
        setPadding(dp(14), dp(9), dp(10), dp(9))
        background = GalleryGlazeSurfaces.drawable(
            context,
            GalleryGlazeSurfaces.Role.RAISED,
            GalleryGlazeContract.SHAPE_CONTAINER_DP,
        )
        val labels = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        labels.addView(TextView(context).apply {
            text = title
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
        })
        labels.addView(TextView(context).apply {
            text = subtitle
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setPadding(0, dp(2), dp(8), 0)
        })
        addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(
            ImageView(context).apply {
                setImageResource(R.drawable.ic_gallery_chevron_right)
                setColorFilter(accentColor())
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(8), dp(12), dp(8), dp(12))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(dp(36), dp(GalleryGlazeContract.GENERAL_TARGET_DP)),
        )
        isClickable = true
        isFocusable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "$title. $subtitle. $actionDescription."
        GalleryInteractionFeedback.applyBoundedRipple(
            this,
            accentColor(),
            GalleryGlazeContract.SHAPE_CONTAINER_DP,
        )
        setOnClickListener { onClick() }
    }

    private fun requestMediaMove(items: List<MediaItem>, destination: GalleryMoveDestination) =
        requestMediaMove(items, destination.relativePath)

    private fun requestMediaMove(items: List<MediaItem>, destinationRelativePath: String) {
        if (items.isEmpty() || pendingMediaMove != null || pendingMediaMutation != null || mediaMoveExecutionInProgress || mediaCopyExecutionInProgress) return
        if (!AndroidMediaMoveRequests.isSupported()) {
            Toast.makeText(this, "Move requires Android 11 or newer in this Development build.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = try {
            AndroidMediaMoveRequests.create(
                contentResolver = contentResolver,
                contentUris = items.map { it.contentUri },
                destinationRelativePath = destinationRelativePath,
            )
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, "Gallery refused an invalid move request.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: IllegalStateException) {
            Toast.makeText(this, "Android-authorized media move is unavailable on this device.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: SecurityException) {
            Toast.makeText(this, "Android denied the move authorization request.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: RuntimeException) {
            Toast.makeText(this, "The Android media provider could not prepare this move.", Toast.LENGTH_SHORT).show()
            return
        }

        pendingMediaMove = AndroidMediaMoveRequests.capture(request)
        try {
            startIntentSenderForResult(
                request.pendingIntent.intentSender,
                MEDIA_MOVE_REQUEST,
                null,
                0,
                0,
                0,
            )
        } catch (_: IntentSender.SendIntentException) {
            pendingMediaMove = null
            Toast.makeText(this, "Android could not open move authorization.", Toast.LENGTH_SHORT).show()
        } catch (_: RuntimeException) {
            pendingMediaMove = null
            Toast.makeText(this, "Android could not open move authorization.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun completeConfirmedMediaMove(move: AndroidMediaMovePendingState) {
        mediaMoveExecutionInProgress = true
        thread(name = "goreecloud-gallery-mediastore-move") {
            val result = try {
                AndroidMediaMoveRequests.execute(contentResolver, move)
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: IllegalStateException) {
                null
            } catch (_: RuntimeException) {
                null
            }

            runOnUiThread {
                mediaMoveExecutionInProgress = false
                clearSelection(render = false)
                thumbnailCache.evictAll()

                val message = when {
                    result == null -> "Move could not be completed"
                    result.failedCount == 0 && result.movedCount == 1 -> "Moved 1 item"
                    result.failedCount == 0 -> "Moved ${result.movedCount} items"
                    result.movedCount == 0 && result.failedCount == 1 -> "Move failed for 1 item"
                    result.movedCount == 0 -> "Move failed for ${result.failedCount} items"
                    else -> "Moved ${result.movedCount} items · ${result.failedCount} failed"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                val accessScope = currentMediaAccessScope()
                if (GalleryMediaAccessPolicy.canRead(accessScope)) {
                    loadLocalLibrary(accessScope)
                } else {
                    renderPermissionState()
                }
            }
        }
    }

    private fun requestMediaDeletion(items: List<MediaItem>) {
        if (items.isEmpty() || pendingMediaMutation != null || pendingMediaMove != null || mediaMoveExecutionInProgress || mediaCopyExecutionInProgress) return
        if (!AndroidMediaMutationRequests.isSupported()) {
            Toast.makeText(
                this,
                "Delete requires Android 11 or newer in this Development build.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val mode = if (currentUserSettings().moveDeletedItemsToRecycleBin) {
            AndroidMediaMutationMode.TRASH
        } else {
            AndroidMediaMutationMode.DELETE
        }

        val request = try {
            AndroidMediaMutationRequests.create(
                contentResolver = contentResolver,
                contentUris = items.map { it.contentUri },
                mode = mode,
            )
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, "Gallery refused an invalid media mutation request.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: IllegalStateException) {
            Toast.makeText(this, "Android media mutation is unavailable on this device.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: SecurityException) {
            Toast.makeText(this, "Android denied the media mutation request.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: RuntimeException) {
            Toast.makeText(this, "The Android media provider could not prepare this request.", Toast.LENGTH_SHORT).show()
            return
        }

        pendingMediaMutation = try {
            AndroidMediaMutationPendingStates.capture(request.mode, request.contentUris)
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, "Gallery refused invalid pending media mutation state.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startIntentSenderForResult(
                request.pendingIntent.intentSender,
                MEDIA_MUTATION_REQUEST,
                null,
                0,
                0,
                0,
            )
        } catch (_: IntentSender.SendIntentException) {
            pendingMediaMutation = null
            Toast.makeText(this, "Android could not open the media confirmation.", Toast.LENGTH_SHORT).show()
        } catch (_: RuntimeException) {
            pendingMediaMutation = null
            Toast.makeText(this, "Android could not open the media confirmation.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun completeConfirmedMediaMutation(mutation: AndroidMediaMutationPendingState) {
        if (mutation.mode == AndroidMediaMutationMode.DELETE) {
            favoriteUris.removeAll(mutation.contentUris)
            persistFavorites()
        }

        viewerVideoSurface?.apply {
            onPlaybackError = null
            stop()
        }
        viewerVideoSurface = null
        viewerOverlay?.let { rootFrame.removeView(it) }
        viewerOverlay = null
        clearSelection(render = false)
        thumbnailCache.evictAll()
        applySystemChrome()

        Toast.makeText(
            this,
            if (mutation.mode == AndroidMediaMutationMode.TRASH) {
                if (mutation.contentUris.size == 1) "Moved to Recycle Bin" else "Moved ${mutation.contentUris.size} items to Recycle Bin"
            } else {
                if (mutation.contentUris.size == 1) "Deleted permanently" else "Deleted ${mutation.contentUris.size} items permanently"
            },
            Toast.LENGTH_SHORT,
        ).show()

        val accessScope = currentMediaAccessScope()
        if (GalleryMediaAccessPolicy.canRead(accessScope)) {
            loadLocalLibrary(accessScope)
        } else {
            renderPermissionState()
        }
    }

    private fun showAuthorizedViewer(items: List<MediaItem>, initialIndex: Int, generation: Int) {
        if (
            generation != loadGeneration ||
            !GalleryMediaAccessPolicy.canRead(currentMediaAccessScope()) ||
            initialIndex !in items.indices
        ) return

        clearSelection(render = false)
        viewerSlideshowStop?.invoke()
        viewerSlideshowStop = null
        viewerOverlay?.let { rootFrame.removeView(it) }
        navigationCapsule.visibility = View.GONE
        selectionActionCapsule.visibility = View.GONE

        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        viewerOverlay = overlay
        rootFrame.addView(
            overlay,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        @Suppress("DEPRECATION")
        run { window.decorView.systemUiVisibility = 0 }

        val mediaHost = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        val preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        mediaHost.addView(
            preview,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        val videoSurface = GalleryVideoPlayerSurface(this).apply {
            visibility = View.GONE
        }
        viewerVideoSurface = videoSurface
        mediaHost.addView(
            videoSurface,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        overlay.addView(
            mediaHost,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                topMargin = dp(72)
                bottomMargin = dp(104)
            },
        )

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = roundedSurface(0xd9141416.toInt(), 22)
        }
        val viewerBack = viewerIconAction(
            R.drawable.ic_gallery_back,
            true,
            "Close viewer",
        ) { closeAuthorizedViewer() }
        topBar.addView(
            viewerBack,
            LinearLayout.LayoutParams(dp(GalleryGlazeContract.GENERAL_TARGET_DP), dp(GalleryGlazeContract.GENERAL_TARGET_DP)),
        )
        val viewerTitle = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val viewerSubtitle = TextView(this).apply {
            setTextColor(0xffc8c8cc.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val viewerTitles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(6), 0)
            addView(viewerTitle)
            addView(viewerSubtitle)
        }
        topBar.addView(viewerTitles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val scaleMode = viewerIconAction(
            R.drawable.ic_gallery_fit,
            true,
            "Viewer scale: Fit",
        ) {}
        topBar.addView(
            scaleMode,
            LinearLayout.LayoutParams(dp(GalleryGlazeContract.GENERAL_TARGET_DP), dp(GalleryGlazeContract.GENERAL_TARGET_DP)).apply {
                marginStart = dp(4)
            },
        )
        val repeatSlideshow = viewerIconAction(
            R.drawable.ic_gallery_repeat,
            true,
            "Repeat slideshow off",
        ) {}
        topBar.addView(
            repeatSlideshow,
            LinearLayout.LayoutParams(dp(GalleryGlazeContract.GENERAL_TARGET_DP), dp(GalleryGlazeContract.GENERAL_TARGET_DP)).apply {
                marginStart = dp(4)
            },
        )
        val slideshow = viewerIconAction(
            R.drawable.ic_gallery_slideshow,
            true,
            "Start photo slideshow",
        ) {}
        topBar.addView(
            slideshow,
            LinearLayout.LayoutParams(dp(GalleryGlazeContract.GENERAL_TARGET_DP), dp(GalleryGlazeContract.GENERAL_TARGET_DP)).apply {
                marginStart = dp(4)
            },
        )
        overlay.addView(
            topBar,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)).apply {
                gravity = Gravity.TOP
                marginStart = dp(10)
                marginEnd = dp(10)
                topMargin = dp(6)
            },
        )

        val previous = viewerIconAction(
            R.drawable.ic_gallery_chevron_left,
            true,
            "Previous media",
        ) {}
        val next = viewerIconAction(
            R.drawable.ic_gallery_chevron_right,
            true,
            "Next media",
        ) {}
        overlay.addView(
            previous,
            FrameLayout.LayoutParams(dp(52), dp(64)).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                marginStart = dp(10)
            },
        )
        overlay.addView(
            next,
            FrameLayout.LayoutParams(dp(52), dp(64)).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                marginEnd = dp(10)
            },
        )

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = roundedSurface(0xe8141416.toInt(), 24)
        }

        val share = viewerIconAction(R.drawable.ic_gallery_share, true, "Share this media") {}
        val favorite = viewerIconAction(
            R.drawable.ic_gallery_favorite_outline,
            true,
            "Favorite this media",
        ) {}
        val edit = viewerIconAction(R.drawable.ic_gallery_edit, true, "Edit this photo") {}
        val deleteSupported = AndroidMediaMutationRequests.isSupported()
        val delete = viewerIconAction(
            R.drawable.ic_gallery_delete,
            deleteSupported,
            when {
                !deleteSupported -> "Delete requires Android 11 or newer in this Development build"
                currentUserSettings().moveDeletedItemsToRecycleBin -> "Move this media to the Android Recycle Bin"
                else -> "Permanently delete this media after Android confirmation"
            },
        ) {}.apply {
            compoundDrawableTintList = ColorStateList.valueOf(0xffff8a80.toInt())
        }
        val more = viewerIconAction(R.drawable.ic_gallery_more, true, "Show media details") {}

        listOf(share, favorite, edit, delete, more).forEachIndexed { index, item ->
            bottomBar.addView(
                item,
                LinearLayout.LayoutParams(0, dp(60), 1f).apply {
                    if (index > 0) marginStart = dp(3)
                },
            )
        }
        overlay.addView(
            bottomBar,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)).apply {
                gravity = Gravity.BOTTOM
                marginStart = dp(10)
                marginEnd = dp(10)
                bottomMargin = dp(16)
            },
        )

        val playbackToggle = viewerIconAction(
            R.drawable.ic_gallery_play,
            true,
            "Play video",
        ) {}.apply {
            visibility = View.GONE
        }
        overlay.addView(
            playbackToggle,
            FrameLayout.LayoutParams(dp(104), dp(48)).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                bottomMargin = dp(96)
            },
        )
        videoSurface.onPlaybackError = { _, _ ->
            videoSurface.visibility = View.GONE
            preview.visibility = View.VISIBLE
            playbackToggle.visibility = View.GONE
            Toast.makeText(this, "Video playback is unavailable for this item.", Toast.LENGTH_SHORT).show()
        }

        var currentIndex = initialIndex
        var activePlaybackPlan: GalleryViewerPlaybackPlan? = null
        var viewerScaleMode = GalleryViewerScaleMode.FIT
        var viewerZoomScale = GalleryViewerZoomPolicy.MIN_SCALE
        var viewerPanX = 0f
        var viewerPanY = 0f
        val slideshowInterval = currentUserSettings().slideshowInterval
        var slideshowRunning = false
        var slideshowPaused = false
        var slideshowRepeats = false
        var slideshowAdvance: Runnable? = null

        fun updateRepeatSlideshowControl() {
            setViewerActionIcon(
                repeatSlideshow,
                R.drawable.ic_gallery_repeat,
                selected = slideshowRepeats,
            )
            repeatSlideshow.contentDescription =
                if (slideshowRepeats) "Repeat slideshow on" else "Repeat slideshow off"
            repeatSlideshow.tooltipText = repeatSlideshow.contentDescription
        }

        fun updateSlideshowControl() {
            setViewerActionIcon(
                slideshow,
                if (slideshowRunning) R.drawable.ic_gallery_pause else R.drawable.ic_gallery_slideshow,
                selected = slideshowRunning,
            )
            slideshow.contentDescription = when {
                slideshowRunning -> "Pause photo slideshow"
                slideshowPaused -> "Resume photo slideshow, ${slideshowInterval.label.lowercase()}"
                else -> "Start photo slideshow, ${slideshowInterval.label.lowercase()}"
            }
            slideshow.tooltipText = slideshow.contentDescription
        }

        fun stopSlideshow(announce: Boolean = false) {
            slideshowRunning = false
            slideshowPaused = false
            slideshowAdvance?.let(overlay::removeCallbacks)
            updateSlideshowControl()
            if (announce) announceForAccessibility("Slideshow stopped")
        }

        fun pauseSlideshow() {
            slideshowRunning = false
            slideshowPaused = true
            slideshowAdvance?.let(overlay::removeCallbacks)
            updateSlideshowControl()
            announceForAccessibility("Photo slideshow paused")
        }

        fun startOrResumeSlideshow() {
            slideshowRunning = true
            slideshowPaused = false
            updateSlideshowControl()
            announceForAccessibility("Photo slideshow playing")
            overlay.postDelayed(
                checkNotNull(slideshowAdvance),
                slideshowInterval.intervalMs,
            )
        }

        fun currentItemSupportsZoom(): Boolean =
            items.getOrNull(currentIndex)?.mimeType?.startsWith("image/") == true

        fun applyViewerTransform() {
            val bounded = GalleryViewerZoomPolicy.boundedTranslation(
                viewportWidth = preview.width,
                viewportHeight = preview.height,
                scale = viewerZoomScale,
                proposedX = viewerPanX,
                proposedY = viewerPanY,
            )
            viewerPanX = bounded.x
            viewerPanY = bounded.y
            preview.scaleX = viewerZoomScale
            preview.scaleY = viewerZoomScale
            preview.translationX = viewerPanX
            preview.translationY = viewerPanY

            val zoomed = GalleryViewerZoomPolicy.isZoomed(viewerZoomScale)
            val scaleIcon = when {
                zoomed -> R.drawable.ic_gallery_zoom
                viewerScaleMode == GalleryViewerScaleMode.FIT -> R.drawable.ic_gallery_fit
                else -> R.drawable.ic_gallery_fill
            }
            setViewerActionIcon(scaleMode, scaleIcon, selected = zoomed)
            scaleMode.contentDescription = when {
                !currentItemSupportsZoom() -> "View options are available for photos only"
                zoomed ->
                    "Viewer zoom ${GalleryViewerZoomPolicy.displayPercent(viewerZoomScale)} percent. " +
                        "Tap for view options. Drag to pan or pinch to change zoom."
                viewerScaleMode == GalleryViewerScaleMode.FIT ->
                    "Viewer view: Fit. Tap for Fit, Fill, and zoom options."
                else ->
                    "Viewer view: Fill. Tap for Fit, Fill, and zoom options."
            }
            scaleMode.tooltipText = scaleMode.contentDescription
        }

        fun resetViewerZoom() {
            viewerZoomScale = GalleryViewerZoomPolicy.MIN_SCALE
            viewerPanX = 0f
            viewerPanY = 0f
        }

        fun applyViewerScaleMode() {
            preview.scaleType = when (viewerScaleMode) {
                GalleryViewerScaleMode.FIT -> ImageView.ScaleType.FIT_CENTER
                GalleryViewerScaleMode.FILL -> ImageView.ScaleType.CENTER_CROP
            }
            applyViewerTransform()
        }

        fun renderCurrentItem() {
            if (
                generation != loadGeneration ||
                !GalleryMediaAccessPolicy.canRead(currentMediaAccessScope()) ||
                currentIndex !in items.indices
            ) {
                closeAuthorizedViewer()
                return
            }
            val item = items[currentIndex]
            val playbackPlan = GalleryViewerPlaybackPolicy.plan(item, currentUserSettings())
            activePlaybackPlan = playbackPlan
            resetViewerZoom()
            videoSurface.stop()
            videoSurface.visibility = View.GONE
            playbackToggle.visibility = View.GONE
            preview.visibility = View.VISIBLE

            val viewerCacheKey = thumbnailCacheKey(VIEWER_THUMBNAIL_NAMESPACE, item.contentUri)
            preview.setImageDrawable(null)
            preview.tag = viewerCacheKey
            preview.contentDescription = if (item.mimeType.startsWith("image/")) {
                "Viewer for ${item.displayName}. Swipe left or right to navigate. " +
                    "Pinch to zoom up to 400 percent and drag to pan while zoomed. " +
                    "Use the view-options control for Fit, Fill, and 2 times zoom."
            } else {
                "Viewer for ${item.displayName}. Swipe left or right to navigate the current collection."
            }
            viewerTitle.text = item.displayName
            viewerSubtitle.text = mediaMetadata(item)
            previous.isEnabled = currentIndex > 0
            previous.alpha = if (previous.isEnabled) 1f else 0.30f
            next.isEnabled = currentIndex < items.lastIndex
            next.alpha = if (next.isEnabled) 1f else 0.30f
            val isFavorite = item.contentUri in favoriteUris
            setViewerActionIcon(
                favorite,
                if (isFavorite) R.drawable.ic_gallery_favorite else R.drawable.ic_gallery_favorite_outline,
                selected = isFavorite,
            )
            favorite.contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites"
            favorite.tooltipText = favorite.contentDescription
            val photoEditable = item.mimeType.startsWith("image/")
            edit.isEnabled = photoEditable
            edit.isClickable = photoEditable
            edit.isFocusable = photoEditable
            edit.alpha = if (photoEditable) 1f else 0.35f
            edit.contentDescription = if (photoEditable) {
                "Edit this photo using an Android photo editor"
            } else {
                "Photo editing is unavailable for this media type"
            }
            scaleMode.isEnabled = photoEditable
            scaleMode.isClickable = photoEditable
            scaleMode.isFocusable = photoEditable
            scaleMode.alpha = if (photoEditable) 1f else 0.35f
            applyViewerScaleMode()
            loadLocalThumbnail(item, preview, generation, VIEWER_THUMBNAIL_DP, VIEWER_THUMBNAIL_NAMESPACE)

            if (playbackPlan.presentation == GalleryViewerPresentation.VIDEO_PLAYBACK) {
                try {
                    videoSurface.load(item.contentUri, playbackPlan)
                    videoSurface.visibility = View.VISIBLE
                    playbackToggle.visibility = View.VISIBLE
                    setViewerActionIcon(
                        playbackToggle,
                        if (playbackPlan.shouldAutoPlay) R.drawable.ic_gallery_pause else R.drawable.ic_gallery_play,
                        selected = playbackPlan.shouldAutoPlay,
                    )
                    playbackToggle.contentDescription =
                        if (playbackPlan.shouldAutoPlay) "Pause video" else "Play video"
                    playbackToggle.tooltipText = playbackToggle.contentDescription
                } catch (_: IllegalArgumentException) {
                    videoSurface.visibility = View.GONE
                    playbackToggle.visibility = View.GONE
                    Toast.makeText(this, "Gallery refused an invalid video item URI.", Toast.LENGTH_SHORT).show()
                } catch (_: RuntimeException) {
                    videoSurface.visibility = View.GONE
                    playbackToggle.visibility = View.GONE
                    Toast.makeText(this, "Video playback is unavailable for this item.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        slideshowAdvance = Runnable {
            if (!slideshowRunning || viewerOverlay !== overlay) {
                stopSlideshow()
                return@Runnable
            }
            val nextPhotoIndex = GallerySlideshowPolicy.nextPhotoIndex(
                currentIndex = currentIndex,
                photoEligibility = items.map { it.mimeType.startsWith("image/") },
                repeat = slideshowRepeats,
            )
            if (nextPhotoIndex == null) {
                stopSlideshow(announce = true)
                return@Runnable
            }
            currentIndex = nextPhotoIndex
            renderCurrentItem()
            overlay.postDelayed(
                checkNotNull(slideshowAdvance),
                slideshowInterval.intervalMs,
            )
        }
        viewerSlideshowStop = { stopSlideshow() }
        updateRepeatSlideshowControl()
        updateSlideshowControl()

        repeatSlideshow.setOnClickListener {
            slideshowRepeats = !slideshowRepeats
            updateRepeatSlideshowControl()
            announceForAccessibility(
                if (slideshowRepeats) "Slideshow repeat enabled" else "Slideshow repeat disabled",
            )
        }

        slideshow.setOnClickListener {
            if (slideshowRunning) {
                pauseSlideshow()
                return@setOnClickListener
            }
            val hasLaterPhoto = GallerySlideshowPolicy.nextPhotoIndex(
                currentIndex = currentIndex,
                photoEligibility = items.map { it.mimeType.startsWith("image/") },
                repeat = slideshowRepeats,
            ) != null
            if (!hasLaterPhoto) {
                stopSlideshow()
                Toast.makeText(
                    this,
                    if (slideshowRepeats) {
                        "At least two photos are required to repeat the slideshow."
                    } else {
                        "No later photos are available for slideshow."
                    },
                    Toast.LENGTH_SHORT,
                ).show()
                return@setOnClickListener
            }
            startOrResumeSlideshow()
        }

        scaleMode.setOnClickListener {
            if (!currentItemSupportsZoom()) return@setOnClickListener
            PopupMenu(this, scaleMode).apply {
                menu.add(0, 1, 0, "Fit entire photo")
                menu.add(0, 2, 1, "Fill viewer")
                menu.add(0, 3, 2, "Zoom 2×")
                if (GalleryViewerZoomPolicy.isZoomed(viewerZoomScale)) {
                    menu.add(0, 4, 3, "Reset zoom")
                }
                setOnMenuItemClickListener { item ->
                    stopSlideshow()
                    when (item.itemId) {
                        1 -> {
                            viewerScaleMode = GalleryViewerScaleMode.FIT
                            resetViewerZoom()
                            applyViewerScaleMode()
                            announceForAccessibility("Viewer set to fit")
                        }
                        2 -> {
                            viewerScaleMode = GalleryViewerScaleMode.FILL
                            resetViewerZoom()
                            applyViewerScaleMode()
                            announceForAccessibility("Viewer set to fill")
                        }
                        3 -> {
                            viewerZoomScale = GalleryViewerZoomPolicy.ACCESSIBLE_PRESET_SCALE
                            viewerPanX = 0f
                            viewerPanY = 0f
                            applyViewerTransform()
                            announceForAccessibility("Viewer zoom 200 percent")
                        }
                        4 -> {
                            resetViewerZoom()
                            applyViewerTransform()
                            announceForAccessibility("Viewer zoom reset")
                        }
                        else -> return@setOnMenuItemClickListener false
                    }
                    true
                }
                show()
            }
        }

        playbackToggle.setOnClickListener {
            val plan = activePlaybackPlan ?: return@setOnClickListener
            if (plan.presentation != GalleryViewerPresentation.VIDEO_PLAYBACK || !videoSurface.hasLoadedVideo()) {
                return@setOnClickListener
            }
            if (videoSurface.isPlaying()) {
                videoSurface.pause()
                setViewerActionIcon(playbackToggle, R.drawable.ic_gallery_play)
                playbackToggle.contentDescription = "Play video"
            } else if (videoSurface.play()) {
                setViewerActionIcon(playbackToggle, R.drawable.ic_gallery_pause, selected = true)
                playbackToggle.contentDescription = "Pause video"
            }
            playbackToggle.tooltipText = playbackToggle.contentDescription
        }

        previous.setOnClickListener {
            if (currentIndex > 0) {
                stopSlideshow()
                currentIndex -= 1
                renderCurrentItem()
            }
        }
        next.setOnClickListener {
            if (currentIndex < items.lastIndex) {
                stopSlideshow()
                currentIndex += 1
                renderCurrentItem()
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    if (!currentItemSupportsZoom()) return false
                    stopSlideshow()
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val nextScale = GalleryViewerZoomPolicy.scaleAfterGesture(
                        currentScale = viewerZoomScale,
                        scaleFactor = detector.scaleFactor,
                    )
                    if (nextScale == viewerZoomScale) return true
                    viewerZoomScale = nextScale
                    if (!GalleryViewerZoomPolicy.isZoomed(viewerZoomScale)) {
                        viewerPanX = 0f
                        viewerPanY = 0f
                    }
                    applyViewerTransform()
                    return true
                }
            },
        )

        var swipeStartX = 0f
        var swipeStartY = 0f
        var panLastRawX = 0f
        var panLastRawY = 0f
        var gestureHadMultiplePointers = false
        preview.setOnTouchListener { _, event ->
            if (currentItemSupportsZoom()) {
                scaleGestureDetector.onTouchEvent(event)
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    swipeStartX = event.x
                    swipeStartY = event.y
                    panLastRawX = event.rawX
                    panLastRawY = event.rawY
                    gestureHadMultiplePointers = false
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    gestureHadMultiplePointers = true
                    stopSlideshow()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (
                        currentItemSupportsZoom() &&
                        GalleryViewerZoomPolicy.isZoomed(viewerZoomScale) &&
                        event.pointerCount == 1 &&
                        !scaleGestureDetector.isInProgress
                    ) {
                        stopSlideshow()
                        val bounded = GalleryViewerZoomPolicy.boundedTranslation(
                            viewportWidth = preview.width,
                            viewportHeight = preview.height,
                            scale = viewerZoomScale,
                            proposedX = viewerPanX + (event.rawX - panLastRawX),
                            proposedY = viewerPanY + (event.rawY - panLastRawY),
                        )
                        viewerPanX = bounded.x
                        viewerPanY = bounded.y
                        panLastRawX = event.rawX
                        panLastRawY = event.rawY
                        applyViewerTransform()
                    }
                    true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    gestureHadMultiplePointers = true
                    panLastRawX = event.rawX
                    panLastRawY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (
                        !GalleryViewerZoomPolicy.allowsNavigationSwipe(
                            scale = viewerZoomScale,
                            hadMultiplePointers = gestureHadMultiplePointers,
                        )
                    ) {
                        true
                    } else {
                        when (
                            GalleryViewerSwipePolicy.resolve(
                                deltaX = event.x - swipeStartX,
                                deltaY = event.y - swipeStartY,
                                minimumDistancePx = dp(VIEWER_SWIPE_DISTANCE_DP).toFloat(),
                                canGoPrevious = currentIndex > 0,
                                canGoNext = currentIndex < items.lastIndex,
                            )
                        ) {
                            GalleryViewerSwipeAction.PREVIOUS -> {
                                stopSlideshow()
                                currentIndex -= 1
                                renderCurrentItem()
                                announceForAccessibility("Previous media")
                            }
                            GalleryViewerSwipeAction.NEXT -> {
                                stopSlideshow()
                                currentIndex += 1
                                renderCurrentItem()
                                announceForAccessibility("Next media")
                            }
                            GalleryViewerSwipeAction.NONE -> Unit
                        }
                        true
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    gestureHadMultiplePointers = false
                    false
                }
                else -> true
            }
        }

        share.setOnClickListener {
            val item = items.getOrNull(currentIndex) ?: return@setOnClickListener
            shareAuthorizedItem(item)
        }
        favorite.setOnClickListener {
            val item = items.getOrNull(currentIndex) ?: return@setOnClickListener
            toggleFavorite(item)
            renderCurrentItem()
        }
        edit.setOnClickListener {
            val item = items.getOrNull(currentIndex) ?: return@setOnClickListener
            editAuthorizedPhoto(item)
        }
        if (deleteSupported) {
            delete.setOnClickListener {
                val item = items.getOrNull(currentIndex) ?: return@setOnClickListener
                requestMediaDeletion(listOf(item))
            }
        }
        more.setOnClickListener {
            val item = items.getOrNull(currentIndex) ?: return@setOnClickListener
            showItemDetails(item)
        }

        renderCurrentItem()
    }

    private fun closeAuthorizedViewer() {
        viewerSlideshowStop?.invoke()
        viewerSlideshowStop = null
        val overlay = viewerOverlay ?: return
        viewerVideoSurface?.apply {
            onPlaybackError = null
            stop()
        }
        viewerVideoSurface = null
        rootFrame.removeView(overlay)
        viewerOverlay = null
        applySystemChrome()
        if (mediaRefreshPending) {
            refreshObservedMediaIfReady()
        } else {
            renderCurrentDestination()
        }
    }

    private fun shareAuthorizedItem(item: MediaItem) {
        val uri = Uri.parse(item.contentUri)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(shareIntent, "Share with"))
        } catch (_: RuntimeException) {
            Toast.makeText(this, "No compatible share destination is available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun editAuthorizedPhoto(item: MediaItem) {
        if (!GalleryPhotoEditorContract.isSupportedMimeType(item.mimeType)) {
            Toast.makeText(this, "This photo format is not yet supported by the Gallery editor.", Toast.LENGTH_SHORT).show()
            return
        }
        if (
            !GalleryMediaAccessPolicy.canRead(currentMediaAccessScope()) ||
            authorizedItems.none { it.contentUri == item.contentUri }
        ) {
            Toast.makeText(this, "This photo is no longer authorized for editing.", Toast.LENGTH_SHORT).show()
            closeAuthorizedViewer()
            return
        }

        val uri = Uri.parse(item.contentUri)
        if (uri.scheme != "content" || uri.authority != "media") {
            Toast.makeText(this, "Gallery refused an unsupported photo source.", Toast.LENGTH_SHORT).show()
            return
        }

        val editorIntent = Intent(this, PhotoEditorActivity::class.java).apply {
            putExtra(GalleryPhotoEditorContract.EXTRA_CONTENT_URI, item.contentUri)
            putExtra(GalleryPhotoEditorContract.EXTRA_DISPLAY_NAME, item.displayName)
            putExtra(GalleryPhotoEditorContract.EXTRA_MIME_TYPE, item.mimeType)
        }
        try {
            closeAuthorizedViewer()
            startActivity(editorIntent)
        } catch (_: RuntimeException) {
            Toast.makeText(this, "The Gallery photo editor could not be opened.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFavorite(item: MediaItem) {
        if (item.contentUri in favoriteUris) {
            favoriteUris.remove(item.contentUri)
            Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show()
        } else {
            favoriteUris.add(item.contentUri)
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show()
        }
        persistFavorites()
        updateHeader()
    }

    private fun persistFavorites() {
        galleryPreferences()
            .edit()
            .putStringSet(FAVORITES_KEY, favoriteUris.toSet())
            .apply()
    }

    private fun showItemDetails(item: MediaItem) {
        val dimensions = if (item.width != null && item.height != null) "${item.width} × ${item.height}" else "Unknown"
        val duration = item.durationMillis?.let(::formatDuration) ?: "Not applicable"
        AlertDialog.Builder(this)
            .setTitle(item.displayName)
            .setMessage(
                listOf(
                    "Type: ${if (item.mimeType.startsWith("video/")) "Video" else "Photo"}",
                    "Album: ${item.albumName ?: "Not grouped"}",
                    "Date: ${DATE_TIME_FORMAT.format(item.capturedAt ?: item.modifiedAt)}",
                    "Dimensions: $dimensions",
                    "Duration: $duration",
                    "Size: ${formatBytes(item.sizeBytes)}",
                ).joinToString("\n"),
            )
            .setPositiveButton("Done", null)
            .show()
    }

    private fun showAlbumDetails(album: AlbumPresentation) {
        val collectionType = if (album.isFavorites) "Favorites" else "Local album"
        AlertDialog.Builder(this)
            .setTitle(album.name)
            .setMessage(
                listOf(
                    "Type: $collectionType",
                    "Items: ${album.count}",
                    "Cover: ${album.cover.displayName}",
                ).joinToString("\n"),
            )
            .setPositiveButton("Done", null)
            .show()
    }

    private fun renderSettings() {
        val settings = currentUserSettings()

        library.addView(settingsSectionHeader("Performance"))
        library.addView(
            settingChoiceRow(
                title = "File loading priority",
                subtitle = "Slow uses one thumbnail worker. Fast uses four local thumbnail workers.",
                value = settings.fileLoadingPriority.label,
            ) { showFileLoadingPriorityDialog() },
        )

        library.addView(settingsSectionHeader("Library"))
        library.addView(
            settingChoiceRow(
                title = "Manage included folders",
                subtitle = "Limit Gallery to selected folders from the current Android-authorized snapshot.",
                value = if (settings.includedAlbumIds.isEmpty()) "All" else settings.includedAlbumIds.size.toString(),
            ) { showFolderSelectionDialog(includeMode = true) },
        )
        library.addView(
            settingChoiceRow(
                title = "Manage excluded folders",
                subtitle = "Hide selected folders without changing Android media permission authority.",
                value = if (settings.excludedAlbumIds.isEmpty()) "None" else settings.excludedAlbumIds.size.toString(),
            ) { showFolderSelectionDialog(includeMode = false) },
        )
        library.addView(
            settingToggleRow(
                title = "Show hidden items",
                subtitle = "Shows hidden-looking items only when Android includes them in the authorized MediaStore snapshot.",
                checked = settings.showHiddenItems,
            ) { setBooleanSetting(SHOW_HIDDEN_ITEMS_KEY, it) },
        )

        library.addView(settingsSectionHeader("Playback"))
        library.addView(
            settingToggleRow(
                title = "Play videos automatically",
                subtitle = "When on, videos begin playing automatically when opened in the viewer.",
                checked = settings.playVideosAutomatically,
            ) { setBooleanSetting(PLAY_VIDEOS_AUTOMATICALLY_KEY, it) },
        )
        library.addView(
            settingToggleRow(
                title = "Loop videos",
                subtitle = "When on, videos repeat continuously while they remain open in the viewer.",
                checked = settings.loopVideos,
            ) { setBooleanSetting(LOOP_VIDEOS_KEY, it) },
        )
        val gifAnimationSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        library.addView(
            settingToggleRow(
                title = "Animate GIFs in thumbnails",
                subtitle = if (gifAnimationSupported) {
                    "When on, GIF cards animate while visible. The setting changes presentation only."
                } else {
                    "Requires Android 9 or newer; static GIF thumbnails remain active on this device."
                },
                checked = gifAnimationSupported && settings.animateGifThumbnails,
                enabled = gifAnimationSupported,
            ) { setBooleanSetting(ANIMATE_GIF_THUMBNAILS_KEY, it) },
        )
        library.addView(
            settingChoiceRow(
                title = "Slideshow speed",
                subtitle = "Choose how long each photo remains visible before the slideshow advances.",
                value = settings.slideshowInterval.label,
            ) { showSlideshowIntervalDialog() },
        )
        library.addView(settingsSectionHeader("Deletion & recovery"))
        library.addView(
            settingToggleRow(
                title = "Move deleted items to Trash",
                subtitle = if (AndroidMediaMutationRequests.isSupported()) {
                    "When on, Delete uses Android's Trash confirmation. When off, Android confirms permanent deletion."
                } else {
                    "Saved preference. Android-authorized Trash/Delete requires Android 11 or newer in this Development build."
                },
                checked = settings.moveDeletedItemsToRecycleBin,
            ) { setBooleanSetting(MOVE_DELETED_TO_RECYCLE_BIN_KEY, it) },
        )

        library.addView(settingsSectionHeader("Appearance"))
        library.addView(
            settingChoiceRow(
                title = "View density",
                subtitle = "Choose a dense media grid or a more spacious grid without changing the underlying library.",
                value = settings.viewDensity.label,
            ) { showViewDensityDialog() },
        )
        library.addView(
            settingChoiceRow(
                title = "Bottom navigation",
                subtitle = "Show destination icons, text labels, or both. Icons-only is the default.",
                value = settings.navigationDisplayMode.label,
            ) { showNavigationDisplayModeDialog() },
        )
        library.addView(
            settingChoiceRow(
                title = "Sort media",
                subtitle = "Choose whether local media appears newest first or oldest first. This changes presentation only.",
                value = settings.sortPreference.label,
            ) { showSortPreferenceDialog() },
        )
        library.addView(
            settingChoiceRow(
                title = "Group media by",
                subtitle = "Choose daily, monthly, or yearly sections, or one continuous grid. This changes presentation only.",
                value = settings.groupingMode.label,
            ) { showGroupingModeDialog() },
        )
        if (settings.albumOrderIds.isNotEmpty()) {
            library.addView(
                settingActionRow(
                    title = "Reset album order",
                    subtitle = "Return Albums to the current date-sort order while keeping Pin/Unpin choices.",
                    actionIcon = R.drawable.ic_gallery_reset,
                    actionDescription = "Reset album order",
                ) {
                    galleryPreferences().edit().remove(ALBUM_ORDER_IDS_KEY).apply()
                    Toast.makeText(this, "Album order reset", Toast.LENGTH_SHORT).show()
                    renderSettingsDestinationOnly()
                },
            )
        }
        library.addView(
            settingToggleRow(
                title = "Rounded-square thumbnails",
                subtitle = "Use GoreeCloud rounded-square clipping for media and album thumbnails.",
                checked = settings.roundedSquareThumbnails,
            ) { setBooleanSetting(ROUNDED_SQUARE_THUMBNAILS_KEY, it) },
        )

        library.addView(settingsSectionHeader("Cache"))
        library.addView(
            settingActionRow(
                title = "Clear cache",
                subtitle = "Clears the current in-memory thumbnail cache. Photos and videos are never deleted.",
                actionIcon = R.drawable.ic_gallery_clear_cache,
                    actionDescription = "Clear cache",
            ) {
                thumbnailCache.evictAll()
                Toast.makeText(this, "Thumbnail cache cleared", Toast.LENGTH_SHORT).show()
            },
        )

        library.addView(settingsSectionHeader("Favorites"))
        library.addView(
            settingActionRow(
                title = "Export Favorites",
                subtitle = "Export Gallery's local favorite content-URI list. Media files are not exported.",
                actionIcon = R.drawable.ic_gallery_share,
                    actionDescription = "Export Favorites",
            ) { createJsonDocument(EXPORT_FAVORITES_REQUEST, "GoreeCloud-Gallery-Favorites.json") },
        )
        library.addView(
            settingActionRow(
                title = "Import Favorites",
                subtitle = "Merge a Gallery Favorites export into the local Favorites set without expanding media permission.",
                actionIcon = R.drawable.ic_gallery_download,
                    actionDescription = "Import Favorites",
            ) { openJsonDocument(IMPORT_FAVORITES_REQUEST) },
        )

        library.addView(settingsSectionHeader("Settings portability"))
        library.addView(
            settingActionRow(
                title = "Export settings",
                subtitle = "Export non-secret Gallery preferences, including folder visibility selections.",
                actionIcon = R.drawable.ic_gallery_share,
                    actionDescription = "Export settings",
            ) { createJsonDocument(EXPORT_SETTINGS_REQUEST, "GoreeCloud-Gallery-Settings.json") },
        )
        library.addView(
            settingActionRow(
                title = "Import settings",
                subtitle = "Import a compatible GoreeCloud Gallery settings file. Unknown fields are ignored.",
                actionIcon = R.drawable.ic_gallery_download,
                    actionDescription = "Import settings",
            ) { openJsonDocument(IMPORT_SETTINGS_REQUEST) },
        )

        library.addView(settingsSectionHeader("Guidance"))
        library.addView(
            settingToggleRow(
                title = "Contextual hints",
                subtitle = "Show optional Gallery tips. Permission, privacy, destructive-action, error, and required system messages remain visible.",
                checked = settings.contextualHintsEnabled,
            ) { enabled ->
                GallerySetupPreferences(this).setContextualHintsEnabled(enabled)
            },
        )
        library.addView(
            settingActionRow(
                title = "Reset dismissed hints",
                subtitle = "Show Gallery tips you previously dismissed without changing onboarding or other settings.",
                actionIcon = R.drawable.ic_gallery_reset,
                    actionDescription = "Reset dismissed hints",
            ) {
                GallerySetupPreferences(this).resetDismissedContextualHints()
                Toast.makeText(this, "Dismissed Gallery hints reset", Toast.LENGTH_SHORT).show()
            },
        )
        library.addView(
            settingActionRow(
                title = "Replay setup",
                subtitle = "Review first-use guidance without resetting favorites, media access, or Gallery preferences.",
                actionIcon = R.drawable.ic_gallery_play,
                    actionDescription = "Replay setup",
            ) { showSetupWizard(replay = true, requestedStep = 0) },
        )
    }

    private fun showSetupWizard(
        replay: Boolean,
        requestedStep: Int? = null,
    ) {
        val setupPreferences = GallerySetupPreferences(this)
        val step = (
            requestedStep ?: if (replay) 0 else setupPreferences.onboardingStep()
        ).coerceIn(0, GallerySetupPreferences.STEP_COUNT - 1)

        if (!replay) {
            setupPreferences.setOnboardingStep(step)
        }

        setupDialog?.dismiss()
        setupDialog = null

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(14))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.OVERLAY,
                GalleryGlazeContract.SHAPE_OVERLAY_DP,
            )
        }

        panel.addView(TextView(this).apply {
            text = if (replay) "Review Gallery setup" else "Set up Gallery"
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, Typeface.BOLD)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        })
        panel.addView(TextView(this).apply {
            text = "Step ${step + 1} of ${GallerySetupPreferences.STEP_COUNT}"
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(3), 0, dp(6))
        })
        panel.addView(
            setupProgressRail(step),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(6),
            ).apply {
                bottomMargin = dp(12)
            },
        )

        val title: String
        val body: String
        when (step) {
            0 -> {
                title = "Your local media library"
                body =
                    "Use the bottom bar for Photos, Albums, Videos, Trash, and Settings. Navigation starts icon-only; change it in Settings > Appearance."
            }
            1 -> {
                title = "You control media access"
                body =
                    "Gallery reads only media Android grants, and browsing stays local. Cloud backup and Protected Photos are not active in this Development build."
            }
            else -> {
                title = "Choose your guidance"
                body =
                    "Optional tips explain useful gestures and actions. Turn them off now or later in Settings."
            }
        }

        panel.addView(TextView(this).apply {
            text = title
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
        })
        panel.addView(TextView(this).apply {
            text = body
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setLineSpacing(0f, 1.05f)
            setPadding(0, dp(5), 0, dp(12))
        })

        if (step == 2) {
            val hintsEnabled = setupPreferences.contextualHintsEnabled()
            panel.addView(
                settingBaseRow(
                    title = "Contextual hints",
                    subtitle = "Optional tips only. Privacy, permission, destructive-action, error, and system messages remain visible.",
                    enabled = true,
                    trailing = settingsToggleIndicator(
                        checked = hintsEnabled,
                        enabled = true,
                    ),
                    stateDescriptionText = if (hintsEnabled) "On" else "Off",
                ) {
                    setupPreferences.setContextualHintsEnabled(!hintsEnabled)
                    showSetupWizard(replay = replay, requestedStep = step)
                },
            )
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }

        if (step > 0) {
            controls.addView(
                setupIconAction(
                    R.drawable.ic_gallery_chevron_left,
                    "Back in Gallery setup",
                    emphasized = false,
                ) {
                    if (!replay) setupPreferences.setOnboardingStep(step - 1)
                    showSetupWizard(replay = replay, requestedStep = step - 1)
                },
                LinearLayout.LayoutParams(
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                ).apply {
                    marginEnd = dp(6)
                },
            )
        } else if (replay) {
            controls.addView(
                setupIconAction(
                    R.drawable.ic_gallery_close,
                    "Return to Gallery",
                    emphasized = false,
                ) {
                    setupDialog?.dismiss()
                    setupDialog = null
                },
                LinearLayout.LayoutParams(
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                    dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                ).apply {
                    marginEnd = dp(6)
                },
            )
        }

        val last = step == GallerySetupPreferences.STEP_COUNT - 1
        controls.addView(
            setupIconAction(
                if (last) R.drawable.ic_gallery_check_white else R.drawable.ic_gallery_chevron_right,
                if (last) "Finish Gallery setup" else "Continue Gallery setup",
                emphasized = true,
            ) {
                if (last) {
                    if (!replay) {
                        setupPreferences.completeSetup()
                    }
                    setupDialog?.dismiss()
                    setupDialog = null
                    renderCurrentDestination()
                } else {
                    if (!replay) setupPreferences.setOnboardingStep(step + 1)
                    showSetupWizard(replay = replay, requestedStep = step + 1)
                }
            },
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ),
        )
        panel.addView(
            controls,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        setupDialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()
            .also { dialog ->
                dialog.setCancelable(replay)
                dialog.setCanceledOnTouchOutside(false)
                dialog.setOnDismissListener {
                    if (setupDialog === dialog) setupDialog = null
                }
                dialog.setOnShowListener {
                    dialog.window?.setBackgroundDrawable(
                        android.graphics.drawable.ColorDrawable(Color.TRANSPARENT),
                    )
                    dialog.window?.setDimAmount(0.42f)
                    val dialogWidth = (resources.displayMetrics.widthPixels - dp(24))
                        .coerceAtMost(dp(560))
                    dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                dialog.show()
            }
    }

    private fun dialogDismissAction(
        description: String,
        onClick: () -> Unit,
    ): ImageView = ImageView(this).apply {
        setImageResource(R.drawable.ic_gallery_close)
        setColorFilter(accentColor())
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setPadding(dp(13), dp(13), dp(13), dp(13))
        background = roundedSurface(withAlpha(accentColor(), 0.08f), 16)
        isClickable = true
        isFocusable = true
        contentDescription = description
        tooltipText = description
        GalleryInteractionFeedback.applyBoundedRipple(this, accentColor(), 16)
        setOnClickListener { onClick() }
    }

    private fun setupProgressRail(step: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        repeat(GallerySetupPreferences.STEP_COUNT) { index ->
            addView(
                View(context).apply {
                    background = roundedSurface(
                        if (index <= step) accentColor() else withAlpha(primaryTextColor(), 0.10f),
                        3,
                    )
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                LinearLayout.LayoutParams(0, dp(4), 1f).apply {
                    if (index > 0) marginStart = dp(6)
                },
            )
        }
    }

    private fun setupIconAction(
        iconResource: Int,
        description: String,
        emphasized: Boolean,
        onClick: () -> Unit,
    ): ImageView = ImageView(this).apply {
        setImageResource(iconResource)
        setColorFilter(if (emphasized) accentColor() else primaryTextColor())
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setPadding(dp(13), dp(13), dp(13), dp(13))
        background = roundedSurface(
            if (emphasized) withAlpha(accentColor(), 0.14f)
            else withAlpha(primaryTextColor(), if (isNightMode()) 0.10f else 0.055f),
            GalleryGlazeContract.SHAPE_CONTROL_DP,
        )
        isClickable = true
        isFocusable = true
        contentDescription = description
        tooltipText = description
        GalleryInteractionFeedback.applyBoundedRipple(
            this,
            if (emphasized) accentColor() else primaryTextColor(),
            GalleryGlazeContract.SHAPE_CONTROL_DP,
        )
        setOnClickListener { onClick() }
    }

    private fun settingsSectionHeader(label: String): TextView = TextView(this).apply {
        text = label
        setTextColor(primaryTextColor())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(2), dp(14), 0, dp(5))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun settingChoiceRow(
        title: String,
        subtitle: String,
        value: String,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ): LinearLayout = settingBaseRow(
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        trailing = settingsPill(value, emphasized = false),
        stateDescriptionText = value,
        onClick = onClick,
    )

    private fun settingActionRow(
        title: String,
        subtitle: String,
        actionIcon: Int,
        actionDescription: String,
        onClick: () -> Unit,
    ): LinearLayout = settingBaseRow(
        title = title,
        subtitle = subtitle,
        enabled = true,
        trailing = settingsIconPill(actionIcon, actionDescription),
        onClick = onClick,
    )

    private fun settingToggleRow(
        title: String,
        subtitle: String,
        checked: Boolean,
        enabled: Boolean = true,
        onToggle: (Boolean) -> Unit,
    ): LinearLayout = settingBaseRow(
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        trailing = settingsToggleIndicator(
            checked = checked,
            enabled = enabled,
        ),
        stateDescriptionText = if (!enabled) "Unavailable" else if (checked) "On" else "Off",
    ) {
        onToggle(!checked)
        renderSettingsDestinationOnly()
    }

    private fun settingBaseRow(
        title: String,
        subtitle: String,
        enabled: Boolean,
        trailing: View,
        stateDescriptionText: String? = null,
        onClick: () -> Unit,
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(60)
            setPadding(dp(12), dp(7), dp(7), dp(7))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.RAISED,
                GalleryGlazeContract.SHAPE_CONTAINER_DP,
            )
            alpha = if (enabled) 1f else 0.55f

            val labels = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
            }
            labels.addView(TextView(context).apply {
                text = title
                setTextColor(primaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                setTypeface(typeface, Typeface.BOLD)
            })
            labels.addView(TextView(context).apply {
                text = subtitle
                setTextColor(secondaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setLineSpacing(0f, 1.04f)
                setPadding(0, dp(2), dp(7), 0)
            })
            addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(trailing)

            isClickable = enabled
            isFocusable = enabled
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = "$title. $subtitle"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                stateDescription = stateDescriptionText
            }
            if (enabled) {
                GalleryInteractionFeedback.applyBoundedRipple(
                    this,
                    accentColor(),
                    GalleryGlazeContract.SHAPE_CONTAINER_DP,
                )
                setOnClickListener { onClick() }
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(3)
            }
        }
    }

    private fun settingsIconPill(iconResource: Int, description: String): TextView = TextView(this).apply {
        text = ""
        gravity = Gravity.CENTER
        minWidth = dp(44)
        minHeight = dp(40)
        setPadding(dp(10), dp(8), dp(10), dp(8))
        setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0)
        compoundDrawableTintList = ColorStateList.valueOf(accentColor())
        background = roundedSurface(withAlpha(accentColor(), 0.13f), 16)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        tooltipText = description
    }

    private fun settingsPill(label: String, emphasized: Boolean): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER_VERTICAL or Gravity.END
        minWidth = dp(44)
        minHeight = dp(32)
        setPadding(dp(6), 0, dp(2), 0)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.25f)
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(if (emphasized) accentColor() else primaryTextColor())
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_gallery_chevron_right, 0)
        compoundDrawableTintList = ColorStateList.valueOf(
            if (emphasized) accentColor() else secondaryTextColor(),
        )
        compoundDrawablePadding = dp(2)
        background = null
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun settingsToggleIndicator(
        checked: Boolean,
        enabled: Boolean,
    ): ImageView = ImageView(this).apply {
        setImageResource(
            if (checked) R.drawable.ic_gallery_toggle_on else R.drawable.ic_gallery_toggle_off,
        )
        setColorFilter(
            when {
                !enabled -> secondaryTextColor()
                checked -> accentColor()
                else -> secondaryTextColor()
            },
        )
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        minimumWidth = dp(48)
        minimumHeight = dp(32)
        setPadding(dp(7), dp(6), dp(7), dp(6))
        background = null
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun renderSettingsDestinationOnly() {
        if (destination != GalleryDestination.SETTINGS) return
        updateHeader()
        library.removeAllViews()
        renderSettings()
    }

    private fun showFileLoadingPriorityDialog() {
        val current = currentUserSettings().fileLoadingPriority
        var dialog: AlertDialog? = null
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(14))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.OVERLAY,
                GalleryGlazeContract.SHAPE_OVERLAY_DP,
            )
        }
        panel.addView(TextView(this).apply {
            text = "File loading priority"
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
        })
        panel.addView(TextView(this).apply {
            text = "Choose how many local workers Gallery uses for thumbnail loading."
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setLineSpacing(0f, 1.06f)
            setPadding(0, dp(4), 0, dp(14))
        })

        GalleryFileLoadingPriority.entries.forEach { priority ->
            val selected = priority == current
            panel.addView(
                glazeDialogChoiceRow(
                    title = priority.label,
                    subtitle = when (priority) {
                        GalleryFileLoadingPriority.SLOW -> "1 local thumbnail worker"
                        GalleryFileLoadingPriority.FAST -> "4 local thumbnail workers"
                    },
                    selected = selected,
                ) {
                    galleryPreferences().edit().putString(FILE_LOADING_PRIORITY_KEY, priority.storedValue).apply()
                    reconfigureThumbnailExecutor(priority)
                    renderSettingsDestinationOnly()
                    dialog?.dismiss()
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(7)
                },
            )
        }

        panel.addView(
            dialogDismissAction("Cancel file loading priority selection") { dialog?.dismiss() },
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ).apply {
                gravity = Gravity.END
            },
        )

        dialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()
        dialog?.setOnShowListener {
            dialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.setDimAmount(0.42f)
            dialog?.window?.setLayout(
                resources.displayMetrics.widthPixels - dp(32),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        dialog?.show()
        dialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setDimAmount(0.42f)
        dialog?.window?.setLayout(
            resources.displayMetrics.widthPixels - dp(32),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun showNavigationDisplayModeDialog() {
        val current = currentUserSettings().navigationDisplayMode
        var dialog: AlertDialog? = null
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(14))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.OVERLAY,
                GalleryGlazeContract.SHAPE_OVERLAY_DP,
            )
        }
        panel.addView(TextView(this).apply {
            text = "Bottom navigation"
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
        })
        panel.addView(TextView(this).apply {
            text = "Choose how the five primary Gallery destinations are shown."
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setLineSpacing(0f, 1.06f)
            setPadding(0, dp(4), 0, dp(14))
        })

        GalleryNavigationDisplayMode.entries.forEach { mode ->
            panel.addView(
                glazeDialogChoiceRow(
                    title = mode.label,
                    subtitle = when (mode) {
                        GalleryNavigationDisplayMode.ICONS_ONLY ->
                            "Compact glyph-only navigation with accessible destination names"
                        GalleryNavigationDisplayMode.TEXT_ONLY ->
                            "Text destination names without visible glyphs"
                        GalleryNavigationDisplayMode.ICONS_AND_TEXT ->
                            "Show both destination glyphs and labels"
                    },
                    selected = mode == current,
                ) {
                    galleryPreferences().edit()
                        .putString(NAVIGATION_DISPLAY_MODE_KEY, mode.storedValue)
                        .apply()
                    renderNavigation()
                    renderSettingsDestinationOnly()
                    dialog?.dismiss()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = dp(7)
                },
            )
        }

        panel.addView(
            dialogDismissAction("Cancel bottom navigation display selection") { dialog?.dismiss() },
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ).apply {
                gravity = Gravity.END
            },
        )

        dialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()
        dialog?.setOnShowListener {
            dialog?.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(Color.TRANSPARENT),
            )
            dialog?.window?.setDimAmount(0.42f)
            dialog?.window?.setLayout(
                resources.displayMetrics.widthPixels - dp(32),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        dialog?.show()
        dialog?.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(Color.TRANSPARENT),
        )
        dialog?.window?.setDimAmount(0.42f)
        dialog?.window?.setLayout(
            resources.displayMetrics.widthPixels - dp(32),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun showViewDensityDialog() {
        val current = currentUserSettings().viewDensity
        var dialog: AlertDialog? = null
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(14))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.OVERLAY,
                GalleryGlazeContract.SHAPE_OVERLAY_DP,
            )
        }
        panel.addView(TextView(this).apply {
            text = "View density"
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
        })
        panel.addView(TextView(this).apply {
            text = "Adjust how many media tiles appear per row. This changes presentation only."
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setLineSpacing(0f, 1.06f)
            setPadding(0, dp(4), 0, dp(14))
        })

        GalleryViewDensity.entries.forEach { density ->
            val selected = density == current
            panel.addView(
                glazeDialogChoiceRow(
                    title = density.label,
                    subtitle = when (density) {
                        GalleryViewDensity.DENSE -> "Use the adaptive Gallery grid baseline"
                        GalleryViewDensity.COMFORTABLE -> "Show one fewer tile per row for larger previews"
                        GalleryViewDensity.SPACIOUS -> "Show two fewer tiles per row for the largest previews"
                    },
                    selected = selected,
                ) {
                    galleryPreferences().edit()
                        .putString(VIEW_DENSITY_KEY, density.storedValue)
                        .apply()
                    renderSettingsDestinationOnly()
                    dialog?.dismiss()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = dp(7)
                },
            )
        }

        panel.addView(
            dialogDismissAction("Cancel view-density selection") { dialog?.dismiss() },
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ).apply {
                gravity = Gravity.END
            },
        )

        dialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()
        dialog?.setOnShowListener {
            dialog?.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(Color.TRANSPARENT),
            )
            dialog?.window?.setDimAmount(0.42f)
            dialog?.window?.setLayout(
                resources.displayMetrics.widthPixels - dp(32),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        dialog?.show()
        dialog?.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(Color.TRANSPARENT),
        )
        dialog?.window?.setDimAmount(0.42f)
        dialog?.window?.setLayout(
            resources.displayMetrics.widthPixels - dp(32),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun showSlideshowIntervalDialog() {
        val current = currentUserSettings().slideshowInterval
        var dialog: AlertDialog? = null
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(14))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.OVERLAY,
                GalleryGlazeContract.SHAPE_OVERLAY_DP,
            )
        }
        panel.addView(TextView(this).apply {
            text = "Slideshow speed"
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
        })
        panel.addView(TextView(this).apply {
            text = "Choose the delay between photos. This is a local presentation preference and does not change media files."
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setLineSpacing(0f, 1.06f)
            setPadding(0, dp(4), 0, dp(14))
        })

        GallerySlideshowInterval.entries.forEach { interval ->
            panel.addView(
                glazeDialogChoiceRow(
                    title = interval.label,
                    subtitle = when (interval) {
                        GallerySlideshowInterval.FAST -> "Move quickly through the current authorized photo collection"
                        GallerySlideshowInterval.NORMAL -> "Use the Gallery default slideshow pace"
                        GallerySlideshowInterval.RELAXED -> "Keep each photo visible longer"
                    },
                    selected = interval == current,
                ) {
                    galleryPreferences().edit()
                        .putString(SLIDESHOW_INTERVAL_KEY, interval.storedValue)
                        .apply()
                    renderSettingsDestinationOnly()
                    dialog?.dismiss()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = dp(7)
                },
            )
        }

        panel.addView(
            dialogDismissAction("Cancel slideshow speed selection") { dialog?.dismiss() },
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ).apply {
                gravity = Gravity.END
            },
        )

        dialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()
        dialog?.setOnShowListener {
            dialog?.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(Color.TRANSPARENT),
            )
            dialog?.window?.setDimAmount(0.42f)
            dialog?.window?.setLayout(
                resources.displayMetrics.widthPixels - dp(32),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        dialog?.show()
        dialog?.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(Color.TRANSPARENT),
        )
        dialog?.window?.setDimAmount(0.42f)
        dialog?.window?.setLayout(
            resources.displayMetrics.widthPixels - dp(32),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun showSortPreferenceDialog() {
        val current = currentUserSettings().sortPreference
        var dialog: AlertDialog? = null
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(14))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.OVERLAY,
                GalleryGlazeContract.SHAPE_OVERLAY_DP,
            )
        }
        panel.addView(TextView(this).apply {
            text = "Sort media"
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
        })
        panel.addView(TextView(this).apply {
            text = "Choose the local timeline order. The setting is app-private and does not modify media files."
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setLineSpacing(0f, 1.06f)
            setPadding(0, dp(4), 0, dp(14))
        })

        GallerySortPreference.entries.forEach { preference ->
            panel.addView(
                glazeDialogChoiceRow(
                    title = preference.label,
                    subtitle = when (preference) {
                        GallerySortPreference.NEWEST -> "Show the most recent authorized media first"
                        GallerySortPreference.OLDEST -> "Show the earliest authorized media first"
                    },
                    selected = preference == current,
                ) {
                    galleryPreferences().edit()
                        .putString(SORT_PREFERENCE_KEY, preference.storedValue)
                        .apply()
                    selectedSort = preference.mediaSortOrder
                    renderSettingsDestinationOnly()
                    dialog?.dismiss()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = dp(7)
                },
            )
        }

        panel.addView(
            dialogDismissAction("Cancel media sort selection") { dialog?.dismiss() },
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ).apply {
                gravity = Gravity.END
            },
        )

        dialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()
        dialog?.setOnShowListener {
            dialog?.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(Color.TRANSPARENT),
            )
            dialog?.window?.setDimAmount(0.42f)
            dialog?.window?.setLayout(
                resources.displayMetrics.widthPixels - dp(32),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        dialog?.show()
        dialog?.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(Color.TRANSPARENT),
        )
        dialog?.window?.setDimAmount(0.42f)
        dialog?.window?.setLayout(
            resources.displayMetrics.widthPixels - dp(32),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun showGroupingModeDialog() {
        val current = currentUserSettings().groupingMode
        var dialog: AlertDialog? = null
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(14))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.OVERLAY,
                GalleryGlazeContract.SHAPE_OVERLAY_DP,
            )
        }
        panel.addView(TextView(this).apply {
            text = "Group media by"
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
        })
        panel.addView(TextView(this).apply {
            text = "Choose how the authorized local media grid is divided into presentation sections."
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setLineSpacing(0f, 1.06f)
            setPadding(0, dp(4), 0, dp(14))
        })

        GalleryGroupingMode.entries.forEach { mode ->
            panel.addView(
                glazeDialogChoiceRow(
                    title = mode.label,
                    subtitle = when (mode) {
                        GalleryGroupingMode.DAY -> "Keep Today, Yesterday, and calendar-day sections"
                        GalleryGroupingMode.MONTH -> "Group the same media into month sections"
                        GalleryGroupingMode.YEAR -> "Group the same media into year sections"
                        GalleryGroupingMode.NONE -> "Show one continuous grid without section headers"
                    },
                    selected = mode == current,
                ) {
                    galleryPreferences().edit()
                        .putString(GROUPING_MODE_KEY, mode.storedValue)
                        .apply()
                    renderSettingsDestinationOnly()
                    dialog?.dismiss()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = dp(7)
                },
            )
        }

        panel.addView(
            dialogDismissAction("Cancel media grouping selection") { dialog?.dismiss() },
            LinearLayout.LayoutParams(
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
                dp(GalleryGlazeContract.GENERAL_TARGET_DP),
            ).apply {
                gravity = Gravity.END
            },
        )

        dialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()
        dialog?.setOnShowListener {
            dialog?.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(Color.TRANSPARENT),
            )
            dialog?.window?.setDimAmount(0.42f)
            dialog?.window?.setLayout(
                resources.displayMetrics.widthPixels - dp(32),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        dialog?.show()
        dialog?.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(Color.TRANSPARENT),
        )
        dialog?.window?.setDimAmount(0.42f)
        dialog?.window?.setLayout(
            resources.displayMetrics.widthPixels - dp(32),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun glazeDialogChoiceRow(
        title: String,
        subtitle: String,
        selected: Boolean,
        onClick: () -> Unit,
    ): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(64)
        setPadding(dp(14), dp(9), dp(10), dp(9))
        background = if (selected) {
            roundedSurface(withAlpha(accentColor(), 0.13f), GalleryGlazeContract.SHAPE_CONTAINER_DP)
        } else {
            GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.RAISED,
                GalleryGlazeContract.SHAPE_CONTAINER_DP,
            )
        }
        val labels = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        labels.addView(TextView(context).apply {
            text = title
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
        })
        labels.addView(TextView(context).apply {
            text = subtitle
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setPadding(0, dp(2), dp(8), 0)
        })
        addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(
            ImageView(context).apply {
                if (selected) {
                    setImageResource(R.drawable.ic_gallery_check_white)
                    setColorFilter(Color.WHITE)
                }
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(6), dp(6), dp(6), dp(6))
                background = roundedSurface(
                    if (selected) accentColor() else withAlpha(primaryTextColor(), 0.06f),
                    15,
                )
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(dp(30), dp(30)),
        )
        isClickable = true
        isFocusable = true
        isSelected = selected
        contentDescription = "$title. $subtitle.${if (selected) " Selected." else ""}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            stateDescription = if (selected) "Selected" else null
        }
        GalleryInteractionFeedback.applyBoundedRipple(
            this,
            if (selected) accentColor() else primaryTextColor(),
            GalleryGlazeContract.SHAPE_CONTAINER_DP,
        )
        setOnClickListener { onClick() }
    }

    private fun showFolderSelectionDialog(includeMode: Boolean) {
        val albums = authorizedItems.buildAlbumCatalog().sortedBy { it.displayName.lowercase() }
        if (albums.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(if (includeMode) "Included folders" else "Excluded folders")
                .setMessage(
                    "No authorized folders are available in the current Gallery snapshot. Grant media access from Photos first, or return after the library has loaded.",
                )
                .setPositiveButton("Done", null)
                .show()
            return
        }

        val key = if (includeMode) INCLUDED_ALBUM_IDS_KEY else EXCLUDED_ALBUM_IDS_KEY
        val selected = galleryPreferences().getStringSet(key, emptySet()).orEmpty().toMutableSet()
        val labels = albums.map { "${it.displayName} · ${itemCountLabel(it.itemCount)}" }.toTypedArray()
        val checked = BooleanArray(albums.size) { albums[it].id in selected }

        AlertDialog.Builder(this)
            .setTitle(if (includeMode) "Manage included folders" else "Manage excluded folders")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val albumId = albums[which].id
                if (isChecked) selected.add(albumId) else selected.remove(albumId)
            }
            .setPositiveButton("Save") { _, _ ->
                galleryPreferences().edit().putStringSet(key, selected.toSet()).apply()
                renderSettingsDestinationOnly()
            }
            .setNeutralButton("Clear") { _, _ ->
                galleryPreferences().edit().remove(key).apply()
                renderSettingsDestinationOnly()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setBooleanSetting(key: String, value: Boolean) {
        galleryPreferences().edit().putBoolean(key, value).apply()
    }

    private fun currentUserSettings(): GalleryUserSettings {
        val preferences = galleryPreferences()
        return GalleryUserSettings(
            fileLoadingPriority = GalleryFileLoadingPriority.fromStored(
                preferences.getString(FILE_LOADING_PRIORITY_KEY, GalleryFileLoadingPriority.FAST.storedValue),
            ),
            viewDensity = GalleryViewDensity.fromStored(
                preferences.getString(VIEW_DENSITY_KEY, GalleryViewDensity.DENSE.storedValue),
            ),
            navigationDisplayMode = GalleryNavigationDisplayMode.fromStored(
                preferences.getString(
                    NAVIGATION_DISPLAY_MODE_KEY,
                    GalleryNavigationDisplayMode.ICONS_ONLY.storedValue,
                ),
            ),
            groupingMode = GalleryGroupingMode.fromStored(
                preferences.getString(GROUPING_MODE_KEY, GalleryGroupingMode.DAY.storedValue),
            ),
            sortPreference = GallerySortPreference.fromStored(
                preferences.getString(SORT_PREFERENCE_KEY, GallerySortPreference.NEWEST.storedValue),
            ),
            pinnedAlbumIds = preferences.getStringSet(PINNED_ALBUM_IDS_KEY, emptySet()).orEmpty().toSet(),
            albumOrderIds = preferences.getString(ALBUM_ORDER_IDS_KEY, null)
                ?.let { encoded ->
                    runCatching { jsonStringList(JSONArray(encoded)) }.getOrDefault(emptyList())
                }
                ?: emptyList(),
            includedAlbumIds = preferences.getStringSet(INCLUDED_ALBUM_IDS_KEY, emptySet()).orEmpty().toSet(),
            excludedAlbumIds = preferences.getStringSet(EXCLUDED_ALBUM_IDS_KEY, emptySet()).orEmpty().toSet(),
            showHiddenItems = preferences.getBoolean(SHOW_HIDDEN_ITEMS_KEY, false),
            playVideosAutomatically = preferences.getBoolean(PLAY_VIDEOS_AUTOMATICALLY_KEY, false),
            loopVideos = preferences.getBoolean(LOOP_VIDEOS_KEY, false),
            slideshowInterval = GallerySlideshowInterval.fromStored(
                preferences.getString(SLIDESHOW_INTERVAL_KEY, GallerySlideshowInterval.NORMAL.storedValue),
            ),
            animateGifThumbnails = preferences.getBoolean(ANIMATE_GIF_THUMBNAILS_KEY, false),
            deleteEmptyFolders = preferences.getBoolean(DELETE_EMPTY_FOLDERS_KEY, false),
            moveDeletedItemsToRecycleBin = preferences.getBoolean(MOVE_DELETED_TO_RECYCLE_BIN_KEY, true),
            roundedSquareThumbnails = preferences.getBoolean(ROUNDED_SQUARE_THUMBNAILS_KEY, true),
            contextualHintsEnabled = GallerySetupPreferences(this).contextualHintsEnabled(),
        )
    }

    private fun visibleAuthorizedItems(): List<MediaItem> =
        GallerySettingsPolicy.visibleItems(authorizedItems, currentUserSettings())

    private fun thumbnailCornerDp(defaultCornerDp: Int): Int =
        if (currentUserSettings().roundedSquareThumbnails) defaultCornerDp else 0

    private fun reconfigureThumbnailExecutor(priority: GalleryFileLoadingPriority) {
        val desiredWorkers = priority.thumbnailWorkerCount
        if (desiredWorkers == thumbnailWorkerCount) return
        thumbnailExecutor.shutdownNow()
        thumbnailWorkerCount = desiredWorkers
        thumbnailExecutor = Executors.newFixedThreadPool(thumbnailWorkerCount)
    }

    private fun createJsonDocument(requestCode: Int, suggestedName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, suggestedName)
        }
        try {
            startActivityForResult(intent, requestCode)
        } catch (_: RuntimeException) {
            Toast.makeText(this, "No document provider is available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openJsonDocument(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        try {
            startActivityForResult(intent, requestCode)
        } catch (_: RuntimeException) {
            Toast.makeText(this, "No document provider is available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeJsonDocument(uri: Uri, json: JSONObject, successMessage: String) {
        try {
            val stream = contentResolver.openOutputStream(uri) ?: throw IOException("Unable to open output document")
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(json.toString(2)) }
            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "The export could not be written.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readJsonDocument(uri: Uri, consume: (JSONObject) -> Unit) {
        try {
            val stream = contentResolver.openInputStream(uri) ?: throw IOException("Unable to open input document")
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            consume(JSONObject(text))
        } catch (_: Exception) {
            Toast.makeText(this, "The selected Gallery file could not be imported.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildFavoritesExportJson(): JSONObject {
        val favorites = JSONArray()
        favoriteUris.sorted().forEach { favorites.put(it) }
        return JSONObject()
            .put("type", FAVORITES_EXPORT_TYPE)
            .put("schemaVersion", GallerySettingsPolicy.EXPORT_SCHEMA_VERSION)
            .put("favorites", favorites)
    }

    private fun importFavorites(json: JSONObject) {
        if (json.optString("type") != FAVORITES_EXPORT_TYPE) {
            throw IllegalArgumentException("Not a GoreeCloud Gallery Favorites export")
        }
        val array = json.getJSONArray("favorites")
        val before = favoriteUris.size
        for (index in 0 until array.length()) {
            val uri = array.optString(index).trim()
            if (uri.isNotBlank()) favoriteUris.add(uri)
        }
        persistFavorites()
        val added = favoriteUris.size - before
        Toast.makeText(
            this,
            if (added == 1) "Imported 1 new Favorite" else "Imported $added new Favorites",
            Toast.LENGTH_SHORT,
        ).show()
        updateHeader()
        renderSettingsDestinationOnly()
    }

    private fun buildSettingsExportJson(): JSONObject {
        val settings = currentUserSettings()
        return JSONObject()
            .put("type", SETTINGS_EXPORT_TYPE)
            .put("schemaVersion", GallerySettingsPolicy.EXPORT_SCHEMA_VERSION)
            .put("fileLoadingPriority", settings.fileLoadingPriority.storedValue)
            .put("viewDensity", settings.viewDensity.storedValue)
            .put("navigationDisplayMode", settings.navigationDisplayMode.storedValue)
            .put("groupingMode", settings.groupingMode.storedValue)
            .put("sortPreference", settings.sortPreference.storedValue)
            .put("pinnedAlbumIds", stringSetJson(settings.pinnedAlbumIds))
            .put("albumOrderIds", stringListJson(settings.albumOrderIds))
            .put("includedAlbumIds", stringSetJson(settings.includedAlbumIds))
            .put("excludedAlbumIds", stringSetJson(settings.excludedAlbumIds))
            .put("showHiddenItems", settings.showHiddenItems)
            .put("playVideosAutomatically", settings.playVideosAutomatically)
            .put("loopVideos", settings.loopVideos)
            .put("slideshowInterval", settings.slideshowInterval.storedValue)
            .put("animateGifThumbnails", settings.animateGifThumbnails)
            .put("deleteEmptyFolders", settings.deleteEmptyFolders)
            .put("moveDeletedItemsToRecycleBin", settings.moveDeletedItemsToRecycleBin)
            .put("roundedSquareThumbnails", settings.roundedSquareThumbnails)
            .put("contextualHintsEnabled", settings.contextualHintsEnabled)
    }

    private fun importSettings(json: JSONObject) {
        if (json.optString("type") != SETTINGS_EXPORT_TYPE) {
            throw IllegalArgumentException("Not a GoreeCloud Gallery settings export")
        }
        val current = currentUserSettings()
        val rawPriority = json.optString("fileLoadingPriority", current.fileLoadingPriority.storedValue)
        val importedPriority = GalleryFileLoadingPriority.entries.firstOrNull { it.storedValue == rawPriority }
            ?: throw IllegalArgumentException("Unsupported loading priority")
        val rawDensity = json.optString("viewDensity", current.viewDensity.storedValue)
        val importedDensity = GalleryViewDensity.entries.firstOrNull { it.storedValue == rawDensity }
            ?: throw IllegalArgumentException("Unsupported view density")
        val rawNavigationDisplayMode = json.optString(
            "navigationDisplayMode",
            current.navigationDisplayMode.storedValue,
        )
        val importedNavigationDisplayMode =
            GalleryNavigationDisplayMode.entries.firstOrNull {
                it.storedValue == rawNavigationDisplayMode
            } ?: throw IllegalArgumentException("Unsupported navigation display mode")
        val rawGrouping = json.optString("groupingMode", current.groupingMode.storedValue)
        val importedGrouping = GalleryGroupingMode.entries.firstOrNull { it.storedValue == rawGrouping }
            ?: throw IllegalArgumentException("Unsupported grouping mode")
        val rawSortPreference = json.optString("sortPreference", current.sortPreference.storedValue)
        val importedSortPreference =
            GallerySortPreference.entries.firstOrNull { it.storedValue == rawSortPreference }
                ?: throw IllegalArgumentException("Unsupported sort preference")
        val rawSlideshowInterval = json.optString(
            "slideshowInterval",
            current.slideshowInterval.storedValue,
        )
        val importedSlideshowInterval =
            GallerySlideshowInterval.entries.firstOrNull { it.storedValue == rawSlideshowInterval }
                ?: throw IllegalArgumentException("Unsupported slideshow interval")

        galleryPreferences().edit()
            .putString(FILE_LOADING_PRIORITY_KEY, importedPriority.storedValue)
            .putString(VIEW_DENSITY_KEY, importedDensity.storedValue)
            .putString(NAVIGATION_DISPLAY_MODE_KEY, importedNavigationDisplayMode.storedValue)
            .putString(GROUPING_MODE_KEY, importedGrouping.storedValue)
            .putString(SORT_PREFERENCE_KEY, importedSortPreference.storedValue)
            .putStringSet(
                PINNED_ALBUM_IDS_KEY,
                json.optJSONArray("pinnedAlbumIds")?.let(::jsonStringSet) ?: current.pinnedAlbumIds,
            )
            .putString(
                ALBUM_ORDER_IDS_KEY,
                stringListJson(
                    json.optJSONArray("albumOrderIds")?.let(::jsonStringList) ?: current.albumOrderIds,
                ).toString(),
            )
            .putStringSet(
                INCLUDED_ALBUM_IDS_KEY,
                json.optJSONArray("includedAlbumIds")?.let(::jsonStringSet) ?: current.includedAlbumIds,
            )
            .putStringSet(
                EXCLUDED_ALBUM_IDS_KEY,
                json.optJSONArray("excludedAlbumIds")?.let(::jsonStringSet) ?: current.excludedAlbumIds,
            )
            .putBoolean(SHOW_HIDDEN_ITEMS_KEY, json.optBoolean("showHiddenItems", current.showHiddenItems))
            .putBoolean(
                PLAY_VIDEOS_AUTOMATICALLY_KEY,
                json.optBoolean("playVideosAutomatically", current.playVideosAutomatically),
            )
            .putBoolean(LOOP_VIDEOS_KEY, json.optBoolean("loopVideos", current.loopVideos))
            .putString(SLIDESHOW_INTERVAL_KEY, importedSlideshowInterval.storedValue)
            .putBoolean(
                ANIMATE_GIF_THUMBNAILS_KEY,
                json.optBoolean("animateGifThumbnails", current.animateGifThumbnails),
            )
            .putBoolean(
                DELETE_EMPTY_FOLDERS_KEY,
                json.optBoolean("deleteEmptyFolders", current.deleteEmptyFolders),
            )
            .putBoolean(
                MOVE_DELETED_TO_RECYCLE_BIN_KEY,
                json.optBoolean("moveDeletedItemsToRecycleBin", current.moveDeletedItemsToRecycleBin),
            )
            .putBoolean(
                ROUNDED_SQUARE_THUMBNAILS_KEY,
                json.optBoolean("roundedSquareThumbnails", current.roundedSquareThumbnails),
            )
            .putBoolean(
                GallerySetupPreferences.CONTEXTUAL_HINTS_ENABLED_KEY,
                json.optBoolean("contextualHintsEnabled", current.contextualHintsEnabled),
            )
            .apply()

        selectedSort = importedSortPreference.mediaSortOrder
        reconfigureThumbnailExecutor(importedPriority)
        thumbnailCache.evictAll()
        renderNavigation()
        Toast.makeText(this, "Gallery settings imported", Toast.LENGTH_SHORT).show()
        renderSettingsDestinationOnly()
    }

    private fun stringSetJson(values: Set<String>): JSONArray = JSONArray().apply {
        values.sorted().forEach { put(it) }
    }

    private fun stringListJson(values: List<String>): JSONArray = JSONArray().apply {
        values.filter(String::isNotBlank).distinct().forEach { put(it) }
    }

    private fun jsonStringList(array: JSONArray): List<String> {
        val values = linkedSetOf<String>()
        for (index in 0 until array.length()) {
            val value = array.optString(index).trim()
            if (value.isNotBlank()) values.add(value)
        }
        return values.toList()
    }

    private fun jsonStringSet(array: JSONArray): Set<String> {
        val values = linkedSetOf<String>()
        for (index in 0 until array.length()) {
            val value = array.optString(index).trim()
            if (value.isNotBlank()) values.add(value)
        }
        return values
    }

    private fun galleryPreferences() =
        getSharedPreferences(GallerySetupPreferences.PREFERENCES_NAME, MODE_PRIVATE)

    private fun viewerIconAction(
        iconResource: Int,
        enabled: Boolean,
        description: String,
        onClick: () -> Unit,
    ): TextView = TextView(this).apply {
        text = ""
        gravity = Gravity.CENTER
        minHeight = dp(GalleryGlazeContract.GENERAL_TARGET_DP)
        minWidth = dp(GalleryGlazeContract.GENERAL_TARGET_DP)
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = roundedSurface(0x26ffffff, 18)
        isEnabled = enabled
        isClickable = enabled
        isFocusable = enabled
        alpha = if (enabled) 1f else 0.35f
        contentDescription = description
        tooltipText = description
        setViewerActionIcon(this, iconResource)
        GalleryInteractionFeedback.applyBoundedRipple(this, Color.WHITE, 18)
        if (enabled) setOnClickListener { onClick() }
    }

    private fun setViewerActionIcon(
        control: TextView,
        iconResource: Int,
        selected: Boolean = false,
    ) {
        control.setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0)
        control.compoundDrawableTintList = ColorStateList.valueOf(
            if (selected) accentColor() else Color.WHITE,
        )
        control.background = if (selected) {
            roundedSurface(withAlpha(accentColor(), 0.24f), 18)
        } else {
            roundedSurface(0x26ffffff, 18)
        }
        control.isSelected = selected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            control.stateDescription = if (selected) "Active" else null
        }
    }

    private fun iconHeaderAction(
        iconResource: Int,
        description: String,
        onClick: () -> Unit,
    ): ImageView = ImageView(this).apply {
        setImageResource(iconResource)
        setColorFilter(primaryTextColor())
        setPadding(dp(13), dp(13), dp(13), dp(13))
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        background = GalleryGlazeSurfaces.drawable(
            context,
            GalleryGlazeSurfaces.Role.CONTROL,
            GalleryGlazeContract.SHAPE_CONTAINER_DP,
        )
        isClickable = true
        isFocusable = true
        contentDescription = description
        tooltipText = description
        GalleryInteractionFeedback.applyBoundedRipple(
            this,
            primaryTextColor(),
            GalleryGlazeContract.SHAPE_CONTAINER_DP,
        )
        setOnClickListener { onClick() }
    }

    private fun toggleSearch() {
        if (destination == GalleryDestination.SETTINGS || inSelectionMode) return
        if (searchContainer.visibility == View.VISIBLE) {
            closeSearch()
        } else {
            searchContainer.visibility = View.VISIBLE
            searchField.requestFocus()
            searchControl.visibility = View.GONE
            searchContainer.post {
                (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun closeSearch(clearQuery: Boolean = true) {
        if (!::searchContainer.isInitialized) return
        searchContainer.visibility = View.GONE
        searchControl.setImageResource(R.drawable.ic_gallery_search)
        searchControl.setColorFilter(primaryTextColor())
        searchControl.contentDescription = "Search the current Gallery destination"
        searchControl.tooltipText = "Search the current Gallery destination"
        searchControl.visibility = View.VISIBLE
        (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(searchField.windowToken, 0)
        if (clearQuery) {
            clearSearchQueryWithoutRender()
            renderCurrentDestination()
        }
    }

    private fun clearSearchQueryWithoutRender() {
        searchQuery = ""
        if (!::searchField.isInitialized || searchField.text.isEmpty()) return

        suppressSearchRender = true
        try {
            searchField.setText("")
        } finally {
            suppressSearchRender = false
        }
    }

    private fun sortOrderLabel(): String =
        if (selectedSort == MediaSortOrder.NEWEST) "Newest first" else "Oldest first"

    private fun sortIconResource(): Int =
        if (selectedSort == MediaSortOrder.NEWEST) {
            R.drawable.ic_gallery_sort_newest
        } else {
            R.drawable.ic_gallery_sort_oldest
        }

    private fun timelineSectionHeader(label: String, count: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(2), dp(16), dp(2), dp(7))

        addView(
            TextView(context).apply {
                text = label
                setTextColor(primaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setTypeface(typeface, Typeface.BOLD)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        addView(
            TextView(context).apply {
                text = itemCountLabel(count)
                setTextColor(secondaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                gravity = Gravity.END
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
    }

    private fun sectionHeader(label: String): TextView = TextView(this).apply {
        text = label
        setTextColor(primaryTextColor())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(2), dp(16), 0, dp(7))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun dateGroupLabel(item: MediaItem): String {
        val date = (item.capturedAt ?: item.modifiedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> DATE_HEADER_FORMAT.format(date)
        }
    }

    private fun monthGroupLabel(item: MediaItem): String {
        val date = (item.capturedAt ?: item.modifiedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        return MONTH_HEADER_FORMAT.format(date)
    }

    private fun yearGroupLabel(item: MediaItem): String {
        val date = (item.capturedAt ?: item.modifiedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.year.toString()
    }

    private fun mediaDisplayTitle(item: MediaItem): String =
        item.displayName.substringBeforeLast('.', missingDelimiterValue = item.displayName)
            .ifBlank { item.displayName }

    private fun mediaDateLabel(item: MediaItem): String {
        val date = (item.capturedAt ?: item.modifiedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return DATE_HEADER_FORMAT.format(date)
    }

    private fun mediaMetadata(item: MediaItem): String {
        val timestamp = item.capturedAt ?: item.modifiedAt
        val kind = if (item.mimeType.startsWith("video/")) "Video" else "Photo"
        return listOfNotNull(
            kind,
            item.albumName?.let { "Album: $it" },
            DATE_TIME_FORMAT.format(timestamp),
            formatBytes(item.sizeBytes),
        ).joinToString(" · ")
    }

    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainder = seconds % 60
        return "$minutes:${remainder.toString().padStart(2, '0')}"
    }

    private fun itemCountLabel(count: Int): String =
        if (count == 1) "1 item" else "$count items"

    private fun videoCountLabel(count: Int): String =
        if (count == 1) "1 video" else "$count videos"

    private fun loadLocalThumbnail(
        item: MediaItem,
        target: ImageView,
        generation: Int,
        sizeDp: Int,
        namespace: String,
    ) {
        val cacheKey = thumbnailCacheKey(namespace, item.contentUri)
        if (namespace == VIEWER_THUMBNAIL_NAMESPACE) {
            loadAuthorizedViewerBitmap(item, target, generation, cacheKey, sizeDp)
            return
        }
        if (
            currentUserSettings().animateGifThumbnails &&
            GalleryAnimatedThumbnailPolicy.isAnimatedGif(item.mimeType)
        ) {
            loadAnimatedGifThumbnail(item, target, generation, cacheKey, sizeDp)
            return
        }
        loadStaticLocalThumbnail(item, target, generation, cacheKey, sizeDp)
    }

    private fun loadAnimatedGifThumbnail(
        item: MediaItem,
        target: ImageView,
        generation: Int,
        cacheKey: String,
        sizeDp: Int,
    ) {
        try {
            thumbnailExecutor.execute {
                val drawable = try {
                    GalleryGifThumbnailLoader.loadAnimated(
                        contentResolver = contentResolver,
                        contentUri = Uri.parse(item.contentUri),
                        requestedEdgePx = dp(sizeDp),
                    )
                } catch (_: SecurityException) {
                    null
                } catch (_: IOException) {
                    null
                } catch (_: RuntimeException) {
                    null
                }
                if (drawable == null) {
                    runOnUiThread {
                        if (generation == loadGeneration && target.tag == cacheKey) {
                            loadStaticLocalThumbnail(item, target, generation, cacheKey, sizeDp)
                        }
                    }
                    return@execute
                }

                runOnUiThread {
                    if (generation != loadGeneration || target.tag != cacheKey) return@runOnUiThread
                    GalleryGifThumbnailLoader.stopIfAnimated(target.drawable)
                    target.setImageDrawable(drawable)
                    val listener = object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(view: View) {
                            if (target.drawable === drawable) GalleryGifThumbnailLoader.startIfAnimated(drawable)
                        }

                        override fun onViewDetachedFromWindow(view: View) {
                            GalleryGifThumbnailLoader.stopIfAnimated(drawable)
                        }
                    }
                    target.addOnAttachStateChangeListener(listener)
                    if (target.isAttachedToWindow) GalleryGifThumbnailLoader.startIfAnimated(drawable)
                }
            }
        } catch (_: RuntimeException) {
            // Executor replacement can cancel queued GIF work; the next render can retry safely.
        }
    }

    private fun loadStaticLocalThumbnail(
        item: MediaItem,
        target: ImageView,
        generation: Int,
        cacheKey: String,
        sizeDp: Int,
    ) {
        thumbnailCache.get(cacheKey)?.let { cached ->
            if (generation == loadGeneration && target.tag == cacheKey) {
                GalleryGifThumbnailLoader.stopIfAnimated(target.drawable)
                target.setImageBitmap(cached)
            }
            return
        }
        try {
            thumbnailExecutor.execute {
                val bitmap = try {
                    contentResolver.loadThumbnail(Uri.parse(item.contentUri), Size(dp(sizeDp), dp(sizeDp)), null)
                } catch (_: SecurityException) {
                    null
                } catch (_: RuntimeException) {
                    null
                }
                if (bitmap == null) return@execute
                thumbnailCache.put(cacheKey, bitmap)
                runOnUiThread {
                    if (generation == loadGeneration && target.tag == cacheKey) {
                        GalleryGifThumbnailLoader.stopIfAnimated(target.drawable)
                        target.setImageBitmap(bitmap)
                    }
                }
            }
        } catch (_: RuntimeException) {
            // Executor replacement can cancel queued thumbnail work; presentation remains safely empty until re-rendered.
        }
    }

    private fun loadAuthorizedViewerBitmap(
        item: MediaItem,
        target: ImageView,
        generation: Int,
        cacheKey: String,
        fallbackSizeDp: Int,
    ) {
        target.post {
            if (
                generation != loadGeneration ||
                target.tag != cacheKey ||
                viewerOverlay == null
            ) return@post

            val viewportWidth = target.width
            val viewportHeight = target.height
            try {
                thumbnailExecutor.execute {
                    val bitmap = GalleryViewerBitmapLoader.load(
                        contentResolver = contentResolver,
                        contentUri = Uri.parse(item.contentUri),
                        mimeType = item.mimeType,
                        viewportWidth = viewportWidth,
                        viewportHeight = viewportHeight,
                        fallbackThumbnailPx = dp(fallbackSizeDp),
                    ) ?: return@execute
                    runOnUiThread {
                        if (
                            generation == loadGeneration &&
                            target.tag == cacheKey &&
                            viewerOverlay != null
                        ) {
                            target.setImageBitmap(bitmap)
                        }
                    }
                }
            } catch (_: RuntimeException) {
                // Executor replacement can cancel queued viewer work; presentation remains safely empty until re-rendered.
            }
        }
    }

    private fun thumbnailCacheKey(namespace: String, contentUri: String): String = "$namespace:$contentUri"

    private fun emptyState(title: String, message: String): LinearLayout {
        val iconResource = when {
            searchQuery.isNotBlank() -> R.drawable.ic_gallery_search
            destination == GalleryDestination.ALBUMS -> R.drawable.ic_gallery_nav_albums
            destination == GalleryDestination.VIDEOS -> R.drawable.ic_gallery_nav_videos
            destination == GalleryDestination.SETTINGS -> R.drawable.ic_gallery_nav_settings
            else -> R.drawable.ic_gallery_nav_photos
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(20), dp(18), dp(18))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.RAISED,
                GalleryGlazeContract.SHAPE_CONTAINER_DP,
            )
            addView(
                ImageView(context).apply {
                    setImageResource(iconResource)
                    setColorFilter(accentColor())
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setPadding(dp(9), dp(9), dp(9), dp(9))
                    background = roundedSurface(withAlpha(accentColor(), 0.10f), 20)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    bottomMargin = dp(8)
                },
            )
            addView(TextView(context).apply {
                text = title
                gravity = Gravity.CENTER
                setTextColor(primaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = message
                gravity = Gravity.CENTER
                setTextColor(secondaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setLineSpacing(0f, 1.06f)
                setPadding(0, dp(6), 0, 0)
            })
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10)
            }
        }
    }

    private fun messageRow(
        title: String,
        message: String,
        actionLabel: String? = null,
        actionIcon: Int? = null,
        onAction: (() -> Unit)? = null,
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(24), dp(18), dp(16))
            background = GalleryGlazeSurfaces.drawable(
                context,
                GalleryGlazeSurfaces.Role.RAISED,
                GalleryGlazeContract.SHAPE_CONTAINER_DP,
            )
            addView(TextView(context).apply {
                text = title
                gravity = Gravity.CENTER
                setTextColor(primaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = message
                gravity = Gravity.CENTER
                setTextColor(secondaryTextColor())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                setLineSpacing(0f, 1.08f)
                setPadding(0, dp(7), 0, if (actionLabel == null) dp(8) else 0)
            })
            if (actionLabel != null && onAction != null) {
                if (actionIcon != null) {
                    addView(
                        ImageView(context).apply {
                            setImageResource(actionIcon)
                            setColorFilter(accentColor())
                            scaleType = ImageView.ScaleType.CENTER_INSIDE
                            setPadding(dp(13), dp(13), dp(13), dp(13))
                            background = roundedSurface(withAlpha(accentColor(), 0.11f), 16)
                            isClickable = true
                            isFocusable = true
                            contentDescription = "$actionLabel $title"
                            tooltipText = actionLabel
                            GalleryInteractionFeedback.applyBoundedRipple(
                                this,
                                accentColor(),
                                16,
                            )
                            setOnClickListener { onAction() }
                        },
                        LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                            topMargin = dp(4)
                        },
                    )
                } else {
                    addView(TextView(context).apply {
                        text = actionLabel
                        gravity = Gravity.CENTER
                        minHeight = dp(48)
                        setPadding(dp(16), dp(12), dp(16), dp(12))
                        setTextColor(primaryTextColor())
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        setTypeface(typeface, Typeface.BOLD)
                        isClickable = true
                        isFocusable = true
                        contentDescription = "$actionLabel $title"
                        GalleryInteractionFeedback.applyBoundedRipple(
                            this,
                            primaryTextColor(),
                            16,
                        )
                        setOnClickListener { onAction() }
                    })
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10)
            }
        }
    }

    private fun currentMediaAccessScope(): GalleryMediaAccessScope {
        fun granted(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        return GalleryMediaAccessPolicy.resolve(
            GalleryMediaPermissionSnapshot(
                apiLevel = Build.VERSION.SDK_INT,
                readExternalStorage = Build.VERSION.SDK_INT <= 32 && granted(Manifest.permission.READ_EXTERNAL_STORAGE),
                readMediaImages = Build.VERSION.SDK_INT >= 33 && granted(Manifest.permission.READ_MEDIA_IMAGES),
                readMediaVideo = Build.VERSION.SDK_INT >= 33 && granted(Manifest.permission.READ_MEDIA_VIDEO),
                readMediaVisualUserSelected =
                    Build.VERSION.SDK_INT >= 34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
            ),
        )
    }

    private fun accessScopeLabel(scope: GalleryMediaAccessScope): String = when (scope) {
        GalleryMediaAccessScope.DENIED -> "Media access denied"
        GalleryMediaAccessScope.LEGACY_FULL -> "Local media access"
        GalleryMediaAccessScope.SELECTED -> "Selected media only"
        GalleryMediaAccessScope.IMAGES -> "Photos authorized"
        GalleryMediaAccessScope.VIDEOS -> "Videos authorized"
        GalleryMediaAccessScope.IMAGES_AND_VIDEOS -> "Photos and videos authorized"
    }

    private fun requestReadableMediaAccess() {
        val permissions = when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
            Build.VERSION.SDK_INT >= 33 ->
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissions(permissions, MEDIA_PERMISSION_REQUEST)
    }

    private fun applySystemChrome() {
        val canvas = canvasColor()
        window.statusBarColor = canvas
        window.navigationBarColor = canvas
        @Suppress("DEPRECATION")
        run {
            var flags = 0
            if (!isNightMode()) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
            }
            window.decorView.systemUiVisibility = flags
        }
    }

    private fun isNightMode(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    private fun canvasColor(): Int = themeColor(android.R.attr.colorBackground, 0xfffafafa.toInt())

    private fun primaryTextColor(): Int = themeColor(android.R.attr.textColorPrimary, 0xff1d1d1f.toInt())

    private fun secondaryTextColor(): Int = themeColor(android.R.attr.textColorSecondary, 0xff666666.toInt())

    private fun accentColor(): Int = themeColor(android.R.attr.colorAccent, 0xff2e7d6f.toInt())

    private fun roundedSurface(color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun selectedTileSurface(radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(withAlpha(accentColor(), 0.10f))
        cornerRadius = dp(radiusDp).toFloat()
        setStroke(dp(2), withAlpha(accentColor(), 0.95f))
    }

    private fun withAlpha(color: Int, alpha: Float): Int = Color.argb(
        (255f * alpha.coerceIn(0f, 1f)).toInt(),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )

    private fun themeColor(attribute: Int, fallback: Int): Int {
        val attributes = obtainStyledAttributes(intArrayOf(attribute))
        return try {
            attributes.getColor(0, fallback)
        } finally {
            attributes.recycle()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class AlbumPresentation(
        val id: String?,
        val name: String,
        val count: Int,
        val cover: MediaItem,
        val isFavorites: Boolean,
    )

    private enum class GalleryDestination {
        PHOTOS,
        ALBUMS,
        VIDEOS,
        TRASH,
        SETTINGS,
    }

    private companion object {
        const val MEDIA_PERMISSION_REQUEST = 4101
        const val MEDIA_MUTATION_REQUEST = 4102
        const val MEDIA_MOVE_REQUEST = 4103
        const val EXPORT_FAVORITES_REQUEST = 4201
        const val IMPORT_FAVORITES_REQUEST = 4202
        const val EXPORT_SETTINGS_REQUEST = 4203
        const val IMPORT_SETTINGS_REQUEST = 4204
        const val STATE_PENDING_MEDIA_MUTATION_MODE = "pending_media_mutation_mode"
        const val STATE_PENDING_MEDIA_MUTATION_URIS = "pending_media_mutation_uris"
        const val STATE_PENDING_MEDIA_MOVE_URIS = "pending_media_move_uris"
        const val STATE_PENDING_MEDIA_MOVE_DESTINATION = "pending_media_move_destination"

        const val GRID_GAP_DP = 3
        const val GRID_CORNER_DP = 8
        const val GRID_THUMBNAIL_DP = 192
        const val ALBUM_GAP_DP = 12
        const val ALBUM_CORNER_DP = 16
        const val ALBUM_THUMBNAIL_DP = 320
        const val ALBUM_QUICK_ACCESS_LIMIT = 4
        const val ALBUM_COVER_ASPECT_HEIGHT = 0.66f
        const val ALBUM_BADGE_DP = 44
        const val VIDEO_CARD_GAP_DP = 10
        const val VIEWER_THUMBNAIL_DP = 720
        const val VIEWER_SWIPE_DISTANCE_DP = 56
        const val DRAG_SELECTION_EDGE_DP = 72
        const val DRAG_SELECTION_SCROLL_STEP_DP = 14
        const val THUMBNAIL_CACHE_KIB = 8 * 1024
        const val MEDIA_REFRESH_DEBOUNCE_MS = 350L
        const val GRID_THUMBNAIL_NAMESPACE = "grid"
        const val ALBUM_THUMBNAIL_NAMESPACE = "album"
        const val VIEWER_THUMBNAIL_NAMESPACE = "viewer"
        const val SELECTION_OVERLAY_TAG = "goreecloud_gallery_selection_overlay"
        const val SELECTION_CHECK_TAG = "goreecloud_gallery_selection_check"
        const val VIDEO_PLAY_TAG = "goreecloud_gallery_video_play"

        const val FAVORITES_KEY = "favorite_content_uris"
        const val FILE_LOADING_PRIORITY_KEY = "file_loading_priority"
        const val VIEW_DENSITY_KEY = "view_density"
        const val NAVIGATION_DISPLAY_MODE_KEY = "navigation_display_mode"
        const val GROUPING_MODE_KEY = "grouping_mode"
        const val SORT_PREFERENCE_KEY = "sort_preference"
        const val PINNED_ALBUM_IDS_KEY = "pinned_album_ids"
        const val ALBUM_ORDER_IDS_KEY = "album_order_ids"
        const val INCLUDED_ALBUM_IDS_KEY = "included_album_ids"
        const val EXCLUDED_ALBUM_IDS_KEY = "excluded_album_ids"
        const val SHOW_HIDDEN_ITEMS_KEY = "show_hidden_items"
        const val PLAY_VIDEOS_AUTOMATICALLY_KEY = "play_videos_automatically"
        const val LOOP_VIDEOS_KEY = "loop_videos"
        const val SLIDESHOW_INTERVAL_KEY = "slideshow_interval"
        const val ANIMATE_GIF_THUMBNAILS_KEY = "animate_gif_thumbnails"
        const val DELETE_EMPTY_FOLDERS_KEY = "delete_empty_folders"
        const val MOVE_DELETED_TO_RECYCLE_BIN_KEY = "move_deleted_items_to_recycle_bin"
        const val ROUNDED_SQUARE_THUMBNAILS_KEY = "rounded_square_thumbnails"

        const val FAVORITES_EXPORT_TYPE = "goreecloud-gallery-favorites"
        const val SETTINGS_EXPORT_TYPE = "goreecloud-gallery-settings"

        val DATE_TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a").withZone(ZoneId.systemDefault())
        val DATE_HEADER_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        val MONTH_HEADER_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

        fun formatBytes(bytes: Long): String = when {
            bytes >= 1024L * 1024L -> String.format("%.1f MiB", bytes / (1024.0 * 1024.0))
            bytes >= 1024L -> String.format("%.1f KiB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
