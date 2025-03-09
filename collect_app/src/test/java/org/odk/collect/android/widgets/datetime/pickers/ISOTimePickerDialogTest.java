package org.odk.collect.android.widgets.datetime.pickers;

import android.content.DialogInterface;

import androidx.fragment.app.FragmentManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.fragments.support.DialogFragmentHelpers;
import org.odk.collect.android.support.CollectHelpers;
import org.odk.collect.android.widgets.datetime.DatePickerDetails;
import org.odk.collect.testshared.RobolectricHelpers;

@RunWith(AndroidJUnit4.class)
public class ISOTimePickerDialogTest {//ISODatePickerDialogTest
    private FragmentManager fragmentManager;
    private ISOTimePickerDialog dialogFragment;
    private DatePickerDetails datePickerDetails;
    private DialogFragmentHelpers.DatePickerTestActivity activity;

    @Before
    public void setup() {
        activity = CollectHelpers.createThemedActivity(DialogFragmentHelpers.DatePickerTestActivity.class);
        fragmentManager = activity.getSupportFragmentManager();

        dialogFragment = new ISOTimePickerDialog();
        datePickerDetails = DialogFragmentHelpers.setUpDatePickerDetails(DatePickerDetails.DatePickerType.ISO);
        dialogFragment.setArguments(DialogFragmentHelpers.getDialogFragmentArguments(datePickerDetails));
    }

    @Test
    public void dialogIsCancellable() {
        dialogFragment.show(fragmentManager, "TAG");
        RobolectricHelpers.runLooper();

        DialogFragmentHelpers.assertDialogIsCancellable(true);
    }

    @Test
    public void dialogShouldShowCorrectDate() {
        dialogFragment.show(fragmentManager, "TAG");
        RobolectricHelpers.runLooper();

        DialogFragmentHelpers.assertDialogShowsCorrectDate(2020, 4, 12, "May 12, 2020");
    }

    @Test
    public void settingDateInDatePicker_changesDateShownInTextView() {
        dialogFragment.show(fragmentManager, "TAG");
        RobolectricHelpers.runLooper();

        DialogFragmentHelpers.assertDialogTextViewUpdatesDate("May 12, 2020", 2020, 4, 12);
    }

    @Test
    public void whenScreenIsRotated_dialogShouldRetainDateInDatePickerAndTextView() {
        DialogFragmentHelpers.assertDialogRetainsDateOnScreenRotation(dialogFragment, "May 12, 2020", 2020, 4, 12);
    }

    @Test
    public void clickingOk_updatesDateInActivity() {
        dialogFragment.show(fragmentManager, "TAG");
        RobolectricHelpers.runLooper();

        DialogFragmentHelpers.assertDateUpdateInActivity(activity, 2020, 4, 12);
    }

    @Test
    public void clickingOk_dismissesTheDialog() {
        dialogFragment.show(fragmentManager, "TAG");
        RobolectricHelpers.runLooper();

        DialogFragmentHelpers.assertDialogIsDismissedOnButtonClick(DialogInterface.BUTTON_POSITIVE);
    }

    @Test
    public void clickingCancel_dismissesTheDialog() {
        dialogFragment.show(fragmentManager, "TAG");
        RobolectricHelpers.runLooper();

        DialogFragmentHelpers.assertDialogIsDismissedOnButtonClick(DialogInterface.BUTTON_NEGATIVE);
    }
}
