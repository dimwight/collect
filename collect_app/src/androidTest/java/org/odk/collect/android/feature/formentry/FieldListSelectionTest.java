package org.odk.collect.android.feature.formentry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.odk.collect.android.support.pages.FormEntryPage;
import org.odk.collect.android.support.rules.CollectTestRule;
import org.odk.collect.android.support.rules.TestRuleChain;

public class FieldListSelectionTest {

    public CollectTestRule rule = new CollectTestRule();

    @Rule
    public RuleChain copyFormChain = TestRuleChain.chain()
            .around(rule);
    @Test
    //https://github.com/getodk/collect/issues/4570
    public void showRepeatsPickerWhenFirstRepeatIsEmpty() {
        rule.startAtMainMenu()
                .copyForm("fieldListSelection.xml")
                .startBlankFormWithRepeatGroup("Empty First Repeat", "Repeat")
                .clickOnAdd(new FormEntryPage("Empty First Repeat"))
                .answerQuestion("Question in repeat", "Not empty!")
                .clickGoToArrow()
                .clickGoUpIcon()
                .clickGoUpIcon()
                .assertTexts("Repeat", "Repeatable Group");
    }
}
