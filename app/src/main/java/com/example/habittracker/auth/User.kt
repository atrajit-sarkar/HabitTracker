package it.atraj.habittracker.auth

data class User(
    val uid: String,
    val email: String?,
    val displayName: String?, // Original name from Google/Firebase
    val photoUrl: String?,
    val customAvatar: String? = null, // Custom emoji avatar, null means use default
    val customDisplayName: String? = null, // Custom name set by user, overrides displayName
    val musicEnabled: Boolean = false, // Background music on/off
    val musicTrack: String = "NONE", // Selected music track name
    val musicVolume: Float = 0.3f // Music volume (0.0 to 1.0)
) {
    // Property to get the effective display name (custom name if set, otherwise original name)
    val effectiveDisplayName: String
        get() = customDisplayName ?: displayName ?: "User"
}
