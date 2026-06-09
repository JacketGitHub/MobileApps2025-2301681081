package com.example.mobileapps2025_2301681081.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobileapps2025_2301681081.data.Note
import com.example.mobileapps2025_2301681081.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    val allNotes = repository.allNotes

    private val _saveResult = MutableStateFlow<Boolean?>(null)
    val saveResult = _saveResult.asStateFlow()

    private val _currentNote = MutableLiveData<Note?>()
    val currentNote: LiveData<Note?> = _currentNote

    fun loadNote(noteId: Int) {
        viewModelScope.launch {
            _currentNote.value = repository.getNoteById(noteId)
        }
    }

    fun resetState() {
        _currentNote.value = null
        _saveResult.value = null
    }

    fun saveNote(title: String, body: String, existingId: Int? = null) {
        val trimmedTitle = title.trim()
        val trimmedBody = body.trim()

        if (trimmedTitle.isEmpty()) {
            _saveResult.value = false
            return
        }

        viewModelScope.launch {
            try {
                if (existingId != null && existingId != 0) {
                    val updated = Note(id = existingId, title = trimmedTitle, body = trimmedBody)
                    repository.update(updated)
                } else {
                    val newNote = Note(title = trimmedTitle, body = trimmedBody)
                    repository.insert(newNote)
                }
                _saveResult.value = true
            } catch (_: Exception) {
                _saveResult.value = false
            }
        }
    }

    fun onSaveResultHandled() {
        _saveResult.value = null
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.delete(note)
        }
    }
}
