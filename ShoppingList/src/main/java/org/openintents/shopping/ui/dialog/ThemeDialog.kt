/*
 * Copyright (C) 2007-2010 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.shopping.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnCancelListener
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView

import org.openintents.shopping.LogConstants
import org.openintents.shopping.R
import org.openintents.shopping.theme.ThemeShoppingList
import org.openintents.shopping.theme.ThemeUtils
import org.openintents.shopping.theme.ThemeUtils.ThemeInfo
import org.openintents.shopping.ui.PreferenceActivity

open class ThemeDialog : AlertDialog, OnClickListener, OnCancelListener, OnItemClickListener {

    companion object {
        private const val TAG = "ThemeDialog"
        private val debug = false || LogConstants.debug

        private const val BUNDLE_THEME = "theme"
    }

    private val mContext: Context
    private var mListener: ThemeDialogListener? = null
    private lateinit var mListView: ListView
    private lateinit var mCheckBox: CheckBox
    private lateinit var mListInfo: List<ThemeInfo>

    constructor(context: Context) : super(context) {
        mContext = context
        init()
    }

    constructor(context: Context, listener: ThemeDialogListener?) : super(context) {
        mContext = context
        mListener = listener
        init()
    }

    private fun init() {
        @Suppress("DEPRECATION")
        setInverseBackgroundForced(true)

        val inflate = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View

        view = inflate.inflate(R.layout.dialog_theme_settings, null)

        setView(view)

        mListView = view.findViewById(R.id.list1)
        mListView.cacheColorHint = 0
        mListView.itemsCanFocus = false
        mListView.choiceMode = ListView.CHOICE_MODE_SINGLE

        val b = Button(mContext)
        b.setText(R.string.get_more_themes)
        b.setOnClickListener {
            val i = Intent(mContext, PreferenceActivity::class.java)
            i.putExtra(PreferenceActivity.EXTRA_SHOW_GET_ADD_ONS, true)
            mContext.startActivity(i)

            pressCancel()
            dismiss()
        }

        val ll = LinearLayout(mContext)
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        ll.setPadding(20, 10, 20, 10)
        ll.addView(b, lp)
        ll.gravity = Gravity.CENTER
        mListView.addFooterView(ll)

        mCheckBox = view.findViewById(R.id.check1)

        setTitle(R.string.theme_pick)

        setButton(Dialog.BUTTON_POSITIVE, mContext.getText(R.string.ok), this)
        setButton(Dialog.BUTTON_NEGATIVE, mContext.getText(R.string.cancel), this)
        setOnCancelListener(this)

        prepareDialog()
    }

    private fun fillThemes() {
        mListInfo = ThemeUtils.getThemeInfos(mContext, ThemeShoppingList.SHOPPING_LIST_THEME)

        val s = Array(mListInfo.size) { i -> mListInfo[i].title }

        mListView.adapter = ArrayAdapter(mContext, android.R.layout.simple_list_item_single_choice, s)

        mListView.onItemClickListener = this
    }

    fun prepareDialog() {
        fillThemes()
        updateList()
        mCheckBox.isChecked = PreferenceActivity.getThemeSetForAll(mContext)
    }

    /**
     * Set selection to currently used theme.
     */
    private fun updateList() {
        var theme = mListener!!.onLoadTheme()

        // Check special cases for backward compatibility:
        if ("1" == theme) {
            theme = mContext.resources.getResourceName(R.style.Theme_ShoppingList)
        } else if ("2" == theme) {
            theme = mContext.resources.getResourceName(R.style.Theme_ShoppingList_Classic)
        } else if ("3" == theme) {
            theme = mContext.resources.getResourceName(R.style.Theme_ShoppingList_Android)
        }

        // Reset selection in case the current theme is not
        // in this list (for example got uninstalled).
        mListView.setItemChecked(-1, false)
        mListView.setSelection(0)

        var pos = 0
        for (ti in mListInfo) {
            if (ti.styleName == theme) {
                mListView.setItemChecked(pos, true)

                // Move list to show the selected item:
                mListView.setSelection(pos)
                break
            }
            pos++
        }
    }

    override fun onSaveInstanceState(): Bundle {
        if (debug) {
            Log.d(TAG, "onSaveInstanceState")
        }

        val b = super.onSaveInstanceState()
        val theme = getSelectedTheme()
        b.putString(BUNDLE_THEME, theme)
        return b
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        if (debug) {
            Log.d(TAG, "onRestore")
        }

        var theme = getSelectedTheme()

        if (savedInstanceState.containsKey(BUNDLE_THEME)) {
            theme = savedInstanceState.getString(BUNDLE_THEME)

            if (debug) {
                Log.d(TAG, "onRestore theme $theme")
            }
        }

        mListener!!.onSetTheme(theme)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == BUTTON_POSITIVE) {
            pressOk()
        } else if (which == BUTTON_NEGATIVE) {
            pressCancel()
        }
    }

    override fun onCancel(arg0: DialogInterface) {
        pressCancel()
    }

    fun pressOk() {
        /* User clicked Yes so do some stuff */
        val theme = getSelectedTheme()
        mListener!!.onSaveTheme(theme)
        mListener!!.onSetTheme(theme)

        val setForAllThemes = mCheckBox.isChecked
        PreferenceActivity.setThemeSetForAll(mContext, setForAllThemes)
        if (setForAllThemes) {
            mListener!!.onSetThemeForAll(theme)
        }
    }

    private fun getSelectedTheme(): String? {
        val pos = mListView.checkedItemPosition

        return if (pos != ListView.INVALID_POSITION) {
            val ti = mListInfo[pos]
            ti.styleName
        } else {
            null
        }
    }

    fun pressCancel() {
        /* User clicked No so do some stuff */
        val theme = mListener!!.onLoadTheme()
        mListener!!.onSetTheme(theme)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val theme = getSelectedTheme()

        if (theme != null) {
            mListener!!.onSetTheme(theme)
        }
    }

    interface ThemeDialogListener {
        fun onSetTheme(theme: String?)

        fun onSetThemeForAll(theme: String?)

        fun onLoadTheme(): String?

        fun onSaveTheme(theme: String?)
    }
}
