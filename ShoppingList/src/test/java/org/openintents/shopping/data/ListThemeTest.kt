package org.openintents.shopping.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** The Compose UI and the legacy UI must read each other's stored list themes. */
class ListThemeTest {

    @Test
    fun fromName_readsLegacyNumericValues() {
        assertEquals(ListTheme.DEFAULT, ListTheme.fromName("1"))
        assertEquals(ListTheme.CLASSIC, ListTheme.fromName("2"))
        assertEquals(ListTheme.ANDROID, ListTheme.fromName("3"))
    }

    @Test
    fun fromName_readsLegacyStyleResourceNames() {
        assertEquals(ListTheme.DEFAULT, ListTheme.fromName("org.openintents.shopping:style/Theme.ShoppingList"))
        assertEquals(
            ListTheme.CLASSIC,
            ListTheme.fromName("org.openintents.shopping:style/Theme.ShoppingList.Classic")
        )
        assertEquals(
            ListTheme.ANDROID,
            ListTheme.fromName("org.openintents.shopping:style/Theme.ShoppingList.Android")
        )
    }

    @Test
    fun fromName_readsEnumNamesAndFallsBackToDefault() {
        assertEquals(ListTheme.CLASSIC, ListTheme.fromName("CLASSIC"))
        assertEquals(ListTheme.ANDROID, ListTheme.fromName("ANDROID"))
        assertEquals(ListTheme.DEFAULT, ListTheme.fromName(null))
        assertEquals(ListTheme.DEFAULT, ListTheme.fromName(""))
        assertEquals(ListTheme.DEFAULT, ListTheme.fromName("com.other.app:style/Fancy"))
    }

    @Test
    fun storedValue_roundTrips() {
        ListTheme.entries.forEach { assertEquals(it, ListTheme.fromName(it.storedValue)) }
    }
}
