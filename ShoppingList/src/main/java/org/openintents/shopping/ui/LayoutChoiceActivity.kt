package org.openintents.shopping.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import org.openintents.shopping.R

class LayoutChoiceActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        fun show(context: Activity): Boolean {
            return if (PreferenceActivity.getShowLayoutChoice(context) && PreferenceActivity.getUsingHoloSearchFromPrefs(context)) {
                context.startActivity(Intent(context, LayoutChoiceActivity::class.java))
                true
            } else {
                false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_layout_choice)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val radioGroup = findViewById<RadioGroup>(R.id.layout_choice)

        if (PreferenceActivity.getUsingHoloSearchFromPrefs(this)) {
            radioGroup.check(R.id.layout_choice_actionbar)
        } else {
            radioGroup.check(R.id.layout_choice_bottom)
        }
        // Use click listeners rather than OnCheckedChangeListener: the current
        // choice is pre-checked, and re-checking it would not fire a change.
        val chooseActionBar = View.OnClickListener { choose(R.id.layout_choice_actionbar) }
        val chooseBottom = View.OnClickListener { choose(R.id.layout_choice_bottom) }
        findViewById<View>(R.id.layout_choice_actionbar).setOnClickListener(chooseActionBar)
        findViewById<View>(R.id.image_actionbar).setOnClickListener(chooseActionBar)
        findViewById<View>(R.id.layout_choice_bottom).setOnClickListener(chooseBottom)
        findViewById<View>(R.id.image_bottom).setOnClickListener(chooseBottom)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun choose(@IdRes checkedId: Int) {
        PreferenceActivity.setUsingHoloSearch(this, checkedId == R.id.layout_choice_actionbar)
        PreferenceActivity.setShowLayoutChoice(this, false)
        startActivity(Intent(this, org.openintents.shopping.ShoppingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }
}
