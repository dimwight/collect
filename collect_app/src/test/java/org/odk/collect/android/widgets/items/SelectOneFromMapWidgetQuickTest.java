package org.odk.collect.android.widgets.items;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.testshared.RobolectricHelpers.populateRecyclerView;
import static java.util.Arrays.asList;

import android.view.View;

import androidx.annotation.NonNull;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.SelectOneData;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.listeners.AdvanceToNextListener;
import org.odk.collect.android.support.MockFormEntryPromptBuilder;
import org.odk.collect.android.utilities.Appearances;
import org.odk.collect.android.widgets.base.QuestionWidgetTest;

import java.util.List;

/**
 * From SelectOneWidgetTest for https://github.com/getodk/collect/issues/5540
 */
public class SelectOneFromMapWidgetQuickTest extends QuestionWidgetTest<SelectOneFromMapWidget, SelectOneData> {
    public static final List<SelectChoice> CHOICES = asList(
            new SelectChoice("AAA", "AAA"),
            new SelectChoice("BBB", "BBB")
    );
    @Mock
    private AdvanceToNextListener listener;

    @NonNull
    @Override
    public SelectOneFromMapWidget createWidget() {
        SelectOneFromMapWidget widget = new SelectOneFromMapWidget(activity, new QuestionDetails(formEntryPrompt), isQuick());
        if (isQuick()) {
            widget.setAutoAdvanceListener(listener);
        }
        widget.setFocus(activity);
        return widget;
    }

    @NonNull
    @Override
    public SelectOneData getNextAnswer() {
        return null;
    }

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void whenQuickAppearanceIsUsed_shouldAdvanceToNextListenerBeCalled() {
        formEntryPrompt = new MockFormEntryPromptBuilder()
                .withSelectChoices(CHOICES)
                .withAppearance("quick")
                .build();

        SelectOneFromMapWidget widget = getWidget();
        populateRecyclerView(widget);

        widget.setData(CHOICES.get(0));
        assertThat(widget.getAnswer().getDisplayText(), is("AAA"));

        verify(listener).advance();
    }

    private boolean isQuick() {
        return Appearances.getSanitizedAppearanceHint(formEntryPrompt).contains("quick");
    }

    // From SelectOneMinimalWidgetTest
    @Override
    public void usingReadOnlyOptionShouldMakeAllClickableElementsDisabled() {
        when(formEntryPrompt.isReadOnly()).thenReturn(true);
        assertThat(getSpyWidget().binding.answer.getVisibility(), is(View.VISIBLE));
        assertThat(getSpyWidget().binding.answer.isEnabled(), is(Boolean.FALSE));
    }
}