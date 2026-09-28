package com.goreecloud.clock.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

data class AlarmSoundOption(val key: String, val title: String)

object AlarmSoundCatalog {
    fun load(context: Context): List<AlarmSoundOption> {
        val options = mutableListOf(
            AlarmSoundOption(AlarmSound.DEFAULT, "System default"),
            AlarmSoundOption(AlarmSound.SILENT, "Silent"),
        )
        runCatching {
            val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
            manager.cursor.use { cursor ->
                var position = 0
                while (cursor.moveToNext()) {
                    val uri = manager.getRingtoneUri(position++)
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    if (uri != null && !title.isNullOrBlank()) {
                        options += AlarmSoundOption(uri.toString(), title)
                    }
                }
            }
        }
        return options.distinctBy { it.key }
    }

    fun title(context: Context, key: String): String = when (key) {
        AlarmSound.DEFAULT -> "System default"
        AlarmSound.SILENT -> "Silent"
        else -> runCatching {
            RingtoneManager.getRingtone(context, Uri.parse(key))?.getTitle(context)
        }.getOrNull().orEmpty().ifBlank { "Selected alarm sound" }
    }

    fun resolveUri(key: String): Uri? = when (key) {
        AlarmSound.SILENT -> null
        AlarmSound.DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        else -> runCatching { Uri.parse(key) }.getOrNull()
    }
}
