package org.openintents.shopping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.openintents.shopping.library.util.PriceConverter
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Unit tests for the extracted price business logic.
 *
 * PriceConverter is the canonical "business logic" unit: it converts between a
 * localized price string and an integer number of cents. These tests pin its
 * round-trip behavior so future refactors (e.g. moving it behind a repository)
 * can't silently change money handling.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PriceConverterTest {

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
        PriceConverter.init()
    }

    @Test
    fun stringFromCentPrice_formatsWithTwoDecimals() {
        assertEquals("1.50", PriceConverter.getStringFromCentPrice(150))
        assertEquals("0.05", PriceConverter.getStringFromCentPrice(5))
        assertEquals("123.00", PriceConverter.getStringFromCentPrice(12300))
    }

    @Test
    fun stringFromCentPrice_zeroIsEmpty() {
        // A zero price renders as the empty string (no "free" label shown in lists).
        assertEquals("", PriceConverter.getStringFromCentPrice(0))
    }

    @Test
    fun centPriceFromString_parsesBackToCents() {
        assertEquals(150L, PriceConverter.getCentPriceFromString("1.50"))
        assertEquals(5L, PriceConverter.getCentPriceFromString("0.05"))
    }

    @Test
    fun centPriceFromString_emptyIsZero() {
        assertEquals(0L, PriceConverter.getCentPriceFromString(""))
    }

    @Test
    fun centPriceFromString_invalidIsNull() {
        assertNull(PriceConverter.getCentPriceFromString("not a price"))
    }

    @Test
    fun roundTrip_isStable() {
        for (cents in longArrayOf(0, 1, 99, 100, 250, 99999)) {
            val text = PriceConverter.getStringFromCentPrice(cents)
            val back = if (text.isEmpty()) 0L else PriceConverter.getCentPriceFromString(text)
            assertEquals(cents, back)
        }
    }
}
