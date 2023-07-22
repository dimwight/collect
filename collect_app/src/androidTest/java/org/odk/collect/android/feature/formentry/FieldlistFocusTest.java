package org.odk.collect.android.feature.formentry;
// collect_app/src/androidTest/java/org/odk/collect/android/feature/formentry/ActiveFormIndexTest.java

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
    private static final String WRAPPER_GT = "wrapper > ";

    public CollectTestRule rule = new CollectTestRule();

    @Rule
    public RuleChain copyFormChain = TestRuleChain.chain().around(rule);

    @Test
    public void questionSelectedInHierarchyIsScrolledToInFormEntry() {
        String groupS1 = "Select one widgets";
        String questionS1 = "Select one widget";
        rule.startAtMainMenu()
                .copyForm("active-form-index.xml")
                .startBlankForm("active-form-index")
                .assertTexts("Text widgets", "String widget")
                .clickForwardButton()
                .assertTexts("Text widgets", "String number widget")
                .clickGoToArrow()
                .assertTexts("Text widgets", "String widget")
                .clickGoUpIcon()
                .clickOnText("wrapper")
                .clickOnGroup(groupS1)
                .assertText(WRAPPER_GT + groupS1)
                .clickOnQuestion(questionS1)
                .assertText(questionS1);
        onView(allOf(withText("Time widget"), isDisplayed()))
                .check(doesNotExist());
    }

}
