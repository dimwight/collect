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
import org.odk.collect.android.support.pages.SendFinalizedFormPage;

@RunWith(AndroidJUnit4.class)
public class FormResubmissionTest {

    public static final String _FORM_NAME = "One Question";
    public static final String _FORM_XML = "one-question.xml";
    public static final String _QUESTION = "what is your age";
    public static final String _ANSWER = "123";
    public static final String _CANCEL = "CArrrrNCEL";

    private final TestDependencies testDependencies = new TestDependencies();
    private final CollectTestRule rule = new CollectTestRule();

    private final StubOpenRosaServer server = testDependencies.server;

    @Rule
    public RuleChain chain = TestRuleChain.chain(testDependencies)
            .around(GrantPermissionRule.grant(Manifest.permission.GET_ACCOUNTS))
            .around(new RecordedIntentsRule())
            .around(rule);

    private MainMenuPage createAndSubmitFormWithFailure() {
        return rule.startAtMainMenu()
                .setServer(server.getURL())
                .copyForm(_FORM_XML)
                .startBlankForm(_FORM_NAME)
                .answerQuestion(_QUESTION, _ANSWER)
                .swipeToEndScreen()
                .clickSaveAndExit()
                .clickSendFinalizedForm(1)
                .clickOnForm(_FORM_NAME)
                .clickSendSelected()
                .clickOnText("CANCEL")
                .pressBack(new MainMenuPage())
                .clickViewSentForm(1)
                .assertTextDoesNotExist(_FORM_NAME)
                .pressBack(new MainMenuPage());
    }

    @Test
    public void whenFailedFormCanBeEdited_ServerRejectsResubmission() {
        server.setNoHttpPostResult(true);
        server.setRejectResubmission(true);
        MainMenuPage mainMenuPage = createAndSubmitFormWithFailure()
//              ;
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
                .pressBack(new MainMenuPage())
                .clickViewSentForm(1)
                .assertTextDoesNotExist(_FORM_NAME);

    }

    @Test
    public void whenFailedFormCannotBeEdited_ServerAcceptsResubmission() {
        CursorLoaderFactory.afterUpdate = true;
        server.setNoHttpPostResult(true);
        MainMenuPage mainMenuPage = createAndSubmitFormWithFailure()
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
                .clickOK(new SendFinalizedFormPage())
                .pressBack(new MainMenuPage())
                .clickViewSentForm(1)
                .assertText(_FORM_NAME)
        ;

    }

}
