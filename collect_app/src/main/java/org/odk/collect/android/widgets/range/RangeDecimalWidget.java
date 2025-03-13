/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.widgets.range;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

import org.javarosa.core.model.data.DecimalData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.views.TrackingTouchSlider;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets.utilities.RangeWidgetUtils;

import java.math.BigDecimal;

@SuppressLint("ViewConstructor")
public class RangeDecimalWidget extends QuestionWidget implements Slider.OnChangeListener {
    TrackingTouchSlider slider;
    TextView currentValue;

    public RangeDecimalWidget(Context context, QuestionDetails prompt) {
        super(context, prompt);
        render();
    }

    @Override
    protected View onCreateAnswerView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        RangeWidgetUtils.RangeWidgetLayoutElements layoutElements = RangeWidgetUtils.setUpLayoutElements(context, prompt);
        slider = layoutElements.getSlider();
        currentValue = layoutElements.getCurrentValue();

        updateActualValueLabel(RangeWidgetUtils.setUpSlider(prompt, slider, false));
        if (slider.isEnabled()) {
            slider.setListener(this);
        }
        return layoutElements.getAnswerView();
    }

    @Override
    public IAnswerData getAnswer() {
        String stringAnswer = currentValue.getText().toString();
        return stringAnswer.isEmpty() ? null : new DecimalData(Double.parseDouble(stringAnswer));
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
    }

    @Override
    public boolean shouldSuppressFlingGesture() {
        return slider.isTrackingTouch();
    }

    @Override
    public void clearAnswer() {
        updateActualValueLabel(null);
        widgetValueChanged();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
        if (fromUser) {
            BigDecimal actualValue = RangeWidgetUtils.getActualValue(getFormEntryPrompt(), value);
            updateActualValueLabel(actualValue);
            widgetValueChanged();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateActualValueLabel(BigDecimal actualValue) {
       if (actualValue != null) {
           float stepSize = slider.getStepSize();
            if (stepSize < 1) {
                int stepChars = String.valueOf(stepSize).length() - 2; // After '0.'
                int shiftedAndCast = (int)
                        (actualValue.doubleValue() * Math.pow(10, stepChars + 1)); // Extra for 9 check
                String builder = String.valueOf(shiftedAndCast);
                if (builder.endsWith("9")) {
                    builder = String.valueOf(++shiftedAndCast);
                }
                builder = builder.replaceAll("0$", "");
                int pointAt = builder.length() - stepChars;
                String top = builder.substring(0, pointAt);
                String tail = builder.substring(pointAt);
                builder = top + "." + tail;
                currentValue.setText(actualValue.toString() + ">" + builder);
            }
            else {
                currentValue.setText(String.valueOf(actualValue.doubleValue()));
            }
        }else {
            currentValue.setText("");
            slider.reset();
        }
    }
}
