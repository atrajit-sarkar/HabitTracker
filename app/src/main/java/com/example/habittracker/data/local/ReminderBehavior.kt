package com.example.habittracker.data.local

enum class ReminderBehavior {
    ONE_TIME,
    ALARM;

    companion object {
        fun from(value: String?): ReminderBehavior =
            values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ONE_TIME
    }
}
