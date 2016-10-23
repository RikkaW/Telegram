package org.telegram.ui.Components;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.telegram.messenger.BuildVars;
import org.telegram.messenger.R;
import org.telegram.messenger.exoplayer.C;

/**
 * Created by Rikka on 2016/10/18.
 */

public class DayNightActivity extends Activity {

    private static final String KEY_LOCAL_NIGHT_MODE = "local_night_mode";

    private static final String TAG = "DayNightActivity";

    public static final int MODE_NIGHT_NO = 1;
    public static final int MODE_NIGHT_YES = 2;
    public static final int MODE_NIGHT_AUTO = 0;
    public static final int MODE_NIGHT_FOLLOW_SYSTEM = -1;
    static final int MODE_NIGHT_UNSPECIFIED = -100;

    private static int sDefaultNightMode = MODE_NIGHT_FOLLOW_SYSTEM;

    private int mLocalNightMode = MODE_NIGHT_UNSPECIFIED;
    private boolean mApplyDayNightCalled;
    private AutoNightModeManager mAutoNightModeManager;

    private int mThemeId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && mLocalNightMode == MODE_NIGHT_UNSPECIFIED) {
            // If we have a icicle and we haven't had a local night mode set yet, try and read
            // it from the icicle
            mLocalNightMode = savedInstanceState.getInt(KEY_LOCAL_NIGHT_MODE,
                    MODE_NIGHT_UNSPECIFIED);
        }

        if (applyDayNight() && mThemeId != 0) {
            // If DayNight has been applied, we need to re-apply the theme for
            // the changes to take effect. On API 23+, we should bypass
            // setTheme(), which will no-op if the theme ID is identical to the
            // current theme ID.
            if (Build.VERSION.SDK_INT >= 23) {
                onApplyThemeResource(getTheme(), mThemeId, false);
            } else {
                setTheme(mThemeId);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setTaskDescription(new ActivityManager.TaskDescription(null, null, ContextCompat.getColor(this, R.color.colorPrimary)));
    }

    @Override
    public void setTheme(@StyleRes final int resid) {
        super.setTheme(resid);
        // Keep hold of the theme id so that we can re-set it later if needed
        mThemeId = resid;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // This will apply day/night if the time has changed, it will also call through to
        // setupAutoNightModeIfNeeded()
        applyDayNight();
    }


    @Override
    protected void onStop() {
        super.onStop();

        // Make sure we clean up any receivers setup for AUTO mode
        if (mAutoNightModeManager != null) {
            mAutoNightModeManager.cleanup();
        }
    }

    public boolean applyDayNight() {
        boolean applied = false;

        final int nightMode = getNightMode();
        final int modeToApply = mapNightMode(nightMode);
        if (modeToApply != MODE_NIGHT_FOLLOW_SYSTEM) {
            applied = updateForNightMode(modeToApply);
        }

        if (nightMode == MODE_NIGHT_AUTO) {
            // If we're already been started, we may need to setup auto mode again
            ensureAutoNightModeManager();
            mAutoNightModeManager.setup();
        }

        mApplyDayNightCalled = true;
        return applied;
    }

    public void setLocalNightMode(final int mode) {
        switch (mode) {
            case MODE_NIGHT_AUTO:
            case MODE_NIGHT_NO:
            case MODE_NIGHT_YES:
            case MODE_NIGHT_FOLLOW_SYSTEM:
                if (mLocalNightMode != mode) {
                    mLocalNightMode = mode;
                    if (mApplyDayNightCalled) {
                        // If we've already applied day night, re-apply since we won't be
                        // called again
                        applyDayNight();
                    }
                }
                break;
            default:
                Log.i(TAG, "setLocalNightMode() called with an unknown mode");
                break;
        }
    }

    int mapNightMode(final int mode) {
        switch (mode) {
            case MODE_NIGHT_AUTO:
                ensureAutoNightModeManager();
                return mAutoNightModeManager.getApplyableNightMode();
            case MODE_NIGHT_UNSPECIFIED:
                // If we don't have a mode specified, just let the system handle it
                return MODE_NIGHT_FOLLOW_SYSTEM;
            default:
                return mode;
        }
    }

    public static void setDefaultNightMode(int mode) {
        switch (mode) {
            case MODE_NIGHT_AUTO:
            case MODE_NIGHT_NO:
            case MODE_NIGHT_YES:
            case MODE_NIGHT_FOLLOW_SYSTEM:
                sDefaultNightMode = mode;
                break;
            default:
                Log.d(TAG, "setDefaultNightMode() called with an unknown mode");
                break;
        }
    }

    public static int getDefaultNightMode() {
        return sDefaultNightMode;
    }

    private int getNightMode() {
        return mLocalNightMode != MODE_NIGHT_UNSPECIFIED ? mLocalNightMode : getDefaultNightMode();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mLocalNightMode != MODE_NIGHT_UNSPECIFIED) {
            // If we have a local night mode set, save it
            outState.putInt(KEY_LOCAL_NIGHT_MODE, mLocalNightMode);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we clean up any receivers setup for AUTO mode
        if (mAutoNightModeManager != null) {
            mAutoNightModeManager.cleanup();
        }
    }

    /**
     * Updates the {@link Resources} configuration {@code uiMode} with the
     * chosen {@code UI_MODE_NIGHT} value.
     */
    private boolean updateForNightMode(final int mode) {
        final Resources res = getResources();
        final Configuration conf = res.getConfiguration();
        final int currentNightMode = conf.uiMode & Configuration.UI_MODE_NIGHT_MASK;

        final int newNightMode = (mode == MODE_NIGHT_YES)
                ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO;

        if (currentNightMode != newNightMode) {
            if (shouldRecreateOnNightModeChange()) {
                if (BuildVars.DEBUG_VERSION) {
                    Log.d(TAG, "applyNightMode() | Night mode changed, recreating Activity");
                }
                // If we've already been created, we need to recreate the Activity for the
                // mode to be applied
                final Activity activity = this;
                activity.recreate();
            } else {
                if (BuildVars.DEBUG_VERSION) {
                    Log.d(TAG, "applyNightMode() | Night mode changed, updating configuration");
                }
                final Configuration newConf = new Configuration(conf);
                newConf.uiMode = newNightMode
                        | (newConf.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
                res.updateConfiguration(newConf, null);
            }
            return true;
        } else {
            if (BuildVars.DEBUG_VERSION) {
                Log.d(TAG, "applyNightMode() | Night mode has not changed. Skipping");
            }
        }
        return false;
    }

    private void ensureAutoNightModeManager() {
        if (mAutoNightModeManager == null) {
            mAutoNightModeManager = new AutoNightModeManager(TwilightManager.getInstance(this));
        }
    }

    @VisibleForTesting
    final AutoNightModeManager getAutoNightModeManager() {
        ensureAutoNightModeManager();
        return mAutoNightModeManager;
    }

    private boolean shouldRecreateOnNightModeChange() {
        if (mApplyDayNightCalled) {
            // If we've already appliedDayNight() (via setTheme), we need to check if the
            // Activity has configChanges set to handle uiMode changes
            final PackageManager pm = getPackageManager();
            try {
                final ActivityInfo info = pm.getActivityInfo(
                        new ComponentName(this, getClass()), 0);
                // We should return true (to recreate) if configChanges does not want to
                // handle uiMode
                return (info.configChanges & ActivityInfo.CONFIG_UI_MODE) == 0;
            } catch (PackageManager.NameNotFoundException e) {
                // This shouldn't happen but let's not crash because of it, we'll just log and
                // return true (since most apps will do that anyway)
                Log.d(TAG, "Exception while getting ActivityInfo", e);
                return true;
            }
        }
        return false;
    }

    final class AutoNightModeManager {
        private TwilightManager mTwilightManager;
        private boolean mIsNight;

        private BroadcastReceiver mAutoTimeChangeReceiver;
        private IntentFilter mAutoTimeChangeReceiverFilter;

        AutoNightModeManager(@NonNull TwilightManager twilightManager) {
            mTwilightManager = twilightManager;
            mIsNight = twilightManager.isNight();
        }

        final int getApplyableNightMode() {
            return mIsNight ? MODE_NIGHT_YES : MODE_NIGHT_NO;
        }

        final void dispatchTimeChanged() {
            final boolean isNight = mTwilightManager.isNight();
            if (isNight != mIsNight) {
                mIsNight = isNight;
                applyDayNight();
            }
        }

        final void setup() {
            cleanup();

            // If we're set to AUTO, we register a receiver to be notified on time changes. The
            // system only send the tick out every minute, but that's enough fidelity for our use
            // case
            if (mAutoTimeChangeReceiver == null) {
                mAutoTimeChangeReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (false/*DEBUG*/) {
                            Log.d("AutoTimeChangeReceiver", "onReceive | Intent: " + intent);
                        }
                        dispatchTimeChanged();
                    }
                };
            }
            if (mAutoTimeChangeReceiverFilter == null) {
                mAutoTimeChangeReceiverFilter = new IntentFilter();
                mAutoTimeChangeReceiverFilter.addAction(Intent.ACTION_TIME_CHANGED);
                mAutoTimeChangeReceiverFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
                mAutoTimeChangeReceiverFilter.addAction(Intent.ACTION_TIME_TICK);
            }
            registerReceiver(mAutoTimeChangeReceiver, mAutoTimeChangeReceiverFilter);
        }

        final void cleanup() {
            if (mAutoTimeChangeReceiver != null) {
                unregisterReceiver(mAutoTimeChangeReceiver);
                mAutoTimeChangeReceiver = null;
            }
        }
    }
}
