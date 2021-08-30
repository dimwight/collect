package org.odk.collect.android.feature.formentry;

import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Appearance.Minimal;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Appearance.Plain;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.A1e;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.A2e;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.A3e;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.AA4e;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.A1h;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.A2h;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.A3h;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.B1e;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.B2e;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.B3h;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Assert.BB4h;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Block.A;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Block.B;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.ItemsetType.FastExternal;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.ItemsetType.Internal;
import static org.odk.collect.android.utilities.SelectOneWidgetUtils.UpdateStage.STAGE_0;
import static org.odk.collect.android.utilities.SelectOneWidgetUtils.UpdateStage.STAGE_2;
import static org.odk.collect.android.utilities.SelectOneWidgetUtils.UpdateStage.STAGE_3;

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

public class
SelectOneResetTest {

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

    enum SectionVariant {
        Internal_Plain(Internal, Plain),
        Internal_Minimal(Internal, Minimal),
        FastExternal_Plain(FastExternal, Plain),
        FastExternal_Minimal(FastExternal, Minimal),
        /*Internal_MinimalAutocomplete(Internal, MinimalAutocomplete),
        FastExternal_MinimalAutocomplete(FastExternal, MinimalAutocomplete),
        Internal_Autocomplete(Internal, Autocomplete),
        FastExternal_Autocomplete(FastExternal, Autocomplete)*/;

        final ItemsetType itemsetType;
        final Appearance appearance;
        private final List<Assert> asserts0;

        public boolean canAlwaysAssert(Assert asserty) {
            return asserts0.contains(asserty);
        }

        SectionVariant(ItemsetType itemsetType, Appearance appearance) {
            this.itemsetType = itemsetType;
            this.appearance = appearance;
            asserts0 = new ArrayList<>(Arrays.asList(Assert.values()));
            if (itemsetType == Internal) {
                return;
            }
            asserts0.removeAll(appearance == Plain
                    ? Arrays.asList(A2h, A3h, B3h)
                    : Arrays.asList(A2h, A2e, A3h, B1e, B2e, B3h)
            );
        }
    }

    enum Block {
        A, B;

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

        @NotNull String stateAfterLabel(@NotNull SectionVariant variant) {
            String name = name();
            return "state_" + name + name + "-" + variant.ordinal();
        }

        @NotNull String countyAfterLabel(@NotNull SectionVariant variant) {
            String name = name();
            return "county_" + name + name + "-" + variant.ordinal();
        }
    }

    static final String TEXT_FORM = "selectOneReset";
    static final String TEXT_NO = "no";
    static final String TEXT_YES = "yes";
    static final String TEXT_SELECT_ANSWER = "Select Answer";
    static final String TEXT_HARLINGEN = "Harlingen";
    static final String TEXT_BROWNSVILLE = "Brownsville";
    static final String TEXT_TEXAS = "Texas";
    static final String TEXT_CAMERON = "Cameron";
    static final String TEXT_WASHINGTON = "Washington";
    static final String TEXT_NORTH = "North";
    static final String TEXT_SOUTH = "South";
    static final String TEXT_WEST = "West";
    static final String TEXT_EAST = "East";

    public CollectTestRule rule = new CollectTestRule();
    @Rule
    public RuleChain copyFormChain = RuleChain
            .outerRule(new ResetStateRule())
            .around(new CopyFormRule(TEXT_FORM + ".xml",
                    Collections.singletonList(TEXT_FORM + "-media/itemsets.csv")))
            .around(rule);

    private SectionVariant variantNow;

    enum Assert {A1e, A2e, A3e, AA4e, A1h, A2h, A3h, B1e, B2e, B3h, BB4h}

    protected final boolean assertAA4e = true;
    protected final boolean assertB1e = true;
    protected final boolean assertBB4h = true;

    @Test
    public void testAllVariants() {
        FormHierarchyPage hierarchy = new MainMenuPage()
                .startBlankForm(TEXT_FORM)
                .clickGoToArrow();
        (STAGE_3).makeLatest();
        Timber.i(UpdateStage.getLatest().name());
        (true ? new Staged()
                : new ForPr()).testVariants(hierarchy);
    }

    private class Staged {
        void testVariants(FormHierarchyPage hierarchy) {
            for (SectionVariant variant : SectionVariant.values()) {
                variantNow = variant;
                boolean testSelectedVariants = false;
                boolean testBlockA = true;
                boolean testBlockB = true &&
                        (this instanceof ForPr ||
                                variant.itemsetType == Internal
                                || STAGE_3.isApplied());
                int lastOrdinal = 3;
                int ordinal = variant.ordinal();
                if (ordinal > lastOrdinal) {
                    break;
                } else if (testSelectedVariants && !(
                        ordinal > 1
                )) {
                    continue;
                }
                Timber.i("testing " + variant + "=" + ordinal);
                hierarchy.clickOnGroup(A.groupLabel(variant));
                if (testBlockA) {
                    testBlockA(hierarchy);
                }
                hierarchy.clickGoUpIcon();
                hierarchy.clickOnGroup(B.groupLabel(variant));
                if (testBlockB) {
                    testBlockB(hierarchy);
                }
                hierarchy.clickGoUpIcon();
                Timber.i("passed " + variant + "=" + ordinal);
            }
        }

        FormHierarchyPage testBlockA(FormHierarchyPage hierarchy) {
            Timber.i(newBlockMsg(A, variantNow));
            String showWardLabel = A.showWardLabel(variantNow);
            String cityLabel = A.cityLabel(variantNow);
            String wardLabel = A.wardLabel(variantNow);
            String stateLabel = A.stateLabel(variantNow);
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
            //A1h
            if (canAssertAtStage(A1h, STAGE_0)) {
                hierarchy.assertTextDoesNotExist(TEXT_NORTH);
                assertInfo(A1h);
            }
            hierarchy.clickOnQuestion(wardLabel);
            //A1e
            if (canAssertAtStage(A1e, STAGE_0)) {
                entry.assertTextDoesNotExist();
                assertInfo(A1e);
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
            //A2h
            if (canAssertAtStage(A2h, STAGE_2)) {
                hierarchy.assertTextDoesNotExist(TEXT_EAST);
                assertInfo(A2h);
            }
            hierarchy.clickOnQuestion(wardLabel);
            //A2e
            if (canAssertAtStage(A2e, STAGE_2)) {
                entry.assertTextDoesNotExist(TEXT_EAST);
                assertInfo(A2e);
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
            entry.clickOnText(TEXT_WASHINGTON)
                    .clickGoToArrow();
            //A3h
            if (canAssertAtStage(A3h, STAGE_2)) {
                hierarchy.assertText(TEXT_WASHINGTON, TEXT_YES)
                        .assertTextDoesNotExist(TEXT_NORTH);
                assertInfo(A3h);
            }
            hierarchy.clickOnQuestion(stateLabel);
            //A3e
            if (canAssertAtStage(A3e, STAGE_0)) {
                entry.swipeToNextQuestion(A.countyLabel(variantNow))
                        .assertTextDoesNotExist()
                        .swipeToNextQuestion(cityLabel)
                        .assertTextDoesNotExist()
                        .swipeToNextQuestion(showWardLabel)
                        .swipeToNextQuestion(wardLabel)
                        .assertTextDoesNotExist();
                assertInfo(A3e);
            }
            if (assertAA4e) {
                entry.swipeToNextQuestion(A.stateAfterLabel(variantNow))
                        .swipeToNextQuestion(A.countyAfterLabel(variantNow))
                        //AA4e
                        .assertText(TEXT_CAMERON);
            }
            assertInfo(AA4e, assertAA4e);
            entry.clickGoToArrow();
            return hierarchy;
        }

        FormHierarchyPage testBlockB(FormHierarchyPage hierarchy) {
            Timber.i(newBlockMsg(B, variantNow));
            String wardLabel = B.wardLabel(variantNow);
            FormEntryPage entry = hierarchy.clickOnQuestion(wardLabel);
            boolean minimal = variantNow.appearance.isMinimal();
            if (minimal) {
                entry.clickOnText(TEXT_NO)
                        .clickOnText(TEXT_BROWNSVILLE)
                        .clickOnText(TEXT_HARLINGEN)
                        .clickOnText(TEXT_YES);
                //B1e
                if (assertB1e) {
                    entry.clickOnText(TEXT_SELECT_ANSWER);
                } else {
                    entry.clickOnText(TEXT_NORTH);
                }
                assertInfo(B1e, assertB1e);
                entry.clickOnText(TEXT_EAST)
                        .clickOnText(TEXT_HARLINGEN)
                        .clickOnText(TEXT_BROWNSVILLE)
                        //B2e
                        .clickOnText(TEXT_SELECT_ANSWER);
                assertInfo(B2e);
                entry.clickOnText(TEXT_NORTH)
                        .scrollToAndClickText(TEXT_TEXAS, 0)
                        .clickOnText(TEXT_WASHINGTON);
            } else {
                entry.scrollToAndClickText(TEXT_NO)
                        .clickOnText(TEXT_HARLINGEN)
                        .scrollToAndClickText(TEXT_YES)
                        .scrollToText(TEXT_WEST)
                        //B1e
                        .assertTextIsNotChecked(TEXT_WEST)
                        .assertTextIsNotChecked(TEXT_EAST);
                assertInfo(B1e);
                entry.clickOnText(TEXT_EAST)
                        .clickOnText(TEXT_BROWNSVILLE)
                        .scrollToText(TEXT_SOUTH)
                        //B2e
                        .assertTextIsNotChecked(TEXT_SOUTH)
                        .assertTextIsNotChecked(TEXT_NORTH);
                assertInfo(B2e);
                entry.clickOnText(TEXT_NORTH)
                        .scrollToAndClickText(TEXT_WASHINGTON, 0);
            }
            entry.clickGoToArrow()
                    //B3h
                    .assertText(TEXT_WASHINGTON, TEXT_YES)
                    .assertTextDoesNotExist(TEXT_NORTH);
            assertInfo(B3h);
            if (assertBB4h) {
                //BB4h
                hierarchy.assertText(TEXT_TEXAS, TEXT_CAMERON);
            }
            assertInfo(BB4h, assertBB4h);

            return hierarchy;
        }

    }

    private class ForPr extends Staged {
        void testVariants_(FormHierarchyPage hierarchy) {
            for (SectionVariant variant : SectionVariant.values()) {
                variantNow = variant;
                int ordinal = variant.ordinal();
                Timber.i("testing " + variant + "=" + ordinal);
                hierarchy.clickOnGroup(A.groupLabel(variant));
                testBlockA(hierarchy)
                        .clickGoUpIcon()
                        .clickOnGroup(B.groupLabel(variant));
                testBlockB(hierarchy)
                        .clickGoUpIcon();
                Timber.i("passed " + variant + "=" + ordinal);
            }
        }

        FormHierarchyPage testBlockA(FormHierarchyPage hierarchy) {
            Timber.i(newBlockMsg(A, variantNow));
            String showWardLabel = A.showWardLabel(variantNow);
            String cityLabel = A.cityLabel(variantNow);
            String wardLabel = A.wardLabel(variantNow);
            String stateLabel = A.stateLabel(variantNow);
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
            //A1h
            if (true) {
                hierarchy.assertTextDoesNotExist(TEXT_NORTH);
            }
            hierarchy.clickOnQuestion(wardLabel)
                    //A1e
                    .assertTextDoesNotExist();
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
            //A2h
            if (true) {
                hierarchy.assertTextDoesNotExist(TEXT_EAST);
            }
            hierarchy.clickOnQuestion(wardLabel)
                    //A2e
                    .assertTextDoesNotExist(TEXT_EAST);
            if (minimal) {
                entry.openSelectMinimalDialog();
            }
            entry.clickOnText(TEXT_NORTH)
                    .clickGoToArrow()
                    .clickOnQuestion(stateLabel);
            if (minimal) {
                entry.openSelectMinimalDialog();
            }
            entry.clickOnText(TEXT_WASHINGTON)
                    .clickGoToArrow()
                    //A3h
                    .assertText(TEXT_WASHINGTON, TEXT_YES)
                    .assertTextDoesNotExist(TEXT_NORTH)
                    //A3e
                    .clickOnQuestion(stateLabel).swipeToNextQuestion(A.countyLabel(variantNow))
                    .assertTextDoesNotExist()
                    .swipeToNextQuestion(cityLabel)
                    .assertTextDoesNotExist()
                    .swipeToNextQuestion(showWardLabel)
                    .swipeToNextQuestion(wardLabel)
                    .assertTextDoesNotExist()
                    .swipeToNextQuestion(A.stateAfterLabel(variantNow))
                    .swipeToNextQuestion(A.countyAfterLabel(variantNow))
                    //AA4e
                    .assertText(TEXT_CAMERON);
            return entry.clickGoToArrow();
        }

        FormHierarchyPage testBlockB(FormHierarchyPage hierarchy) {
            Block block = B;
            Timber.i(newBlockMsg(block, variantNow));
            String wardLabel = block.wardLabel(variantNow);
            FormEntryPage entry = hierarchy.clickOnQuestion(wardLabel);
            boolean minimal = variantNow.appearance.isMinimal();
            if (minimal) {
                entry.clickOnText(TEXT_NO)
                        .clickOnText(TEXT_BROWNSVILLE)
                        .clickOnText(TEXT_HARLINGEN)
                        .clickOnText(TEXT_YES)
                        //B1e
                        .clickOnText(TEXT_SELECT_ANSWER)
                        .clickOnText(TEXT_EAST)
                        .clickOnText(TEXT_HARLINGEN)
                        .clickOnText(TEXT_BROWNSVILLE)
                        //B2e
                        .clickOnText(TEXT_SELECT_ANSWER)
                        .clickOnText(TEXT_NORTH)
                        .scrollToAndClickText(TEXT_TEXAS)
                        .clickOnText(TEXT_WASHINGTON);
            } else {
                entry.scrollToAndClickText(TEXT_NO)
                        .clickOnText(TEXT_HARLINGEN)
                        .scrollToAndClickText(TEXT_YES)
                        .scrollToText(TEXT_WEST)
                        //B1e
                        .assertTextIsNotChecked(TEXT_WEST)
                        .assertTextIsNotChecked(TEXT_EAST)
                        .clickOnText(TEXT_EAST)
                        .clickOnText(TEXT_BROWNSVILLE)
                        .scrollToText(TEXT_SOUTH)
                        //B2e
                        .assertTextIsNotChecked(TEXT_SOUTH)
                        .assertTextIsNotChecked(TEXT_NORTH)
                        .clickOnText(TEXT_NORTH)
                        .scrollToAndClickText(TEXT_WASHINGTON);
            }
            return entry.clickGoToArrow()
                    //B3h
                    .assertText(TEXT_WASHINGTON, TEXT_YES)
                    .assertTextDoesNotExist(TEXT_NORTH)
                    //BB4h
                    .clickOnGroup(B.groupLabel(variantNow))
                    .assertText(TEXT_CAMERON)
                    .clickGoUpIcon();

        }

    }

    private String newBlockMsg(Block block, SectionVariant variant) {
        return "Block " + block.name() + "-" + variant.ordinal();
    }

    private boolean canAssertAtStage(Assert asserty, UpdateStage stage) {
        return (stage.isApplied() ||
                variantNow.canAlwaysAssert(asserty));
    }

    private void assertInfo(Assert asserty) {
        assertInfo(asserty, true);
    }

    private void assertInfo(Assert asserty, boolean didAssert) {
        Timber.i((didAssert ? "Asserted " : "Did not assert ")
                + asserty);
    }
}
