package it.atraj.habittracker.auth

data class User(
    val uid: String,
    val email: String?,
    val displayName: String?, // Original name from Google/Firebase
    val photoUrl: String?,
    val customAvatar: String? = null, // Custom emoji avatar, null means use default
    val customDisplayName: String? = null // Custom name set by user, overrides displayName
) {
    // Property to get the effective display name (custom name if set, otherwise original name)
    val effectiveDisplayName: String
        get() = customDisplayName ?: displayName ?: "User"
}
