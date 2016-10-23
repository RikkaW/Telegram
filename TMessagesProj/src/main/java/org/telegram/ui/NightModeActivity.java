package org.telegram.ui;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.Cells.HeaderCell;
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

    private TextSettingsCell sunriseCell;
    private TextSettingsCell sunsetCell;

    private int[] hourOfDay, minute;
    private static final int SUNRISE = 0;
    private static final int SUNSET = 1;


    @Override
    public View createView(Context context) {
        hourOfDay = new int[2];
        minute = new int[2];

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);

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

        int nightMode = preferences.getInt("nightMode", DayNightActivity.MODE_NIGHT_FOLLOW_SYSTEM);

        TextSettingsCell nightModeCell = new TextSettingsCell(context);
        nightModeCell.setTextAndValue(LocaleController.getString("NightModeStatus", R.string.NightModeStatus), NightModeActivity.getNightModeStatus(nightMode), false);
        nightModeCell.setForeground(R.drawable.list_selector);
        ((LinearLayout) fragmentView).addView(nightModeCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextInfoPrivacyCell infoCell = new TextInfoPrivacyCell(context);
        infoCell.setText(LocaleController.getString("NightModeInfo", R.string.NightModeInfo));
        ((LinearLayout) fragmentView).addView(infoCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        HeaderCell headerCell = new HeaderCell(context);
        headerCell.setText(LocaleController.getString("NightModeAutoSwitch", R.string.NightModeAutoSwitch));
        ((LinearLayout) fragmentView).addView(headerCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));


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

        TextInfoPrivacyCell infoCell2 = new TextInfoPrivacyCell(context);
        infoCell2.setText(LocaleController.getString("NightModeAutoSwitchInfo", R.string.NightModeAutoSwitchInfo));
        ((LinearLayout) fragmentView).addView(infoCell2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

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

                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putInt("nightMode", nightMode);
                                editor.commit();
                                DayNightActivity.setDefaultNightMode(nightMode);

                                getParentActivity().getWindow().setWindowAnimations(R.style.AnimationFadeInOut);
                                getParentActivity().recreate();
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
