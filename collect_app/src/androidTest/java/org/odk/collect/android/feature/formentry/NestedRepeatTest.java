package org.odk.collect.android.feature.formentry;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.odk.collect.android.support.pages.FormEntryPage;
import org.odk.collect.android.support.rules.CollectTestRule;
import org.odk.collect.android.support.rules.TestRuleChain;

@RunWith(AndroidJUnit4.class)
public class NestedRepeatTest {

    private final CollectTestRule rule = new CollectTestRule();

    @Rule
    public RuleChain copyFormChain = TestRuleChain.chain()
            .around(rule);

    @Test
    public void nestedRepeatsCreatedWithOuterRepeat() {
        rule.startAtMainMenu()
                .copyForm("NestedRepeats.xml")
                .startBlankForm("NestedRepeats")
                .assertText("Person > 1")
                .clickPlus("Person")
                .clickOnAdd(new FormEntryPage(""))
                .assertText("Person > 2")
                .swipeToNextQuestion("name?")//Already there?
                .assertText("Person > 2 > Child > 1")
                .swipeToNextQuestion("colour?")//Already there?
                .assertText("Person > 2 > Child > 1 > Pet > 1");

    }

}
