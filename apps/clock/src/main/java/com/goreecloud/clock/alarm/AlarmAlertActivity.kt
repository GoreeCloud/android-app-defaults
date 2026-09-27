package com.goreecloud.clock.alarm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.goreecloud.clock.ClockApplication
import com.goreecloud.clock.ui.theme.ClockTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmAlertActivity : ComponentActivity() {
    private var alarmId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)

        val app = application as ClockApplication
        val alarm = app.alarmStore.get(alarmId)
        if (alarm == null) {
            finish()
            return
        }

        setContent {
            ClockTheme(darkTheme = isSystemInDarkTheme()) {
                AlarmAlert(
                    alarm = alarm,
                    onSnooze = {
                        app.alarmScheduler.scheduleSnooze(alarm)
                        stopAlert()
                    },
                    onDismiss = ::stopAlert,
                )
            }
        }
    }

    private fun stopAlert() {
        NotificationManagerCompat.from(this).cancel(AlarmReceiver.notificationId(alarmId))
        finishAndRemoveTask()
    }
}

@Composable
private fun AlarmAlert(
    alarm: Alarm,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    val time = LocalTime.of(alarm.hour, alarm.minute)
        .format(DateTimeFormatter.ofPattern("h:mm a"))

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = alarm.label.ifBlank { "Alarm" },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))
            Button(onClick = onSnooze) {
                Text("Snooze ${alarm.snoozeMinutes} min")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}
