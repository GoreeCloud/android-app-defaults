package com.goreecloud.gallery

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.WeakHashMap

/**
 * Physical-device visual refinement for Gallery's native Glaze chrome.
 *
 * GalleryActivity and RecycleBinActivity retain all navigation/media authority. This helper only
 * refines already-rendered first-party controls. It never changes Android permissions, media scope,
 * mutation authority, destination semantics, or Activity-owned navigation accessibility identity.
 * Repeated layout work is intentionally bounded to direct bottom/viewer chrome plus the five
 * primary navigation controls; large media grids are not repeatedly traversed.
 */
object GalleryUiRefinement {
    private data class Installation(
        val root: FrameLayout,
        val listener: ViewTreeObserver.OnGlobalLayoutListener,
    )

    private val installations = WeakHashMap<Activity, Installation>()

    private val navigationIcons = mapOf(
        "Photos" to R.drawable.ic_gallery_nav_photos,
        "Albums" to R.drawable.ic_gallery_nav_albums,
        "Videos" to R.drawable.ic_gallery_nav_videos,
        "Trash" to R.drawable.ic_gallery_nav_trash,
        "Settings" to R.drawable.ic_gallery_nav_settings,
    )

    private val persistentControlDescriptions = setOf(
        "Back to Albums",
        "Search the current Gallery destination",
        "Change Gallery sort order",
        "Close search",
        "Gallery media access action",
    )

    private val primaryPersistentControlDescriptions = setOf(
        "Search the current Gallery destination",
        "Change Gallery sort order",
        "Gallery media access action",
    )

    private val recycleBinActionDescriptions = setOf(
        "Select all currently loaded trashed media",
        "Restore selected media through Android confirmation",
        "Permanently delete selected media through Android confirmation",
        "Clear Trash selection",
    )

    private val recycleBinViewerDescriptions = setOf(
        "Close Trash viewer",
        "Previous trashed media",
        "Next trashed media",
        "Restore this media through Android confirmation",
        "Permanently delete this media through Android confirmation",
        "Show details for this trashed media",
    )

    private val destructiveDescriptions = setOf(
        "Permanently delete selected media through Android confirmation",
        "Permanently delete this media through Android confirmation",
    )

    fun install(activity: Activity) {
        if (
            activity !is GalleryActivity &&
            activity !is RecycleBinActivity
        ) return
        if (installations.containsKey(activity)) return

        val androidContent = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = androidContent.getChildAt(0) as? FrameLayout ?: return
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            refine(activity, root)
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
        installations[activity] = Installation(root, listener)
        root.post { refine(activity, root) }
    }

    fun uninstall(activity: Activity) {
        val installation = installations.remove(activity) ?: return
        if (installation.root.viewTreeObserver.isAlive) {
            installation.root.viewTreeObserver.removeOnGlobalLayoutListener(installation.listener)
        }
    }

    private fun refine(activity: Activity, root: FrameLayout) {
        if (root.getTag(R.id.gallery_ui_refinement_tag) != ROOT_REFINED_MARKER) {
            refinePersistentControls(activity, root)
            root.setTag(R.id.gallery_ui_refinement_tag, ROOT_REFINED_MARKER)
        }

        when (activity) {
            is GalleryActivity -> {
                findNavigationCapsule(root)?.let { refineNavigation(activity, it) }
            }
            is RecycleBinActivity -> {
                findNavigationCapsule(root)?.let { refineNavigation(activity, it) }
                refineRecycleBinChrome(activity, root)
            }
        }
    }

    private fun refinePersistentControls(activity: Activity, root: FrameLayout) {
        walk(root) { view ->
            val description = view.contentDescription?.toString() ?: return@walk
            val canonicalDescription = when {
                description.startsWith("Gallery media access action") ->
                    "Gallery media access action"
                else -> description
            }
            if (canonicalDescription !in persistentControlDescriptions) return@walk
            val primary = canonicalDescription in primaryPersistentControlDescriptions
            styleControl(
                activity = activity,
                view = view,
                marker = "control:$canonicalDescription",
                role = if (primary) GalleryGlazeSurfaces.Role.CONTROL else GalleryGlazeSurfaces.Role.RAISED,
                elevationDp = if (primary) 2 else 1,
            )
        }
    }

