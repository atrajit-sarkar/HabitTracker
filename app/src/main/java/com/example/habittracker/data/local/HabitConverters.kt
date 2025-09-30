package com.example.habittracker.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class HabitConverters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromEpochMillis(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun toEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun fromHabitFrequency(frequency: HabitFrequency): String = frequency.name

    @TypeConverter
    fun toHabitFrequency(frequency: String): HabitFrequency = HabitFrequency.valueOf(frequency)

    @TypeConverter
    fun fromNotificationSound(sound: NotificationSound): String = sound.name

    @TypeConverter
    fun toNotificationSound(soundName: String): NotificationSound = NotificationSound.fromName(soundName)

    @TypeConverter
    fun fromHabitAvatar(avatar: HabitAvatar): String {
        return Json.encodeToString(avatar)
    }

    @TypeConverter
    fun toHabitAvatar(avatarJson: String): HabitAvatar {
        return try {
            Json.decodeFromString(avatarJson)
        } catch (e: Exception) {
            HabitAvatar.DEFAULT
        }
    }

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): Long? = dateTime?.toInstant(ZoneOffset.UTC)?.toEpochMilli()

    @TypeConverter
    fun toLocalDateTime(epochMilli: Long?): LocalDateTime? = epochMilli?.let {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC)
    }
}
