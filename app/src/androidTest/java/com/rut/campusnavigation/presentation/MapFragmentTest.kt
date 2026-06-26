package com.rut.campusnavigation.presentation

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rut.campusnavigation.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MapFragmentTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() { hiltRule.inject() }

    /** Карта отображается при запуске приложения. */
    @Test
    fun mapView_isDisplayedOnLaunch() {
        onView(withId(R.id.mapView)).check(matches(isDisplayed()))
    }

    /** Кнопка поиска видна и кликабельна. */
    @Test
    fun fabSearch_isDisplayedAndClickable() {
        onView(withId(R.id.fabSearch)).check(matches(isDisplayed()))
        onView(withId(R.id.fabSearch)).check(matches(isClickable()))
    }

    /** Нажатие на FAB поиска открывает SearchFragment. */
    @Test
    fun clickFabSearch_navigatesToSearchFragment() {
        onView(withId(R.id.fabSearch)).perform(click())
        onView(withId(R.id.etSearch)).check(matches(isDisplayed()))
    }

    /** FAB избранного виден. */
    @Test
    fun fabFavorites_isDisplayed() {
        onView(withId(R.id.fabFavorites)).check(matches(isDisplayed()))
    }
}
