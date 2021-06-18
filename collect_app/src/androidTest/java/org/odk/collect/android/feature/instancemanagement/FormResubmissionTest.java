package org.odk.collect.android.feature.instancemanagement;

import android.Manifest;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.odk.collect.android.RecordedIntentsRule;
import org.odk.collect.android.dao.CursorLoaderFactory;
import org.odk.collect.android.support.CollectTestRule;
import org.odk.collect.android.support.StubOpenRosaServer;
import org.odk.collect.android.support.TestDependencies;
import org.odk.collect.android.support.TestRuleChain;
import org.odk.collect.android.support.pages.MainMenuPage;

@RunWith(AndroidJUnit4.class)
public class FormResubmissionTest {

    public static final String _FORM_NAME = "One Question";
    public static final String _QUESTION = "what is your age";
    public static final String _FORM_XML = "one-question.xml";
    public static final String _ANSWER0 = "1";
    public static final String _ANSWER1 = "12345678";

    private final TestDependencies testDependencies = new TestDependencies();
    private final CollectTestRule rule = new CollectTestRule();

    private final StubOpenRosaServer server = testDependencies.server;

    @Rule
    public RuleChain chain = TestRuleChain.chain(testDependencies)
            .around(GrantPermissionRule.grant(Manifest.permission.GET_ACCOUNTS))
            .around(new RecordedIntentsRule())
            .around(rule);

    @Test
    public void serverRejectsResubmissionBefore() {
        server.setNoHttpPostResult(true);
        server.setRejectResubmission(true);
        MainMenuPage mainMenuPage = rule.startAtMainMenu()
                .setServer(server.getURL())
                .copyForm(_FORM_XML)
                .startBlankForm(_FORM_NAME)
                .answerQuestion(_QUESTION, _ANSWER0)
                .swipeToEndScreen()
                .clickSaveAndExit()
                .clickSendFinalizedForm(1)
                .clickOnForm(_FORM_NAME)
                .clickSendSelected()
//                .clickOnText("CANCEL")
                .pressBack(new MainMenuPage());
        server.setNoHttpPostResult(false);
        mainMenuPage
                .clickEditSavedForm(1)
                .clickOnForm(_FORM_NAME)
                .clickOnQuestion(_QUESTION)
                .swipeToEndScreen()
                .clickSaveAndExit()
                .clickSendFinalizedForm(1)
                .clickOnForm(_FORM_NAME)
                .clickSendSelected()
//                .assertText("Error")
                .pressBack(new MainMenuPage());

    }


    @Test
    public void serverRejectsResubmissionAfter() {
        CursorLoaderFactory.AS_UPDATED = true;
        server.setNoHttpPostResult(true);
        MainMenuPage mainMenuPage = rule.startAtMainMenu()
                .setServer(server.getURL())
                .copyForm(_FORM_XML)
                .startBlankForm(_FORM_NAME)
                .answerQuestion(_QUESTION, _ANSWER0)
                .swipeToEndScreen()
                .clickSaveAndExit()
                .clickEditSavedForm(1)
                .assertText(_FORM_NAME)
                .pressBack(new MainMenuPage())
                .clickSendFinalizedForm(1)
                .clickOnForm(_FORM_NAME)
                .clickSendSelected()
//                .clickOnText("CANCEL")
                .pressBack(new MainMenuPage());
        server.setNoHttpPostResult(false);
        mainMenuPage
                .clickEditSavedForm(1)
                .assertTextDoesNotExist(_FORM_NAME)
                .pressBack(new MainMenuPage())
                .clickSendFinalizedForm(1)
                .clickOnForm(_FORM_NAME)
                .clickSendSelected()
                .pressBack(new MainMenuPage());

    }

}
