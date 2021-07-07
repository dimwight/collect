package org.odk.collect.android.feature.formentry;

import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.odk.collect.android.support.CollectTestRule;
import org.odk.collect.android.support.CopyFormRule;
import org.odk.collect.android.support.ResetStateRule;
import org.odk.collect.android.support.pages.FormEntryPage;
import org.odk.collect.android.support.pages.FormHierarchyPage;
import org.odk.collect.android.support.pages.MainMenuPage;
import org.odk.collect.android.utilities.SelectOneWidgetUtils.UpdateStage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Appearance.Autocomplete;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Appearance.Minimal;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Appearance.MinimalAutocomplete;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Appearance.Plain;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.ABC1e;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.ABC2e;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.ABC3e;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.BC1h;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.BC2h;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.BC3h;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.DE1;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.DE2;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.DE3;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Block.A;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Block.B;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Block.C;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Block.D;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Block.E;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.ItemsetType.FastExternal;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.ItemsetType.Internal;
import static org.odk.collect.android.utilities.SelectOneWidgetUtils.UpdateStage.STAGE_0;

public class SelectOneResetTest {

    enum ItemsetType {
        Internal,
        FastExternal
    }

    enum Appearance {
        Plain,
        Autocomplete,
        Minimal,
        MinimalAutocomplete;

        public boolean isMinimal() {
            return this == Minimal || this == MinimalAutocomplete;
        }
    }

    enum Assert {BC1h, ABC1e, BC2h, ABC2e, BC3h, ABC3e, DE1, DE2, DE3}

    enum SectionVariant {
        Internal_Plain(Internal, Plain),
        Internal_Minimal(Internal, Minimal),
        FastExternal_Plain(FastExternal, Plain),
        FastExternal_Minimal(FastExternal, Minimal),
        Internal_MinimalAutocomplete(Internal, MinimalAutocomplete),
        FastExternal_MinimalAutocomplete(FastExternal, MinimalAutocomplete),
        Internal_Autocomplete(Internal, Autocomplete),
        FastExternal_Autocomplete(FastExternal, Autocomplete);

        final ItemsetType itemsetType;
        final Appearance appearance;
        private final List<Assert> asserts0;

        SectionVariant(ItemsetType itemsetType, Appearance appearance) {
            this.itemsetType = itemsetType;
            this.appearance = appearance;
            asserts0 = new ArrayList<>(Arrays.asList(Assert.values()));
            if (itemsetType == Internal) {
                return;
            }
            asserts0.removeAll(appearance == Plain
                    ? Arrays.asList()
                    : Arrays.asList()
            );
        }

        public boolean canAlwaysAssert(Assert asserty) {
            return asserts0.contains(asserty);
        }
    }

    enum Block {
        A, B, C, D, E;

        @NotNull String groupLabel(@NotNull SectionVariant variant) {
            return "group_" + name() + "-" + variant.ordinal();
        }

        @NotNull String stateLabel(@NotNull SectionVariant variant) {
            return "state_" + name() + "-" + variant.ordinal();
        }

        @NotNull String countyLabel(@NotNull SectionVariant variant) {
            return "county_" + name() + "-" + variant.ordinal();
        }

        @NotNull String cityLabel(@NotNull SectionVariant variant) {
            return "city_" + name() + "-" + variant.ordinal();
        }

        @NotNull String wardLabel(@NotNull SectionVariant variant) {
            return "ward_" + name() + "-" + variant.ordinal();
        }

        @NotNull String showWardLabel(@NotNull SectionVariant variant) {
            return "show-ward_" + name() + "-" + variant.ordinal();
        }

    }

