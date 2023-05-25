package org.odk.collect.android.formentry

import android.app.Activity
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.odk.collect.android.R
import org.odk.collect.android.formentry.saving.FormSaveViewModel
import org.odk.collect.shadows.ShadowAndroidXAlertDialog
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow.extract

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowAndroidXAlertDialog::class])
class QuitFormDialogTest {

    private val formSaveViewModel = mock(FormSaveViewModel::class.java)
    private val formEntryViewModel = mock(FormEntryViewModel::class.java)

    @Test
    fun isCancellable() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val dialog = showDialog(activity)

        assertThat(shadowOf(dialog).isCancelable, equalTo(true))
    }

    @Test
    fun clickingDiscardChanges_callsExitOnFormEntryViewModel() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val dialog = showDialog(activity)

        val shadowDialog = extract<ShadowAndroidXAlertDialog>(dialog)
        val view = shadowDialog.getView()
        view.findViewById<View>(R.id.discard_changes).performClick()

        verify(formEntryViewModel).exit()
    }

    private fun showDialog(activity: Activity) = QuitFormDialog.show(
        activity,
        formSaveViewModel,
        formEntryViewModel,
        null
    )
}
