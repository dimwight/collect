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
import org.odk.collect.android.support.CopyFormRule;
import org.odk.collect.android.support.pages.FormEntryPage;
import org.odk.collect.android.support.pages.FormHierarchyPage;
import org.odk.collect.android.support.pages.MainMenuPage;
import org.odk.collect.android.support.rules.CollectTestRule;

import java.util.Random;

public class FieldListSelectionTest {

    private static final String WRAPPER_GT = "wrapper > ";


    public CollectTestRule rule = new CollectTestRule();

    @Rule
    public RuleChain copyFormChain = RuleChain
            .outerRule(new org.odk.collect.android.support.rules.ResetStateRule())
            .around(new org.odk.collect.android.support.rules.CopyFormRule("fieldListSelection.xml", null))
            .around(rule);

    @Test
    public void questionSelectedInHierarchyIsScrolledToInFormEntry() {
        if (false
            //!_UpdateStage.STAGE_1.isApplied()
        ) {
            return;
        }
        boolean _stringWidget = false;
        FormHierarchyPage hierarchy = openFormInHierarchy();
        String groupLabel =
                _stringWidget ? "Text widgets" : "Select one widgets";
        String questionLabel =
                _stringWidget ? " String widget" : "Select one widget";
        hierarchyToFormEntry(hierarchy, groupLabel, questionLabel);
    }

    @Test
    public void formEntryToHierarchyRetracesQuestionSelectionSteps() {
        if (false
            //!_UpdateStage.STAGE_2.isApplied()
        ) {
            return;
        }
        FormHierarchyPage hierarchy = openFormInHierarchy();
        String groupLabel = "Select one widgets";
        String questionLabel = "Select one widget";
        FormEntryPage page = hierarchyToFormEntry(hierarchy, groupLabel, questionLabel);
        formEntryBackToHierarchy(page, groupLabel, questionLabel);
    }

    @Test
    public void scrollingInFormEntrySelectsQuestionInHierarchy() {
        if (false
            //    !_UpdateStage.STAGE_3.isApplied()
        ) {
            return;
        }
        FormHierarchyPage hierarchy = openFormInHierarchy();
        String groupLabel0 = "Select one widgets";
        String questionLabel0 = "Select one widget";
        FormEntryPage page = hierarchyToFormEntry(hierarchy, groupLabel0, questionLabel0);
        page.flingUpAndWait(1000);
        String groupLabel1 = "Select multi widgets";
        String questionLabel1 = "Grid select multiple widget";
        formEntryBackToHierarchy(page, groupLabel1, questionLabel1);
    }

    @Test
    public void interactionInFormEntrySelectsQuestionInHierarchy() {
        if (false
            //   !_UpdateStage.STAGE_4.isApplied()
        ) {
            return;
        }
        FormHierarchyPage hierarchy = openFormInHierarchy();
        String groupLabel0 = "Select multi widgets";
        String questionLabel0 = "Grid select multiple widget"; //SelectMultipleListAdapter
        FormEntryPage page = hierarchyToFormEntry(hierarchy, groupLabel0, questionLabel0);
        if (false) {
            onView(withIndex(withText("Select Answer"), 1)).perform(click());
            page.closeSelectMinimalDialog();
        }
        String groupLabel1 = "List group";
        boolean clickRadioButton = true ||
                new Random().nextDouble() > 0.5;
        String questionLabel1 = clickRadioButton ? "List widget" : "List multi widget";
        onView(withIndex(withClassName(endsWith(
                clickRadioButton ? "RadioButton" : "CheckBox")
        ), 0)).perform(click());
        formEntryBackToHierarchy(page, groupLabel1, questionLabel1);
    }

    private FormHierarchyPage openFormInHierarchy() {
        return new MainMenuPage()
                .startBlankForm("fieldListSelection")
                .clickGoToArrow()
                .assertText(WRAPPER_GT + "Text widgets");
    }

    private FormEntryPage hierarchyToFormEntry(FormHierarchyPage hierarchy,
                                               String groupLabel,
                                               String questionLabel) {
        return hierarchy.clickOnGroup(groupLabel)
                .assertText(WRAPPER_GT + groupLabel)
                .clickOnQuestion(questionLabel)
                .assertText(questionLabel);
    }

    private void formEntryBackToHierarchy(FormEntryPage page,
                                          String groupLabel,
                                          String questionLabel) {
        page.clickGoToArrow()
                .assertText(WRAPPER_GT + groupLabel)
                .assertText(questionLabel)
                .clickGoUpIcon()
                .assertText(WRAPPER_GT + "Text widgets"); //Bug or form design?
    }

}
