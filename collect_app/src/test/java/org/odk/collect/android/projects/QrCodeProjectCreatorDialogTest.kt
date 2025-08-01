package org.odk.collect.android.projects

import android.Manifest
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import com.google.android.material.appbar.MaterialToolbar
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.mock
import org.odk.collect.android.R
import org.odk.collect.android.fakes.FakePermissionsProvider
import org.odk.collect.android.injection.config.AppDependencyModule
import org.odk.collect.android.mainmenu.MainMenuActivity
import org.odk.collect.android.support.CollectHelpers
import org.odk.collect.async.Scheduler
import org.odk.collect.fragmentstest.FragmentScenarioLauncherRule
import org.odk.collect.permissions.PermissionsChecker
import org.odk.collect.permissions.PermissionsProvider
import org.odk.collect.projects.ProjectCreator
import org.odk.collect.qrcode.BarcodeScannerViewContainer
import org.odk.collect.testshared.FakeBarcodeScannerViewFactory
import org.odk.collect.testshared.FakeScheduler
import org.robolectric.shadows.ShadowToast

@RunWith(AndroidJUnit4::class)
class QrCodeProjectCreatorDialogTest {

    private val permissionsProvider = FakePermissionsProvider()
    private val barcodeScannerViewFactory = FakeBarcodeScannerViewFactory()
    private val scheduler = FakeScheduler()

    @get:Rule
    val launcherRule = FragmentScenarioLauncherRule()

    @Before
    fun setup() {
        permissionsProvider.setPermissionGranted(true)

        CollectHelpers.overrideAppDependencyModule(object : AppDependencyModule() {
            override fun providesBarcodeScannerViewFactory(): BarcodeScannerViewContainer.Factory {
                return barcodeScannerViewFactory
            }

            override fun providesPermissionsProvider(permissionsChecker: PermissionsChecker?): PermissionsProvider {
                return permissionsProvider
            }

            override fun providesScheduler(workManager: WorkManager?): Scheduler {
                return scheduler
            }
        })
    }

    @Test
    fun `If camera permission is not granted the dialog should not be dismissed`() {
        permissionsProvider.setPermissionGranted(false)
        val scenario = launcherRule.launch(QrCodeProjectCreatorDialog::class.java)
        scenario.onFragment {
            assertThat(it.isVisible, `is`(true))
        }
    }

    // https://github.com/getodk/collect/issues/5266
    @Test
    fun `requestCameraPermission() should be called in onStart() to make sure it is called after returning to the dialog`() {
        val scenario = launcherRule.launch(
            fragmentClass = QrCodeProjectCreatorDialog::class.java,
            initialState = Lifecycle.State.CREATED
        )

        assertThat(permissionsProvider.requestedPermissions, equalTo(emptyList()))

        scenario.moveToState(Lifecycle.State.STARTED)

        assertThat(
            permissionsProvider.requestedPermissions,
            equalTo(listOf(Manifest.permission.CAMERA))
        )
    }

    @Test
    fun `The dialog should be dismissed after clicking on the 'Cancel' button`() {
        val scenario = launcherRule.launch(QrCodeProjectCreatorDialog::class.java)
        scenario.onFragment {
            assertThat(it.isVisible, `is`(true))
            onView(withText(org.odk.collect.strings.R.string.cancel)).inRoot(isDialog())
                .perform(click())
            assertThat(it.isVisible, `is`(false))
        }
    }

    @Test
    fun `The dialog should be dismissed after clicking on a device back button`() {
        val scenario = launcherRule.launch(QrCodeProjectCreatorDialog::class.java)
        scenario.onFragment {
            assertThat(it.isVisible, `is`(true))
            onView(isRoot()).perform(pressBack())
            assertThat(it.isVisible, `is`(false))
        }
    }

    @Test
    fun `The dialog should have the option to import settings from file`() {
        val scenario = launcherRule.launch(QrCodeProjectCreatorDialog::class.java)
        scenario.onFragment { fragment ->
            val toolbar = fragment.requireView()
                .findViewById<MaterialToolbar>(org.odk.collect.androidshared.R.id.toolbar)
            val importMenuItem = toolbar.menu.findItem(R.id.menu_item_scan_sd_card)

            assertThat(importMenuItem, `is`(notNullValue()))
        }
    }

    @Test
    fun `The ManualProjectCreatorDialog should be displayed after switching to the manual mode`() {
        val scenario = launcherRule.launch(QrCodeProjectCreatorDialog::class.java)
        scenario.onFragment {
            onView(withText(org.odk.collect.strings.R.string.configure_manually)).inRoot(isDialog())
                .perform(scrollTo(), click())
            assertThat(
                it.activity!!.supportFragmentManager.findFragmentByTag(
                    ManualProjectCreatorDialog::class.java.name
                ),
                `is`(notNullValue())
            )
        }
    }

    @Test
    fun `Successful project creation goes to main menu`() {
        Intents.init()
        val scenario = launcherRule.launch(QrCodeProjectCreatorDialog::class.java)

        barcodeScannerViewFactory.scan(
            "{\n" +
                "  \"general\": {\n" +
                "  },\n" +
                "  \"admin\": {\n" +
                "  }\n" +
                "}"
        )

        scenario.onFragment {
            Intents.intended(IntentMatchers.hasComponent(MainMenuActivity::class.java.name))
            Intents.release()
        }
    }

    @Test
    fun `When QR code is invalid a toast should be displayed and scanning continues`() {
        val projectCreator = mock<ProjectCreator>()
        launcherRule.launch(QrCodeProjectCreatorDialog::class.java)

        barcodeScannerViewFactory.scan("{*}")
        assertThat(
            ShadowToast.getTextOfLatestToast(),
            `is`(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(org.odk.collect.strings.R.string.invalid_qrcode)
            )
        )
        verifyNoInteractions(projectCreator)

        scheduler.runForeground()
        assertThat(barcodeScannerViewFactory.isScanning, equalTo(true))
    }

    @Test
    fun `When QR code contains GD protocol a toast should be displayed and scanning continues`() {
        val projectCreator = mock<ProjectCreator>()
        launcherRule.launch(QrCodeProjectCreatorDialog::class.java)

        barcodeScannerViewFactory.scan(
            "{\n" +
                "  \"general\": {\n" +
                "       \"protocol\" : \"google_sheets\"" +
                "  },\n" +
                "  \"admin\": {\n" +
                "  }\n" +
                "}"
        )
        assertThat(
            ShadowToast.getTextOfLatestToast(),
            `is`(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(org.odk.collect.strings.R.string.settings_with_gd_protocol)
            )
        )
        verifyNoInteractions(projectCreator)

        scheduler.runForeground()
        assertThat(barcodeScannerViewFactory.isScanning, equalTo(true))
    }
}
