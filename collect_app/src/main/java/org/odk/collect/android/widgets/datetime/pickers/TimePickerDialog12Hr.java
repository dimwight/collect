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
import org.odk.collect.android.R;
import org.odk.collect.android.widgets.datetime.DatePickerDetails;
import org.odk.collect.android.widgets.datetime.DateTimeUtils;
import org.odk.collect.android.widgets.utilities.DateTimeWidgetUtils;
import org.odk.collect.android.widgets.viewmodels.DateTimeViewModel;

public abstract class TimePickerDialog12Hr extends DialogFragment {
    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private NumberPicker amPmPicker;

    private TextView gregorianTimeText;

    private DateTimeViewModel viewModel;
    private TimeChangeListener dateChangeListener;

    public interface TimeChangeListener {
        void onTimeChanged(LocalDateTime selectedTime);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        System.out.println("6330C: onAttach");

        if (context instanceof TimeChangeListener) {
            dateChangeListener = (TimeChangeListener) context;
        }

        viewModel = new ViewModelProvider(this).get(DateTimeViewModel.class);
        if (viewModel.getLocalDateTime() == null) {
            viewModel.setLocalDateTime((LocalDateTime) getArguments().getSerializable(DateTimeWidgetUtils.DATE));
        }
        viewModel.setDatePickerDetails((DatePickerDetails) getArguments().getSerializable(DateTimeWidgetUtils.DATE_PICKER_DETAILS));

        viewModel.getSelectedDate().observe(this, localDateTime -> {
            if (localDateTime != null && dateChangeListener != null) {
                dateChangeListener.onTimeChanged(localDateTime);
            }
        });
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
        setUpPickers();
    }

    private void setUpPickers() {
        System.out.println("6330C: setUpPickers");
        hourPicker = getDialog().findViewById(R.id.year_picker);
        hourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> hourUpdated());
        minutePicker = getDialog().findViewById(R.id.day_picker);
        minutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateGregorianTimeLabel());
        amPmPicker = getDialog().findViewById(R.id.month_picker);
        amPmPicker.setOnValueChangedListener((picker, oldVal, newVal) -> amPmUpdated());

        hidePickersIfNeeded();
    }

    private void hidePickersIfNeeded() {
        System.out.println("6330C: hidePickersIfNeeded");
        if (viewModel.getDatePickerDetails().isMonthYearMode()) {
            minutePicker.setVisibility(View.GONE);
        } else if (viewModel.getDatePickerDetails().isYearMode()) {
            minutePicker.setVisibility(View.GONE);
            amPmPicker.setVisibility(View.GONE);
        }
    }

    protected void updateGregorianTimeLabel() {
        System.out.println("6330C: updateGregorianTimeLabel");
        String label = DateTimeWidgetUtils.getDateTimeLabel(DateTimeUtils.getDateAsGregorian(getOriginalTime()).toDate(),
                viewModel.getDatePickerDetails(), false, getContext());
        gregorianTimeText.setText(label);
    }

    protected void setUpMinutePicker(int minuteOfHour, int minutesInHour) {
        System.out.println("6330C: setUpMinutePicker");
        setUpMinutePicker(1, minuteOfHour, minutesInHour);
    }

    protected void setUpMinutePicker(int minMinute, int minuteOfHour, int minutesInHour) {
        System.out.println("6330C: setUpMinutePicker");
        minutePicker.setMinValue(minMinute);
        minutePicker.setMaxValue(minutesInHour);
        if (viewModel.getDatePickerDetails().isSpinnerMode()) {
            minutePicker.setValue(minuteOfHour);
        }
    }

    protected void setUpAmPmPicker(int monthOfHour, String[] amPmsArray) {
        amPmPicker.setDisplayedValues(null);
        amPmPicker.setMaxValue(amPmsArray.length - 1);
        amPmPicker.setDisplayedValues(amPmsArray);
        if (!viewModel.getDatePickerDetails().isYearMode()) {
            amPmPicker.setValue(monthOfHour - 1);
        }
        System.out.println("6330C: setUpAmPmPicker " + monthOfHour);
    }

    protected void setUpHourPicker(int hour, int minSupportedHour, int maxSupportedHour) {
        System.out.println("6330C: setUpHourPicker");
        hourPicker.setMinValue(minSupportedHour);
        hourPicker.setMaxValue(maxSupportedHour);
        hourPicker.setValue(hour);
    }

    protected void amPmUpdated() {
        System.out.println("6330C: amPmUpdated");
        updateMinutes();
        updateGregorianTimeLabel();
    }

    protected void hourUpdated() {
        System.out.println("6330C: hourUpdated");
        updateMinutes();
        updateGregorianTimeLabel();
    }

    public int getMinute() {
        System.out.println("6330C: getPickerMinute");
        return minutePicker.getValue();
    }

    public String getAmPm() {
        int value = amPmPicker.getValue();
        System.out.println("6330C: getPickerAmPm " + value);
        return amPmPicker.getDisplayedValues()[value];
    }

    public int getAmPmId() {
        System.out.println("6330C: getAmPmId");
        return amPmPicker.getValue();
    }

    public int getHour() {
        System.out.println("6330C: getPickerHour");
        return hourPicker.getValue();
    }

    public LocalDateTime getTime() {
        System.out.println("6330C: getTime");
        LocalDateTime ldt = new LocalDateTime().withTime
                (19, 13, 0, 0);
        return getMinute() == 0 ? false ? ldt :
                viewModel.getLocalDateTime()
                : getOriginalTime();
    }

    protected abstract void updateMinutes();

    protected abstract LocalDateTime getOriginalTime();
}
