package org.openintents.shopping.test.test;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

import org.openintents.intents.ShoppingListIntents;

public class BarcodeScannerActivity extends Activity {

    private static final String TAG = "BarcodeScannerActivity";

    String resultModeData = null;
    private Random random = new Random();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent callingIntent = getIntent();

        if (callingIntent != null) {
            resultModeData = callingIntent.getDataString();
        }

        // Return result to caller
        Log.d(TAG, "Return result to " + getCallingPackage());

        Intent resultIntent = new Intent(getIntent());

        resultIntent.setData(Uri.parse(resultModeData));
        Log.d(TAG, "Result intent data: " + resultIntent.getDataString());

        String barcodeName = ShoppingActivityTest.BARCODE_SCANNER_ITEM;
        String barcode = "barcode_" + random.nextInt(1000000);

        Log.d(TAG, "Return: " + barcodeName + ", " + barcode);

        ArrayList<String> newEntry = new ArrayList<String>();
        newEntry.add(barcodeName);
        resultIntent.putStringArrayListExtra(
                ShoppingListIntents.EXTRA_STRING_ARRAYLIST_SHOPPING, newEntry);

        ArrayList<String> barcodeList = new ArrayList<String>();
        barcodeList.add(barcode);
        resultIntent
                .putStringArrayListExtra(
                        ShoppingListIntents.EXTRA_STRING_ARRAYLIST_BARCODE,
                        barcodeList);

        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

}
