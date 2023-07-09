package org.odk.collect.android.feature.formentry;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.endsWith;
import static org.odk.collect.android.support.matchers.CustomMatchers.withIndex;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.odk.collect.android.support.pages.FormEntryPage;
import org.odk.collect.android.support.pages.FormHierarchyPage;
import org.odk.collect.android.support.rules.CollectTestRule;
import org.odk.collect.android.support.rules.TestRuleChain;

import java.util.Random;

public class FieldListSelectionTest {

    private static final String WRAPPER_GT = "wrapper > ";

    public CollectTestRule rule = new CollectTestRule();

    @Rule
    public RuleChain copyFormChain = TestRuleChain.chain().around(rule);

    private FormHierarchyPage openFormInHierarchy() {
        return rule.startAtMainMenu()
                .copyForm("fieldListSelection.xml")
                .startBlankForm("fieldListSelection")
                .clickGoToArrow()
                .assertText(WRAPPER_GT + "Text widgets");
    }

    private FormEntryPage hierarchyToFormEntry(FormHierarchyPage hierarchy,
                                               String group,
                                               String question,
                                               String assertHidden) {
        FormEntryPage page = hierarchy.clickOnGroup(group)
//                .assertText(WRAPPER_GT + group)
                .clickOnQuestion(question)
//                .assertText(question)
                ;
        if (!assertHidden.isEmpty()) {
            page.assertTextDoesNotExist(assertHidden);
        }
        return page;
    }

    @Test
    public void questionSelectedInHierarchyIsScrolledToInFormEntry() {
        if (false
            //!_UpdateStage.STAGE_1.isApplied()
        ) {
            return;
        }
        FormHierarchyPage hierarchy = openFormInHierarchy();
        hierarchyToFormEntry(hierarchy,
                "Select one widgets",
                "Select one widget",
                "Time widget");
    }

    private void formEntryBackToHierarchy(FormEntryPage page,
                                          String group,
                                          String question) {
        page.clickGoToArrow()
                .assertText(WRAPPER_GT + group)
                .assertText(question)
                .clickGoUpIcon()
                .assertText(WRAPPER_GT + "Text widgets"); //Bug or form design?
    }

    @Test
    public void formEntryToHierarchyRetracesQuestionSelectionSteps() {
        if (false
            //!_UpdateStage.STAGE_2.isApplied()
        ) {
            return;
        }
        FormHierarchyPage hierarchy = openFormInHierarchy();
        String question = "Select one widget";
        FormEntryPage page = hierarchy.clickOnGroup("Select one widgets")
//                .assertText(WRAPPER_GT + group)
                .clickOnQuestion(question)
//                .assertText(question)
                ;
        formEntryBackToHierarchy(page, "Select one widgets", question);
    }

    @Test
    public void scrollingInFormEntrySelectsQuestionInHierarchy() {
        if (false
            //    !_UpdateStage.STAGE_3.isApplied()
        ) {
            return;
        }
        FormHierarchyPage hierarchy = openFormInHierarchy();
        String group0 = "Select one widgets";
        String question0 = "Select one widget";
        FormEntryPage page = hierarchyToFormEntry(hierarchy,
                group0, question0, "");
        page.flingUpAndWait(1000);
        String group1 = "Select multi widgets";
        String question1 = "Grid select multiple widget";
        formEntryBackToHierarchy(page, group1, question1);
    }

    @Test
    public void interactionInFormEntrySelectsQuestionInHierarchy() {
        if (false
            //   !_UpdateStage.STAGE_4.isApplied()
        ) {
            return;
        }
        FormHierarchyPage hierarchy = openFormInHierarchy();
        String group0 = "Select multi widgets";
        String question0 = "Grid select multiple widget"; //SelectMultipleListAdapter
        FormEntryPage page = hierarchyToFormEntry(hierarchy, group0,
                question0, "");
        if (false) {
            onView(withIndex(withText("Select Answer"), 1)).perform(click());
            page.closeSelectMinimalDialog();
        }
        String group1 = "List group";
        boolean clickRadioButton = true ||
                new Random().nextDouble() > 0.5;
        String question1 = clickRadioButton ? "List widget" : "List multi widget";
        onView(withIndex(withClassName(endsWith(
                clickRadioButton ? "RadioButton" : "CheckBox")
        ), 0)).perform(click());
        formEntryBackToHierarchy(page, group1, question1);
    }

}
