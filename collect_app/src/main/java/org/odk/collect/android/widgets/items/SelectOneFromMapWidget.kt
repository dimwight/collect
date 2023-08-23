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
import org.javarosa.core.model.data.helper.Selection
import org.javarosa.form.api.FormEntryPrompt
import org.odk.collect.android.databinding.SelectOneFromMapWidgetAnswerBinding
import org.odk.collect.android.formentry.questions.QuestionDetails
import org.odk.collect.android.listeners.AdvanceToNextListener
import org.odk.collect.android.widgets.QuestionWidget
import org.odk.collect.android.widgets.interfaces.WidgetDataReceiver
import org.odk.collect.android.widgets.items.SelectOneFromMapDialogFragment.Companion.ARG_FORM_INDEX
import org.odk.collect.android.widgets.items.SelectOneFromMapDialogFragment.Companion.ARG_SELECTED_INDEX
import org.odk.collect.androidshared.ui.DialogFragmentUtils
import org.odk.collect.permissions.PermissionListener
import timber.log.Timber

@SuppressLint("ViewConstructor")
class SelectOneFromMapWidget(
    context: Context,
    questionDetails: QuestionDetails,
    private val autoAdvance: Boolean,
) :
    QuestionWidget(context, questionDetails), WidgetDataReceiver {
    private var autoAdvanceListener: AdvanceToNextListener? = null

    init {
        if (context is AdvanceToNextListener) {
            autoAdvanceListener = context
        }
        render()
    }

    lateinit var binding: SelectOneFromMapWidgetAnswerBinding
    private var answer: SelectOneData? = null

    override fun onCreateAnswerView(
        context: Context,
        prompt: FormEntryPrompt,
        answerFontSize: Int
    ): View {
        Timber.d("5540: 41")
        binding = SelectOneFromMapWidgetAnswerBinding.inflate(LayoutInflater.from(context))

        binding.button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontSize.toFloat())
        binding.button.setOnClickListener {
            permissionsProvider.requestEnabledLocationPermissions(
                context as Activity,
                object : PermissionListener {
                    override fun granted() {
                        DialogFragmentUtils.showIfNotShowing(
                            SelectOneFromMapDialogFragment::class.java,
                            Bundle().also {
                                it.putSerializable(ARG_FORM_INDEX, prompt.index)
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
        Timber.d("5540: 72")
        return answer
    }

    override fun clearAnswer() {
        Timber.d("5540: 77")
        updateAnswer(null)
        widgetValueChanged()
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        Timber.d("5540: 83")

    }

    override fun setData(answer: Any?) {
        Timber.d("5540+: setData")
        updateAnswer(answer as SelectOneData)

        // https://github.com/getodk/collect/issues/5540
        if (autoAdvance) {
            autoAdvanceListener?.advance()
        }

        widgetValueChanged()
    }

    private fun updateAnswer(answer: SelectOneData?) {
        if (false) {
            Timber.d("5540+: this=${this.answer == null}")
            Timber.d("5540+: answer=${answer == null}")
            val newAnswer = answer != null &&
                    !answer.equals(this.answer)
            Timber.d("5540+: newAnswer=${newAnswer}")
        }
        this.answer = answer

        binding.answer.text = if (answer != null) {
            val choice = (answer.value as Selection).choice
            formEntryPrompt.getSelectChoiceText(choice)
        } else {
            ""
        }
    }

    override fun widgetValueChanged() {
        Timber.d("5540+: widgetValueChanged")
        super.widgetValueChanged()
    }
}
