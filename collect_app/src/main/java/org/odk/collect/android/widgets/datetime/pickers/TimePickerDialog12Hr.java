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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.joda.time.LocalDateTime;
import org.joda.time.chrono.ISOChronology;
import org.odk.collect.android.R;
import org.odk.collect.android.widgets.datetime.DatePickerDetails;
import org.odk.collect.android.widgets.datetime.DateTimeUtils;
import org.odk.collect.android.widgets.utilities.DateTimeWidgetUtils;
import org.odk.collect.android.widgets.viewmodels.DateTimeViewModel;

public final class TimePickerDialog12Hr extends DialogFragment {
    private static final int MIN_SUPPORTED_HOUR = 1;
    private static final int MAX_SUPPORTED_HOUR = 12;
    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private NumberPicker amPmPicker;

    private TextView gregorianTimeText;

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
            viewModel.setLocalDateTime((LocalDateTime) getArguments().getSerializable(DateTimeWidgetUtils.DATE));
        }
        viewModel.setDatePickerDetails((DatePickerDetails) getArguments().getSerializable(DateTimeWidgetUtils.DATE_PICKER_DETAILS));

        viewModel.getSelectedDate().observe(this, localDateTime -> {
            if (localDateTime != null && timeChangeListener != null) {
                timeChangeListener.onTimeChanged(localDateTime);
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
                    LocalDateTime date = DateTimeUtils.getDateAsGregorian(getOriginalTime());
                    viewModel.setSelectedDate(date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth());
                    dismiss();
                })
                .setNegativeButton(org.odk.collect.strings.R.string.cancel, (dialog, id) -> dismiss())
                .create();
    }

    @Override
    public void onDestroyView() {
        System.out.println("6330C: onDestroyView");
        viewModel.setLocalDateTime(DateTimeUtils.getDateAsGregorian(getOriginalTime()));
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        System.out.println("6330C: onResume");
        super.onResume();
        gregorianTimeText = getDialog().findViewById(R.id.date_gregorian);
        //setUpPickers();
        System.out.println("6330C: setUpPickers");
        hourPicker = getDialog().findViewById(R.id.hour_picker);
        hourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> hourUpdated());
        minutePicker = getDialog().findViewById(R.id.minute_picker);
        minutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateGregorianDateTimeLabel());
        amPmPicker = getDialog().findViewById(R.id.am_pm_picker);
        amPmPicker.setOnValueChangedListener((picker, oldVal, newVal) -> amPmUpdated());

        //hidePickersIfNeeded();
        System.out.println("6330C: hidePickersIfNeeded");
        if (viewModel.getDatePickerDetails().isMonthYearMode()) {
            minutePicker.setVisibility(View.GONE);
        } else if (viewModel.getDatePickerDetails().isYearMode()) {
            minutePicker.setVisibility(View.GONE);
            amPmPicker.setVisibility(View.GONE);
        }
        // setUpValues();
        System.out.println("6330I: setUpPickValues");
        //setUpDatePicker();
        System.out.println("6330I: setUpDatePicker");
        LocalDateTime ldt = DateTimeUtils.getCurrentDateTime()
                //.skipDaylightSavingGapIfExists(getDate())
                .toDateTime()
                .withChronology(ISOChronology.getInstance())
                .toLocalDateTime();
        setUpHourPicker(ldt.getHourOfDay());
        setUpMinutePicker(ldt.getMinuteOfHour());
        setUpAmPmPicker();
        updateGregorianDateTimeLabel();
    }

    protected void updateGregorianDateTimeLabel() {
        System.out.println("6330C: updateGregorianTimeLabel");
        String label = DateTimeWidgetUtils.getDateTimeLabel(DateTimeUtils.getDateAsGregorian(getOriginalTime()).toDate(),
                viewModel.getDatePickerDetails(), false, getContext());
        gregorianTimeText.setText(label);
    }

    protected void setUpMinutePicker(int minuteOfHour) {
        //Day
        System.out.println("6330C: setUpMinutePicker");
        //Day
        System.out.println("6330C: setUpMinutePicker");
        minutePicker.setMinValue(1);
        minutePicker.setMaxValue(60);
        if (viewModel.getDatePickerDetails().isSpinnerMode()) {
            minutePicker.setValue(minuteOfHour);
        }
    }

    protected void setUpAmPmPicker() {
        //Month
        // In Myanmar calendar we don't have specified amount of months, it's dynamic so clear
        // values first to avoid ArrayIndexOutOfBoundsException
        amPmPicker.setDisplayedValues(null);
        amPmPicker.setMaxValue(amPmsArray.length - 1);
        amPmPicker.setDisplayedValues(amPmsArray);
        if (!viewModel.getDatePickerDetails().isYearMode()) {
            amPmPicker.setValue(0);
        }
        System.out.println("6330C: setUpAmPmPicker ");
    }

    protected void setUpHourPicker(int hour) {
        //Year
        System.out.println("6330C: setUpHourPicker");
        hourPicker.setMinValue(MIN_SUPPORTED_HOUR);
        hourPicker.setMaxValue(MAX_SUPPORTED_HOUR);
        hourPicker.setValue(hour);
    }

    protected void amPmUpdated() {//Month
        System.out.println("6330C: amPmUpdated");
//        updateMinutes();
        updateGregorianDateTimeLabel();
    }

    protected void hourUpdated() {//year
        System.out.println("6330C: hourUpdated");
//        updateMinutes();
        updateGregorianDateTimeLabel();
    }

    public int getMinute() {//Day
        System.out.println("6330C: getPickerMinute");
        return minutePicker.getValue();
    }

    public String getAmPm() {//Month
        int value = getAmPmId();
        System.out.println("6330C: getPickerAmPm " + value);
        return amPmPicker.getDisplayedValues()[value];
    }

    public int getAmPmId() {//Month
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
                : getOriginalTime();
    }

    protected void updateMinutes() {
    }//Days

    protected LocalDateTime getOriginalTime() {//Date
        System.out.println("6330I: getOriginalTime");
        LocalDateTime ldt = new LocalDateTime();
        return ldt;
    }
}
