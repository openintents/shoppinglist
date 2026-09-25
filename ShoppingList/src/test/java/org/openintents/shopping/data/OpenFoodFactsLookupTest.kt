package org.openintents.shopping.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Parsing of Open Food Facts API v2 responses (Robolectric: real org.json). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OpenFoodFactsLookupTest {

    @Test
    fun textThatIsNoBarcode_isNotLookedUp() {
        // Returns without going online (no network in unit tests).
        assertEquals(LookupResult.NotFound, OpenFoodFactsLookup("test").lookup("hello"))
    }

    @Test
    fun parsesLocalizedNameAndBrand() {
        val json = """{"code":"3017620422003","status":1,"product":
            {"product_name":"Nutella","product_name_de":"Nutella Nuss-Nougat-Creme","brands":"Ferrero, Nutella"}}"""
        assertEquals(
            "Nutella Nuss-Nougat-Creme (Ferrero)",
            OpenFoodFactsLookup.parseProductName(json, "de")
        )
        assertEquals("Nutella (Ferrero)", OpenFoodFactsLookup.parseProductName(json, "fr"))
    }

    @Test
    fun addsTheBrandWhenTheNameLacksIt() {
        val json = """{"status":1,"product":{"product_name":"Hazelnut spread","brands":"Ferrero"}}"""
        assertEquals("Hazelnut spread (Ferrero)", OpenFoodFactsLookup.parseProductName(json, "en"))
    }

    @Test
    fun unknownProductOrBadJson_isNull() {
        assertNull(OpenFoodFactsLookup.parseProductName("""{"status":0,"status_verbose":"product not found"}""", "en"))
        assertNull(OpenFoodFactsLookup.parseProductName("""{"status":1,"product":{"brands":"X"}}""", "en"))
        assertNull(OpenFoodFactsLookup.parseProductName("<html>", "en"))
    }

    @Test
    fun isBarcode() {
        assertTrue(OpenFoodFactsLookup.isBarcode("3017620422003"))
        assertTrue(OpenFoodFactsLookup.isBarcode("96385074"))
        assertFalse(OpenFoodFactsLookup.isBarcode("12ab"))
        assertFalse(OpenFoodFactsLookup.isBarcode("../../etc"))
    }
}
