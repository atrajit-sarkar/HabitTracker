package com.example.habittracker.data.local

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

enum class NotificationSound(val displayName: String, val systemSoundType: Int?) {
    DEFAULT("Default", RingtoneManager.TYPE_NOTIFICATION),
    RINGTONE("Ringtone", RingtoneManager.TYPE_RINGTONE),
    ALARM("Alarm", RingtoneManager.TYPE_ALARM),
    SYSTEM_DEFAULT("System Default", null);

    fun getUri(context: Context): Uri? {
        return when (this) {
            SYSTEM_DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            else -> systemSoundType?.let { type ->
                RingtoneManager.getDefaultUri(type)
            }
        }
    }

    companion object {
        fun fromName(name: String): NotificationSound {
            return values().find { it.name == name } ?: DEFAULT
        }
    }
}