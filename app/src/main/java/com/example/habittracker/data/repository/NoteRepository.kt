package it.atraj.habittracker.data.repository

import it.atraj.habittracker.data.local.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotes(): Flow<List<Note>>
    suspend fun getNote(noteId: String): Note?
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(noteId: String)
}
