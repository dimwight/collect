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

import java.util.Arrays;

/**
 * @author Grzegorz Orczykowski (gorczykowski@soldevelo.com)
 */
public abstract class CustomDatePickerDialog extends DialogFragment {
    protected final static String[] monthsArray = new String[]{
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            // getResources().getStringArray(R.array.islamic_months);
    };
    private static final int MIN_SUPPORTED_YEAR = 1900; //1900 in Gregorian calendar
    private static final int MAX_SUPPORTED_YEAR = 2100; //2100 in Gregorian calendar
    private NumberPicker dayPicker;
    private NumberPicker monthPicker;
    private NumberPicker yearPicker;

    private TextView gregorianDateText;

    private DateTimeViewModel viewModel;
    private DateChangeListener dateChangeListener;

    public interface DateChangeListener {
        void onDateChanged(LocalDateTime selectedDate);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //   System.out.println("6330C: onAttach");

        if (context instanceof DateChangeListener) {
            dateChangeListener = (DateChangeListener) context;
        }

        viewModel = new ViewModelProvider(this).get(DateTimeViewModel.class);
        if (viewModel.getLocalDateTime() == null) {
            viewModel.setLocalDateTime((LocalDateTime) getArguments().getSerializable(DateTimeWidgetUtils.DATE));
        }
        viewModel.setDatePickerDetails((DatePickerDetails) getArguments().getSerializable(DateTimeWidgetUtils.DATE_PICKER_DETAILS));

        viewModel.getSelectedDate().observe(this, localDateTime -> {
            if (localDateTime != null && dateChangeListener != null) {
                dateChangeListener.onDateChanged(localDateTime);
            }
        });
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //   System.out.println("6330C: onCreateDialog");
        return new MaterialAlertDialogBuilder(getActivity())
                .setTitle(org.odk.collect.strings.R.string.select_date)
                .setView(R.layout.custom_date_picker_dialog)
                .setPositiveButton(org.odk.collect.strings.R.string.ok, (dialog, id) -> {
                    LocalDateTime date = DateTimeUtils.getDateAsGregorian(getOriginalDate());
                    viewModel.setSelectedDate(date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth());
                    dismiss();
                })
                .setNegativeButton(org.odk.collect.strings.R.string.cancel, (dialog, id) -> dismiss())
                .create();
    }

