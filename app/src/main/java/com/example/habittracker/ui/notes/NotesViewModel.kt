package it.atraj.habittracker.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.atraj.habittracker.data.local.Note
import it.atraj.habittracker.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    val notes: StateFlow<List<Note>> = noteRepository.getNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Selection state
    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNoteIds: StateFlow<Set<String>> = _selectedNoteIds.asStateFlow()

    val isSelectionMode: Boolean
        get() = _selectedNoteIds.value.isNotEmpty()

    fun toggleNoteSelection(noteId: String) {
        val currentSelection = _selectedNoteIds.value.toMutableSet()
        if (currentSelection.contains(noteId)) {
            currentSelection.remove(noteId)
        } else {
            currentSelection.add(noteId)
        }
        _selectedNoteIds.value = currentSelection
    }

    fun selectAll() {
        _selectedNoteIds.value = notes.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun deleteSelectedNotes() {
        viewModelScope.launch {
            _selectedNoteIds.value.forEach { noteId ->
                noteRepository.deleteNote(noteId)
            }
            clearSelection()
        }
    }

    fun loadNote(noteId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentNote.value = noteRepository.getNote(noteId)
            _isLoading.value = false
        }
    }

    fun clearCurrentNote() {
        _currentNote.value = null
    }

    fun saveNote(title: String, content: String, noteId: String = "") {
        viewModelScope.launch {
            val note = Note(
                id = noteId,
                title = title,
                content = content,
                createdAt = if (noteId.isEmpty()) System.currentTimeMillis() else _currentNote.value?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            noteRepository.saveNote(note)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }
}