    static final String TEXT_NO = "no";
    static final String TEXT_YES = "yes";
    static final String TEXT_SELECT_ANSWER = "Select Answer";
    static final String TEXT_HARLINGEN = "Harlingen";
    static final String TEXT_BROWNSVILLE = "Brownsville";
    static final String TEXT_TEXAS = "Texas";
    static final String TEXT_WASHINGTON = "Washington";
    static final String TEXT_NORTH = "North";
    static final String TEXT_SOUTH = "South";
    static final String TEXT_WEST = "West";
    static final String TEXT_EAST = "East";
    static final String TEXT_FORM = "selectOneReset";

    public CollectTestRule rule = new CollectTestRule();
    @Rule
    public RuleChain copyFormChain = RuleChain
            .outerRule(new ResetStateRule())
            .around(new CopyFormRule(TEXT_FORM + ".xml",
                    Collections.singletonList(TEXT_FORM + "-media/itemsets.csv")))
            .around(rule);

    private SectionVariant variantNow;

    @Test
    public void testAllVariants() {
        STAGE_0.makeLatest();
        Timber.i("updateStage = " + UpdateStage.getLatest());

        boolean testSelectedVariants = false;
        boolean testBlockB = true;
        boolean testBlockA = testBlockB && false;
        boolean testBlockC = testBlockA && false;
        boolean testBlocksDE = false;
        boolean testBlockE = testBlocksDE && true;
        int stopAfter = 3;

        FormHierarchyPage hierarchy = new MainMenuPage()
                .startBlankForm(TEXT_FORM)
                .clickGoToArrow();
        for (SectionVariant variant : SectionVariant.values()) {
            variantNow = variant;
            int ordinal = variant.ordinal();
            if (ordinal > stopAfter) {
                break;
            } else if (testSelectedVariants && !(
                    ordinal == 2
            )) {
                continue;
            }
            Timber.i("testing " + variant + "=" + ordinal);
            if (testBlockA) {
                testBlockABC(A, hierarchy);
            }
            hierarchy.clickOnGroup(B.groupLabel(variant));
            if (testBlockB) {
                testBlockABC(B, hierarchy);
            }
            hierarchy.clickOnGroup(C.groupLabel(variant));
            if (testBlockC) {
                testBlockABC(C, hierarchy);
            }
            hierarchy.clickGoUpIcon();
            hierarchy.clickGoUpIcon();
            hierarchy.clickOnGroup(D.groupLabel(variant));
            if (testBlocksDE) {
                testBlocksDE(hierarchy, testBlockE);
            }
            hierarchy.clickGoUpIcon();
            Timber.i("passed " + variant + "=" + ordinal);
        }
    }

