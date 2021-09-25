package org.odk.collect.android.feature.formentry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.odk.collect.android.javarosawrapper.FormController._UpdateStage;
import org.odk.collect.android.support.CollectTestRule;
import org.odk.collect.android.support.CopyFormRule;
import org.odk.collect.android.support.ResetStateRule;
import org.odk.collect.android.support.pages.MainMenuPage;

public class FieldListSelectionTest {

    public CollectTestRule rule = new CollectTestRule();

    @Rule
    public RuleChain copyFormChain = RuleChain
            .outerRule(new ResetStateRule())
            .around(new CopyFormRule("fieldListSelection.xml", null))
            .around(rule);

    @Test
    public void hierarchyToFormEntry() {
        if (!_UpdateStage.STAGE_1.isApplied()) {
            return;
        }
        new MainMenuPage()
                .startBlankForm("fieldListSelection")
                .clickGoToArrow()
                .clickOnGroup("Select one widgets")
                .clickOnQuestion("Select one widget");


    }

}
