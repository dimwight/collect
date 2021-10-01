package org.odk.collect.android.feature.formentry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.odk.collect.android.javarosawrapper.FormController._UpdateStage;
import org.odk.collect.android.support.CollectTestRule;
import org.odk.collect.android.support.CopyFormRule;
import org.odk.collect.android.support.ResetStateRule;
import org.odk.collect.android.support.pages.FormEntryPage;
import org.odk.collect.android.support.pages.FormHierarchyPage;
import org.odk.collect.android.support.pages.MainMenuPage;

public class FieldListSelectionTest {

    public CollectTestRule rule = new CollectTestRule();

    @Rule
    public RuleChain copyFormChain = RuleChain
            .outerRule(new ResetStateRule())
            .around(new CopyFormRule("fieldListSelection.xml", null))
            .around(rule);

//    @Test
    public void questionSelectedInHierarchyIsScrolledToInFormEntry() {
        if (!_UpdateStage.STAGE_1.isApplied()) {
            return;
        }
        FormHierarchyPage hierarchy = openFormInHierarchy();
        String groupLabel = "Select one widgets";
        String questionLabel = "Select one widget";
        hierarchyToFormEntry(hierarchy, groupLabel, questionLabel);
    }

    //    @Test
    public void formEntryToHierarchyRetracesQuestionSelectionSteps() {
        if (!_UpdateStage.STAGE_2.isApplied()) {
            return;
        }
        FormHierarchyPage hierarchy = openFormInHierarchy();
        String groupLabel = "Select one widgets";
        String questionLabel = "Select one widget";
        FormEntryPage page = hierarchyToFormEntry(hierarchy, groupLabel, questionLabel);
        formEntryBackToHierarchy(page, questionLabel);
    }

    //    @Test
    public void scrollingInFormEntrySelectsQuestionInHierarchy() {
        if (!_UpdateStage.STAGE_3.isApplied()) {
            return;
        }
        FormHierarchyPage hierarchy = openFormInHierarchy();
        String groupLabel = "Select one widgets";
        String questionLabel = "Select one widget";
        FormEntryPage page = hierarchyToFormEntry(hierarchy, groupLabel, questionLabel);
        String scrolledQuestionLabel = "Grid select multiple widget";
        page.flingUpAndWait(1000);
        formEntryBackToHierarchy(page, scrolledQuestionLabel);
    }

    @Test
    public void interactionInFormEntrySelectsQuestionInHierarchy() {
        if (false &&
                !_UpdateStage.STAGE_4.isApplied()) {
            return;
        }
        FormHierarchyPage hierarchy = openFormInHierarchy();
        String groupLabel = "List group";
        String questionLabel0 = "List widget";
        String questionLabel1 = "Grid select multiple widget";
        FormEntryPage page = hierarchyToFormEntry(hierarchy, groupLabel, questionLabel0);
        page.scrollToAndClickText("A");
        formEntryBackToHierarchy(page, questionLabel1);
    }

    private FormHierarchyPage openFormInHierarchy() {
        return new MainMenuPage()
                .startBlankForm("fieldListSelection")
                .clickGoToArrow();
    }

    private FormEntryPage hierarchyToFormEntry(FormHierarchyPage hierarchy,
                                               String groupLabel,
                                               String questionLabel) {
        return hierarchy.clickOnGroup(groupLabel)
                .clickOnQuestion(questionLabel)
                .assertText(questionLabel);
    }

    private void formEntryBackToHierarchy(FormEntryPage page,
                                          String questionLabel) {
        page.clickGoToArrow()
                .assertText(questionLabel)
                .clickGoUpIcon()
                .assertText("wrapper > Text widgets"); //Bug or form design?
    }

}
