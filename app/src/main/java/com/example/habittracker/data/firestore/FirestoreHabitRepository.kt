package com.example.habittracker.data.firestore

import com.example.habittracker.auth.AuthRepository
import com.example.habittracker.data.HabitRepository
import com.example.habittracker.data.local.Habit
import com.example.habittracker.data.local.HabitAvatar
import com.example.habittracker.data.local.HabitAvatarType
import com.example.habittracker.data.local.HabitCompletion
import com.example.habittracker.data.local.HabitFrequency
import com.example.habittracker.data.local.NotificationSound
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirestoreHabitRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : HabitRepository {

    private fun getUserCollection() = authRepository.currentUserSync?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("habits")
    }

    private fun getUserCompletionsCollection() = authRepository.currentUserSync?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("completions")
    }

    override fun observeHabits(): Flow<List<Habit>> =
        authRepository.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                val userCollection = firestore.collection("users").document(user.uid).collection("habits")
                callbackFlow {
                    val listener = userCollection
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                close(error)
                                return@addSnapshotListener
                            }
                            val habits = snapshot
                                ?.toFirestoreHabits()
                                ?.mapNotNull { runCatching { it.toHabit() }.getOrNull() }
                                ?.filterNot { it.isDeleted }
                                ?.sortedWith(compareBy({ it.reminderHour }, { it.reminderMinute }))
                                ?: emptyList()
                            trySend(habits)
                        }
                    awaitClose { listener.remove() }
                }
            }
        }

    override fun observeDeletedHabits(): Flow<List<Habit>> =
        authRepository.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                val userCollection = firestore.collection("users").document(user.uid).collection("habits")
                callbackFlow {
                    val listener = userCollection
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                close(error)
                                return@addSnapshotListener
                            }
                            val habits = snapshot
                                ?.toFirestoreHabits()
                                ?.mapNotNull { runCatching { it.toHabit() }.getOrNull() }
                                ?.filter { it.isDeleted }
                                ?.sortedWith(compareByDescending<Habit> { it.deletedAt ?: Instant.EPOCH })
                                ?: emptyList()
                            trySend(habits)
                        }
                    awaitClose { listener.remove() }
                }
            }
        }

    override suspend fun getHabitById(id: Long): Habit {
        val userCollection = getUserCollection() ?: throw IllegalStateException("User not authenticated")
        // First attempt: numericId match
        val numericSnapshot = userCollection.whereEqualTo("numericId", id).limit(1).get().await()
        val numericDoc = numericSnapshot.documents.firstOrNull()
        if (numericDoc != null) return numericDoc.toFirestoreHabit()?.toHabit()
            ?: throw NoSuchElementException("Habit not found")
        // Fallback: legacy docs without numericId -> scan minimal fields (could be optimized)
        val allSnapshot = userCollection.get().await()
        val legacyDoc = allSnapshot.documents.firstOrNull { it.id.hashCode().toLong() == id }
        return legacyDoc?.toFirestoreHabit()?.toHabit() ?: throw NoSuchElementException("Habit not found")
    }

    override suspend fun insertHabit(habit: Habit): Long {
        val userCollection = getUserCollection() ?: throw IllegalStateException("User not authenticated")
        // Generate a new document so we have a Firestore string ID
        val docRef = userCollection.document()
        val numericId = docRef.id.hashCode().toLong()
        val firestoreHabit = habit.toFirestoreHabit(docRef.id, numericId)
        docRef.set(firestoreHabit).await()
        return numericId
    }

    override suspend fun updateHabit(habit: Habit) {
        val userCollection = getUserCollection() ?: throw IllegalStateException("User not authenticated")
        // Need to locate document by matching numericId field (single query)
        val snapshot = userCollection.whereEqualTo("numericId", habit.id).get().await()
        val doc = snapshot.documents.firstOrNull()
            ?: throw NoSuchElementException("Habit not found for update")
        val firestoreHabit = habit.toFirestoreHabit(doc.id, habit.id)
        userCollection.document(doc.id).set(firestoreHabit).await()
    }

    override suspend fun deleteHabit(habit: Habit) {
        val userCollection = getUserCollection() ?: throw IllegalStateException("User not authenticated")
        val snapshot = userCollection.whereEqualTo("numericId", habit.id).get().await()
        snapshot.documents.firstOrNull()?.reference?.delete()?.await()
    }

    override suspend fun moveToTrash(habitId: Long) {
        val userCollection = getUserCollection() ?: throw IllegalStateException("User not authenticated")
        val snapshot = userCollection.whereEqualTo("numericId", habitId).get().await()
        snapshot.documents.firstOrNull()?.reference?.update(
            mapOf(
                "isDeleted" to true,
                "deletedAt" to System.currentTimeMillis()
            )
        )?.await()
    }

    override suspend fun restoreFromTrash(habitId: Long) {
        val userCollection = getUserCollection() ?: throw IllegalStateException("User not authenticated")
        val snapshot = userCollection.whereEqualTo("numericId", habitId).get().await()
        snapshot.documents.firstOrNull()?.reference?.update(
            mapOf(
                "isDeleted" to false,
                "deletedAt" to null
            )
        )?.await()
    }

    override suspend fun permanentlyDeleteHabit(habitId: Long) {
        val userCollection = getUserCollection() ?: throw IllegalStateException("User not authenticated")
        val userCompletionsCollection = getUserCompletionsCollection() ?: throw IllegalStateException("User not authenticated")
        val snapshot = userCollection.whereEqualTo("numericId", habitId).get().await()
        val habitDoc = snapshot.documents.firstOrNull()
        habitDoc?.reference?.delete()?.await()
        if (habitDoc != null) {
            val completions = userCompletionsCollection
                .whereEqualTo("habitId", habitId.toString())
                .get().await()
            completions.documents.forEach { doc -> doc.reference.delete() }
        }
    }

    override suspend fun emptyTrash() {
        val userCollection = getUserCollection() ?: throw IllegalStateException("User not authenticated")
        val deletedHabits = userCollection.whereEqualTo("isDeleted", true).get().await()
        deletedHabits.documents.forEach { doc ->
            val numericId = (doc.get("numericId") as? Long) ?: doc.id.hashCode().toLong()
            permanentlyDeleteHabit(numericId)
        }
    }

    override suspend fun cleanupOldDeletedHabits() {
        val userCollection = getUserCollection() ?: throw IllegalStateException("User not authenticated")
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
        
        val oldDeletedHabits = userCollection
            .whereEqualTo("isDeleted", true)
            .whereLessThan("deletedAt", thirtyDaysAgo)
            .get().await()
        
        oldDeletedHabits.documents.forEach { doc ->
            val habitId = doc.id
            permanentlyDeleteHabit(habitId.hashCode().toLong())
        }
    }

    override suspend fun markCompletedToday(habitId: Long) {
        markCompletedForDate(habitId, LocalDate.now())
    }

    override suspend fun markCompletedForDate(habitId: Long, date: LocalDate) {
        val userCollection = getUserCollection() ?: throw IllegalStateException("User not authenticated")
        val userCompletionsCollection = getUserCompletionsCollection() ?: throw IllegalStateException("User not authenticated")
        
        val completionId = "${habitId}_${date.toEpochDay()}"
        val completion = FirestoreHabitCompletion(
            id = completionId,
            habitId = habitId.toString(),
            completedDate = date.toEpochDay(),
            completedAt = System.currentTimeMillis()
        )
        
        userCompletionsCollection.document(completionId).set(completion).await()
        
        // Update lastCompletedDate in habit if this is more recent
        val habit = getHabitById(habitId)
        val shouldUpdate = habit.lastCompletedDate == null || date.isAfter(habit.lastCompletedDate)
        if (shouldUpdate) {
            val snapshot = userCollection.whereEqualTo("numericId", habitId).get().await()
            snapshot.documents.firstOrNull()?.reference?.update("lastCompletedDate", date.toEpochDay())?.await()
        }
    }

    override suspend fun getHabitCompletions(habitId: Long): List<HabitCompletion> {
        val userCompletionsCollection = getUserCompletionsCollection() ?: throw IllegalStateException("User not authenticated")
        
        val completions = userCompletionsCollection
            .whereEqualTo("habitId", habitId.toString())
            .orderBy("completedDate", Query.Direction.DESCENDING)
            .get().await()
        
        return completions.toFirestoreHabitCompletions().map { it.toHabitCompletion() }
    }
}

