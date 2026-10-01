package com.goreecloud.since.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.since.domain.model.DisplayFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SincePreferencesRuntimeTest {
    @Test
    fun displayPreferencesPersistAcrossRepositoryInstances() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SincePreferencesRepository(context)

        try {
            repository.setDefaultDisplayFormat(DisplayFormat.YEARS)
            repository.setShowSeconds(false)

            assertEquals(DisplayFormat.YEARS, repository.defaultDisplayFormat.first())
            assertFalse(repository.showSeconds.first())

            val reopened = SincePreferencesRepository(context)
            assertEquals(DisplayFormat.YEARS, reopened.defaultDisplayFormat.first())
            assertFalse(reopened.showSeconds.first())
        } finally {
            repository.setDefaultDisplayFormat(DisplayFormat.DAYS)
            repository.setShowSeconds(true)

            assertEquals(DisplayFormat.DAYS, repository.defaultDisplayFormat.first())
            assertTrue(repository.showSeconds.first())
        }
    }
}
