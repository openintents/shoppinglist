package org.openintents.distribution

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

//public class DistributionLibraryActivity extends Activity {//Temp - FragmentActivity for 3.x compatibility
open class DistributionLibraryFragmentActivity : AppCompatActivity() {

    companion object {
        private const val MENU_DISTRIBUTION_START: Int = Menu.FIRST
        private const val DIALOG_DISTRIBUTION_START: Int = 1
    }

    protected lateinit var mDistribution: DistributionLibrary

    /**
     * Called when the activity is first created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDistribution = DistributionLibrary(this, MENU_DISTRIBUTION_START,
            DIALOG_DISTRIBUTION_START)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        mDistribution.onCreateOptionsMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mDistribution.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("DEPRECATION")
    override fun onCreateDialog(id: Int): Dialog {
        return mDistribution.onCreateDialog(id)
    }

    @Suppress("DEPRECATION")
    override fun onPrepareDialog(id: Int, dialog: Dialog) {
        super.onPrepareDialog(id, dialog)
        mDistribution.onPrepareDialog(id, dialog)
    }
}