// Extension functions to convert between local and Firestore models
private fun Habit.toFirestoreHabit(docId: String, numericId: Long? = null): FirestoreHabit {
    return FirestoreHabit(
        id = docId,
        numericId = numericId ?: (if (id != 0L) id else docId.hashCode().toLong()),
        title = title,
        description = description,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        reminderEnabled = reminderEnabled,
        frequency = frequency.name,
        dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth,
        monthOfYear = monthOfYear,
        notificationSound = notificationSound.name,
        avatar = FirestoreHabitAvatar(
            type = avatar.type.name,
            value = avatar.value,
            backgroundColor = avatar.backgroundColor
        ),
        lastCompletedDate = lastCompletedDate?.toEpochDay(),
        createdAt = createdAt.toEpochMilli(),
        isDeleted = isDeleted,
        deletedAt = deletedAt?.toEpochMilli()
    )
}

private fun FirestoreHabit.toHabit(): Habit {
    return Habit(
        id = numericId ?: id.hashCode().toLong(),
        title = title,
        description = description,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        reminderEnabled = reminderEnabled,
        frequency = try { HabitFrequency.valueOf(frequency) } catch (e: Exception) { HabitFrequency.DAILY },
        dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth,
        monthOfYear = monthOfYear,
        notificationSound = try { NotificationSound.valueOf(notificationSound) } catch (e: Exception) { NotificationSound.DEFAULT },
        avatar = HabitAvatar(
            type = try { HabitAvatarType.valueOf(avatar.type) } catch (e: Exception) { HabitAvatarType.DEFAULT_ICON },
            value = avatar.value,
            backgroundColor = avatar.backgroundColor
        ),
        lastCompletedDate = lastCompletedDate?.let { LocalDate.ofEpochDay(it) },
        createdAt = Instant.ofEpochMilli(createdAt),
        isDeleted = isDeleted,
        deletedAt = deletedAt?.let { Instant.ofEpochMilli(it) }
    )
}

private fun FirestoreHabitCompletion.toHabitCompletion(): HabitCompletion {
    return HabitCompletion(
        habitId = habitId.toLongOrNull() ?: habitId.hashCode().toLong(),
        completedDate = LocalDate.ofEpochDay(completedDate),
        completedAt = java.time.LocalDateTime.ofInstant(
            Instant.ofEpochMilli(completedAt),
            java.time.ZoneId.systemDefault()
        )
    )
}