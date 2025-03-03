/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.widgets.datetime.pickers;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.joda.time.LocalDateTime;
import org.joda.time.chrono.ISOChronology;
import org.odk.collect.android.R;
import org.odk.collect.android.widgets.datetime.DateTimeUtils;
import org.odk.collect.android.widgets.utilities.DateTimeWidgetUtils;
import org.odk.collect.android.widgets.viewmodels.DateTimeViewModel;

public final class TimePickerDialog12Hr extends DialogFragment {
    private static final int MIN_SUPPORTED_HOUR = 0;
    private static final int MAX_SUPPORTED_HOUR = 12;
    public static final NumberPicker.Formatter FORMATTER = new NumberPicker.Formatter() {
        @SuppressLint("DefaultLocale")
        @Override
        public String format(int value) {
            return String.format("%02d", value);
        }
    };
    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private NumberPicker amPmPicker;

    private TextView gregorianTimeText;

    private DateTimeViewModel viewModel;
    private CustomTimePickerDialog.TimeChangeListener timeChangeListener;
    private String[] amPmsArray;
    private int amPmAt;
    private int hourNow;
    private int hourThen;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        System.out.println("6330C: onAttach");

        if (context instanceof CustomTimePickerDialog.TimeChangeListener) {
            timeChangeListener = (CustomTimePickerDialog.TimeChangeListener) context;
        }

        viewModel = new ViewModelProvider(this).get(DateTimeViewModel.class);
        if (viewModel.getLocalDateTime() == null) {
            LocalDateTime ldt = (LocalDateTime) getArguments().getSerializable(DateTimeWidgetUtils.TIME);
            viewModel.setLocalDateTime(ldt);
        }

        viewModel.getSelectedTime().observe(this, localDateTime -> {
            if (localDateTime != null && timeChangeListener != null) {
                timeChangeListener.onTimeChanged(localDateTime.toDateTime());
            }
        });
        amPmsArray = new String[]{"AM", "PM"};
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        System.out.println("6330C: onCreateDialog");
        return new MaterialAlertDialogBuilder(getActivity())
                .setTitle(org.odk.collect.strings.R.string.select_time)
                .setView(R.layout.time_picker_dialog12_hr)
                .setPositiveButton(org.odk.collect.strings.R.string.ok, (dialog, id) -> {
                    LocalDateTime date = DateTimeUtils.getDateAsGregorian(getPickerTime());
                    viewModel.setSelectedTime(date.getHourOfDay(), date.getMinuteOfHour());
                    dismiss();
                })
                .setNegativeButton(org.odk.collect.strings.R.string.cancel, (dialog, id) -> dismiss())
                .create();
    }

    @Override
    public void onDestroyView() {
        System.out.println("6330C: onDestroyView");
        LocalDateTime ldt = DateTimeUtils.getDateAsGregorian(getPickerTime());
        viewModel.setLocalDateTime(ldt);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        System.out.println("6330C: onResume");
        super.onResume();
        gregorianTimeText = getDialog().findViewById(R.id.date_time_gregorian);
        //setUpPickers();
        System.out.println("6330C: setUpPickers");
        hourPicker = getDialog().findViewById(R.id.hour_picker);
        hourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> hourUpdated());
        minutePicker = getDialog().findViewById(R.id.minute_picker);
        minutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateGregorianDateTimeLabel());
        amPmPicker = getDialog().findViewById(R.id.am_pm_picker);
        amPmPicker.setOnValueChangedListener((picker, oldVal, newVal) -> amPmUpdated());

        //hidePickersIfNeeded();
        // setUpValues();
        //setUpDatePicker();
        LocalDateTime ldt = DateTimeUtils
                .skipDaylightSavingGapIfExists(getTime())
                .toDateTime()
                .withChronology(ISOChronology.getInstance())
                .toLocalDateTime();
        setUpHourPicker(ldt.getHourOfDay());
        setUpMinutePicker(ldt.getMinuteOfHour());
        setUpAmPmPicker();
        updateGregorianDateTimeLabel();
    }

    protected void updateGregorianDateTimeLabel() {
        String label = getPickerTime().toString();
        System.out.println("6330C: updateGregorianTimeLabel" +
                "2025-03-05T19:11:00.000 "
                + label);
        gregorianTimeText.setText(label.replaceAll(
                ".+(\\d\\d:\\d\\d):.+", "$1"));
    }

    protected void setUpHourPicker(int hour) {
        //Year
        System.out.println("6330C: setUpHourPicker");
        hourPicker.setFormatter(FORMATTER);
        hourPicker.setMinValue(MIN_SUPPORTED_HOUR);
        hourPicker.setMaxValue(MAX_SUPPORTED_HOUR);
        int h12 = hour % 12;
        hourPicker.setValue(h12);
        amPmAt = h12 == hour - 12 ? 1 : 0;
    }

    protected void setUpMinutePicker(int minuteOfHour) {
        //Day
        System.out.println("6330C: setUpMinutePicker");
        //Day
        System.out.println("6330C: setUpMinutePicker");
        minutePicker.setFormatter(FORMATTER);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(minuteOfHour);
    }

    protected void setUpAmPmPicker() {
        amPmPicker.setMaxValue(amPmsArray.length - 1);
        amPmPicker.setDisplayedValues(amPmsArray);
        amPmPicker.setValue(amPmAt);
    }

    protected void amPmUpdated() {//Month
        System.out.println("6330C: amPmUpdated");
//        updateMinutes();
        updateGregorianDateTimeLabel();
    }

    protected void hourUpdated() {
        System.out.println("6330C: hourUpdated");
        updateGregorianDateTimeLabel();
        hourThen = hourNow;
        hourNow = true ? getHour() :
                getTime().getHourOfDay();
        boolean hourUp = hourThen < hourNow;
        if (true && (hourUp ? hourNow == 11 : hourNow == 1)) {
            amPmAt = amPmAt == 1 ? 0 : 1;
            hours[amPmAt].applyToPicker();
            int h12 = hourNow % 12;
            hourPicker.setValue(h12);
        } else if (hourNow == 0 && hourThen == 11) {

        } else if (hourNow == 12 && hourThen == 11) {

        }
    }

    private class PickerHours {
        private final int min;
        private final int max;

        PickerHours(int min, int max) {
            this.min = min;
            this.max = max;
        }

        void applyToPicker() {
            hourPicker.setMinValue(min);
            hourPicker.setMaxValue(max);
        }
    }

    PickerHours[] hours = new PickerHours[]{
            new PickerHours(0, 11),
            new PickerHours(1, 12),
    };

    public int getMinute() {//Day
        System.out.println("6330C: getPickerMinute");
        return minutePicker.getValue();
    }

    public String getAmPm() {//Month
        int value = getAmPmAt();
        System.out.println("6330C: getPickerAmPm " + value);
        return amPmPicker.getDisplayedValues()[value];
    }

    public int getAmPmAt() {//Month
        System.out.println("6330C: getAmPmId");
        return amPmPicker.getValue();
    }

    public int getHour() {//Year
        System.out.println("6330C: getPickerHour");
        return hourPicker.getValue();
    }

    public LocalDateTime getTime() {//Date
        System.out.println("6330C: getTime");
        return getMinute() == 0 ? viewModel.getLocalDateTime()
                : getPickerTime();
    }

    protected void updateMinutes() {
    }//Days
    protected LocalDateTime getPickerTime() {//Date
        System.out.println("6330I: getPickerTime");
        return new LocalDateTime().withTime(
                getHour() + 12 * getAmPmAt(),
                getMinute(), 0, 0);
    }
}