    @Override
    public void onDestroyView() {
        //   System.out.println("6330C: onDestroyView");
        viewModel.setLocalDateTime(DateTimeUtils.getDateAsGregorian(getOriginalDate()));
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        System.out.println("6330C: onResume");
        super.onResume();
        gregorianDateText = getDialog().findViewById(R.id.date_gregorian);
        //setUpPickers();
        System.out.println("6330C: setUpPickers");
        dayPicker = getDialog().findViewById(R.id.day_picker);
        dayPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateGregorianDateLabel());
        monthPicker = getDialog().findViewById(R.id.month_picker);
        monthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> monthUpdated());
        yearPicker = getDialog().findViewById(R.id.year_picker);
        yearPicker.setOnValueChangedListener((picker, oldVal, newVal) -> yearUpdated());

        // hidePickersIfNeeded();
        System.out.println("6330C: hidePickersIfNeeded");
        if (viewModel.getDatePickerDetails().isMonthYearMode()) {
            dayPicker.setVisibility(View.GONE);
        } else if (viewModel.getDatePickerDetails().isYearMode()) {
            dayPicker.setVisibility(View.GONE);
            monthPicker.setVisibility(View.GONE);
        }
        boolean inSubClass = getClass() != ISODatePickerDialog.class;
        if (inSubClass) return;
        //    System.out.println("6330I: onResume");
        // setUpValues();
        System.out.println("6330I: setUpPickValues");
        //setUpDatePicker();
        System.out.println("6330I: setUpDatePicker");
        LocalDateTime ldt = DateTimeUtils
                .skipDaylightSavingGapIfExists(getDate())
                .toDateTime()
                .withChronology(ISOChronology.getInstance())
                .toLocalDateTime();
        setUpDayPicker(ldt.getDayOfMonth(), ldt.dayOfMonth().getMaximumValue());
        setUpMonthPicker(ldt.getMonthOfYear(), monthsArray);
        setUpYearPicker(ldt.getYear(), MIN_SUPPORTED_YEAR, MAX_SUPPORTED_YEAR);
        updateGregorianDateLabel();
    }

    protected void updateGregorianDateLabel() {
        System.out.println("6330C: updateGregorianDateLabel");
        String label = DateTimeWidgetUtils.getDateTimeLabel(DateTimeUtils.getDateAsGregorian(getOriginalDate()).toDate(),
                viewModel.getDatePickerDetails(), false, getContext());
        gregorianDateText.setText(label);
    }

    protected void setUpDayPicker(int dayOfMonth, int daysInMonth) {
        System.out.println("6330C: setUpDayPicker");
        setUpDayPicker(1, dayOfMonth, daysInMonth);
    }

    protected void setUpDayPicker(int minDay, int dayOfMonth, int daysInMonth) {
        System.out.println("6330C: setUpDayPicker");
        dayPicker.setMinValue(minDay);
        dayPicker.setMaxValue(daysInMonth);
        if (viewModel.getDatePickerDetails().isSpinnerMode()) {
            dayPicker.setValue(dayOfMonth);
        }
    }

    protected void setUpMonthPicker(int monthOfYear, String[] monthsArray) {
        // In Myanmar calendar we don't have specified amount of months, it's dynamic so clear
        // values first to avoid ArrayIndexOutOfBoundsException
        monthPicker.setDisplayedValues(null);
        monthPicker.setMaxValue(monthsArray.length - 1);
        monthPicker.setDisplayedValues(monthsArray);
        if (!viewModel.getDatePickerDetails().isYearMode()) {
            monthPicker.setValue(monthOfYear - 1);
        }
        System.out.println("6330C: setUpMonthPicker " + monthOfYear);
    }

    protected void setUpYearPicker(int year, int minSupportedYear, int maxSupportedYear) {
        System.out.println("6330C: setUpYearPicker");
        yearPicker.setMinValue(minSupportedYear);
        yearPicker.setMaxValue(maxSupportedYear);
        yearPicker.setValue(year);
    }

    protected void monthUpdated() {
        System.out.println("6330C: monthUpdated");
        updateDays();
        updateGregorianDateLabel();
    }

    protected void yearUpdated() {
        System.out.println("6330C: yearUpdated");
        updateDays();
        updateGregorianDateLabel();
    }

    public int getDay() {
        System.out.println("6330C: getPickerDay");
        return dayPicker.getValue();
    }

    public String getMonth() {
        int at = false ? monthPicker.getValue() : getMonthId();
        System.out.println("6330C: getPickerMonth " + at);
        return monthPicker.getDisplayedValues()[at];
    }

    public int getMonthId() {
        System.out.println("6330C: getPickerMonthAt");
        return monthPicker.getValue();
    }

    public int getYear() {
        System.out.println("6330C: getPickerYear");
        return yearPicker.getValue();
    }

    public LocalDateTime getDate() {
        System.out.println("6330C: getDate");
        LocalDateTime ldt = new LocalDateTime().withDate(2020, 5, 12);
        return getDay() == 0 ? false ? ldt :
                viewModel.getLocalDateTime()
                : getOriginalDate();
    }

    protected void updateDays() {
        System.out.println("6330I: updateDays");
        LocalDateTime ldt = getOriginalDate();
        setUpDayPicker(ldt.getDayOfMonth(), ldt.dayOfMonth().getMaximumValue());
    }

    protected LocalDateTime getOriginalDate() {
        System.out.println("6330I: getOriginalDate");
        System.out.println("6330I: getPickedDate_");
        int d = getDay();
        int m = false ? Arrays.asList(monthsArray).indexOf(getMonth())
                : getMonthId();
        int y = getYear();

        LocalDateTime ldt = new LocalDateTime(y, m + 1, 1, 0, 0, 0, 0, ISOChronology.getInstance());
        if (d > ldt.dayOfMonth().getMaximumValue()) {
            d = ldt.dayOfMonth().getMaximumValue();
        }
        if (d < ldt.dayOfMonth().getMinimumValue()) {
            d = ldt.dayOfMonth().getMinimumValue();
        }

        return new LocalDateTime(y, m + 1, d, 0, 0, 0, 0, ISOChronology.getInstance());
    }
}
