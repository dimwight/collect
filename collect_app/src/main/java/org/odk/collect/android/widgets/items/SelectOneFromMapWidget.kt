package org.odk.collect.android.widgets.items

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentActivity
import org.javarosa.core.model.data.IAnswerData
import org.javarosa.core.model.data.SelectOneData
import org.javarosa.core.model.data.StringData
import org.javarosa.core.model.data.helper.Selection
import org.javarosa.core.model.instance.TreeElement
import org.javarosa.form.api.FormEntryPrompt
import org.odk.collect.android.databinding.SelectOneFromMapWidgetAnswerBinding
import org.odk.collect.android.exception.JavaRosaException
import org.odk.collect.android.formentry.questions.QuestionDetails
import org.odk.collect.android.javarosawrapper.FormController
import org.odk.collect.android.listeners.AdvanceToNextListener
import org.odk.collect.android.widgets.QuestionWidget
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver
import org.odk.collect.android.widgets.items.SelectOneFromMapDialogFragment.Companion.ARG_FOCUS_DOUBLES
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
    // #6136?
    private val controller: FormController? = null,
    private val autoAdvanceListener: AdvanceToNextListener
) : QuestionWidget(context, questionDetails), WidgetDataReceiver {

    init {
        render()
    }

    private var doubles: DoubleArray? = null
    private val useController = controller != null && false

    // #6136
    private fun findFocusPrompt(): FormEntryPrompt? {
        var prompt: FormEntryPrompt? = null
        try {
            val thisIndex = controller?.getQuestionPrompt()?.index
            var name: String?
            while (controller?.currentPromptIsQuestion() == useController) {
                controller.stepToNextScreenEvent()
                prompt = controller.getQuestionPrompt()
                name = prompt?.index?.reference?.nameLast
                if (name == "focus") {
                    break
                }
            }
            controller?.jumpToIndex(thisIndex)
        } catch (e: JavaRosaException) {
            Timber.d(e)
        }
        return prompt
    }

    lateinit var binding: SelectOneFromMapWidgetAnswerBinding
    private var answer: SelectOneData? = null

    private fun findFocus(prompt: FormEntryPrompt): TreeElement? {
        fun accessField(obj: Any?, name: String): Any? {
            val field = obj?.javaClass?.getDeclaredField(name)
            field?.isAccessible = true
            return field?.get(obj)
        }

        val mTreeElement = accessField(prompt, "mTreeElement")
        val parent = accessField(mTreeElement, "parent")
        return (parent as TreeElement).getChildAt(1)
    }

    override fun onCreateAnswerView(
        context: Context,
        prompt: FormEntryPrompt,
        answerFontSize: Int
    ): View {
        binding = SelectOneFromMapWidgetAnswerBinding.inflate(LayoutInflater.from(context))

        // #6136
        val focus = if (controller != null && useController) {
            controller.getAnswer(
                findFocusPrompt()?.index?.reference
            )?.value
        } else {
            findFocus(prompt)?.value
        }

        if (focus != null) {
            val split = (focus as String).split(" ")
            if (doubles == null) {
                doubles = DoubleArray(3)
            }
            split.forEachIndexed { i, s ->
                doubles!![i] = s.toDouble()
            }
        }

        binding.button.setOnClickListener {
            permissionsProvider.requestEnabledLocationPermissions(
                context as Activity,
                object : PermissionListener {
                    override fun granted() {
                        DialogFragmentUtils.showIfNotShowing(
                            SelectOneFromMapDialogFragment::class.java,
                            Bundle().also {
                                it.putSerializable(ARG_FORM_INDEX, prompt.index)
                                // #6136
                                if (doubles != null) {
                                    it.putDoubleArray(ARG_FOCUS_DOUBLES, doubles)
                                }
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
        this.doubles = answer.focus

        val store = StringData("${doubles?.get(0)} ${doubles?.get(1)} ${doubles?.get(2)}")

        if (useController) {
            controller?.saveAnswer(findFocusPrompt()?.index, store)
        } else {
            findFocus(formEntryPrompt)
                ?.setAnswer(store)
        }
    }
}
