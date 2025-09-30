package com.example.habittracker.auth

data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)