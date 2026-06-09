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
        onView(withId(R.id.fabAddNote)).perform(click())

        onView(withId(R.id.editTitle)).perform(typeText("Espresso Title"), closeSoftKeyboard())
        onView(withId(R.id.editBody)).perform(typeText("Espresso Body Content"), closeSoftKeyboard())

        onView(withId(R.id.buttonSave)).perform(click())

        onView(withText("Espresso Title")).check(matches(isDisplayed()))
    }

    @Test
    fun testEmptyTitleShowsError() {
        onView(withId(R.id.fabAddNote)).perform(click())

        onView(withId(R.id.buttonSave)).perform(click())

        onView(withId(R.id.editTitle)).check(matches(isDisplayed()))
    }
}
