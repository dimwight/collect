package org.odk.collect.android.widgets.datetime.pickers;

import org.joda.time.LocalDateTime;

public class ISODatePickerDialog extends CustomDatePickerDialog {
    public ISODatePickerDialog() {
    }

    @Override
    protected void updateDays() {
        System.out.println("updateDays?");
    }

    @Override
    protected LocalDateTime getOriginalDate() {
        System.out.println("getOriginalDate?");
        return null;
    }
}
