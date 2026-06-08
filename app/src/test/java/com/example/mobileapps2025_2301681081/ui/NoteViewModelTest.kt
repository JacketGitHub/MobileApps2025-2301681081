package com.example.mobileapps2025_2301681081.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.mobileapps2025_2301681081.data.Note
import com.example.mobileapps2025_2301681081.data.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: NoteRepository
    private lateinit var viewModel: NoteViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        whenever(repository.allNotes).thenReturn(flowOf(emptyList()))
        viewModel = NoteViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveNote with empty title sets saveResult to false`() = runTest {
        viewModel.saveNote("", "body")
        assertEquals(false, viewModel.saveResult.value)
        verify(repository, never()).insert(any())
    }

    @Test
    fun `saveNote with valid input calls repository insert`() = runTest {
        viewModel.saveNote("Title", "Body")
        testDispatcher.scheduler.advanceUntilIdle()

        val captor = argumentCaptor<Note>()
        verify(repository).insert(captor.capture())
        assertEquals("Title", captor.firstValue.title)
        assertEquals("Body", captor.firstValue.body)
        assertEquals(true, viewModel.saveResult.value)
    }

    @Test
    fun `saveNote with existingId calls repository update`() = runTest {
        viewModel.saveNote("Updated Title", "Updated Body", existingId = 123)
        testDispatcher.scheduler.advanceUntilIdle()

        val captor = argumentCaptor<Note>()
        verify(repository).update(captor.capture())
        assertEquals(123, captor.firstValue.id)
        assertEquals("Updated Title", captor.firstValue.title)
        assertEquals(true, viewModel.saveResult.value)
    }

    @Test
    fun `deleteNote calls repository delete`() = runTest {
        val note = Note(id = 1, title = "T", body = "B")
        viewModel.deleteNote(note)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(repository).delete(note)
    }
}
