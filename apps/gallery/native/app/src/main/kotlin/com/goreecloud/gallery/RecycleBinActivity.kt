package com.goreecloud.gallery

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
    private lateinit var headerTitle: TextView
    private lateinit var headerSubtitle: TextView
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
                        if (mutation.contentUris.size == 1) "Restored from Recycle Bin"
                        else "Restored ${mutation.contentUris.size} items"
                    AndroidMediaMutationMode.DELETE ->
                        if (mutation.contentUris.size == 1) "Deleted permanently"
                        else "Deleted ${mutation.contentUris.size} items permanently"
                    AndroidMediaMutationMode.TRASH -> "Recycle Bin updated"
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

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(horizontalGutterDp()), dp(14), dp(horizontalGutterDp()), dp(110))
            setBackgroundColor(canvasColor())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            textAction("‹", "Back to GoreeCloud Gallery") { finish() }.apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
            },
            LinearLayout.LayoutParams(dp(48), dp(48)),
        )

        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        headerTitle = TextView(this).apply {
            text = "Recycle Bin"
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTypeface(typeface, Typeface.BOLD)
        }
        headerSubtitle = TextView(this).apply {
            text = "Android controls Trash retention and expiration"
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        }
        titles.addView(headerTitle)
        titles.addView(headerSubtitle)
        header.addView(titles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(4)
        })
        header.addView(
            textAction("Refresh", "Refresh Recycle Bin") { loadRecycleBin() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)),
        )
        content.addView(header)

        content.addView(TextView(this).apply {
            text = "Items here remain under Android MediaStore Trash authority. Restore and permanent deletion always require Android confirmation."
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setLineSpacing(0f, 1.08f)
            setPadding(dp(4), dp(10), dp(4), dp(8))
        })

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        content.addView(body)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        root.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        actionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = roundedSurface(if (isNightMode()) 0xf21d1d1f.toInt() else 0xf2ffffff.toInt(), 26)
            elevation = dp(4).toFloat()
        }
        root.addView(
            actionBar,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply {
                gravity = Gravity.BOTTOM
                marginStart = dp(22)
                marginEnd = dp(22)
                bottomMargin = dp(12)
            },
        )

        setContentView(root)
        applySystemChrome()
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
            headerTitle.text = "Recycle Bin"
            headerSubtitle.text = "Requires Android 11 or newer"
            body.addView(emptyState("Recycle Bin unavailable", "Android MediaStore Trash browsing requires Android 11 or newer."))
            return
        }
        if (!hasReadableMediaAccess()) {
            trashedItems = emptyList()
            headerTitle.text = "Recycle Bin"
            headerSubtitle.text = "Media access required"
            body.addView(emptyState("Media access required", "Open GoreeCloud Gallery and allow Android media access before browsing the Recycle Bin."))
            return
        }

        headerTitle.text = "Recycle Bin"
        headerSubtitle.text = "Loading Android MediaStore Trash…"
        body.addView(messageRow("Loading Recycle Bin", "Reading only image/video items Android currently exposes as trashed."))

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
        headerTitle.text = "Recycle Bin"
        headerSubtitle.text = buildString {
            append(if (count == 1) "1 item" else "$count items")
            if (rejectedRows > 0) append(" · $rejectedRows skipped")
            append(" · Android controls retention")
        }

        if (trashedItems.isEmpty()) {
            body.addView(emptyState("Recycle Bin is empty", "Photos and videos moved to Android Trash will appear here when Android exposes them to Gallery."))
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
            headerTitle.text = "Recycle Bin"
            headerSubtitle.text = "Recycle Bin unavailable"
            body.removeAllViews()
            body.addView(messageRow("Recycle Bin unavailable", message))
            renderActionBar()
        }
    }

    private fun renderMediaGrid(items: List<MediaItem>, currentGeneration: Int) {
        val columns = gridColumns()
        val gap = dp(4)
        val totalGap = gap * (columns - 1)
        val tileSize = ((resources.displayMetrics.widthPixels - dp(horizontalGutterDp() * 2) - totalGap) / columns)
            .coerceAtLeast(dp(72))

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
            isSelected = selected
            contentDescription = tileDescription(item, selected)
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
                addView(TextView(context).apply {
                    text = item.durationMillis?.let(::formatDuration) ?: "VIDEO"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setPadding(dp(7), dp(3), dp(7), dp(3))
                    background = roundedSurface(0xb3000000.toInt(), 9)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.END or Gravity.BOTTOM
                    marginEnd = dp(5)
                    bottomMargin = dp(5)
                })
            }
            addView(
                TextView(context).apply {
                    tag = SELECTION_CHECK_TAG
                    visibility = if (selected) View.VISIBLE else View.GONE
                    text = "✓"
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setTypeface(typeface, Typeface.BOLD)
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
            tile.findViewWithTag<View>(SELECTION_OVERLAY_TAG)?.visibility = if (selected) View.VISIBLE else View.GONE
            tile.findViewWithTag<View>(SELECTION_CHECK_TAG)?.visibility = if (selected) View.VISIBLE else View.GONE
        }
        headerTitle.text = if (selectedUris.isEmpty()) "Recycle Bin" else if (selectedUris.size == 1) "1 selected" else "${selectedUris.size} selected"
        if (selectedUris.isEmpty()) {
            headerSubtitle.text = if (trashedItems.size == 1) "1 item · Android controls retention" else "${trashedItems.size} items · Android controls retention"
        } else {
            headerSubtitle.text = "Restore or permanently delete the current selection"
        }
        renderActionBar()
    }

    private fun renderActionBar() {
        if (!::actionBar.isInitialized) return
        actionBar.removeAllViews()
        if (selectedUris.isEmpty() || viewerOverlay != null) {
            actionBar.visibility = View.GONE
            return
        }
        actionBar.visibility = View.VISIBLE
        val actions = listOf(
            textAction("Select all", "Select all currently loaded trashed media") {
                selectedUris.clear()
                selectedUris.addAll(GallerySelectionPolicy.selectAll(trashedItems))
                renderSelectionState()
            },
            textAction("Restore", "Restore selected media through Android confirmation") {
                requestMutation(AndroidMediaMutationMode.RESTORE)
            },
            textAction("Delete permanently", "Permanently delete selected media through Android confirmation") {
                requestMutation(AndroidMediaMutationMode.DELETE)
            },
            textAction("Cancel", "Clear Recycle Bin selection") {
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

        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        viewerOverlay = overlay
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
            background = roundedSurface(0xd9141416.toInt(), 22)
        }
        val viewerBack = viewerAction("‹", "Close Recycle Bin viewer") { closeViewer() }.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
        }
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
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)).apply {
                gravity = Gravity.TOP
                marginStart = dp(10)
                marginEnd = dp(10)
                topMargin = dp(6)
            },
        )

        val previous = viewerAction("‹", "Previous trashed media") {}
        val next = viewerAction("›", "Next trashed media") {}
        previous.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
        next.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
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
            background = roundedSurface(0xe8141416.toInt(), 24)
        }
        val restore = viewerAction("Restore", "Restore this media through Android confirmation") {}
        val purge = viewerAction("Delete permanently", "Permanently delete this media through Android confirmation") {}
        val more = viewerAction("More", "Show details for this trashed media") {}
        listOf(restore, purge, more).forEachIndexed { index, action ->
            bottomBar.addView(action, LinearLayout.LayoutParams(0, dp(60), 1f).apply {
                if (index > 0) marginStart = dp(3)
            })
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
            preview.contentDescription = "Recycle Bin viewer for ${item.displayName}"
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
            Toast.makeText(this, "Gallery refused an invalid Recycle Bin request.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: IllegalStateException) {
            Toast.makeText(this, "Recycle Bin mutation is unavailable on this device.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: SecurityException) {
            Toast.makeText(this, "Android denied the Recycle Bin request.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: RuntimeException) {
            Toast.makeText(this, "Android could not prepare the Recycle Bin request.", Toast.LENGTH_SHORT).show()
            return
        }

        pendingMutation = try {
            AndroidMediaMutationPendingStates.capture(request.mode, request.contentUris)
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, "Gallery refused invalid pending Recycle Bin state.", Toast.LENGTH_SHORT).show()
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
                    "State: In Android Recycle Bin",
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
            "${item.displayName}. In Recycle Bin. Double tap to open viewer. Long press to select."
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

    private fun textAction(label: String, description: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        minHeight = dp(48)
        setPadding(dp(10), 0, dp(10), 0)
        setTextColor(accentColor())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, if (label.length > 12) 10.5f else 12f)
        setTypeface(typeface, Typeface.BOLD)
        background = roundedSurface(withAlpha(accentColor(), 0.10f), 18)
        isClickable = true
        isFocusable = true
        contentDescription = description
        setOnClickListener { onClick() }
    }

    private fun viewerAction(label: String, description: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        minHeight = dp(48)
        minWidth = dp(48)
        setPadding(dp(8), 0, dp(8), 0)
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, if (label.length > 12) 10.5f else 12f)
        setTypeface(typeface, Typeface.BOLD)
        background = roundedSurface(0x26ffffff, 18)
        isClickable = true
        isFocusable = true
        contentDescription = description
        setOnClickListener { onClick() }
    }

    private fun sectionHeader(label: String): TextView = TextView(this).apply {
        text = label
        setTextColor(primaryTextColor())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(2), dp(8), 0, dp(10))
    }

    private fun messageRow(title: String, message: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = roundedSurface(withAlpha(primaryTextColor(), if (isNightMode()) 0.10f else 0.045f), 20)
        addView(TextView(context).apply {
            text = title
            setTextColor(primaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
        })
        addView(TextView(context).apply {
            text = message
            setTextColor(secondaryTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(5), 0, 0)
        })
    }

    private fun emptyState(title: String, message: String): View = messageRow(title, message)

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

    private fun canvasColor(): Int = if (isNightMode()) 0xff0b0b0d.toInt() else 0xfffbfbfc.toInt()
    private fun primaryTextColor(): Int = if (isNightMode()) Color.WHITE else 0xff202124.toInt()
    private fun secondaryTextColor(): Int = if (isNightMode()) 0xffb8b8bd.toInt() else 0xff73757a.toInt()
    private fun accentColor(): Int = if (isNightMode()) 0xff70d8ca.toInt() else 0xff008f83.toInt()

    private fun horizontalGutterDp(): Int = when {
        resources.configuration.screenWidthDp >= 840 -> 32
        resources.configuration.screenWidthDp >= 600 -> 24
        else -> 16
    }

    private fun gridColumns(): Int = when {
        resources.configuration.screenWidthDp >= 840 -> 6
        resources.configuration.screenWidthDp >= 600 -> 5
        resources.configuration.screenWidthDp >= 360 -> 4
        else -> 3
    }

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
