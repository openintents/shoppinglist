package org.openintents.shopping.ui.dialog

import android.content.Context
import org.openintents.shopping.R

class NewListDialog : RenameListDialog {

    constructor(context: Context) : super(context) {
        setTitle(R.string.ask_new_list)
    }

    constructor(context: Context, listener: DialogActionListener) : super(context) {
        setTitle(R.string.ask_new_list)
        setDialogActionListener(listener)
    }
}
