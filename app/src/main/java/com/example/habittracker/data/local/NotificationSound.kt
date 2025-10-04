package com.example.habittracker.data.local

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

data class NotificationSound(
    val id: String,
    val displayName: String,
    val uri: String,
    val type: SoundType = SoundType.NOTIFICATION
) {
    enum class SoundType {
        NOTIFICATION, RINGTONE, ALARM, CUSTOM
    }

    companion object {
        // Default sound IDs
        const val DEFAULT_ID = "default"
        const val SYSTEM_DEFAULT_ID = "system_default"
        const val DEFAULT_RINGTONE_ID = "default_ringtone"
        const val DEFAULT_ALARM_ID = "default_alarm"

        val DEFAULT = NotificationSound(
            id = DEFAULT_ID,
            displayName = "Default Notification",
            uri = "",
            type = SoundType.NOTIFICATION
        )

        val SYSTEM_DEFAULT = NotificationSound(
            id = SYSTEM_DEFAULT_ID,
            displayName = "System Default",
            uri = "",
            type = SoundType.NOTIFICATION
        )

        fun fromId(id: String, allSounds: List<NotificationSound>): NotificationSound {
            return allSounds.find { it.id == id } ?: DEFAULT
        }

        /**
         * Get all available notification sounds from the device
         */
        fun getAllAvailableSounds(context: Context): List<NotificationSound> {
            val sounds = mutableListOf<NotificationSound>()

            // Add default sounds
            sounds.add(DEFAULT)
            sounds.add(SYSTEM_DEFAULT)

            try {
                // Get notification sounds
                val notificationManager = RingtoneManager(context)
                notificationManager.setType(RingtoneManager.TYPE_NOTIFICATION)
                
                val cursor = notificationManager.cursor
                var index = 0
                while (cursor.moveToNext()) {
                    try {
                        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                        val uri = notificationManager.getRingtoneUri(index)
                        
                        if (title != null && uri != null) {
                            sounds.add(
                                NotificationSound(
                                    id = "notification_$index",
                                    displayName = title,
                                    uri = uri.toString(),
                                    type = SoundType.NOTIFICATION
                                )
                            )
                        }
                        index++
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationSound", "Error reading notification sound: ${e.message}")
                    }
                }
                cursor.close()
            } catch (e: Exception) {
                android.util.Log.e("NotificationSound", "Error loading notification sounds: ${e.message}")
            }

            try {
                // Get ringtones
                val ringtoneManager = RingtoneManager(context)
                ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE)
                
                val cursor = ringtoneManager.cursor
                var index = 0
                while (cursor.moveToNext() && index < 10) { // Limit ringtones to 10
                    try {
                        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                        val uri = ringtoneManager.getRingtoneUri(index)
                        
                        if (title != null && uri != null) {
                            sounds.add(
                                NotificationSound(
                                    id = "ringtone_$index",
                                    displayName = "$title (Ringtone)",
                                    uri = uri.toString(),
                                    type = SoundType.RINGTONE
                                )
                            )
                        }
                        index++
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationSound", "Error reading ringtone: ${e.message}")
                    }
                }
                cursor.close()
            } catch (e: Exception) {
                android.util.Log.e("NotificationSound", "Error loading ringtones: ${e.message}")
            }

            try {
                // Get alarm sounds
                val alarmManager = RingtoneManager(context)
                alarmManager.setType(RingtoneManager.TYPE_ALARM)
                
                val cursor = alarmManager.cursor
                var index = 0
                while (cursor.moveToNext() && index < 10) { // Limit alarms to 10
                    try {
                        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                        val uri = alarmManager.getRingtoneUri(index)
                        
                        if (title != null && uri != null) {
                            sounds.add(
                                NotificationSound(
                                    id = "alarm_$index",
                                    displayName = "$title (Alarm)",
                                    uri = uri.toString(),
                                    type = SoundType.ALARM
                                )
                            )
                        }
                        index++
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationSound", "Error reading alarm sound: ${e.message}")
                    }
                }
                cursor.close()
            } catch (e: Exception) {
                android.util.Log.e("NotificationSound", "Error loading alarm sounds: ${e.message}")
            }

            return sounds
        }

        /**
         * Get the actual URI for playing the sound
         */
        fun getActualUri(context: Context, sound: NotificationSound): Uri? {
            return when (sound.id) {
                DEFAULT_ID -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                SYSTEM_DEFAULT_ID -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                else -> if (sound.uri.isNotEmpty()) Uri.parse(sound.uri) else null
            }
        }
    }

    fun getUri(context: Context): Uri? {
        return getActualUri(context, this)
    }
}