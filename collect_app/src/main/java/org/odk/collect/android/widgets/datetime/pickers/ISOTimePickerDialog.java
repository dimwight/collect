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
import android.widget.NumberPicker.Formatter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.joda.time.LocalDateTime;
import org.joda.time.chrono.ISOChronology;
import org.odk.collect.android.R;
import org.odk.collect.android.widgets.datetime.DateTimeUtils;
import org.odk.collect.android.widgets.utilities.DateTimeWidgetUtils;
import org.odk.collect.android.widgets.viewmodels.DateTimeViewModel;

public final class ISOTimePickerDialog extends DialogFragment {
    private static final int MIN_SUPPORTED_HOUR = 0;
    private static final int MAX_SUPPORTED_HOUR = 23;
    private static final Formatter FORMATTER_HR = new Formatter() {
        @SuppressLint("DefaultLocale")
        @Override
        public String format(int h24) {
            int h12 = h24 - (h24 < 1 ? -12 : h24 < 13 ? 0 : 12);
            return String.format("%02d", h12);
        }
    };
    private static final Formatter FORMATTER_MIN = new Formatter() {
        @SuppressLint("DefaultLocale")
        @Override
        public String format(int value) {
            return String.format("%02d", value);
        }
    };
    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private NumberPicker amPmPicker;

    private TextView timeText;

    private DateTimeViewModel viewModel;
    private CustomTimePickerDialog.TimeChangeListener timeChangeListener;
    private String[] amPmsArray;

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
            viewModel.setLocalDateTime(false ? ldt
                    : new LocalDateTime().withTime(getDebugHour(),
                    30, 0, 0));
        }

        viewModel.getSelectedTime().observe(this, localDateTime -> {
            if (localDateTime != null && timeChangeListener != null) {
                timeChangeListener.onTimeChanged(localDateTime.toDateTime());
            }
        });
        amPmsArray = new String[]{"AM", "PM"};
    }

    @NonNull
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
        if (false) viewModel.setLocalDateTime(ldt);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        System.out.println("6330C: onResume");
        super.onResume();
        timeText = getDialog().findViewById(R.id.date_time_gregorian);
        //setUpPickers();
        System.out.println("6330C: setUpPickers");
        hourPicker = getDialog().findViewById(R.id.hour_picker);
        hourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> hourUpdated());
        minutePicker = getDialog().findViewById(R.id.minute_picker);
        minutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateTimeLabel());
        amPmPicker = getDialog().findViewById(R.id.am_pm_picker);
        amPmPicker.setOnValueChangedListener((picker, oldVal, newVal) -> amPmUpdated());

        LocalDateTime ldt = DateTimeUtils
                .skipDaylightSavingGapIfExists(minutePicker.getValue() == 0 ? viewModel.getLocalDateTime()
                        : getPickerTime())
                .toDateTime()
                .withChronology(ISOChronology.getInstance())
                .toLocalDateTime();
        int hour = ldt.getHourOfDay();
        System.out.println("6330C: setUpHourPicker");
        hourPicker.setFormatter(FORMATTER_HR);
        hourPicker.setMinValue(MIN_SUPPORTED_HOUR);
        hourPicker.setMaxValue(MAX_SUPPORTED_HOUR);
        hourPicker.setValue(hour);
        int minuteOfHour = ldt.getMinuteOfHour();
        System.out.println("6330C: setUpMinutePicker");
        minutePicker.setFormatter(FORMATTER_MIN);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(minuteOfHour);
        System.out.println("6330C: setUpAmPmPicker");
        amPmPicker.setMaxValue(amPmsArray.length - 1);
        amPmPicker.setDisplayedValues(amPmsArray);
        hourUpdated();
        updateTimeLabel();
    }

    private void updateTimeLabel() {
        String label = getPickerTime().toString();
        System.out.println("6330C: updateTimeLabel");
        timeText.setText(label.replaceAll(
                ".+(\\d\\d:\\d\\d):.+", "$1"));
    }

    private void amPmUpdated() {
        int amPm = amPmPicker.getValue();
        int hour = hourPicker.getValue();
        System.out.println("6330C: amPmUpdated " + amPm + " " + hour);
        hourPicker.setValue((hour + 12) % 24);
        updateTimeLabel();
    }

    private static int getDebugHour() {
        return 0;
    }

    private void hourUpdated() {
        updateTimeLabel();
        amPmPicker.setValue(hourPicker.getValue() < 12 ? 0 : 1);
    }

    private LocalDateTime getPickerTime() {
        return new LocalDateTime().withTime(
                hourPicker.getValue(), minutePicker.getValue(), 0, 0);
    }
}
