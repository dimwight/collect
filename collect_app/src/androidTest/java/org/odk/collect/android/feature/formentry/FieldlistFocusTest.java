package org.odk.collect.android.feature.formentry;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.odk.collect.android.support.rules.CollectTestRule;
import org.odk.collect.android.support.rules.TestRuleChain;

public class FieldlistFocusTest {

    public CollectTestRule rule = new CollectTestRule();

    @Rule
    public RuleChain copyFormChain = TestRuleChain.chain().around(rule);

    @Test
    public void questionSelectedInHierarchyIsScrolledToInFieldlist() {
        String groupS1 = "Select one widgets";
        String questionS1 = "Select one widget";
        rule.startAtMainMenu()
                .copyForm("fieldlist-focus.xml")
                .startBlankForm("fieldlist-focus")
                .assertTexts("Text widgets", "String widget")
                .clickForwardButton()
                .assertTexts("Text widgets", "String number widget")
                .clickGoToArrow()
                .assertTexts("Text widgets", "String widget")
                .clickGoUpIcon()
                .clickOnText("wrapper")
                .clickOnGroup(groupS1)
                .assertText("wrapper > " + groupS1)
                .clickOnQuestion(questionS1)
                .assertText(questionS1);
        onView(allOf(withText("Time widget"), isDisplayed()))
                .check(doesNotExist());
    }

}