    private fun refineNavigation(activity: Activity, capsule: LinearLayout) {
        val mode = GalleryNavigationDisplayMode.ICONS_ONLY
        val capsuleMarker = "navigation-rail-v7:glyph-only"
        if (capsule.getTag(R.id.gallery_navigation_surface_tag) != capsuleMarker) {
            capsule.background = GalleryGlazeSurfaces.drawable(
                activity,
                GalleryGlazeSurfaces.Role.CHROME,
                GalleryGlazeContract.NAVIGATION_RADIUS_DP,
            )
            capsule.elevation = dp(activity, GalleryGlazeContract.NAVIGATION_ELEVATION_DP).toFloat()
            capsule.setPadding(0, 0, 0, 0)
            capsule.clipChildren = true
            capsule.clipToOutline = true
            capsule.setTag(R.id.gallery_navigation_surface_tag, capsuleMarker)
        }

        for (index in 0 until capsule.childCount) {
            val item = capsule.getChildAt(index) as? TextView ?: continue
            val label = navigationLabel(item) ?: continue
            val icon = navigationIcons[label] ?: continue
            val activityDescription = item.contentDescription?.toString().orEmpty()
            val selected = activityDescription == "$label, selected" || item.isSelected
            val marker = "navigation:$label:$selected:glyph-only:v7"
            if (item.getTag(R.id.gallery_ui_refinement_tag) == marker) continue

            val foreground = if (selected) activityAccent(activity) else activityPrimaryText(activity)
            GalleryNavigationStyling.apply(
                activity = activity,
                item = item,
                label = label,
                iconRes = icon,
                mode = mode,
                selected = selected,
                foreground = foreground,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                item.stateDescription = if (selected) "Selected" else null
            }
            item.setTag(R.id.gallery_ui_refinement_tag, marker)
        }
    }

    private fun findNavigationCapsule(root: FrameLayout): LinearLayout? {
        for (index in 0 until root.childCount) {
            val child = root.getChildAt(index) as? LinearLayout ?: continue
            if (child.childCount != navigationIcons.size) continue
            val labels = (0 until child.childCount).mapNotNull { childIndex ->
                (child.getChildAt(childIndex) as? TextView)?.let(::navigationLabel)
            }
            if (labels.size == navigationIcons.size && labels.toSet() == navigationIcons.keys) {
                return child
            }
        }
        return null
    }

    private fun navigationLabel(item: TextView): String? {
        val visibleLabel = item.text?.toString().orEmpty()
        if (visibleLabel in navigationIcons) return visibleLabel

        val description = item.contentDescription?.toString().orEmpty()
        return navigationIcons.keys.firstOrNull { label ->
            description == label || description.startsWith("$label,")
        }
    }

    private fun navigationDisplayMode(activity: Activity): GalleryNavigationDisplayMode =
        GalleryNavigationDisplayMode.fromStored(
            activity.getSharedPreferences(
                GallerySetupPreferences.PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            ).getString(GalleryNavigationDisplayMode.PREFERENCE_KEY, null),
        )

    private fun refineRecycleBinChrome(activity: RecycleBinActivity, root: FrameLayout) {
        var actionBar: LinearLayout? = null
        for (index in 0 until root.childCount) {
            when (val child = root.getChildAt(index)) {
                is LinearLayout -> if (refineRecycleBinActionBar(activity, child)) actionBar = child
                is FrameLayout -> refineRecycleBinViewerOverlay(activity, child)
            }
        }
        refineRecycleBinScrollReservation(activity, root, actionBar)
    }

    private fun refineRecycleBinActionBar(activity: RecycleBinActivity, child: LinearLayout): Boolean {
        val params = child.layoutParams as? FrameLayout.LayoutParams ?: return false
        if (
            params.gravity == -1 ||
            (params.gravity and Gravity.VERTICAL_GRAVITY_MASK) != Gravity.BOTTOM ||
            child.childCount != RECYCLE_BIN_ACTION_COUNT
        ) return false

        val barMarker = "recycle-action-bar:${child.childCount}:v2"
        if (child.getTag(R.id.gallery_ui_refinement_tag) != barMarker) {
            child.background = GalleryGlazeSurfaces.drawable(
                activity,
                GalleryGlazeSurfaces.Role.CHROME,
                GalleryGlazeContract.SHAPE_CAPSULE_DP,
            )
            child.elevation = dp(activity, GalleryGlazeContract.NAVIGATION_ELEVATION_DP).toFloat()
            child.clipChildren = true
            child.clipToOutline = true
            child.setTag(R.id.gallery_ui_refinement_tag, barMarker)
        }

        for (index in 0 until child.childCount) {
            val control = child.getChildAt(index)
            val description = control.contentDescription?.toString() ?: continue
            if (description !in recycleBinActionDescriptions) continue
            styleControl(activity, control, "recycle-action:$description:v3")
            applyDestructiveSemantics(activity, control, description)
        }
        return true
    }

