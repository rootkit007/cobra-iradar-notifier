package com.greatnowhere.radar;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class ErrorDialogFragment extends DialogFragment {
    // Global field to contain the error dialog
    private Dialog mDialog;

    // Default constructor. Sets the dialog field to null
    public ErrorDialogFragment() {
        super();
        mDialog = null;
    }

    // Set the dialog to display
    public void setDialog(Dialog dialog) {
        mDialog = dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return mDialog;
    }    	
}