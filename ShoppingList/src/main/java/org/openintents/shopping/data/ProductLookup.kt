package org.openintents.shopping.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Finds a product name for a barcode (EAN/UPC), or null if unknown or offline. */
fun interface ProductLookup {
    fun productName(barcode: String): String?
}

/**
 * [ProductLookup] using Open Food Facts (open data, https://world.openfoodfacts.org).
 * Only the barcode is sent. Uses no library: HttpURLConnection + org.json.
 */
class OpenFoodFactsLookup(private val userAgent: String) : ProductLookup {

    override fun productName(barcode: String): String? {
        val code = barcode.trim()
        if (!isBarcode(code)) return null
        val lang = Locale.getDefault().language
        val url = URL(
            "https://world.openfoodfacts.org/api/v2/product/$code" +
                "?fields=product_name,product_name_$lang,generic_name,brands"
        )
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            // Open Food Facts asks apps to identify themselves.
            connection.setRequestProperty("User-Agent", userAgent)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseProductName(body, lang)
        } catch (e: java.io.IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** EAN-8, UPC-A, EAN-13 or GTIN-14: digits only. */
        fun isBarcode(text: String): Boolean = text.length in 8..14 && text.all { it in '0'..'9' }

        /** The product name from an Open Food Facts API v2 response, or null. */
        fun parseProductName(json: String, lang: String): String? {
            val root = try {
                JSONObject(json)
            } catch (e: org.json.JSONException) {
                return null
            }
            if (root.optInt("status", 0) != 1) return null
            val product = root.optJSONObject("product") ?: return null
            val name = listOf("product_name_$lang", "product_name", "generic_name")
                .map { product.optString(it).trim() }
                .firstOrNull { it.isNotEmpty() } ?: return null
            // Add the brand unless the name already contains it ("Nutella (Ferrero)").
            val brand = product.optString("brands").split(',').firstOrNull()?.trim().orEmpty()
            return if (brand.isEmpty() || name.contains(brand, ignoreCase = true)) name else "$name ($brand)"
        }
    }
}
