package org.odk.collect.android.feature.formentry;

import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Appearance.Minimal;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Appearance.Plain;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Block.A;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.Block.B;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.ItemsetType.FastExternal;
import static org.odk.collect.android.feature.formentry.SelectOneResetTest.ItemsetType.Internal;

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

import java.util.Collections;

import timber.log.Timber;

public class
SelectOneResetTest {

    enum ItemsetType {
        Internal,
        FastExternal
    }

    enum Appearance {
        Plain,
        Minimal,
        MinimalAutocomplete,
//        Autocomplete
        ;

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

        SectionVariant(ItemsetType itemsetType, Appearance appearance) {
            this.itemsetType = itemsetType;
            this.appearance = appearance;
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

    @Test
    public void testAllVariants() {
        FormHierarchyPage hierarchy = new MainMenuPage()
                .startBlankForm(TEXT_FORM)
                .clickGoToArrow();
        for (SectionVariant variant : SectionVariant.values()) {
            int ordinal = variant.ordinal();
            Timber.i("testing " + variant + "=" + ordinal);
            hierarchy.clickOnGroup(A.groupLabel(variant));
            testBlockA(hierarchy, variant)
                    .clickGoUpIcon()
                    .clickOnGroup(B.groupLabel(variant));
            testBlockB(hierarchy, variant)
                    .clickGoUpIcon();
            Timber.i("passed " + variant + "=" + ordinal);
        }
    }

    FormHierarchyPage testBlockA(FormHierarchyPage hierarchy, SectionVariant variant) {
        Timber.i(newBlockMsg(A, variant));
        String showWardLabel = A.showWardLabel(variant);
        String cityLabel = A.cityLabel(variant);
        String wardLabel = A.wardLabel(variant);
        String stateLabel = A.stateLabel(variant);
        boolean minimal = variant.appearance.isMinimal();
        FormEntryPage entry;
        if (variant.itemsetType == Internal) {
            entry = hierarchy.clickOnQuestion(cityLabel);
        } else {
            entry = hierarchy.clickOnQuestion(showWardLabel)
                    .clickOnText(TEXT_NO)
                    .clickOnText(TEXT_YES)
                    .clickGoToArrow()
                    //A1h
                    .assertTextDoesNotExist(TEXT_NORTH)
                    .clickOnQuestion(wardLabel)
                    //A1e
                    .assertTextDoesNotExist();
            if (minimal) {
                entry.openSelectMinimalDialog();
            }
            entry.clickOnText(TEXT_NORTH)
                    .swipeToPreviousQuestion(showWardLabel)
                    .swipeToPreviousQuestion(cityLabel);
        }
        if (minimal) {
            entry.openSelectMinimalDialog();
        }
        entry.clickOnText(TEXT_HARLINGEN)
                .clickGoToArrow()
                //A2h
                .assertTextDoesNotExist(TEXT_NORTH)
                .clickOnQuestion(wardLabel)
                //A2e
                .assertTextDoesNotExist(TEXT_NORTH);
        if (minimal) {
            entry.openSelectMinimalDialog();
        }
        entry.clickOnText(TEXT_EAST)
                .clickGoToArrow()
                .clickOnQuestion(stateLabel);
        if (minimal) {
            entry.openSelectMinimalDialog();
        }
        entry.clickOnText(TEXT_WASHINGTON)
                .clickGoToArrow()
                //A3h
                .assertText(TEXT_WASHINGTON, TEXT_YES)
                .assertTextDoesNotExist(TEXT_HARLINGEN)
                .assertTextDoesNotExist(TEXT_EAST)
                //A3e
                .clickOnQuestion(stateLabel)
                .swipeToNextQuestion(A.countyLabel(variant))
                .assertTextDoesNotExist()
                .swipeToNextQuestion(cityLabel)
                .assertTextDoesNotExist()
                .swipeToNextQuestion(showWardLabel)
                .swipeToNextQuestion(wardLabel)
                .assertTextDoesNotExist()
                .swipeToNextQuestion(A.stateAfterLabel(variant))
                .swipeToNextQuestion(A.countyAfterLabel(variant))
                //AA4e
                .assertText(TEXT_CAMERON);
        return entry.clickGoToArrow();
    }

    FormHierarchyPage testBlockB(FormHierarchyPage hierarchy, SectionVariant variant) {
        Timber.i(newBlockMsg(B, variant));
        boolean minimal = variant.appearance.isMinimal();
        boolean internal = variant.itemsetType == Internal;
        FormEntryPage entry = hierarchy.clickOnQuestion(internal ? B.cityLabel(variant)
                : B.showWardLabel(variant));
        if (minimal) {
            if (!internal) {
                entry.clickOnText(TEXT_NO)
                        .clickOnText(TEXT_YES)
                        //B1e
                        .clickOnText(TEXT_SELECT_ANSWER)
                        .clickOnText(TEXT_NORTH);
            }
            entry.clickOnText(TEXT_BROWNSVILLE)
                    .clickOnText(TEXT_HARLINGEN)
                    //B2e
                    .clickOnText(TEXT_SELECT_ANSWER)
                    .clickOnText(TEXT_EAST)
                    .scrollToAndClickText(TEXT_TEXAS, 0)
                    .clickOnText(TEXT_WASHINGTON);
        } else {
            if (!internal) {
                entry.scrollToAndClickText(TEXT_NO)
                        .clickOnText(TEXT_YES)
                        .scrollToText(TEXT_SOUTH)
                        //B1e
                        .assertTextIsNotChecked(TEXT_NORTH)
                        .assertTextIsNotChecked(TEXT_SOUTH);
            }
            entry.scrollToAndClickText(TEXT_HARLINGEN)
                    .scrollToText(TEXT_WEST)
                    //B2e
                    .assertTextIsNotChecked(TEXT_EAST)
                    .assertTextIsNotChecked(TEXT_WEST)
                    .clickOnText(TEXT_EAST)
                    .scrollToAndClickText(TEXT_WASHINGTON, 0);
        }
        return entry.clickGoToArrow()
                //B3h
                .assertText(TEXT_WASHINGTON, TEXT_YES)
                .assertTextDoesNotExist(TEXT_HARLINGEN)
                .assertTextDoesNotExist(TEXT_EAST)
                //BB4h
                .assertText(TEXT_TEXAS, TEXT_CAMERON);
    }

    private String newBlockMsg(Block block, SectionVariant variant) {
        return "Block " + block.name() + "-" + variant.ordinal();
    }
}
