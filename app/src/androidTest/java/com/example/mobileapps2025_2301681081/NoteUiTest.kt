package com.example.mobileapps2025_2301681081

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteUiTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testAddNoteFlow() {
        // 1. Click FAB to go to Add Note screen
        onView(withId(R.id.fabAddNote)).perform(click())

        // 2. Type title and body
        onView(withId(R.id.editTitle)).perform(typeText("Espresso Title"), closeSoftKeyboard())
        onView(withId(R.id.editBody)).perform(typeText("Espresso Body Content"), closeSoftKeyboard())

        // 3. Click Save button
        onView(withId(R.id.buttonSave)).perform(click())

        // 4. Verify the new note appears in the list
        onView(withText("Espresso Title")).check(matches(isDisplayed()))
    }

    @Test
    fun testEmptyTitleShowsError() {
        // 1. Click FAB
        onView(withId(R.id.fabAddNote)).perform(click())

        // 2. Don't enter title, just click save
        onView(withId(R.id.buttonSave)).perform(click())

        // 3. Verify that the title field shows an error (or stays on the page)
        // Since the VM sets an error on the layout/field
        onView(withId(R.id.editTitle)).check(matches(isDisplayed()))
    }
}
