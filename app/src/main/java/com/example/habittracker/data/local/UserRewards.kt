package it.atraj.habittracker.data.local

import java.time.LocalDate

/**
 * User rewards and streak freeze data
 */
data class UserRewards(
    val diamonds: Int = 0,
    val freezeDays: Int = 0,
    val firstFreezePurchaseDate: LocalDate? = null // Track when freeze was first purchased
)
