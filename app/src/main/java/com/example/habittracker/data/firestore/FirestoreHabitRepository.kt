package com.example.habittracker.data.firestore

import com.example.habittracker.auth.AuthRepository
import com.example.habittracker.data.HabitRepository
import com.example.habittracker.data.local.Habit
import com.example.habittracker.data.local.HabitAvatar
import com.example.habittracker.data.local.HabitAvatarType
import com.example.habittracker.data.local.HabitCompletion
import com.example.habittracker.data.local.HabitFrequency
import com.example.habittracker.data.local.NotificationSound
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
                                android.util.Log.e("FirestoreRepo", "Error observing deleted habits", error)
                                close(error)
                                return@addSnapshotListener
                            }
                            android.util.Log.d("FirestoreRepo", "Deleted habits snapshot received, total docs: ${snapshot?.documents?.size}")
                            
                            val allHabits = snapshot?.toFirestoreHabits() ?: emptyList()
                            android.util.Log.d("FirestoreRepo", "Converted to ${allHabits.size} FirestoreHabit objects")
                            
                            allHabits.forEachIndexed { index, habit ->
                                android.util.Log.d("FirestoreRepo", "Habit $index: title='${habit.title}', isDeleted=${habit.isDeleted}")
                            }
                            
                            val habits = allHabits
                                .mapNotNull { runCatching { it.toHabit() }.getOrNull() }
                                .filter { it.isDeleted }
                                .sortedWith(compareByDescending<Habit> { it.deletedAt ?: Instant.EPOCH })
                            
                            android.util.Log.d("FirestoreRepo", "Filtered deleted habits: ${habits.size}")
                            habits.forEach { habit ->
                                android.util.Log.d("FirestoreRepo", "Deleted habit: ${habit.title}, deletedAt=${habit.deletedAt}")
                            }
                            
                            trySend(habits)
                        }
                    awaitClose { listener.remove() }
                }
            }
        }

    override suspend fun getHabitById(id: Long): Habit {
        val doc = findHabitDocument(id) ?: throw NoSuchElementException("Habit not found")
        return doc.toFirestoreHabit()?.toHabit() ?: throw NoSuchElementException("Habit not found")
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
        val doc = findHabitDocument(habit.id)
            ?: throw NoSuchElementException("Habit not found for update")
        val firestoreHabit = habit.toFirestoreHabit(doc.id, habit.id)
        doc.reference.set(firestoreHabit).await()
    }

    override suspend fun deleteHabit(habit: Habit) {
        findHabitDocument(habit.id)?.reference?.delete()?.await()
    }

    override suspend fun moveToTrash(habitId: Long) {
        android.util.Log.d("FirestoreRepo", "moveToTrash called for habitId: $habitId")
        val doc = findHabitDocument(habitId)
        if (doc == null) {
            android.util.Log.e("FirestoreRepo", "moveToTrash: Document not found for habitId: $habitId")
            return
        }
        android.util.Log.d("FirestoreRepo", "moveToTrash: Found document ${doc.id}, updating isDeleted=true")
        doc.reference.update(
            mapOf(
                "isDeleted" to true,
                "deletedAt" to System.currentTimeMillis()
            )
        ).await()
        android.util.Log.d("FirestoreRepo", "moveToTrash: Successfully updated document ${doc.id}")
    }

    override suspend fun restoreFromTrash(habitId: Long) {
        android.util.Log.d("FirestoreRepo", "restoreFromTrash called for habitId: $habitId")
        val doc = findHabitDocument(habitId)
        if (doc == null) {
            android.util.Log.e("FirestoreRepo", "restoreFromTrash: Document not found for habitId: $habitId")
            return
        }
        android.util.Log.d("FirestoreRepo", "restoreFromTrash: Found document ${doc.id}, updating isDeleted=false")
        doc.reference.update(
            mapOf(
                "isDeleted" to false,
                "deletedAt" to null
            )
        ).await()
        android.util.Log.d("FirestoreRepo", "restoreFromTrash: Successfully updated document ${doc.id}")
    }

    override suspend fun permanentlyDeleteHabit(habitId: Long) {
        android.util.Log.d("FirestoreRepo", "permanentlyDeleteHabit called for habitId: $habitId")
        val userCompletionsCollection = getUserCompletionsCollection() ?: throw IllegalStateException("User not authenticated")
        val habitDoc = findHabitDocument(habitId)
        
        if (habitDoc == null) {
            android.util.Log.e("FirestoreRepo", "permanentlyDeleteHabit: Document not found for habitId: $habitId")
            return
        }
        
        android.util.Log.d("FirestoreRepo", "permanentlyDeleteHabit: Found document ${habitDoc.id}, deleting...")
        habitDoc.reference.delete().await()
        android.util.Log.d("FirestoreRepo", "permanentlyDeleteHabit: Document ${habitDoc.id} deleted")
        
        val completions = userCompletionsCollection
            .whereEqualTo("habitId", habitId.toString())
            .get().await()
        android.util.Log.d("FirestoreRepo", "permanentlyDeleteHabit: Deleting ${completions.documents.size} completions")
        completions.documents.forEach { doc -> doc.reference.delete() }
        
        // Legacy completions may have stored the Firestore document ID instead of numericId
        val legacyCompletions = userCompletionsCollection
            .whereEqualTo("habitId", habitDoc.id)
            .get().await()
        android.util.Log.d("FirestoreRepo", "permanentlyDeleteHabit: Deleting ${legacyCompletions.documents.size} legacy completions")
        legacyCompletions.documents.forEach { doc -> doc.reference.delete() }
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
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
        
        android.util.Log.d("FirestoreRepo", "cleanupOldDeletedHabits: thirtyDaysAgo=$thirtyDaysAgo (${java.util.Date(thirtyDaysAgo)})")
        
        val oldDeletedHabits = userCollection
            .whereEqualTo("isDeleted", true)
            .whereLessThan("deletedAt", thirtyDaysAgo)
            .get().await()
        
        android.util.Log.d("FirestoreRepo", "cleanupOldDeletedHabits: Found ${oldDeletedHabits.documents.size} habits to clean up")
        
        oldDeletedHabits.documents.forEach { doc ->
            val deletedAt = doc.getLong("deletedAt")
            android.util.Log.d("FirestoreRepo", "cleanupOldDeletedHabits: Deleting habit '${doc.getString("title")}', deletedAt=$deletedAt (${deletedAt?.let { java.util.Date(it) }})")
            val numericId = (doc.get("numericId") as? Long) ?: doc.id.hashCode().toLong()
            permanentlyDeleteHabit(numericId)
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
            findHabitDocument(habitId)?.reference?.update("lastCompletedDate", date.toEpochDay())?.await()
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

    private suspend fun findHabitDocument(habitId: Long): DocumentSnapshot? {
        val userCollection = getUserCollection() ?: return null

        android.util.Log.d("FirestoreRepo", "Finding habit document for ID: $habitId")

        // Primary lookup using the numericId field
        val numericSnapshot = userCollection.whereEqualTo("numericId", habitId).limit(1).get().await()
        val numericDoc = numericSnapshot.documents.firstOrNull()
        if (numericDoc != null) {
            android.util.Log.d("FirestoreRepo", "Found habit by numericId: ${numericDoc.id}")
            return numericDoc
        }

        android.util.Log.d("FirestoreRepo", "No match by numericId, scanning all documents...")

        // Fallback: scan existing documents and compare against legacy identifiers
        val allSnapshot = userCollection.get().await()
        android.util.Log.d("FirestoreRepo", "Total documents in collection: ${allSnapshot.documents.size}")
        
        val matchedDoc = allSnapshot.documents.firstOrNull { doc ->
            val docIdHash = doc.id.hashCode().toLong()
            val storedNumeric = (doc.get("numericId") as? Number)?.toLong()
            val storedIdString = doc.getString("id")
            val storedIdHash = storedIdString?.hashCode()?.toLong()
            val storedIdLong = storedIdString?.toLongOrNull()
            
            android.util.Log.d("FirestoreRepo", "Checking doc ${doc.id}: docIdHash=$docIdHash, storedNumeric=$storedNumeric, storedIdString=$storedIdString, storedIdHash=$storedIdHash, storedIdLong=$storedIdLong")
            
            habitId == docIdHash || habitId == storedNumeric || habitId == storedIdHash || habitId == storedIdLong
        }

        matchedDoc?.let { doc ->
            android.util.Log.d("FirestoreRepo", "Found habit by fallback match: ${doc.id}")
            // Backfill numericId for faster lookups in the future
            val currentNumeric = (doc.get("numericId") as? Number)?.toLong()
            if (currentNumeric == null) {
                android.util.Log.d("FirestoreRepo", "Backfilling numericId=$habitId for doc ${doc.id}")
                doc.reference.update("numericId", habitId).await()
            }
        } ?: android.util.Log.e("FirestoreRepo", "No matching document found for habitId=$habitId")

        return matchedDoc
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
        notificationSound = notificationSoundName, // Legacy field for backward compatibility
        notificationSoundId = notificationSoundId,
        notificationSoundName = notificationSoundName,
        notificationSoundUri = notificationSoundUri,
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
        notificationSoundId = notificationSoundId,
        notificationSoundName = notificationSoundName,
        notificationSoundUri = notificationSoundUri,
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

