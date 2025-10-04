package com.example.habittracker.data.local

import kotlinx.serialization.Serializable

@Serializable
enum class HabitAvatarType {
    EMOJI,
    DEFAULT_ICON,
    CUSTOM_IMAGE
}

@Serializable
data class HabitAvatar(
    val type: HabitAvatarType = HabitAvatarType.DEFAULT_ICON,
    val value: String = "🎯", // emoji character, icon name, or image path
    val backgroundColor: String = "#6650A4" // hex color for background
) {
    companion object {
        val DEFAULT = HabitAvatar()
        
        // Predefined emoji options
        val POPULAR_EMOJIS = listOf(
            "🎯", "💪", "📚", "🏃", "🧘", "💧", "🥗", "😴", 
            "🎵", "🎨", "💼", "🌱", "⏰", "🔥", "✨", "📝",
            "🏋️", "🧠", "❤️", "🌟", "🎊", "🚀", "🌈", "⚡"
        )
        
        // Predefined background colors
        val BACKGROUND_COLORS = listOf(
            "#6650A4", "#006C62", "#7B1FA2", "#3949AB", "#00838F",
            "#F57C00", "#D32F2F", "#689F38", "#455A64", "#512DA8"
        )
    }
}