    void testBlockABC(Block block, FormHierarchyPage hierarchy) {
        Timber.i(newBlockMsg(block, variantNow));
        String showWardLabel = block.showWardLabel(variantNow);
        String cityLabel = block.cityLabel(variantNow);
        String wardLabel = block.wardLabel(variantNow);
        String stateLabel = block.stateLabel(variantNow);
        boolean minimal = variantNow.appearance.isMinimal();
        FormEntryPage entry = hierarchy
                .clickOnQuestion(showWardLabel)
                .clickOnText(TEXT_NO)
                .swipeToPreviousQuestion(cityLabel);
        if (minimal) {
            entry.openSelectMinimalDialog();
        }
        entry.clickOnText(TEXT_HARLINGEN)
                .swipeToNextQuestion(showWardLabel)
                .clickOnText(TEXT_YES)
                .clickGoToArrow();
        //BC1h
        if (block != A && canAssertAtStage(BC1h, 0)) {
            hierarchy.assertTextDoesNotExist(TEXT_NORTH);
        }
        hierarchy.clickOnQuestion(wardLabel);
        //ABC1e
        if (canAssertAtStage(ABC1e, 0)) {
            entry.assertTextDoesNotExist();
        }
        if (minimal) {
            entry.openSelectMinimalDialog();
        }
        entry.clickOnText(TEXT_EAST)
                .swipeToPreviousQuestion(showWardLabel)
                .swipeToPreviousQuestion(cityLabel);
        if (minimal) {
            entry.openSelectMinimalDialog();
        }
        entry.clickOnText(TEXT_BROWNSVILLE)
                .clickGoToArrow();
        //BC2h
        if (block != A && canAssertAtStage(BC2h, 0)) {
            hierarchy.assertTextDoesNotExist(TEXT_EAST);
        }
        hierarchy.clickOnQuestion(wardLabel);
        //ABC2e
        if ((canAssertAtStage(ABC2e, 0))) {
            entry.assertTextDoesNotExist();
        }
        if (minimal) {
            entry.openSelectMinimalDialog();
        }
        entry.clickOnText(TEXT_NORTH)
                .clickGoToArrow()
                .clickOnQuestion(stateLabel);
        if (minimal) {
            entry.openSelectMinimalDialog();
        }
        entry.clickOnText(TEXT_WASHINGTON);
        //BC3h
        if (block != A && canAssertAtStage(BC3h, 0)) {
            entry.clickGoToArrow()
                    .assertText(TEXT_WASHINGTON, TEXT_YES)
                    .assertTextDoesNotExist(TEXT_NORTH)
                    .clickOnQuestion(stateLabel);
        }
        //ABC3e
        if (canAssertAtStage(ABC3e, 0)) {
            entry.swipeToNextQuestion(block.countyLabel(variantNow))
                    .assertTextDoesNotExist()
                    .swipeToNextQuestion(block.cityLabel(variantNow))
                    .assertTextDoesNotExist()
                    .swipeToNextQuestion(block.showWardLabel(variantNow))
                    .swipeToNextQuestion(block.wardLabel(variantNow))
                    .assertTextDoesNotExist();
        }
        entry.clickGoToArrow();

    }

