package com.goreecloud.gallery

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoEditorSystemBarsRuntimeTest {
    @Test
    fun photoEditorChromeStaysInsideAndroidSafeAreas() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "gallery-editor-insets-test.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/GoreeCloud Gallery Tests",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = checkNotNull(resolver.insert(collection, values))

        try {
            resolver.openOutputStream(uri, "w").use { stream ->
                checkNotNull(stream)
                val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                try {
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                } finally {
                    bitmap.recycle()
                }
            }
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null,
            )

            val intent = Intent(context, PhotoEditorActivity::class.java).apply {
                putExtra(GalleryPhotoEditorContract.EXTRA_CONTENT_URI, uri.toString())
                putExtra(
                    GalleryPhotoEditorContract.EXTRA_DISPLAY_NAME,
                    "gallery-editor-insets-test.png",
                )
                putExtra(GalleryPhotoEditorContract.EXTRA_MIME_TYPE, "image/png")
            }
            val scenario: ActivityScenario<PhotoEditorActivity> = ActivityScenario.launch(intent)
            scenario.use {
                it.onActivity { activity ->
                    val androidContent = activity.findViewById<ViewGroup>(android.R.id.content)
                    val root = androidContent.getChildAt(0) as FrameLayout
                    val insets = root.rootWindowInsets
                    assertNotNull("Photo editor root must receive Android window insets", insets)

                    val safe = currentSafeInsets(checkNotNull(insets))
                    val decorRect = Rect().also { activity.window.decorView.getGlobalVisibleRect(it) }

                    val cancel = findByContentDescription(
                        root,
                        "Cancel editing and keep the original photo",
                    )
                    val save = findByContentDescription(
                        root,
                        "Save the edited photo as a new copy and keep the original",
                    )
                    assertNotNull("Photo editor Cancel control must be rendered", cancel)
                    assertNotNull("Photo editor Save copy control must be rendered", save)

                    listOf(checkNotNull(cancel), checkNotNull(save)).forEach { control ->
                        val rect = Rect().also { control.getGlobalVisibleRect(it) }
                        assertTrue(
                            "Photo editor top controls must stay below status/cutout safe edge",
                            rect.top >= decorRect.top + safe.top,
                        )
                        assertTrue(
                            "Photo editor top controls must stay inside left safe edge",
                            rect.left >= decorRect.left + safe.left,
                        )
                        assertTrue(
                            "Photo editor top controls must stay inside right safe edge",
                            rect.right <= decorRect.right - safe.right,
                        )
                    }

                    val controlsScroll = (0 until root.childCount)
                        .map(root::getChildAt)
                        .filterIsInstance<HorizontalScrollView>()
                        .single()
                    val controlsRect = Rect().also { controlsScroll.getGlobalVisibleRect(it) }
                    assertTrue(
                        "Photo editor controls must stay above navigation/gesture safe edge",
                        controlsRect.bottom <= decorRect.bottom - safe.bottom,
                    )
                    assertTrue(
                        "Photo editor controls must stay inside left safe edge",
                        controlsRect.left >= decorRect.left + safe.left,
                    )
                    assertTrue(
                        "Photo editor controls must stay inside right safe edge",
                        controlsRect.right <= decorRect.right - safe.right,
                    )
                }
            }
        } finally {
            resolver.delete(uri, null, null)
        }
    }

    private fun findByContentDescription(root: ViewGroup, description: String): View? {
        for (index in 0 until root.childCount) {
            val child = root.getChildAt(index)
            if (child.contentDescription?.toString() == description) return child
            if (child is ViewGroup) {
                findByContentDescription(child, description)?.let { return it }
            }
        }
        return null
    }

    private fun currentSafeInsets(insets: WindowInsets): Rect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val barsAndCutout = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            val gestures = insets.getInsets(WindowInsets.Type.mandatorySystemGestures())
            return Rect(
                maxOf(barsAndCutout.left, gestures.left),
                maxOf(barsAndCutout.top, gestures.top),
                maxOf(barsAndCutout.right, gestures.right),
                maxOf(barsAndCutout.bottom, gestures.bottom),
            )
        }

        @Suppress("DEPRECATION")
        val cutout = insets.displayCutout
        @Suppress("DEPRECATION")
        return Rect(
            maxOf(insets.systemWindowInsetLeft, cutout?.safeInsetLeft ?: 0),
            maxOf(insets.systemWindowInsetTop, cutout?.safeInsetTop ?: 0),
            maxOf(insets.systemWindowInsetRight, cutout?.safeInsetRight ?: 0),
            maxOf(insets.systemWindowInsetBottom, cutout?.safeInsetBottom ?: 0),
        )
    }
}
