package org.telegram.ui;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.DayNightActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TwilightManager;

import java.util.Calendar;

/**
 * Created by Rikka on 2016/10/21.
 */

public class NightModeActivity extends BaseFragment implements TimePickerDialog.OnTimeSetListener {

    private SharedPreferences preferences;

    private ShadowSectionCell followSystemCell;
    private HeaderCell nightTimeHeaderCell;
    private TextCheckCell useLocationCell;
    private TextSettingsCell sunriseCell;
    private TextSettingsCell sunsetCell;
    private TextInfoPrivacyCell autoInfoCell;

    private int[] hourOfDay, minute;
    private static final int SUNRISE = 0;
    private static final int SUNSET = 1;

    @Override
    public View createView(Context context) {
        hourOfDay = new int[2];
        minute = new int[2];

        preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);

        hourOfDay[SUNRISE] = preferences.getInt("nightModeSunrise", 6);
        minute[SUNRISE] = preferences.getInt("nightModeSunriseMinute", 0);

        hourOfDay[SUNSET] = preferences.getInt("nightModeSunset", 22);
        minute[SUNSET] = preferences.getInt("nightModeSunsetMinute", 0);

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("NightMode", R.string.NightMode));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new LinearLayout(context);
        fragmentView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        fragmentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fragmentView.setBackgroundColor(ContextCompat.getColor(context, R.color.settings_background));

        ((LinearLayout) fragmentView).setOrientation(LinearLayout.VERTICAL);
        ((LinearLayout) fragmentView).setLayoutTransition(new LayoutTransition());

        int nightMode = preferences.getInt("nightMode", DayNightActivity.MODE_NIGHT_FOLLOW_SYSTEM);

        final TextSettingsCell nightModeCell = new TextSettingsCell(context);
        nightModeCell.setTextAndValue(LocaleController.getString("NightModeStatus", R.string.NightModeStatus), NightModeActivity.getNightModeStatus(nightMode), false);
        nightModeCell.setForeground(R.drawable.list_selector);
        ((LinearLayout) fragmentView).addView(nightModeCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        followSystemCell = new ShadowSectionCell(context);
        ((LinearLayout) fragmentView).addView(followSystemCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        nightTimeHeaderCell = new HeaderCell(context);
        nightTimeHeaderCell.setText(LocaleController.getString("NightModeAutoSwitch", R.string.NightModeAutoSwitch));
        ((LinearLayout) fragmentView).addView(nightTimeHeaderCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        useLocationCell = new TextCheckCell(context);
        useLocationCell.setForeground(R.drawable.list_selector);
        useLocationCell.setTextAndValueAndCheck(LocaleController.getString("NightModeLocation", R.string.NightModeLocation), LocaleController.getString("NightModeLocationInfo", R.string.NightModeLocationInfo), preferences.getBoolean("nightModeUseLocation", false), true, true);
        ((LinearLayout) fragmentView).addView(useLocationCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        useLocationCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!useLocationCell.isChecked()
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && getParentActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    getParentActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                } else {
                    useLocationCell.setChecked(!useLocationCell.isChecked());
                    preferences.edit().putBoolean("nightModeUseLocation", useLocationCell.isChecked()).apply();

                    resetNightModeAutoViewsVisibility();
                }
            }
        });

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.SECOND, 0);

        CharSequence sunset, sunrise;
        if (DateFormat.is24HourFormat(context)) {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay[SUNRISE]);
            calendar.set(Calendar.MINUTE, minute[SUNRISE]);
            sunrise = DateFormat.format("HH:mm", calendar);
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay[SUNSET]);
            calendar.set(Calendar.MINUTE, minute[SUNSET]);
            sunset = DateFormat.format("HH:mm", calendar);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay[SUNRISE]);
            calendar.set(Calendar.MINUTE, minute[SUNRISE]);
            sunrise = DateFormat.format("hh:mm a", calendar);
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay[SUNSET]);
            calendar.set(Calendar.MINUTE, minute[SUNSET]);
            sunset = DateFormat.format("hh:mm a", calendar);
        }

        sunsetCell = new TextSettingsCell(context);
        sunsetCell.setForeground(R.drawable.list_selector);
        sunsetCell.setTextAndValue(LocaleController.getString("NightModeStartTime", R.string.NightModeStartTime), sunset.toString(), true);
        ((LinearLayout) fragmentView).addView(sunsetCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        sunriseCell = new TextSettingsCell(context);
        sunriseCell.setForeground(R.drawable.list_selector);
        sunriseCell.setTextAndValue(LocaleController.getString("NightModeEndTime", R.string.NightModeEndTime), sunrise.toString(), false);
        ((LinearLayout) fragmentView).addView(sunriseCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        autoInfoCell = new TextInfoPrivacyCell(context);
        autoInfoCell.setText(LocaleController.getString("NightModeAutoSwitchInfo", R.string.NightModeAutoSwitchInfo));
        ((LinearLayout) fragmentView).addView(autoInfoCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        nightModeCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
                builder.setItems(new String[]{
                                getNightModeStatus(DayNightActivity.MODE_NIGHT_NO),
                                getNightModeStatus(DayNightActivity.MODE_NIGHT_YES),
                                getNightModeStatus(DayNightActivity.MODE_NIGHT_AUTO),
                                getNightModeStatus(DayNightActivity.MODE_NIGHT_FOLLOW_SYSTEM),
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                                int nightMode = DayNightActivity.MODE_NIGHT_FOLLOW_SYSTEM;

                                switch (i) {
                                    case 0: nightMode = DayNightActivity.MODE_NIGHT_NO; break;
                                    case 1: nightMode = DayNightActivity.MODE_NIGHT_YES; break;
                                    case 2: nightMode = DayNightActivity.MODE_NIGHT_AUTO; break;
                                    case 3: nightMode = DayNightActivity.MODE_NIGHT_FOLLOW_SYSTEM; break;
                                }

                                if (nightMode == DayNightActivity.MODE_NIGHT_FOLLOW_SYSTEM) {
                                    Toast.makeText(getParentActivity(), LocaleController.getString("NightModeFollowSystemInfo", R.string.NightModeFollowSystemInfo), Toast.LENGTH_SHORT).show();
                                }

                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putInt("nightMode", nightMode);
                                editor.commit();
                                DayNightActivity.setDefaultNightMode(nightMode);

                                getParentActivity().getWindow().setWindowAnimations(R.style.AnimationFadeInOut);
                                if (!((DayNightActivity) getParentActivity()).applyDayNight()) {
                                    resetViewsVisibility();
                                    nightModeCell.setValue(NightModeActivity.getNightModeStatus(nightMode));
                                }
                            }
                        });
                showDialog(builder.create());
            }
        });

        sunsetCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDataPickerDialog(false, hourOfDay[SUNSET], minute[SUNSET]);
            }
        });

        sunriseCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDataPickerDialog(true, hourOfDay[SUNRISE], minute[SUNRISE]);
            }
        });

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getParentActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                useLocationCell.setChecked(false);
                preferences.edit().putBoolean("nightModeUseLocation", useLocationCell.isChecked()).apply();
                TwilightManager.setUseLocation(false);
            }
        }

        resetViewsVisibility();
    }

    private void resetViewsVisibility() {
        int nightMode = preferences.getInt("nightMode", DayNightActivity.MODE_NIGHT_FOLLOW_SYSTEM);

        if (nightMode == DayNightActivity.MODE_NIGHT_AUTO) {
            resetNightModeAutoViewsVisibility();
        } else {
            nightTimeHeaderCell.setVisibility(View.GONE);
            sunriseCell.setVisibility(View.GONE);
            sunsetCell.setVisibility(View.GONE);
            autoInfoCell.setVisibility(View.GONE);
            useLocationCell.setVisibility(View.GONE);
        }
    }

    private void resetNightModeAutoViewsVisibility() {
        if (useLocationCell.isChecked()) {
            sunriseCell.setVisibility(View.GONE);
            sunsetCell.setVisibility(View.GONE);
        } else {
            sunriseCell.setVisibility(View.VISIBLE);
            sunsetCell.setVisibility(View.VISIBLE);
        }

        nightTimeHeaderCell.setVisibility(View.VISIBLE);
        autoInfoCell.setVisibility(View.VISIBLE);
        useLocationCell.setVisibility(View.VISIBLE);

    }

    @Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            TwilightManager.setUseLocation(true);
            useLocationCell.setChecked(true);
        }
    }

    private boolean isEditingSunrise;

    private void showDataPickerDialog(boolean isEnd, int hour, int minute) {
        isEditingSunrise = isEnd;
        showDialog(new TimePickerDialog(getParentActivity(), this, hour, minute, DateFormat.is24HourFormat(getParentActivity())));
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        if (isEditingSunrise) {
            this.hourOfDay[SUNRISE] = hourOfDay;
            this.minute[SUNRISE] = minute;

            preferences.edit()
                    .putInt("nightModeSunrise", hourOfDay)
                    .putInt("nightModeSunriseMinute", minute)
                    .apply();
            sunriseCell.setValue(getFormattedTime(view.getContext(), hourOfDay, minute));
            TwilightManager.setSunrise(hourOfDay, minute);
        } else {
            this.hourOfDay[SUNSET] = 0;
            this.minute[SUNSET] = 0;

            preferences.edit()
                    .putInt("nightModeSunset", hourOfDay)
                    .putInt("nightModeSunsetMinute", minute)
                    .apply();
            sunsetCell.setValue(getFormattedTime(view.getContext(), hourOfDay, minute));
            TwilightManager.setSunset(hourOfDay, minute);
        }
    }

    private static String getFormattedTime(Context context, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);

        if (DateFormat.is24HourFormat(context)) {
            return DateFormat.format("HH:mm", calendar).toString();
        } else {
            return DateFormat.format("hh:mm a", calendar).toString();
        }
    }

    public static String getNightModeStatus(int value) {
        switch (value) {
            case DayNightActivity.MODE_NIGHT_YES:
                return LocaleController.getString("NightModeEnable", R.string.NightModeEnable);
            case DayNightActivity.MODE_NIGHT_AUTO:
                return LocaleController.getString("NightModeAuto", R.string.NightModeAuto);
            case DayNightActivity.MODE_NIGHT_FOLLOW_SYSTEM:
                return LocaleController.getString("NightModeFollowSystem", R.string.NightModeFollowSystem);
            default:
                return LocaleController.getString("NightModeDisable", R.string.NightModeDisable);
        }
    }
}
