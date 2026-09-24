package org.openintents.shopping.ui.compose.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import org.openintents.shopping.data.FakeSettingsRepository

/** Pure-JVM tests for the settings ViewModel state machine. */
class SettingsViewModelTest {

    @Test
    fun defaults_loadedInitially() {
        val vm = SettingsViewModel(FakeSettingsRepository())
        // From the catalog defaults (mirroring preferences.xml).
        assertEquals(false, vm.state.value.bools["hidechecked"])
        assertEquals(true, vm.state.value.bools["showprice"])
        assertEquals("2", vm.state.value.choices["fontsize"])
    }

    @Test
    fun setBool_reflectedInState() {
        val vm = SettingsViewModel(FakeSettingsRepository())
        vm.setBool("hidechecked", true)
        assertEquals(true, vm.state.value.bools["hidechecked"])
    }

    @Test
    fun setChoice_reflectedInState() {
        val vm = SettingsViewModel(FakeSettingsRepository())
        vm.setChoice("fontsize", "3")
        assertEquals("3", vm.state.value.choices["fontsize"])
    }
}
