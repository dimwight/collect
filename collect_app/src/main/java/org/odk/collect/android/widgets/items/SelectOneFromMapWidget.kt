package org.odk.collect.android.widgets.items

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentActivity
import org.javarosa.core.model.FormIndex
import org.javarosa.core.model.data.IAnswerData
import org.javarosa.core.model.data.SelectOneData
import org.javarosa.core.model.data.StringData
import org.javarosa.core.model.data.helper.Selection
import org.javarosa.form.api.FormEntryPrompt
import org.odk.collect.android.databinding.SelectOneFromMapWidgetAnswerBinding
import org.odk.collect.android.exception.JavaRosaException
import org.odk.collect.android.formentry.questions.QuestionDetails
import org.odk.collect.android.javarosawrapper.FormController
import org.odk.collect.android.listeners.AdvanceToNextListener
import org.odk.collect.android.widgets.QuestionWidget
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver
import org.odk.collect.android.widgets.items.SelectOneFromMapDialogFragment.Companion.ARG_FORM_INDEX
import org.odk.collect.android.widgets.items.SelectOneFromMapDialogFragment.Companion.ARG_SELECTED_INDEX
import org.odk.collect.android.widgets.items.SelectOneFromMapDialogFragment.SelectOneFromMapData
import org.odk.collect.androidshared.ui.DialogFragmentUtils
import org.odk.collect.permissions.PermissionListener
import timber.log.Timber

@SuppressLint("ViewConstructor")
class SelectOneFromMapWidget(
    context: Context,
    questionDetails: QuestionDetails,
    private val autoAdvance: Boolean,
    // #6136
    private val controller: FormController,
    private val autoAdvanceListener: AdvanceToNextListener
) : QuestionWidget(context, questionDetails), WidgetDataReceiver {

    lateinit var focusStore: FormIndex
    lateinit var focusSource: FormIndex

    init {
        render()
        // #6136
        if (true)
        try {
            val thisIndex = controller.getQuestionPrompt()!!.index
            var prompt: FormEntryPrompt
            // while (controller.currentPromptIsQuestion()) {
                controller.stepToNextScreenEvent()
                prompt = controller.getQuestionPrompt()!!
            //  }
            controller.saveAnswer(prompt.index, StringData("Hi"))
            val data = controller.getAnswer(prompt.index.reference)
            controller.jumpToIndex(thisIndex)
        } catch (e: JavaRosaException) {
            Timber.d(e)
        }

    }

    lateinit var binding: SelectOneFromMapWidgetAnswerBinding
    private var answer: SelectOneData? = null

    override fun onCreateAnswerView(
        context: Context,
        prompt: FormEntryPrompt,
        answerFontSize: Int
    ): View {
        // val children = prompt.question.children
        binding = SelectOneFromMapWidgetAnswerBinding.inflate(LayoutInflater.from(context))

        binding.button.setOnClickListener {
            permissionsProvider.requestEnabledLocationPermissions(
                context as Activity,
                object : PermissionListener {
                    override fun granted() {
                        DialogFragmentUtils.showIfNotShowing(
                            SelectOneFromMapDialogFragment::class.java,
                            Bundle().also {
                                it.putSerializable(ARG_FORM_INDEX, prompt.index)
                                // #6136 +MapFocus
                                (answer?.value as? Selection)?.index?.let { index ->
                                    it.putInt(ARG_SELECTED_INDEX, index)
                                }
                            },
                            (context as FragmentActivity).supportFragmentManager
                        )
                    }
                }
            )
        }

        binding.answer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize.toFloat())
        updateAnswer(questionDetails.prompt.answerValue as? SelectOneData)

        return binding.root
    }

    override fun getAnswer(): IAnswerData? {
        return answer
    }

    override fun clearAnswer() {
        updateAnswer(null)
        widgetValueChanged()
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {}

    override fun setData(answer: Any?) {
        updateAnswer(answer as SelectOneData)
        widgetValueChanged()
        if (autoAdvance) {
            autoAdvanceListener.advance()
        }
    }

    private fun updateAnswer(answer: SelectOneData?) {
        this.answer = answer

        binding.answer.text = if (answer != null) {
            val choice = (answer.value as Selection).choice
            formEntryPrompt.getSelectChoiceText(choice)
        } else {
            ""
        }
        binding.answer.visibility = if (binding.answer.text.isBlank()) {
            GONE
        } else {
            VISIBLE
        }
        // #6136
        if (answer !is SelectOneFromMapData) {
            return
        }
        val focus = answer.focus
        val store = "${focus?.get(0)} ${focus?.get(1)} ${focus?.get(2)}"

        val focus2 = DoubleArray(3)
        var at = 0
        for (s in store.split(" ")) {
            focus2[at++] = s.toDouble()
        }

        val list = ArrayList<Double>()
        store.split(" ").forEach {
            list.add(it.toDouble())
        }
        val focus3 = list.toTypedArray<Double>()
    }
}
