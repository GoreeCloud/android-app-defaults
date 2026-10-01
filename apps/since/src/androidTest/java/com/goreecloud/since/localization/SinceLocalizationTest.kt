package com.goreecloud.since.localization

import android.content.res.Configuration
import android.os.LocaleList
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.since.R
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SinceLocalizationTest {
    @Test
    fun arabicResourcesResolveWithRtlLayoutDirection() {
        val arabic = Locale.forLanguageTag("ar")
        val configuration = Configuration(
            ApplicationProvider.getApplicationContext<android.content.Context>()
                .resources
                .configuration
        ).apply {
            setLocales(LocaleList(arabic))
            setLayoutDirection(arabic)
        }

        val localizedContext =
            ApplicationProvider.getApplicationContext<android.content.Context>()
                .createConfigurationContext(configuration)

        assertEquals(
            View.LAYOUT_DIRECTION_RTL,
            localizedContext.resources.configuration.layoutDirection,
        )
        assertEquals("إضافة متتبّع", localizedContext.getString(R.string.add_tracker))
        assertEquals("حفظ", localizedContext.getString(R.string.save))
        assertEquals("حدث دائم", localizedContext.getString(R.string.tracker_kind_event))
        assertEquals(
            "تنسيق العرض الافتراضي",
            localizedContext.getString(R.string.settings_default_display_format),
        )
        assertEquals(
            "إظهار الثواني",
            localizedContext.getString(R.string.settings_show_seconds),
        )
        assertEquals(
            "1 يوم · 2 ساعة · 3 دقيقة · 4 ثانية",
            localizedContext.getString(R.string.elapsed_days_detail_seconds, 1, 2, 3, 4),
        )
    }
}
