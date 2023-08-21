/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.odk.collect.android.widgets.items

import android.annotation.SuppressLint
import android.content.Context
import android.widget.RadioButton
import org.javarosa.core.model.data.IAnswerData
import org.javarosa.core.model.data.SelectOneData
import org.odk.collect.android.adapters.AbstractSelectListAdapter
import org.odk.collect.android.adapters.SelectOneListAdapter
import org.odk.collect.android.exception.JavaRosaException
import org.odk.collect.android.formentry.media.FormMediaUtils
import org.odk.collect.android.formentry.questions.QuestionDetails
import org.odk.collect.android.javarosawrapper.FormController
import org.odk.collect.android.listeners.AdvanceToNextListener
import org.odk.collect.android.utilities.Appearances.getNumberOfColumns
import org.odk.collect.android.utilities.Appearances.isCompactAppearance
import org.odk.collect.android.utilities.Appearances.isNoButtonsAppearance
import org.odk.collect.android.utilities.SelectOneWidgetUtils
import org.odk.collect.android.widgets.interfaces.SelectChoiceLoader
import timber.log.Timber

/**
 * SelectOneWidgets handles select-one fields using radio buttons.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
@SuppressLint("ViewConstructor")
class SelectOneWidgetK(
    context: Context?,
    questionDetails: QuestionDetails?,
    private val autoAdvance: Boolean,
    private val formController: FormController?,
    selectChoiceLoader: SelectChoiceLoader?
) :
    BaseSelectListWidget(context, questionDetails, selectChoiceLoader) {
    private var listener: AdvanceToNextListener? = null

    init {
        if (context is AdvanceToNextListener) {
            listener = context
        }
    }

    override fun setUpAdapter(): AbstractSelectListAdapter {
        val numColumns = getNumberOfColumns(formEntryPrompt, screenUtils)
        val noButtonsMode = isCompactAppearance(formEntryPrompt) || isNoButtonsAppearance(
            formEntryPrompt
        )
        recyclerViewAdapter = SelectOneListAdapter(
            selectedValue,
            this,
            context,
            items,
            formEntryPrompt,
            getReferenceManager(),
            getAudioHelper(),
            FormMediaUtils.getPlayColor(formEntryPrompt, themeUtils),
            numColumns,
            noButtonsMode,
            mediaUtils
        )
        return recyclerViewAdapter
    }

    override fun getAnswer(): IAnswerData? {
        val selectedItem = (recyclerViewAdapter as SelectOneListAdapter).selectedItem
        return selectedItem?.let { SelectOneData(it) }
    }

    protected val selectedValue: String?
        protected get() {
            val selectedItem =
                SelectOneWidgetUtils.getSelectedItem(getQuestionDetails().prompt, items)
            return selectedItem?.value
        }

    override fun setChoiceSelected(choiceIndex: Int, isSelected: Boolean) {
        val button = RadioButton(context)
        button.tag = choiceIndex
        button.isChecked = isSelected
        (recyclerViewAdapter as SelectOneListAdapter).onCheckedChanged(button, isSelected)
    }

    override fun onItemClicked() {
        if (autoAdvance && listener != null) {
            listener!!.advance()
        }
        clearFollowingItemsetWidgets()
        widgetValueChanged()
    }

    override fun clearAnswer() {
        clearFollowingItemsetWidgets()
        super.clearAnswer()
    }

    /**
     * If there are "fast external itemset" selects right after this select, assume that they are linked to the current question and clear them.
     */
    private fun clearFollowingItemsetWidgets() {
        if (formController == null) {
            return
        }
        if (formController.currentCaptionPromptIsQuestion()) {
            try {
                val startFormIndex = formController.getQuestionPrompt()!!.index
                formController.stepToNextScreenEvent()
                while (formController.currentCaptionPromptIsQuestion()
                    && formController.getQuestionPrompt()!!
                        .formElement.getAdditionalAttribute(null, "query") != null
                ) {
                    formController.saveAnswer(formController.getQuestionPrompt()!!.index, null)
                    formController.stepToNextScreenEvent()
                }
                formController.jumpToIndex(startFormIndex)
            } catch (e: JavaRosaException) {
                Timber.d(e)
            }
        }
    }

    fun setListener(listener: AdvanceToNextListener?) {
        this.listener = listener
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {}
}