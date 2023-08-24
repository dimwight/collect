package org.odk.collect.android.widgets.items;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.odk.collect.testshared.RobolectricHelpers.populateRecyclerView;
import static java.util.Arrays.asList;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.javarosa.core.model.SelectChoice;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.odk.collect.android.formentry.questions.AudioVideoImageTextLabel;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.AdvanceToNextListener;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.widgets.base.GeneralSelectOneWidgetTest;
import org.odk.collect.android.widgets.support.FormEntryPromptSelectChoiceLoader;

/**
 * Derived from SelectOneWidgetTest for https://github.com/getodk/collect/issues/5540
 */
public class SelectOneFromMapWidgetQuickTest extends GeneralSelectOneWidgetTest<SelectOneWidget> {
    @Mock
    private AdvanceToNextListener listener;

    @NonNull
    @Override
    public SelectOneWidget createWidget() {
        SelectOneWidget selectOneWidget = new SelectOneWidget(activity, new QuestionDetails(formEntryPrompt), isQuick(), null, new FormEntryPromptSelectChoiceLoader());
        if (isQuick()) {
            selectOneWidget.setListener(listener);
        }
        selectOneWidget.setFocus(activity);
        return selectOneWidget;
    }

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void whenQuickAppearanceIsUsed_shouldAdvanceToNextListenerBeCalledInButtonsMode() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(asList(
                        new SelectChoice("AAA", "AAA"),
                        new SelectChoice("BBB", "BBB")
                ))
                .withAppearance("quick")
                .build();

        SelectOneWidget widget = getWidget();
        populateRecyclerView(widget);

        clickChoice(widget, 0); // Select AAA
        assertThat(widget.getAnswer().getDisplayText(), is("AAA"));

        verify(listener).advance();
    }

    @Test
    public void whenQuickAppearanceIsUsed_shouldAdvanceToNextListenerBeCalledInNoButtonsMode() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(asList(
                        new SelectChoice("AAA", "AAA"),
                        new SelectChoice("BBB", "BBB")
                ))
                .withAppearance("quick no-buttons")
                .build();

        SelectOneWidget widget = getWidget();
        populateRecyclerView(widget);

        clickChoice(widget, 0); // Select AAA
        assertThat(widget.getAnswer().getDisplayText(), is("AAA"));

        verify(listener).advance();
    }

    @Test
    public void whenQuickAppearanceIsNotUsed_shouldNotAdvanceToNextListenerBeCalledInButtonsMode() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(asList(
                        new SelectChoice("AAA", "AAA"),
                        new SelectChoice("BBB", "BBB")
                ))
                .build();

        SelectOneWidget widget = getWidget();
        populateRecyclerView(widget);

        clickChoice(widget, 0); // Select AAA
        assertThat(widget.getAnswer().getDisplayText(), is("AAA"));

        verify(listener, times(0)).advance();
    }

    @Test
    public void whenQuickAppearanceIsNotUsed_shouldNotAdvanceToNextListenerBeCalledInNoButtonsMode() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(asList(
                        new SelectChoice("AAA", "AAA"),
                        new SelectChoice("BBB", "BBB")
                ))
                .withAppearance("no-buttons")
                .build();

        SelectOneWidget widget = getWidget();
        populateRecyclerView(widget);

        clickChoice(widget, 0); // Select AAA
        assertThat(widget.getAnswer().getDisplayText(), is("AAA"));

        verify(listener, times(0)).advance();
    }

    private void clickChoice(SelectOneWidget widget, int index) {
        if (Appearances.isNoButtonsAppearance(formEntryPrompt)) {
            clickNoButtonChoice(widget, index);
        } else {
            clickButtonChoice(widget, index);
        }
    }

    private void clickNoButtonChoice(SelectOneWidget widget, int index) {
        widget.binding.choicesRecyclerView.getChildAt(index).performClick();
    }

    private void clickButtonChoice(SelectOneWidget widget, int index) {
        ((AudioVideoImageTextLabel) getChoiceView(widget, index)).getLabelTextView().performClick();
    }

    private ViewGroup getChoiceView(SelectOneWidget widget, int index) {
        return (ViewGroup) widget.binding.choicesRecyclerView.getChildAt(index);
    }

    private boolean isQuick() {
        return Appearances.getSanitizedAppearanceHint(formEntryPrompt).contains("quick");
    }

    @Override
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        // ?
    }
}
