package com.goreecloud.gallery

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.LruCache
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import com.goreecloud.gallery.android.AndroidMediaMutationMode
import com.goreecloud.gallery.android.AndroidMediaMutationPendingState
import com.goreecloud.gallery.android.AndroidMediaMutationPendingStates
import com.goreecloud.gallery.android.AndroidMediaMutationRequests
import com.goreecloud.gallery.android.AndroidTrashedMediaStoreReader
import com.goreecloud.gallery.core.GallerySelectionPolicy
import com.goreecloud.gallery.core.MediaItem
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * Development first-party browser for Android MediaStore Trash.
 *
 * Android MediaStore remains the only authoritative Trash state. This activity does not maintain a
 * second deleted-item database or gain filesystem-wide authority. Restore and permanent purge are
 * always routed through Android-owned confirmation requests.
 */
class RecycleBinActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var body: LinearLayout
    private lateinit var libraryScroll: ScrollView
    private lateinit var headerTitle: TextView
    private lateinit var headerSubtitle: TextView
    private lateinit var navigationCapsule: LinearLayout
    private lateinit var actionBar: LinearLayout

    private var generation = 0
    private var trashedItems: List<MediaItem> = emptyList()
    private val selectedUris = linkedSetOf<String>()
    private val renderedTiles = linkedMapOf<String, FrameLayout>()
    private var pendingMutation: AndroidMediaMutationPendingState? = null
    private var viewerOverlay: View? = null

    private val thumbnailExecutor: ExecutorService = Executors.newFixedThreadPool(2)
    private val thumbnailCache = object : LruCache<String, Bitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = maxOf(1, value.allocationByteCount / 1024)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingMutation = restorePendingMutation(savedInstanceState)
        buildSurface()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingMutation?.let { mutation ->
            outState.putString(
                STATE_PENDING_MUTATION_MODE,
                AndroidMediaMutationPendingStates.modeName(mutation),
            )
            outState.putStringArray(
                STATE_PENDING_MUTATION_URIS,
                AndroidMediaMutationPendingStates.contentUriValues(mutation),
            )
        }
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (pendingMutation == null && viewerOverlay == null) loadRecycleBin()
    }

    override fun onDestroy() {
        generation += 1
        thumbnailExecutor.shutdownNow()
        thumbnailCache.evictAll()
        super.onDestroy()
    }

    @Deprecated("Recycle Bin back navigation is handled explicitly for viewer and selection state.")
    override fun onBackPressed() {
        if (viewerOverlay != null) {
            closeViewer()
            return
        }
        if (selectedUris.isNotEmpty()) {
            selectedUris.clear()
            renderSelectionState()
            return
        }
        super.onBackPressed()
    }

    @Deprecated("Development Recycle Bin uses Android activity results for system mutation confirmation.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != RECYCLE_MUTATION_REQUEST) return

        val mutation = pendingMutation
        pendingMutation = null
        if (mutation == null) return

        if (resultCode == RESULT_OK) {
            if (mutation.mode == AndroidMediaMutationMode.DELETE) {
                removePurgedFavorites(mutation.contentUris.toSet())
            }
            selectedUris.clear()
            thumbnailCache.evictAll()
            closeViewer(render = false)
            Toast.makeText(
                this,
                when (mutation.mode) {
                    AndroidMediaMutationMode.RESTORE ->
                        if (mutation.contentUris.size == 1) "Restored from Trash"
                        else "Restored ${mutation.contentUris.size} items"
                    AndroidMediaMutationMode.DELETE ->
                        if (mutation.contentUris.size == 1) "Deleted permanently"
                        else "Deleted ${mutation.contentUris.size} items permanently"
                    AndroidMediaMutationMode.TRASH -> "Trash updated"
                },
                Toast.LENGTH_SHORT,
            ).show()
            loadRecycleBin()
        } else {
            Toast.makeText(
                this,
                if (mutation.mode == AndroidMediaMutationMode.RESTORE) "Restore canceled" else "Permanent delete canceled",
                Toast.LENGTH_SHORT,
            ).show()
            if (viewerOverlay == null) renderSelectionState()
        }
    }

    private fun restorePendingMutation(savedInstanceState: Bundle?): AndroidMediaMutationPendingState? {
        val restored = AndroidMediaMutationPendingStates.restore(
            modeName = savedInstanceState?.getString(STATE_PENDING_MUTATION_MODE),
            contentUris = savedInstanceState?.getStringArray(STATE_PENDING_MUTATION_URIS)?.asList(),
        ) ?: return null

        // This Activity can only originate Restore or permanent Delete requests for already-trashed
        // media. Never let restored Bundle state manufacture ordinary Trash authority here.
        return restored.takeIf {
            it.mode == AndroidMediaMutationMode.RESTORE || it.mode == AndroidMediaMutationMode.DELETE
        }
    }

    private fun buildSurface() {
        root = FrameLayout(this).apply {
            setBackgroundColor(canvasColor())
        }

        val designMetrics = GalleryDesignSystem.metrics(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(designMetrics.horizontalGutterDp),
                dp(designMetrics.topPaddingDp),
                dp(designMetrics.horizontalGutterDp),
                dp(GalleryGlazeContract.CONTENT_BOTTOM_INSET_DP),
            )
            setBackgroundColor(canvasColor())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            ImageView(this).apply {
                setImageResource(R.mipmap.ic_gallery_launcher)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                marginEnd = dp(10)
            },
        )

        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        headerTitle = TextView(this).apply {
            text = "Trash"
            GalleryDesignSystem.applyTitle(this, primaryTextColor())
            ellipsize = TextUtils.TruncateAt.END
        }
        headerSubtitle = TextView(this).apply {
            text = "Android controls Trash retention and expiration"
            GalleryDesignSystem.applySubtitle(this, secondaryTextColor())
            ellipsize = TextUtils.TruncateAt.END
        }
        titles.addView(headerTitle)
        titles.addView(headerSubtitle)
        header.addView(titles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(4)
        })
        header.addView(
            headerIconAction(R.drawable.ic_gallery_refresh, "Refresh Trash") { loadRecycleBin() },
            LinearLayout.LayoutParams(dp(48), dp(48)),
        )
        content.addView(header)

        content.addView(
            messageRow(
                "Android-managed Trash",
                "Recently deleted photos and videos remain under Android MediaStore authority. Restore and permanent deletion always require Android confirmation.",
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(3)
                bottomMargin = 0
            },
        )

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, 0)
        }
        content.addView(body)

        val contentHost = FrameLayout(this).apply {
            val widthPx = if (resources.configuration.screenWidthDp > GalleryGlazeContract.CONTENT_MAX_WIDTH_DP) {
                dp(GalleryGlazeContract.CONTENT_MAX_WIDTH_DP)
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }
            addView(
                content,
                FrameLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                },
            )
        }
        libraryScroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(
                contentHost,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }
        val widthDp = resources.configuration.screenWidthDp
        val usesRail = GalleryGlazeContract.usesNavigationRail(widthDp)
        root.addView(
            libraryScroll,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                if (usesRail) {
                    marginStart = dp(GalleryGlazeContract.navigationRailLaneDp(widthDp))
                } else {
                    bottomMargin = dp(GalleryGlazeContract.NAVIGATION_RESERVED_SPACE_DP)
                }
            },
        )

        navigationCapsule = buildNavigationCapsule()
        root.addView(navigationCapsule, navigationCapsuleLayoutParams())

        actionBar = bottomCapsuleSurface().apply {
            visibility = View.GONE
        }
        root.addView(actionBar, bottomCapsuleLayoutParams())

        setContentView(root)
        applySystemChrome()
    }

    private fun buildNavigationCapsule(): LinearLayout = bottomCapsuleSurface().apply {
        val navigationMode = GalleryNavigationDisplayMode.ICONS_ONLY
        val usesRail = GalleryGlazeContract.usesNavigationRail(resources.configuration.screenWidthDp)
        orientation = if (usesRail) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val items = listOf(
            Triple("Photos", R.drawable.ic_gallery_nav_photos, GalleryNavigationContract.PHOTOS),
            Triple("Albums", R.drawable.ic_gallery_nav_albums, GalleryNavigationContract.ALBUMS),
            Triple("Videos", R.drawable.ic_gallery_nav_videos, GalleryNavigationContract.VIDEOS),
            Triple("Trash", R.drawable.ic_gallery_nav_trash, GalleryNavigationContract.TRASH),
            Triple("Settings", R.drawable.ic_gallery_nav_settings, GalleryNavigationContract.SETTINGS),
        )
        items.forEach { (label, icon, target) ->
            val selected = target == GalleryNavigationContract.TRASH
            addView(
                TextView(this@RecycleBinActivity).apply {
                    GalleryNavigationStyling.apply(
                        activity = this@RecycleBinActivity,
                        item = this,
                        label = label,
                        iconRes = icon,
                        mode = navigationMode,
                        selected = selected,
                        foreground = if (selected) accentColor() else secondaryTextColor(),
                    )
                    isSelected = selected
                    isClickable = true
                    isFocusable = true
                    contentDescription = label + if (selected) ", selected" else ""
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        stateDescription = if (selected) "Selected" else null
                    }
                    setOnClickListener {
                        if (target != GalleryNavigationContract.TRASH) {
                            openGalleryDestination(target)
                        }
                    }
                },
                if (usesRail) {
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(GalleryGlazeContract.NAVIGATION_RAIL_ITEM_HEIGHT_DP),
                    )
                } else {
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                },
            )
        }
    }

    private fun openGalleryDestination(target: String) {
        startActivity(
            Intent(this, GalleryActivity::class.java)
                .putExtra(GalleryNavigationContract.EXTRA_DESTINATION, target)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    private fun navigationCapsuleLayoutParams(): FrameLayout.LayoutParams {
        val widthDp = resources.configuration.screenWidthDp
        if (GalleryGlazeContract.usesNavigationRail(widthDp)) {
            return FrameLayout.LayoutParams(
                dp(GalleryGlazeContract.NAVIGATION_RAIL_WIDTH_DP),
                dp(
                    (GalleryGlazeContract.NAVIGATION_RAIL_ITEM_HEIGHT_DP * 5) + 8,
                ),
            ).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                marginStart = dp(GalleryGlazeContract.NAVIGATION_RAIL_SIDE_MARGIN_DP)
            }
        }
        return bottomCapsuleLayoutParams()
    }

    private fun updateLibraryViewportForChrome() {
        if (!::libraryScroll.isInitialized) return
        val params = libraryScroll.layoutParams as? FrameLayout.LayoutParams ?: return
        val widthDp = resources.configuration.screenWidthDp
        val usesRail = GalleryGlazeContract.usesNavigationRail(widthDp)
        val inSelectionMode = selectedUris.isNotEmpty()
        val desiredStartMargin = if (usesRail && !inSelectionMode) {
            dp(GalleryGlazeContract.navigationRailLaneDp(widthDp))
        } else {
            0
        }
        val desiredBottomMargin = if (!usesRail || inSelectionMode) {
            dp(GalleryGlazeContract.NAVIGATION_RESERVED_SPACE_DP)
        } else {
            0
        }
        if (
            params.marginStart != desiredStartMargin ||
            params.bottomMargin != desiredBottomMargin
        ) {
            params.marginStart = desiredStartMargin
            params.bottomMargin = desiredBottomMargin
            libraryScroll.layoutParams = params
        }
    }

    private fun bottomCapsuleLayoutParams(): FrameLayout.LayoutParams {
        val metrics = GalleryDesignSystem.metrics(this)
        return FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(metrics.navigationHeightDp),
        ).apply {
            gravity = Gravity.BOTTOM
            marginStart = dp(metrics.navigationSideMarginDp)
            marginEnd = dp(metrics.navigationSideMarginDp)
            bottomMargin = dp(metrics.navigationBottomMarginDp)
        }
    }

    private fun bottomCapsuleSurface(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(4), dp(4), dp(4), dp(4))
        background = GalleryGlazeSurfaces.drawable(
            context,
            GalleryGlazeSurfaces.Role.CHROME,
            GalleryGlazeContract.NAVIGATION_RADIUS_DP,
        )
        elevation = dp(GalleryGlazeContract.NAVIGATION_ELEVATION_DP).toFloat()
        clipToOutline = true
    }

    private fun loadRecycleBin() {
        if (viewerOverlay != null || pendingMutation != null) return
        val currentGeneration = ++generation
        selectedUris.clear()
        renderedTiles.clear()
        renderActionBar()
        body.removeAllViews()

        if (!AndroidTrashedMediaStoreReader.isSupported()) {
            trashedItems = emptyList()
            headerTitle.text = "Trash"
            headerSubtitle.text = "Requires Android 11 or newer"
            body.addView(emptyState("Trash unavailable", "Android MediaStore Trash browsing requires Android 11 or newer."))
            return
        }
        if (!hasReadableMediaAccess()) {
            trashedItems = emptyList()
            headerTitle.text = "Trash"
            headerSubtitle.text = "Media access required"
            body.addView(emptyState("Media access required", "Choose media in Gallery before opening Trash."))
            return
        }

        headerTitle.text = "Trash"
        headerSubtitle.text = "Loading Android MediaStore Trash…"
        body.addView(messageRow("Loading Trash", "Reading only image/video items Android currently exposes as trashed."))

        thread(name = "goreecloud-gallery-recycle-bin") {
            try {
                val result = AndroidTrashedMediaStoreReader(contentResolver).readLatest(MAX_TRASH_ROWS)
                runOnUiThread {
                    if (currentGeneration != generation) return@runOnUiThread
                    trashedItems = result.items
                    renderRecycleBin(currentGeneration, result.rejectedRowCount)
                }
            } catch (_: SecurityException) {
                renderFailure(currentGeneration, "Android denied the current Trash read.")
            } catch (_: RuntimeException) {
                renderFailure(currentGeneration, "The Android media provider could not read Trash right now.")
            }
        }
    }

    private fun renderRecycleBin(currentGeneration: Int, rejectedRows: Int) {
        if (currentGeneration != generation) return
        body.removeAllViews()
        renderedTiles.clear()
        val count = trashedItems.size
        headerTitle.text = "Trash"
        headerSubtitle.text = buildString {
            append(if (count == 1) "1 item" else "$count items")
            if (rejectedRows > 0) append(" · $rejectedRows skipped")
            append(" · Android controls retention")
        }

        if (trashedItems.isEmpty()) {
            body.addView(emptyState("Trash is empty", "Photos and videos moved to Android Trash will appear here when Android exposes them to Gallery."))
            return
        }

        body.addView(sectionHeader("Recently deleted"))
        renderMediaGrid(trashedItems, currentGeneration)
    }

    private fun renderFailure(currentGeneration: Int, message: String) {
        runOnUiThread {
            if (currentGeneration != generation) return@runOnUiThread
            trashedItems = emptyList()
            selectedUris.clear()
            renderedTiles.clear()
            headerTitle.text = "Trash"
            headerSubtitle.text = "Trash unavailable"
            body.removeAllViews()
            body.addView(messageRow("Trash unavailable", message))
            renderActionBar()
        }
    }

    private fun renderMediaGrid(items: List<MediaItem>, currentGeneration: Int) {
        val widthDp = resources.configuration.screenWidthDp
        val columns = gridColumns()
        val gap = dp(GalleryGlazeContract.mediaGapDp(widthDp))
        val totalGap = gap * (columns - 1)
        val contentWidthPx = minOf(
            resources.displayMetrics.widthPixels,
            dp(GalleryGlazeContract.CONTENT_MAX_WIDTH_DP),
        )
        val tileSize = (
            (contentWidthPx - dp(GalleryGlazeContract.horizontalGutterDp(widthDp) * 2) - totalGap) / columns
        ).coerceAtLeast(dp(GalleryGlazeContract.MIN_GRID_TILE_DP))

        items.chunked(columns).forEachIndexed { rowIndex, rowItems ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
            }
            rowItems.forEachIndexed { columnIndex, item ->
                row.addView(
                    mediaTile(item, items, currentGeneration),
                    LinearLayout.LayoutParams(0, tileSize, 1f).apply {
                        if (columnIndex > 0) marginStart = gap
                    },
                )
            }
            repeat(columns - rowItems.size) { spacerIndex ->
                row.addView(
                    Space(this),
                    LinearLayout.LayoutParams(0, tileSize, 1f).apply {
                        if (rowItems.isNotEmpty() || spacerIndex > 0) marginStart = gap
                    },
                )
            }
            body.addView(
                row,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tileSize).apply {
                    if (rowIndex > 0) topMargin = gap
                },
            )
        }
    }

    private fun mediaTile(item: MediaItem, items: List<MediaItem>, currentGeneration: Int): FrameLayout {
        val selected = item.contentUri in selectedUris
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = roundedSurface(withAlpha(primaryTextColor(), 0.08f), 14)
            tag = gridCacheKey(item.contentUri)
        }
        loadThumbnail(item, image, currentGeneration, GRID_THUMBNAIL_PX, GRID_CACHE_PREFIX)

        return FrameLayout(this).apply {
            background = roundedSurface(withAlpha(primaryTextColor(), 0.08f), 14)
            clipToOutline = true
            isClickable = true
            isLongClickable = true
            isFocusable = true
            GalleryInteractionFeedback.applyBoundedRipple(this, Color.WHITE, 14)
            isSelected = selected
            contentDescription = tileDescription(item, selected)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                stateDescription = if (selected) "Selected" else null
            }
            setOnClickListener {
                if (selectedUris.isNotEmpty()) {
                    toggleSelection(item)
                } else {
                    val index = items.indexOfFirst { it.contentUri == item.contentUri }
                    if (index >= 0) showViewer(items, index, currentGeneration)
                }
            }
            setOnLongClickListener {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                toggleSelection(item)
                true
            }
            addView(image, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(
                View(context).apply {
                    tag = SELECTION_OVERLAY_TAG
                    visibility = if (selected) View.VISIBLE else View.GONE
                    background = roundedSurface(withAlpha(accentColor(), 0.22f), 14)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            if (item.mimeType.startsWith("video/")) {
                addView(
                    ImageView(context).apply {
                        tag = VIDEO_PLAY_TAG
                        visibility = if (selectedUris.isEmpty()) View.VISIBLE else View.GONE
                        setImageResource(R.drawable.ic_gallery_play)
                        setColorFilter(Color.WHITE)
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        setPadding(dp(12), dp(12), dp(12), dp(12))
                        background = roundedSurface(0xb3000000.toInt(), 20)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    },
                    FrameLayout.LayoutParams(dp(40), dp(40)).apply {
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
                    setPadding(dp(6), dp(6), dp(6), dp(6))
                    background = roundedSurface(accentColor(), 14)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                FrameLayout.LayoutParams(dp(28), dp(28)).apply {
                    gravity = Gravity.END or Gravity.TOP
                    marginEnd = dp(5)
                    topMargin = dp(5)
                },
            )
        }.also { renderedTiles[item.contentUri] = it }
    }

    private fun toggleSelection(item: MediaItem) {
        val updated = GallerySelectionPolicy.toggle(selectedUris, item, trashedItems)
        selectedUris.clear()
        selectedUris.addAll(updated)
        renderSelectionState()
    }

    private fun renderSelectionState() {
        val byUri = trashedItems.associateBy { it.contentUri }
        renderedTiles.forEach { (uri, tile) ->
            val item = byUri[uri] ?: return@forEach
            val selected = uri in selectedUris
            tile.isSelected = selected
            tile.contentDescription = tileDescription(item, selected)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                tile.stateDescription = if (selected) "Selected" else null
            }
            tile.findViewWithTag<View>(SELECTION_OVERLAY_TAG)?.visibility = if (selected) View.VISIBLE else View.GONE
            tile.findViewWithTag<View>(SELECTION_CHECK_TAG)?.visibility = if (selected) View.VISIBLE else View.GONE
            tile.findViewWithTag<View>(VIDEO_PLAY_TAG)?.visibility =
                if (selectedUris.isEmpty()) View.VISIBLE else View.GONE
        }
        headerTitle.text = if (selectedUris.isEmpty()) "Trash" else if (selectedUris.size == 1) "1 selected" else "${selectedUris.size} selected"
        if (selectedUris.isEmpty()) {
            headerSubtitle.text = if (trashedItems.size == 1) "1 item · Android controls retention" else "${trashedItems.size} items · Android controls retention"
        } else {
            headerSubtitle.text = "Restore or permanently delete the current selection"
        }
        renderActionBar()
    }

    private fun renderActionBar() {
        if (!::actionBar.isInitialized || !::navigationCapsule.isInitialized) return
        updateLibraryViewportForChrome()
        actionBar.removeAllViews()
        if (selectedUris.isEmpty() || viewerOverlay != null) {
            actionBar.visibility = View.GONE
            navigationCapsule.visibility = if (viewerOverlay == null) View.VISIBLE else View.GONE
            return
        }
        navigationCapsule.visibility = View.GONE
        actionBar.visibility = View.VISIBLE
        val actions = listOf(
            iconAction(R.drawable.ic_gallery_select_all, "Select all currently loaded trashed media") {
                selectedUris.clear()
                selectedUris.addAll(GallerySelectionPolicy.selectAll(trashedItems))
                renderSelectionState()
            },
            iconAction(R.drawable.ic_gallery_restore, "Restore selected media through Android confirmation") {
                requestMutation(AndroidMediaMutationMode.RESTORE)
            },
            iconAction(
                R.drawable.ic_gallery_delete,
                "Permanently delete selected media through Android confirmation",
                destructive = true,
            ) {
                requestMutation(AndroidMediaMutationMode.DELETE)
            },
            iconAction(R.drawable.ic_gallery_close, "Clear Trash selection") {
                selectedUris.clear()
                renderSelectionState()
            },
        )
        actions.forEachIndexed { index, action ->
            actionBar.addView(action, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                if (index > 0) marginStart = dp(2)
            })
        }
    }

    private fun showViewer(items: List<MediaItem>, initialIndex: Int, currentGeneration: Int) {
        if (
            currentGeneration != generation ||
            !hasReadableMediaAccess() ||
            initialIndex !in items.indices
        ) return

        selectedUris.clear()
        renderSelectionState()
        viewerOverlay?.let { root.removeView(it) }
        actionBar.visibility = View.GONE
        navigationCapsule.visibility = View.GONE

        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        viewerOverlay = overlay
        val viewerChromeWidth = if (resources.configuration.screenWidthDp >= GalleryGlazeContract.SETTINGS_MAX_WIDTH_DP + 20) {
            dp(GalleryGlazeContract.SETTINGS_MAX_WIDTH_DP)
        } else {
            ViewGroup.LayoutParams.MATCH_PARENT
        }
        root.addView(
            overlay,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        @Suppress("DEPRECATION")
        run { window.decorView.systemUiVisibility = 0 }

        val preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
        }
        overlay.addView(
            preview,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                topMargin = dp(72)
                bottomMargin = dp(104)
            },
        )

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GalleryDesignSystem.mediaChromeSurface(
                context,
                GalleryGlazeContract.SHAPE_ROUNDED_DP,
                strong = true,
            )
        }
        val viewerBack = viewerIconAction(
            R.drawable.ic_gallery_back,
            "Close Trash viewer",
        ) { closeViewer() }
        topBar.addView(viewerBack, LinearLayout.LayoutParams(dp(48), dp(48)))
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
        topBar.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), 0, dp(6), 0)
                addView(viewerTitle)
                addView(viewerSubtitle)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        overlay.addView(
            topBar,
            FrameLayout.LayoutParams(viewerChromeWidth, dp(68)).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                marginStart = dp(10)
                marginEnd = dp(10)
                topMargin = dp(6)
            },
        )

        val previous = viewerIconAction(
            R.drawable.ic_gallery_chevron_left,
            "Previous trashed media",
        ) {}
        val next = viewerIconAction(
            R.drawable.ic_gallery_chevron_right,
            "Next trashed media",
        ) {}
        overlay.addView(previous, FrameLayout.LayoutParams(dp(52), dp(64)).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            marginStart = dp(10)
        })
        overlay.addView(next, FrameLayout.LayoutParams(dp(52), dp(64)).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            marginEnd = dp(10)
        })

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = GalleryDesignSystem.mediaChromeSurface(
                context,
                GalleryGlazeContract.SHAPE_ROUNDED_DP,
                strong = true,
            )
        }
        val restore = viewerIconAction(
            R.drawable.ic_gallery_restore,
            "Restore this media through Android confirmation",
        ) {}
        val purge = viewerIconAction(
            R.drawable.ic_gallery_delete,
            "Permanently delete this media through Android confirmation",
            destructive = true,
        ) {}
        val more = viewerIconAction(
            R.drawable.ic_gallery_more,
            "Show details for this trashed media",
        ) {}
        listOf(restore, purge, more).forEachIndexed { index, action ->
            bottomBar.addView(action, LinearLayout.LayoutParams(0, dp(60), 1f).apply {
                if (index > 0) marginStart = dp(3)
            })
        }
        overlay.addView(
            bottomBar,
            FrameLayout.LayoutParams(viewerChromeWidth, dp(72)).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                marginStart = dp(10)
                marginEnd = dp(10)
                bottomMargin = dp(16)
            },
        )

        var currentIndex = initialIndex

        fun renderCurrent() {
            if (
                currentGeneration != generation ||
                !hasReadableMediaAccess() ||
                currentIndex !in items.indices
            ) {
                closeViewer()
                return
            }
            val item = items[currentIndex]
            val cacheKey = viewerCacheKey(item.contentUri)
            preview.setImageDrawable(null)
            preview.tag = cacheKey
            preview.contentDescription = "Trash viewer for ${item.displayName}"
            viewerTitle.text = item.displayName
            viewerSubtitle.text = "${currentIndex + 1} of ${items.size} · ${mediaMetadata(item)}"
            previous.isEnabled = currentIndex > 0
            previous.alpha = if (previous.isEnabled) 1f else 0.30f
            next.isEnabled = currentIndex < items.lastIndex
            next.alpha = if (next.isEnabled) 1f else 0.30f
            loadThumbnail(item, preview, currentGeneration, VIEWER_THUMBNAIL_PX, VIEWER_CACHE_PREFIX)
        }

        previous.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex -= 1
                renderCurrent()
            }
        }
        next.setOnClickListener {
            if (currentIndex < items.lastIndex) {
                currentIndex += 1
                renderCurrent()
            }
        }
        restore.setOnClickListener {
            items.getOrNull(currentIndex)?.let { requestMutation(AndroidMediaMutationMode.RESTORE, listOf(it)) }
        }
        purge.setOnClickListener {
            items.getOrNull(currentIndex)?.let { requestMutation(AndroidMediaMutationMode.DELETE, listOf(it)) }
        }
        more.setOnClickListener {
            items.getOrNull(currentIndex)?.let(::showItemDetails)
        }

        renderCurrent()
    }

    private fun closeViewer(render: Boolean = true) {
        val overlay = viewerOverlay ?: return
        root.removeView(overlay)
        viewerOverlay = null
        applySystemChrome()
        if (render) {
            renderSelectionState()
        }
    }

    private fun requestMutation(mode: AndroidMediaMutationMode, explicitItems: List<MediaItem>? = null) {
        if (pendingMutation != null) return
        val selectedItems = explicitItems ?: GallerySelectionPolicy.resolve(trashedItems, selectedUris)
        if (selectedItems.isEmpty()) return
        val request = try {
            AndroidMediaMutationRequests.create(
                contentResolver = contentResolver,
                contentUris = selectedItems.map { it.contentUri },
                mode = mode,
            )
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, "Gallery refused an invalid Trash request.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: IllegalStateException) {
            Toast.makeText(this, "Trash mutation is unavailable on this device.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: SecurityException) {
            Toast.makeText(this, "Android denied the Trash request.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: RuntimeException) {
            Toast.makeText(this, "Android could not prepare the Trash request.", Toast.LENGTH_SHORT).show()
            return
        }

        pendingMutation = try {
            AndroidMediaMutationPendingStates.capture(request.mode, request.contentUris)
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, "Gallery refused invalid pending Trash state.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startIntentSenderForResult(request.pendingIntent.intentSender, RECYCLE_MUTATION_REQUEST, null, 0, 0, 0)
        } catch (_: IntentSender.SendIntentException) {
            pendingMutation = null
            Toast.makeText(this, "Android could not open the confirmation.", Toast.LENGTH_SHORT).show()
        } catch (_: RuntimeException) {
            pendingMutation = null
            Toast.makeText(this, "Android could not open the confirmation.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadThumbnail(
        item: MediaItem,
        target: ImageView,
        currentGeneration: Int,
        sizePx: Int,
        cachePrefix: String,
    ) {
        val key = "$cachePrefix:${item.contentUri}"
        target.tag = key
        thumbnailCache.get(key)?.let {
            target.setImageBitmap(it)
            return
        }
        try {
            thumbnailExecutor.execute {
                val bitmap = try {
                    contentResolver.loadThumbnail(Uri.parse(item.contentUri), Size(sizePx, sizePx), null)
                } catch (_: Exception) {
                    null
                }
                if (bitmap != null) thumbnailCache.put(key, bitmap)
                runOnUiThread {
                    if (currentGeneration == generation && target.tag == key && bitmap != null) {
                        target.setImageBitmap(bitmap)
                    }
                }
            }
        } catch (_: RuntimeException) {
            // Activity teardown or executor shutdown can cancel presentation work without changing Trash state.
        }
    }

    private fun showItemDetails(item: MediaItem) {
        val dimensions = if (item.width != null && item.height != null) "${item.width} × ${item.height}" else "Unknown"
        val duration = item.durationMillis?.let(::formatDuration) ?: "Not applicable"
        AlertDialog.Builder(this)
            .setTitle(item.displayName)
            .setMessage(
                listOf(
                    "State: In Android Trash",
                    "Type: ${if (item.mimeType.startsWith("video/")) "Video" else "Photo"}",
                    "Album: ${item.albumName ?: "Not grouped"}",
                    "Date: ${DATE_TIME_FORMAT.format(item.capturedAt ?: item.modifiedAt)}",
                    "Dimensions: $dimensions",
                    "Duration: $duration",
                    "Size: ${formatBytes(item.sizeBytes)}",
                    "Retention: Controlled by Android MediaStore",
                ).joinToString("\n"),
            )
            .setPositiveButton("Done", null)
            .show()
    }

    private fun removePurgedFavorites(contentUris: Set<String>) {
        val preferences = getSharedPreferences(LOCAL_STATE_PREFERENCES, MODE_PRIVATE)
        val current = preferences.getStringSet(FAVORITES_KEY, emptySet()).orEmpty().toMutableSet()
        if (current.removeAll(contentUris)) {
            preferences.edit().putStringSet(FAVORITES_KEY, current).apply()
        }
    }

    private fun hasReadableMediaAccess(): Boolean = when {
        Build.VERSION.SDK_INT >= 33 -> {
            checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                (Build.VERSION.SDK_INT >= 34 &&
                    checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED)
        }
        else -> checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun tileDescription(item: MediaItem, selected: Boolean): String =
        if (selectedUris.isNotEmpty()) {
            "${item.displayName}. ${if (selected) "Selected" else "Not selected"}. Double tap to toggle selection."
        } else {
            "${item.displayName}. In Trash. Double tap to open viewer. Long press to select."
        }

    private fun mediaMetadata(item: MediaItem): String {
        val kind = if (item.mimeType.startsWith("video/")) "Video" else "Photo"
        return listOfNotNull(
            kind,
            item.albumName,
            formatBytes(item.sizeBytes),
        ).joinToString(" · ")
    }

    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainder = seconds % 60
        return "$minutes:${remainder.toString().padStart(2, '0')}"
    }

    private fun headerIconAction(
        iconResource: Int,
        description: String,
        onClick: () -> Unit,
    ): TextView = TextView(this).apply {
        text = ""
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(12), dp(12), dp(12))
        setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0)
        compoundDrawableTintList = ColorStateList.valueOf(accentColor())
        GalleryDesignSystem.applyIconButton(this, accentColor())
        contentDescription = description
        tooltipText = description
        GalleryInteractionFeedback.applyBoundedRipple(
            this,
            accentColor(),
            GalleryGlazeContract.SHAPE_CONTROL_DP,
        )
        setOnClickListener { onClick() }
    }

    private fun iconAction(
        iconResource: Int,
        description: String,
        destructive: Boolean = false,
        onClick: () -> Unit,
    ): TextView = TextView(this).apply {
        text = ""
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(12), dp(12), dp(12))
        setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0)
        val foreground = if (destructive) getColor(android.R.color.holo_red_light) else accentColor()
        compoundDrawableTintList = ColorStateList.valueOf(foreground)
        GalleryDesignSystem.applyIconButton(
            this,
            foreground,
            destructive = destructive,
        )
        contentDescription = description
        tooltipText = description
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            stateDescription = if (destructive) "Destructive action" else null
        }
        GalleryInteractionFeedback.applyBoundedRipple(this, foreground, 18)
        setOnClickListener { onClick() }
    }

    private fun viewerIconAction(
        iconResource: Int,
        description: String,
        destructive: Boolean = false,
        onClick: () -> Unit,
    ): TextView = TextView(this).apply {
        text = ""
        gravity = Gravity.CENTER
        minHeight = dp(48)
        minWidth = dp(48)
        setPadding(dp(12), dp(12), dp(12), dp(12))
        setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0)
        compoundDrawableTintList = ColorStateList.valueOf(
            if (destructive) 0xffff8a80.toInt() else Color.WHITE,
        )
        background = GalleryDesignSystem.mediaIconBackground(
            context = context,
            foreground = if (destructive) 0xffff8a80.toInt() else Color.WHITE,
            destructive = destructive,
        )
        isClickable = true
        isFocusable = true
        contentDescription = description
        tooltipText = description
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            stateDescription = if (destructive) "Destructive action" else null
        }
        GalleryInteractionFeedback.applyBoundedRipple(
            this,
            if (destructive) 0xffff8a80.toInt() else Color.WHITE,
            18,
        )
        setOnClickListener { onClick() }
    }

    private fun sectionHeader(label: String): TextView = TextView(this).apply {
        text = label
        GalleryDesignSystem.applySectionLabel(this, primaryTextColor())
        setPadding(dp(2), dp(14), 0, dp(8))
    }

    private fun messageRow(title: String, message: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(2), dp(5), dp(2), dp(5))
        background = null
        addView(TextView(context).apply {
            text = title
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTypeface(typeface, Typeface.BOLD)
        })
        addView(TextView(context).apply {
            text = message
            GalleryDesignSystem.applySupportingText(this, secondaryTextColor())
            setPadding(0, dp(2), 0, 0)
        })
    }

    private fun emptyState(title: String, message: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(22), dp(16), dp(18))
        background = GalleryDesignSystem.emptyStateSurface(context)
        addView(
            ImageView(context).apply {
                setImageResource(R.drawable.ic_gallery_nav_trash)
                setColorFilter(accentColor())
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                alpha = 0.82f
            },
            LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                bottomMargin = dp(7)
            },
        )
        addView(TextView(context).apply {
            text = title
            gravity = Gravity.CENTER
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        })
        addView(TextView(context).apply {
            text = message
            gravity = Gravity.CENTER
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.75f)
            setLineSpacing(0f, 1.04f)
            setPadding(0, dp(4), 0, 0)
        })
    }

    private fun roundedSurface(color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun withAlpha(color: Int, alpha: Float): Int = Color.argb(
        (255 * alpha.coerceIn(0f, 1f)).toInt(),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )

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

    private fun canvasColor(): Int = getColor(R.color.gallery_canvas)
    private fun primaryTextColor(): Int = getColor(R.color.gallery_text_primary)
    private fun secondaryTextColor(): Int = getColor(R.color.gallery_text_secondary)
    private fun accentColor(): Int = getColor(R.color.gallery_accent)

    private fun horizontalGutterDp(): Int =
        GalleryGlazeContract.horizontalGutterDp(resources.configuration.screenWidthDp)

    private fun gridColumns(): Int =
        GalleryGlazeContract.trashGridColumns(resources.configuration.screenWidthDp)

    private fun gridCacheKey(contentUri: String): String = "$GRID_CACHE_PREFIX:$contentUri"
    private fun viewerCacheKey(contentUri: String): String = "$VIEWER_CACHE_PREFIX:$contentUri"
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val MAX_TRASH_ROWS = 250
        private const val RECYCLE_MUTATION_REQUEST = 7301
        private const val STATE_PENDING_MUTATION_MODE = "pending_recycle_mutation_mode"
        private const val STATE_PENDING_MUTATION_URIS = "pending_recycle_mutation_uris"
        private const val GRID_THUMBNAIL_PX = 256
        private const val VIEWER_THUMBNAIL_PX = 1280
        private const val GRID_CACHE_PREFIX = "recycle-grid"
        private const val VIEWER_CACHE_PREFIX = "recycle-viewer"
        private const val SELECTION_OVERLAY_TAG = "goreecloud_recycle_selection_overlay"
        private const val SELECTION_CHECK_TAG = "goreecloud_recycle_selection_check"
        private const val VIDEO_PLAY_TAG = "goreecloud_recycle_video_play"
        private const val LOCAL_STATE_PREFERENCES = "goreecloud_gallery_local_state"
        private const val FAVORITES_KEY = "favorite_content_uris"

        private val DATE_TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a").withZone(ZoneId.systemDefault())

        private fun formatBytes(bytes: Long): String = when {
            bytes >= 1024L * 1024L -> String.format("%.1f MiB", bytes / (1024.0 * 1024.0))
            bytes >= 1024L -> String.format("%.1f KiB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
