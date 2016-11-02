package org.telegram.messenger.support.iap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.telegram.messenger.BuildConfig;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.support.iap.utils.IabBroadcastReceiver;
import org.telegram.messenger.support.iap.utils.IabHelper;
import org.telegram.messenger.support.iap.utils.IabResult;
import org.telegram.messenger.support.iap.utils.Inventory;
import org.telegram.messenger.support.iap.utils.Purchase;

/**
 * Created by Rikka on 2016/9/3.
 */
public class DonateHelper implements IabBroadcastReceiver.IabBroadcastListener {

    private static final String TAG = "DonateHelper";

    public static final String SKU_DONATE_1 = "donate_1";
    public static final String SKU_DONATE_2 = "donate_2";
    public static final String SKU_DONATE_5 = "donate_5";
    public static final String SKU_DONATE_10 = "donate_10";

    private static final int RC_REQUEST = 10001;

    private Context mContext;

    // The helper object
    private IabHelper mHelper;

    // Provides purchase notification while this app is running
    private IabBroadcastReceiver mBroadcastReceiver;

    private boolean isSuccess;

    public DonateHelper(Context context) {
        mContext = context;

        String base64EncodedPublicKey = BuildConfig.BASE64_PUBLIC_KEY;

        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(context, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result);
                    isSuccess = false;
                    return;
                }

                isSuccess = true;

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(DonateHelper.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                mContext.registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            consume(inventory, SKU_DONATE_1);
            consume(inventory, SKU_DONATE_2);
            consume(inventory, SKU_DONATE_5);
            consume(inventory, SKU_DONATE_10);

            Log.d(TAG, "Query inventory was successful.");
        }
    };

    private void consume(Inventory inventory, String sku) {
        Purchase gasPurchase = inventory.getPurchase(sku);
        if (gasPurchase != null/* && verifyDeveloperPayload(gasPurchase)*/) {
            Log.d(TAG, "Consuming " + sku);
            try {
                mHelper.consumeAsync(inventory.getPurchase(sku), mConsumeFinishedListener);
            } catch (IabHelper.IabAsyncInProgressException e) {
                complain("Error consuming. Another async operation in progress.");
            }
            return;
        }
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (mPurchaseSuccessListener != null) {
                mPurchaseSuccessListener.onSuccess(purchase.getSku());
            }

            if (true/*purchase.getSku().equals(SKU_DONATE_1)*/) {
                // Consume it.
                try {
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming. Another async operation in progress.");
                    return;
                }
            }
        }
    };

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
                Log.d(TAG, "Consumption successful. Provisioning.");
            } else {
                complain("Error while consuming: " + result);
            }
            Log.d(TAG, "End consumption flow.");
        }
    };


    private boolean verifyDeveloperPayload(Purchase purchase) {
        return true;
    }


    @Override
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error querying inventory. Another async operation in progress.");
        }
    }

    void complain(String message) {
        Log.e(TAG, "**** Error: " + message);
        alert("Error: " + message);
    }

    void alert(String message) {
        /*AlertDialog.Builder bld = new AlertDialog.Builder(mContext);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();*/
        //Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    public interface OnPurchaseSuccessListener {
        void onSuccess(String sku);
    }

    private OnPurchaseSuccessListener mPurchaseSuccessListener;

    public void start(Activity activity, String sku, @Nullable OnPurchaseSuccessListener listener) {
        Log.d(TAG, "Launching purchase flow for gas.");

        mPurchaseSuccessListener = listener;

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "";

        try {
            mHelper.launchPurchaseFlow(activity, sku, RC_REQUEST,
                    mPurchaseFinishedListener, payload);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error launching purchase flow. Another async operation in progress.");
        }
    }

    public void onDestroy() {
        // very important:
        if (mBroadcastReceiver != null) {
            mContext.unregisterReceiver(mBroadcastReceiver);
        }

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            try {
                mHelper.disposeWhenFinished();
            } catch (IllegalArgumentException ignored) {

            }
            mHelper = null;
        }
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return true;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            //super.onActivityResult(requestCode, resultCode, data);
            return false;
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
        return true;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public Dialog getDialog(final Activity activity) {
        final DonateHelper.OnPurchaseSuccessListener listener = new DonateHelper.OnPurchaseSuccessListener() {
            @Override
            public void onSuccess(String sku) {
                if (!activity.isFinishing()) {
                    Toast.makeText(activity, LocaleController.getString("DonateThanks", R.string.DonateThanks), Toast.LENGTH_SHORT).show();
                }
            }
        };
        return new AlertDialog.Builder(activity)
                .setItems(new CharSequence[]{"1 USD", "2 USD", "5 USD", "10 USD"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                start(activity, DonateHelper.SKU_DONATE_1, listener);
                                break;
                            case 1:
                                start(activity, DonateHelper.SKU_DONATE_2, listener);
                                break;
                            case 2:
                                start(activity, DonateHelper.SKU_DONATE_5, listener);
                                break;
                            case 3:
                                start(activity, DonateHelper.SKU_DONATE_10, listener);
                                break;
                        }
                    }
                }).create();
    }
}
