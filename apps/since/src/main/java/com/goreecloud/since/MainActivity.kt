package com.goreecloud.since

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.goreecloud.since.ui.SinceApp
import com.goreecloud.since.ui.theme.SinceTheme
import java.time.Clock

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sinceApplication = application as SinceApplication

        setContent {
            SinceTheme {
                SinceApp(
                    repository = sinceApplication.trackerRepository,
                    clock = Clock.systemUTC(),
                )
            }
        }
    }
}
