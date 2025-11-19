package it.atraj.habittracker.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import it.atraj.habittracker.auth.AuthRepository
import it.atraj.habittracker.data.local.Note
import it.atraj.habittracker.data.repository.NoteRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNoteRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : NoteRepository {

    override fun getNotes(): Flow<List<Note>> =
        authRepository.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                val notesCollection = firestore.collection("users").document(user.uid).collection("notes")
                callbackFlow {
                    val listener = notesCollection
                        .orderBy("updatedAt", Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                close(error)
                                return@addSnapshotListener
                            }
                            val notes = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(Note::class.java)
                            } ?: emptyList()
                            trySend(notes)
                        }
                    awaitClose { listener.remove() }
                }
            }
        }

    override suspend fun getNote(noteId: String): Note? {
        val uid = authRepository.currentUserSync?.uid ?: return null
        return try {
            val doc = firestore.collection("users").document(uid)
                .collection("notes").document(noteId).get().await()
            doc.toObject(Note::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveNote(note: Note) {
        val uid = authRepository.currentUserSync?.uid ?: return
        val notesCollection = firestore.collection("users").document(uid).collection("notes")
        
        val noteToSave = if (note.id.isEmpty()) {
            val newDoc = notesCollection.document()
            note.copy(id = newDoc.id)
        } else {
            note.copy(updatedAt = System.currentTimeMillis())
        }
        
        notesCollection.document(noteToSave.id).set(noteToSave).await()
    }

    override suspend fun deleteNote(noteId: String) {
        val uid = authRepository.currentUserSync?.uid ?: return
        firestore.collection("users").document(uid)
            .collection("notes").document(noteId).delete().await()
    }
}
