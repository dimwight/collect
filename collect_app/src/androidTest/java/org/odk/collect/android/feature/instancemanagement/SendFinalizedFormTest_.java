package org.odk.collect.android.feature.instancemanagement;

import android.Manifest;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.odk.collect.android.RecordedIntentsRule;
import org.odk.collect.android.support.CollectTestRule;
import org.odk.collect.android.support.StubOpenRosaServer_;
import org.odk.collect.android.support.TestDependencies;
import org.odk.collect.android.support.TestRuleChain;
import org.odk.collect.android.support.pages.MainMenuPage;

@RunWith(AndroidJUnit4.class)
public class SendFinalizedFormTest_ {

    private final TestDependencies testDependencies = new TestDependencies();
    private final CollectTestRule rule = new CollectTestRule();

    private final StubOpenRosaServer_ server = new StubOpenRosaServer_();

    @Rule
    public RuleChain chain = TestRuleChain.chain(testDependencies)
            .around(GrantPermissionRule.grant(Manifest.permission.GET_ACCOUNTS))
            .around(new RecordedIntentsRule())
            .around(rule);

    @Test
    public void canViewSentForms_() {
        server.setNoHttpPostResult(true);
        MainMenuPage mainMenuPage = rule.startAtMainMenu()
                .setServer(server.getURL())
                .copyForm("one-question.xml")
                .startBlankForm("One Question")
                .answerQuestion("what is your age", "123")
                .swipeToEndScreen()
                .clickSaveAndExit()

                .clickSendFinalizedForm(1)
                .clickOnForm("One Question")
                .clickSendSelected() //Set breakpoint here!
//               clickOnText("CANCEL")
                .pressBack(new MainMenuPage());

        server.setNoHttpPostResult(false);
        mainMenuPage.clickEditSavedForm(1)
                .clickOnForm("One Question")
                .assertText("123");
    }


}
