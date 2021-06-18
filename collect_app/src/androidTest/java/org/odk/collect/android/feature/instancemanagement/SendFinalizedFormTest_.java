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
import org.odk.collect.android.support.StubOpenRosaServer;
import org.odk.collect.android.support.StubOpenRosaServer_;
import org.odk.collect.android.support.TestDependencies;
import org.odk.collect.android.support.TestRuleChain;
import org.odk.collect.android.support.pages.MainMenuPage;

@RunWith(AndroidJUnit4.class)
public class SendFinalizedFormTest_ {

    public static final String _FORM_NAME = "One Question";
    public static final String _QUESTION = "what is your age";
    public static final String _FORM_XML = "one-question.xml";
    public static final String _ANSWER = "123";

    private final TestDependencies testDependencies = new TestDependencies();
    private final CollectTestRule rule = new CollectTestRule();

    private final StubOpenRosaServer server =
            true?testDependencies.server: new StubOpenRosaServer_();

    @Rule
    public RuleChain chain = TestRuleChain.chain(testDependencies)
            .around(GrantPermissionRule.grant(Manifest.permission.GET_ACCOUNTS))
            .around(new RecordedIntentsRule())
            .around(rule);

    @Test
    public void canViewSentForms_() {
        if (server instanceof StubOpenRosaServer_) {
            ((StubOpenRosaServer_) server).setNoHttpPostResult(true);
        }
        MainMenuPage mainMenuPage = rule.startAtMainMenu()
                .setServer(server.getURL())
                .copyForm(_FORM_XML)
                .startBlankForm(_FORM_NAME)
                .answerQuestion(_QUESTION, _ANSWER)
                .swipeToEndScreen()
                .clickSaveAndExit()

                .clickSendFinalizedForm(1)
                .clickOnForm(_FORM_NAME)
                .clickSendSelected() //Set breakpoint here!
//               clickOnText("CANCEL")
                .pressBack(new MainMenuPage());

        if (server instanceof StubOpenRosaServer_) {
            ((StubOpenRosaServer_) server).setNoHttpPostResult(false);
        }
        mainMenuPage.clickSendFinalizedForm(1)
                .clickOnForm(_FORM_NAME)
                .clickSendSelected();
    }


}
