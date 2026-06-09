package org.openintents.shopping.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.EditText
import org.openintents.shopping.R
import org.openintents.shopping.ui.PreferenceActivity

open class RenameListDialog : AlertDialog, DialogInterface.OnClickListener {

    protected lateinit var mEditText: EditText
    private val mContext: Context
    private var mDialogActionListener: DialogActionListener? = null

    constructor(context: Context) : super(context) {
        mContext = context
        init()
    }

    constructor(context: Context, name: String, listener: DialogActionListener) : super(context) {
        mContext = context
        init()
        setName(name)
        setDialogActionListener(listener)
    }

    private fun init() {
        val inflater = LayoutInflater.from(mContext)
        val view = inflater.inflate(R.layout.dialog_rename_list, null)
        setView(view)

        mEditText = view.findViewById(R.id.edittext)

        val kl = PreferenceActivity.getCapitalizationKeyListenerFromPrefs(mContext)
        mEditText.keyListener = kl

        setIcon(android.R.drawable.ic_menu_edit)
        setTitle(R.string.ask_rename_list)

        setButton(mContext.getText(R.string.ok), this)
        @Suppress("DEPRECATION")
        setButton2(mContext.getText(R.string.cancel), this)
    }

    fun setName(name: String) {
        mEditText.setText(name)

        // To move cursor position to the end of list's name
        mEditText.setSelection(name.length)
    }

    fun setDialogActionListener(listener: DialogActionListener) {
        mDialogActionListener = listener
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == BUTTON1) {
            pressOk()
        }
    }

    fun pressOk() {
        val name = mEditText.text.toString()
        mDialogActionListener!!.onAction(name)
    }
}
