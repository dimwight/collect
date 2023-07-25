package org.odk.collect.android.feature.formentry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.odk.collect.android.support.rules.CollectTestRule;
import org.odk.collect.android.support.rules.TestRuleChain;

public class For5547 {

    public CollectTestRule rule = new CollectTestRule();

    @Rule
    public RuleChain copyFormChain = TestRuleChain.chain().around(rule);

    @Test
    public void questionSelectedInHierarchyIsScrolledToInFieldlist() {
        rule.startAtMainMenu()
                .openProjectSettingsDialog()
                .clickSettings()
                .clickUserAndDeviceIdentity()
                .clickFormMetadata()
                .clickPhoneNumber()
                .inputText("1234")
                .clickOKOnDialog()
                .clickPhoneNumber()
                .pressBack(rule.startAtMainMenu());
    }

}
