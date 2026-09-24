package org.openintents.shopping

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.openintents.shopping.data.ItemEdit
import org.openintents.shopping.data.ListTheme
import org.openintents.shopping.data.ProviderShoppingRepository
import org.openintents.shopping.ui.PreferenceActivity
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Renders the legacy and the Compose UI with the same sample data to PNGs in
 * ShoppingList/build/screenshots/. Skipped unless run with -Pscreenshots, e.g.
 *   ./gradlew :ShoppingList:testPlayDebugUnitTest --tests "*ScreenshotTest.composeUiClassic" -Pscreenshots
 * Run one test per Gradle invocation: several activities in one run can leave
 * the capture of later ones mid-animation (e.g. with the drawer shown).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xhdpi")
class ScreenshotTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val repo by lazy { ProviderShoppingRepository(context) }
    private var listId = -1L

    @Before
    fun setUp() {
        assumeTrue(System.getProperty("screenshots") != null)
        PreferenceActivity.setShowLayoutChoice(context, false)
        listId = repo.createList("Groceries")
        repo.createList("Hardware store")
        listOf("Milk", "Bread", "Eggs", "Apples", "Coffee", "Butter").forEach { repo.addItem(listId, it) }
        val items = repo.getItems(listId)
        items.first { it.name == "Coffee" }.let {
            repo.updateItem(it, ItemEdit("Coffee", "2", 499, "packs", "1", null))
        }
        items.first { it.name == "Milk" }.let {
            repo.updateItem(it, ItemEdit("Milk", "1", 119, "l", null, null))
        }
        repo.toggleItemBought(items.first { it.name == "Bread" })
        repo.toggleItemBought(items.first { it.name == "Eggs" })
        repo.setActiveList(listId)
    }

    @Test
    fun legacyUi() = capture(org.openintents.shopping.ui.ShoppingActivity::class.java, "legacy-ui.png")

    @Test
    fun composeUi() = capture(ShoppingActivity::class.java, "compose-ui.png")

    @Test
    fun composeUiClassic() {
        repo.setListTheme(listId, ListTheme.CLASSIC)
        capture(ShoppingActivity::class.java, "compose-ui-classic.png")
    }

    @Test
    fun composeUiBugdroid() {
        repo.setListTheme(listId, ListTheme.ANDROID)
        capture(ShoppingActivity::class.java, "compose-ui-bugdroid.png")
    }

    private fun <T : Activity> capture(cls: Class<T>, fileName: String) {
        val activity = Robolectric.buildActivity(cls).setup().get()
        // Let loaders / coroutines (real background threads) finish and redraw.
        repeat(60) {
            Thread.sleep(100)
            shadowOf(Looper.getMainLooper()).idle()
        }
        val view = activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, fileName).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
