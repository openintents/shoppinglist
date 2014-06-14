package org.openintents.shopping.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.openintents.shopping.R;
import org.openintents.shopping.ui.PreferenceActivity;

public class RenameListDialog extends AlertDialog implements OnClickListener {

    Context mContext;

    protected EditText mEditText;

    DialogActionListener mDialogActionListener;

    public RenameListDialog(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public RenameListDialog(Context context, String name,
                            DialogActionListener listener) {
        super(context);
        mContext = context;
        init();
        setName(name);
        setDialogActionListener(listener);
    }

    /**
     * @param context
     */
    private void init() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.dialog_rename_list, null);
        setView(view);

        mEditText = (EditText) view.findViewById(R.id.edittext);

        KeyListener kl = PreferenceActivity
                .getCapitalizationKeyListenerFromPrefs(mContext);
        mEditText.setKeyListener(kl);

        setIcon(android.R.drawable.ic_menu_edit);
        setTitle(R.string.ask_rename_list);

        setButton(mContext.getText(R.string.ok), this);
        setButton2(mContext.getText(R.string.cancel), this);
    }

    public void setName(String name) {
        mEditText.setText(name);

        // To move cursor position to the end of list's name
        mEditText.setSelection(name.length());
    }

    public void setDialogActionListener(DialogActionListener listener) {
        mDialogActionListener = listener;
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON1) {
            pressOk();
        }

    }

    public void pressOk() {
        String name = mEditText.getText().toString();
        mDialogActionListener.onAction(name);
    }
}
