package org.odk.collect.android.feature.formentry;
// collect_app/src/androidTest/java/org/odk/collect/android/feature/formentry/FieldListSelectionTest.java

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.odk.collect.android.activities.FormHierarchyActivity.Stages3027.STAGE_1;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.odk.collect.android.support.pages.FormHierarchyPage;
import org.odk.collect.android.support.rules.CollectTestRule;
import org.odk.collect.android.support.rules.TestRuleChain;

public class FieldListSelectionTest {
    private static final String WRAPPER_GT = "wrapper > ";

    public CollectTestRule rule = new CollectTestRule();

    @Rule
    public RuleChain copyFormChain = TestRuleChain.chain().around(rule);

    @Test
    public void questionSelectedInHierarchyIsScrolledToInFormEntry() {
        if (false &&
                !STAGE_1.isLive()) {
            return;
        }
        FormHierarchyPage hierarchy = rule.startAtMainMenu()
                .copyForm("fieldListSelection.xml")
                .startBlankForm("fieldListSelection")
                .clickGoToArrow()
                .assertText(WRAPPER_GT + "Text widgets");
        String group = "Select one widgets";
        String question = "Select one widget";
        hierarchy.clickOnGroup(group)
                .assertText(WRAPPER_GT + group)
                .clickOnQuestion(question)
                .assertText(question);
        onView(allOf(withText("String widget"), isDisplayed()))
                .check(doesNotExist());
    }

}
