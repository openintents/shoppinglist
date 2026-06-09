package org.openintents.shopping.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.openintents.shopping.ui.PreferenceActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the Compose settings write the SAME default SharedPreferences that the
 * legacy app reads — so changing a setting in Compose drives existing behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repo = SharedPrefsSettingsRepository(context)
    }

    @Test
    fun boolean_roundTrip() {
        repo.setBoolean("hidechecked", true)
        assertTrue(repo.getBoolean("hidechecked", false))
    }

    @Test
    fun string_roundTrip() {
        repo.setString("fontsize", "3")
        assertEquals("3", repo.getString("fontsize", "2"))
    }

    @Test
    fun writesAreVisibleToLegacyGetters() {
        repo.setBoolean("hidechecked", true)
        assertTrue(PreferenceActivity.getHideCheckedItemsFromPrefs(context))

        repo.setString("fontsize", "3")
        assertEquals(3, PreferenceActivity.getFontSizeFromPrefs(context))
    }
}