    private fun refineRecycleBinScrollReservation(
        activity: RecycleBinActivity,
        root: FrameLayout,
        actionBar: LinearLayout?,
    ) {
        val scroll = (0 until root.childCount)
            .map(root::getChildAt)
            .filterIsInstance<ScrollView>()
            .singleOrNull() ?: return
        val actionVisible = actionBar?.visibility == View.VISIBLE
        val desiredBottomPadding = if (actionVisible) dp(activity, RECYCLE_BIN_ACTION_RESERVED_DP) else 0
        if (scroll.paddingBottom == desiredBottomPadding && scroll.clipToPadding) return

        // The selection-action capsule is intentionally persistent chrome. Reserve a real viewport
        // lane for it instead of allowing media tiles to paint underneath the controls on-device.
        scroll.setPadding(scroll.paddingLeft, scroll.paddingTop, scroll.paddingRight, desiredBottomPadding)
        scroll.clipToPadding = true
    }

    private fun refineRecycleBinViewerOverlay(activity: RecycleBinActivity, overlay: FrameLayout) {
        if (!containsDescription(overlay, "Close Trash viewer")) return

        for (index in 0 until overlay.childCount) {
            val child = overlay.getChildAt(index)
            if (child is LinearLayout) {
                val params = child.layoutParams as? FrameLayout.LayoutParams
                val verticalGravity = params?.gravity?.and(Gravity.VERTICAL_GRAVITY_MASK)
                if (verticalGravity == Gravity.TOP || verticalGravity == Gravity.BOTTOM) {
                    val marker = "recycle-viewer-chrome:$verticalGravity"
                    if (child.getTag(R.id.gallery_ui_refinement_tag) != marker) {
                        child.background = GalleryGlazeSurfaces.drawable(
                            activity,
                            GalleryGlazeSurfaces.Role.OVERLAY,
                            GalleryGlazeContract.SHAPE_ROUNDED_DP,
                        )
                        child.setTag(R.id.gallery_ui_refinement_tag, marker)
                    }
                }
            }
        }

        walk(overlay) { view ->
            val description = view.contentDescription?.toString() ?: return@walk
            if (description !in recycleBinViewerDescriptions) return@walk
            styleControl(activity, view, "recycle-viewer:$description:v2")
            applyDestructiveSemantics(activity, view, description)
        }
    }

    private fun applyDestructiveSemantics(activity: Activity, view: View, description: String) {
        if (description !in destructiveDescriptions) return
        (view as? TextView)?.setTextColor(activityError(activity))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.stateDescription = "Destructive action"
        }
    }

    private fun containsDescription(root: View, expected: String): Boolean {
        if (root.contentDescription?.toString() == expected) return true
        if (root !is ViewGroup) return false
        for (index in 0 until root.childCount) {
            if (containsDescription(root.getChildAt(index), expected)) return true
        }
        return false
    }

    private fun styleControl(
        activity: Activity,
        view: View,
        marker: String,
        role: GalleryGlazeSurfaces.Role = GalleryGlazeSurfaces.Role.CONTROL,
        elevationDp: Int = 2,
    ) {
        if (view.getTag(R.id.gallery_ui_refinement_tag) == marker) return
        view.background = GalleryGlazeSurfaces.drawable(
            activity,
            role,
            GalleryGlazeContract.SHAPE_CONTROL_DP,
        )
        view.elevation = dp(activity, elevationDp).toFloat()
        view.setTag(R.id.gallery_ui_refinement_tag, marker)
    }

    private fun walk(root: View, visitor: (View) -> Unit) {
        visitor(root)
        if (root !is ViewGroup) return
        for (index in 0 until root.childCount) {
            walk(root.getChildAt(index), visitor)
        }
    }

    private fun activityAccent(activity: Activity): Int = themeColor(
        activity,
        android.R.attr.colorAccent,
        0xff2e7d6f.toInt(),
    )

    private fun activityError(activity: Activity): Int = themeColor(
        activity,
        android.R.attr.colorError,
        0xffb3261e.toInt(),
    )

    private fun activityPrimaryText(activity: Activity): Int = themeColor(
        activity,
        android.R.attr.textColorPrimary,
        0xff1d1d1f.toInt(),
    )

    private fun themeColor(activity: Activity, attribute: Int, fallback: Int): Int {
        val attributes = activity.obtainStyledAttributes(intArrayOf(attribute))
        return try {
            attributes.getColor(0, fallback)
        } finally {
            attributes.recycle()
        }
    }

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()

    private const val ROOT_REFINED_MARKER = "gallery-ui-refinement-root-v3"
    private const val RECYCLE_BIN_ACTION_COUNT = 4
    private const val RECYCLE_BIN_ACTION_RESERVED_DP = 86
}
