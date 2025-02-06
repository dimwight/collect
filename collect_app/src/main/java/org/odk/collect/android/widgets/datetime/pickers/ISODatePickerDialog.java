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

import org.joda.time.LocalDateTime;
import org.joda.time.chrono.ISOChronology;
import org.odk.collect.android.widgets.datetime.DateTimeUtils;

import java.util.Arrays;

public class ISODatePickerDialog extends CustomDatePickerDialog {

    private static final int MIN_SUPPORTED_YEAR = 1900; //1900 in Gregorian calendar
    private static final int MAX_SUPPORTED_YEAR = 2100; //2100 in Gregorian calendar

    private String[] monthsArray;

    @Override
    public void onResume() {
        System.out.println("6330I: onResume");
        super.onResume();
        monthsArray = new String[]{
                "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        };
        // getResources().getStringArray(R.array.islamic_months);
        setUpValues();
    }

    @Override
    protected void updateDays() {
        System.out.println("6330I: updateDays");
        LocalDateTime localDateTime = getCurrentIsoDate();
        setUpDayPicker(localDateTime.getDayOfMonth(), localDateTime.dayOfMonth().getMaximumValue());
    }

    @Override
    protected LocalDateTime getOriginalDate() {
        System.out.println("6330I: getOriginalDate");
        return getCurrentIsoDate();
    }

    private void setUpDatePicker() {
        System.out.println("6330I: setUpDatePicker");
        LocalDateTime isoDate = DateTimeUtils
                .skipDaylightSavingGapIfExists(getDate())
                .toDateTime()
                .withChronology(ISOChronology.getInstance())
                .toLocalDateTime();
        setUpDayPicker(isoDate.getDayOfMonth(), isoDate.dayOfMonth().getMaximumValue());
        setUpMonthPicker(isoDate.getMonthOfYear(), monthsArray);
        setUpYearPicker(isoDate.getYear(), MIN_SUPPORTED_YEAR, MAX_SUPPORTED_YEAR);
    }

    private void setUpValues() {
        System.out.println("6330I: setUpPickValues");
        setUpDatePicker();
        updateGregorianDateLabel();
    }

    private LocalDateTime getCurrentIsoDate() {
        System.out.println("6330I: getPickedIslamicDate");
        int isoDay = getDay();
        int isoMonth = Arrays.asList(monthsArray).indexOf(getMonth());
        int isoYear = getYear();

        LocalDateTime isoDate = new LocalDateTime(isoYear, isoMonth + 1, 1, 0, 0, 0, 0, ISOChronology.getInstance());
        if (isoDay > isoDate.dayOfMonth().getMaximumValue()) {
            isoDay = isoDate.dayOfMonth().getMaximumValue();
        }
        if (isoDay < isoDate.dayOfMonth().getMinimumValue()) {
            isoDay = isoDate.dayOfMonth().getMinimumValue();
        }

        return new LocalDateTime(isoYear, isoMonth + 1, isoDay, 0, 0, 0, 0, ISOChronology.getInstance());
    }
}