    void testBlocksDE(FormHierarchyPage hierarchy, boolean testBlockE) {
        Block block = D;
        Timber.i(newBlockMsg(block, variantNow));
        String wardLabel = block.wardLabel(variantNow);
        FormEntryPage entry = hierarchy.clickOnQuestion(wardLabel);
        int minimalBrownsvilles = 0;
        if (variantNow.appearance.isMinimal()) {
            entry.clickOnText(TEXT_NO, 0)
                    .clickOnText(TEXT_BROWNSVILLE, 0)
                    .clickOnText(TEXT_HARLINGEN)
                    .clickOnText(TEXT_YES, 0);
            //DE1
            if (canAssertAtStage(DE1, 0)) {
                entry.clickOnText(TEXT_SELECT_ANSWER)
                ;
            } else {
                entry.clickOnText(TEXT_NORTH, 0);
            }
            entry.clickOnText(TEXT_EAST)
                    .clickOnText(TEXT_HARLINGEN, 0)
                    .clickOnText(TEXT_BROWNSVILLE);
            minimalBrownsvilles++;
            //DE2
            if (canAssertAtStage(DE2, 0)) {
                entry.clickOnText(TEXT_SELECT_ANSWER);
            } else {
                entry.clickOnText(TEXT_EAST);
            }
            entry.clickOnText(TEXT_NORTH)
                    .scrollToAndClickText(TEXT_TEXAS, 0)
                    .clickOnText(TEXT_WASHINGTON);
        } else {
            entry.scrollToAndClickText(TEXT_NO, 0)
                    .clickOnText(TEXT_HARLINGEN, 0)
                    .scrollToAndClickText(TEXT_YES, 0)
                    .scrollToText(TEXT_WEST, 0);
            //DE1
            if (canAssertAtStage(DE1, 0)) {
                entry.assertTextIsNotChecked(TEXT_WEST, 0)
                        .assertTextIsNotChecked(TEXT_EAST, 0);
            }
            entry.clickOnText(TEXT_EAST, 0)
                    .clickOnText(TEXT_BROWNSVILLE, 0)
                    .scrollToText(TEXT_SOUTH, 0);
            //DE2
            if (canAssertAtStage(DE2, 0)) {
                entry.assertTextIsNotChecked(TEXT_SOUTH, 0)
                        .assertTextIsNotChecked(TEXT_NORTH, 0);
            }
            entry.clickOnText(TEXT_NORTH, 0)
                    .scrollToAndClickText(TEXT_WASHINGTON, 0);
        }
        entry.clickGoToArrow();
        //DE3
        if (canAssertAtStage(DE3, 0)) {
            minimalBrownsvilles--;
            hierarchy.assertText(TEXT_WASHINGTON, TEXT_YES)
                    .assertTextDoesNotExist(TEXT_NORTH);
        }

        if (!testBlockE) {
            return;
        }

        block = E;
        Timber.i(newBlockMsg(block, variantNow));
        String groupLabel = block.groupLabel(variantNow);
        wardLabel = block.wardLabel(variantNow);
        entry = hierarchy.clickOnGroup(groupLabel)
                .clickOnQuestion(wardLabel)
                .scrollToText(wardLabel, 0)
                .clickOnText(TEXT_NO, 1);
        if (variantNow.appearance.isMinimal()) {
            entry.scrollToAndClickText(TEXT_BROWNSVILLE,
                    minimalBrownsvilles)
                    .clickOnText(TEXT_HARLINGEN)
                    .clickOnText(TEXT_YES, 1);
            //DE1
            if (canAssertAtStage(DE1, 0)) {
                entry.scrollToAndClickText(TEXT_SELECT_ANSWER, 3);
            } else {
                entry.scrollToAndClickText(TEXT_NORTH, 1);
            }
            entry.clickOnText(TEXT_EAST)
                    .clickOnText(TEXT_HARLINGEN)
                    .clickOnText(TEXT_BROWNSVILLE);
            //DE2
            if (canAssertAtStage(DE2, 0)) {
                entry.scrollToAndClickText(TEXT_SELECT_ANSWER, 3);
            } else {
                entry.scrollToAndClickText(TEXT_EAST);
            }
            entry.clickOnText(TEXT_NORTH)
                    .scrollToAndClickText(TEXT_TEXAS)
                    .clickOnText(TEXT_WASHINGTON);
        } else {
            entry.clickOnText(TEXT_HARLINGEN)
                    .scrollToAndClickText(TEXT_YES, 1)
                    .scrollToText(TEXT_WEST, 0);
            //DE1
            if (canAssertAtStage(DE1, 0)) {
                entry.assertTextIsNotChecked(TEXT_WEST, 0)
                        .assertTextIsNotChecked(TEXT_EAST, 0);
            }
            entry.clickOnText(TEXT_EAST)
                    .clickOnText(TEXT_BROWNSVILLE)
                    .scrollToText(TEXT_SOUTH, 0);
            //DE2
            if (canAssertAtStage(DE2, 0)) {
                entry.assertTextIsNotChecked(TEXT_SOUTH, 0)
                        .assertTextIsNotChecked(TEXT_NORTH, 0);
            }
            entry.clickOnText(TEXT_NORTH)
                    .scrollToAndClickText(TEXT_WASHINGTON, 1);
        }
        entry.clickGoToArrow()
                .clickOnGroup(groupLabel);
        //DE3
        if (canAssertAtStage(DE3, 0)) {
            hierarchy.assertText(TEXT_WASHINGTON, TEXT_YES)
                    .assertTextDoesNotExist(TEXT_NORTH);
        }
        hierarchy.clickGoUpIcon();
    }

    private boolean canAssertAtStage(Assert asserty, int stage) {
        return variantNow.ordinal() < 2
                && (UpdateStage.values()[stage].isApplied() ||
                variantNow.canAlwaysAssert(asserty));
    }

    private String newBlockMsg(Block block, SectionVariant variant) {
        return "Block " + block.name() + "-" + variant.ordinal();
    }
}
