package org.odk.collect.android.formhierarchy

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.javarosa.core.model.FormIndex
import org.javarosa.core.model.GroupDef
import org.javarosa.core.model.instance.TreeReference
import org.javarosa.form.api.FormEntryCaption
import org.javarosa.form.api.FormEntryController
import org.javarosa.form.api.FormEntryModel
import org.odk.collect.android.R
import org.odk.collect.android.databinding.FormHierarchyLayoutBinding
import org.odk.collect.android.exception.JavaRosaException
import org.odk.collect.android.formentry.FormEntryViewModel
import org.odk.collect.android.formentry.ODKView
import org.odk.collect.android.formentry.repeats.DeleteRepeatDialogFragment
import org.odk.collect.android.formhierarchy.QuestionAnswerProcessor.getQuestionAnswer
import org.odk.collect.android.javarosawrapper.FormController
import org.odk.collect.android.javarosawrapper.FormIndexUtils
import org.odk.collect.android.javarosawrapper.JavaRosaFormController
import org.odk.collect.android.javarosawrapper.JavaRosaFormController.STEP_INTO_GROUP
import org.odk.collect.android.utilities.FormEntryPromptUtils
import org.odk.collect.android.utilities.HtmlUtils
import org.odk.collect.androidshared.ui.DialogFragmentUtils.showIfNotShowing
import timber.log.Timber

class FormHierarchyFragment(
    private val viewOnly: Boolean,
    private val viewModelFactory: ViewModelProvider.Factory,
    private val menuHost: MenuHost
) :
    Fragment(R.layout.form_hierarchy_layout) {
    private lateinit var menuProvider: FormHiearchyMenuProvider
    private lateinit var formEntryViewModel: FormEntryViewModel
    private lateinit var controller: FormController
    private lateinit var formHierarchyViewModel: FormHierarchyViewModel

    /**
     * The index of the question or the field list the FormController was set to when the hierarchy
     * was accessed. Used to jump the user back to where they were if applicable.
     */
    private var startIndex: FormIndex? = null


    override fun onAttach(context: Context) {
        println("5194c: onAttach")
        super.onAttach(context)

        formEntryViewModel =
            ViewModelProvider(requireActivity(), viewModelFactory)[FormEntryViewModel::class.java]
        controller = formEntryViewModel.formController
        formHierarchyViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T {
                    @Suppress("UNCHECKED_CAST")
                    return FormHierarchyViewModel() as T
                }
            })[FormHierarchyViewModel::class]

        requireActivity().title = controller.getFormTitle()

        startIndex = controller.getFormIndex()

        menuProvider = FormHiearchyMenuProvider(formEntryViewModel, formHierarchyViewModel,
            viewOnly, object : FormHiearchyMenuProvider.OnClickListener {
                override fun onGoUpClicked() {

                    // If `repeatGroupPickerIndex` is set it means we're currently displaying
                    // a list of repeat instances. If we unset `repeatGroupPickerIndex`,
                    // we will go back up to the previous screen.
                    if (formHierarchyViewModel.shouldShowRepeatGroupPicker(11)) {
                        // Exit the picker.
                        formHierarchyViewModel.repeatGroupPickerIndex = null
                    } else {
                        // Enter the picker if coming from a repeat group.
                        val screenIndex = formHierarchyViewModel.screenIndex
                        val event = controller.getEvent(screenIndex)
                        if (event == FormEntryController.EVENT_REPEAT || event == FormEntryController.EVENT_PROMPT_NEW_REPEAT) {
                            formHierarchyViewModel.repeatGroupPickerIndex = screenIndex
                        }

                        controller.stepToOuterScreenEvent()
                    }

                    refreshView(true)
                }

                override fun onAddRepeatClicked() {
                    controller.jumpToIndex(formHierarchyViewModel.repeatGroupPickerIndex)
                    formEntryViewModel.jumpToNewRepeat()
                    formEntryViewModel.addRepeat()

                    requireActivity().finish()
                }

                override fun onDeleteRepeatClicked() {
                    showIfNotShowing(
                        DeleteRepeatDialogFragment::class.java, childFragmentManager
                    )
                }
            })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        println("5194c: onViewCreated")
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val formController = controller
                    formController.getAuditEventLogger()!!.flush()
                    navigateToTheLastRelevantIndex(formController)
                    requireActivity().finish()
                }
            }
        )

        formHierarchyViewModel.startIndex = controller.getFormIndex()
        menuHost.addMenuProvider(menuProvider, viewLifecycleOwner)

        val binding = FormHierarchyLayoutBinding.bind(view)

        val recyclerView = binding.list
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )

        configureButtons(binding, controller)
        refreshView()

        // Scroll to the last question the user was looking at
        // TODO: avoid another iteration through all displayed elements
        if (recyclerView.adapter!!.itemCount > 0 //5194
        ) {
            binding.empty.visibility = View.GONE
            recyclerView.post {
                var position = 0
                // Iterate over all the elements currently displayed looking for a match with the
                // startIndex which can either represent a question or a field list.
                val elementsToDisplay =
                    formHierarchyViewModel.elementsToDisplay!!
                for (hierarchyItem in elementsToDisplay) {
                    val startIndex = formHierarchyViewModel.startIndex!!
                    val indexToCheck = hierarchyItem.formIndex
                    val indexIsInFieldList =
                        controller.indexIsInFieldList(startIndex)
                    if (startIndex == indexToCheck
                        || (indexIsInFieldList && indexToCheck.toString()
                            .startsWith(startIndex.toString()))
                    ) {
                        position = elementsToDisplay.indexOf(hierarchyItem)
                        break
                    }
                }
                (recyclerView.layoutManager as LinearLayoutManager)
                    .scrollToPositionWithOffset(position, 0)
            }
        }

        childFragmentManager.setFragmentResultListener(
            DeleteRepeatDialogFragment.REQUEST_DELETE_REPEAT,
            viewLifecycleOwner
        ) { _, _ -> onRepeatDeleted() }
    }

    private fun refreshView() {
        refreshView(false)
    }

    /**
     * @see .refreshView
     */
    private fun refreshView(isGoingUp: Boolean) {
        println("5194c: refreshView")
        val binding = FormHierarchyLayoutBinding.bind(requireView())
        val groupIcon = binding.groupIcon
        val groupPathTextView = binding.pathtext
        val recyclerView = binding.list

        try {
            val formController = controller

            // Save the current index so we can return to the problematic question
            // in the event of an error.
            formHierarchyViewModel.currentIndex = formController.getFormIndex()

            calculateElementsToDisplay(formController, groupIcon, groupPathTextView)
            recyclerView.adapter =
                HierarchyListAdapter(
                    formHierarchyViewModel.elementsToDisplay!!
                ) { item: HierarchyItem? ->
                    this.onElementClick(item!!)
                }

            formController.jumpToIndex(formHierarchyViewModel.currentIndex)

            // Prevent a redundant middle screen (common on many forms
            // that use presentation groups to display labels).
            if (isDisplayingSingleGroup && !formHierarchyViewModel.screenIndex
                    ?.isBeginningOfFormIndex!!
            ) {
                if (isGoingUp) {
                    // Back out once more.
                    goUpLevel()
                } else {
                    // Enter automatically.
                    formController.jumpToIndex(
                        formHierarchyViewModel.elementsToDisplay!![0].formIndex
                    )
                    refreshView()
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            createErrorDialog(e.message)
        }
    }

    private fun calculateElementsToDisplay(
        formController: FormController,
        groupIcon: ImageView,
        groupPathTextView: TextView
    ) {
        println("5194c: calculateElementsToDisplay")
        val elementsToDisplay: MutableList<HierarchyItem> = ArrayList()

        jumpToHierarchyStartIndex()

        var event = formController.getEvent()

        if (event == FormEntryController.EVENT_BEGINNING_OF_FORM
            && !formHierarchyViewModel.shouldShowRepeatGroupPicker(1)
        ) {
            // The beginning of form has no valid prompt to display.
            groupIcon.visibility = View.GONE
            groupPathTextView.visibility = View.GONE
        } else {
            groupIcon.visibility = View.VISIBLE
            groupPathTextView.visibility = View.VISIBLE
            groupPathTextView.text = currentPath

            val drawable = ContextCompat.getDrawable( //5194
                requireContext(),
                if (formController.indexContainsRepeatableGroup(formHierarchyViewModel.screenIndex)
                    || formHierarchyViewModel.shouldShowRepeatGroupPicker(2)
                ) {
                    R.drawable.ic_repeat
                } else {
                    R.drawable.ic_folder_open
                }

            )
            groupIcon.setImageDrawable(drawable)
        }

        // Refresh the current event in case we did step forward.
        event = formController.getEvent()

        // Ref to the parent group that's currently being displayed.
        //
        // Because of the guard conditions below, we will skip
        // everything until we exit this group.
        var visibleGroupRef: TreeReference? = null

        while (event != FormEntryController.EVENT_END_OF_FORM) {
//            println("5194c: event = ${event}")
            // get the ref to this element
            val currentRef = formController.getFormIndex()!!.reference

            // retrieve the current group
            val curGroup = visibleGroupRef ?: formHierarchyViewModel.contextGroupRef

            if (curGroup != null && !curGroup.isAncestorOf(currentRef, false)) {
                // We have left the current group
                if (visibleGroupRef == null) {
                    // We are done.
                    event = formController.stepToNextEvent(STEP_INTO_GROUP)
                    continue
                } else {
                    // exit the inner group
                    visibleGroupRef = null
                }
            }

            if (visibleGroupRef != null) {
                // We're in a group within the one we want to list
                // skip this question/group/repeat and move to the next index.
                event = formController.stepToNextEvent(STEP_INTO_GROUP)
                continue
            }

            when (event) {
                FormEntryController.EVENT_PROMPT_NEW_REPEAT -> {}
                FormEntryController.EVENT_QUESTION -> {
                    // Nothing but repeat group instances should show up in the picker.
                    if (formHierarchyViewModel.shouldShowRepeatGroupPicker(3)) {
                        event = formController.stepToNextEvent(STEP_INTO_GROUP)
                        continue
                    }

                    val fp = formController.getQuestionPrompt()
                    val label = fp!!.shortText
                    val answerDisplay = getQuestionAnswer(fp, requireContext(), formController)
                    elementsToDisplay.add(
                        HierarchyItem(
                            fp.index,
                            HierarchyItemType.QUESTION,
                            FormEntryPromptUtils.styledQuestionText(label, fp.isRequired),
                            answerDisplay
                        )
                    )
                }

                FormEntryController.EVENT_GROUP -> {
                    if (!formController.isGroupRelevant()) {
                        event = formController.stepToNextEvent(STEP_INTO_GROUP)
                        continue
                    }
                    // Nothing but repeat group instances should show up in the picker.
                    if (formHierarchyViewModel.shouldShowRepeatGroupPicker(4)) {
                        event = formController.stepToNextEvent(STEP_INTO_GROUP)
                        continue
                    }

                    val index = formController.getFormIndex()

                    // Only display groups with a specific appearance attribute.
                    if (!formController.isDisplayableGroup(index)) {
                        event = formController.stepToNextEvent(STEP_INTO_GROUP)
                        continue
                    }

                    // Don't render other groups' children.
                    val contextGroupRef = formHierarchyViewModel.contextGroupRef
                    if (contextGroupRef != null && !contextGroupRef.isAncestorOf(
                            currentRef,
                            false
                        )
                    ) {
                        event = formController.stepToNextEvent(STEP_INTO_GROUP)
                        continue
                    }

                    visibleGroupRef = currentRef

                    val caption = formController.getCaptionPrompt()

                    elementsToDisplay.add(
                        HierarchyItem(
                            caption!!.index,
                            HierarchyItemType.VISIBLE_GROUP,
                            HtmlUtils.textToHtml(caption.shortText)
                        )
                    )

                    // Skip to the next item outside the group.
                    event = formController.stepOverGroup()
                    continue
                }

                FormEntryController.EVENT_REPEAT -> {
                    val forPicker = formHierarchyViewModel.shouldShowRepeatGroupPicker(5)
                    // Only break to exclude non-relevant repeat from picker
                    if (!formController.isGroupRelevant() && forPicker) {
                        event = formController.stepToNextEvent(STEP_INTO_GROUP)
                        continue
                    }

                    visibleGroupRef = currentRef

                    // Don't render other groups' children.
                    val contextGroupRef = formHierarchyViewModel.contextGroupRef
                    if (contextGroupRef != null
                        && !contextGroupRef.isAncestorOf(currentRef, false)
                    ) {
                        event = formController.stepToNextEvent(STEP_INTO_GROUP)
                        continue
                    }

                    val fc = formController.getCaptionPrompt()

                    if (forPicker) {
                        // Don't render other groups' instances.
                        val repeatGroupPickerRef: String =
                            formHierarchyViewModel.repeatGroupPickerIndex!!.reference
                                .toString(false)
                        if (currentRef.toString(false) != repeatGroupPickerRef) {
                            event = formController.stepToNextEvent(STEP_INTO_GROUP)
                            continue
                        }

                        val itemNumber = fc!!.multiplicity + 1

                        // e.g. `friends > 1`
                        var repeatLabel = fc.shortText + " > " + itemNumber

                        // If the child of the group has a more descriptive label, use that instead.
                        if (fc.formElement.children.size == 1 && fc.formElement.getChild(0) is GroupDef) {
                            formController.stepToNextEvent(STEP_INTO_GROUP)
                            val itemLabel = formController.getCaptionPrompt()!!.shortText
                            if (itemLabel != null) {
                                // e.g. `1. Alice`
                                repeatLabel = "$itemNumber.\u200E $itemLabel"
                            }
                        }

                        elementsToDisplay.add(
                            HierarchyItem(
                                fc.index,
                                HierarchyItemType.REPEAT_INSTANCE,
                                HtmlUtils.textToHtml(repeatLabel)
                            )
                        )
                    } else if (fc!!.multiplicity == 0) {
                        elementsToDisplay.add(
                            HierarchyItem(
                                fc.index,
                                HierarchyItemType.REPEATABLE_GROUP,
                                HtmlUtils.textToHtml(fc.shortText)
                            )
                        )
                    }
                }
            }

            event = formController.stepToNextEvent(STEP_INTO_GROUP)
        }

        formHierarchyViewModel.elementsToDisplay = elementsToDisplay
        println("5194c: calculateElementsToDisplay~")
    }

    /**
     * Goes to the start of the hierarchy view based on where the user came from.
     * Backs out until the index is at the beginning of a repeat group or the beginning of the form.
     */
    private fun jumpToHierarchyStartIndex() {
        println("5194c: jumpToHierarchyStartIndex")
        val startIndex = controller.getFormIndex()

        // If we're not at the first level, we're inside a repeated group so we want to only
        // display everything enclosed within that group.
        formHierarchyViewModel.contextGroupRef = null

        // Save the index to the screen itself, before potentially moving into it.
        formHierarchyViewModel.screenIndex = startIndex

        // If we're currently at a displayable group, record the name of the node and step to the next
        // node to display.
        if (controller.isDisplayableGroup(startIndex)) {
            formHierarchyViewModel.contextGroupRef = controller.getFormIndex()!!.reference
            controller.stepToNextEvent(STEP_INTO_GROUP)
        } else {
            var potentialStartIndex = FormIndexUtils.getPreviousLevel(startIndex)
            // Step back until we hit a displayable group or the beginning.
            while (!isScreenEvent(controller, potentialStartIndex)) {
                potentialStartIndex = FormIndexUtils.getPreviousLevel(potentialStartIndex)
            }

            formHierarchyViewModel.screenIndex = potentialStartIndex

            // Check to see if the question is at the first level of the hierarchy.
            // If it is, display the root level from the beginning.
            // Otherwise we're at a displayable group.
            if (formHierarchyViewModel.screenIndex == null) {
                formHierarchyViewModel.screenIndex = FormIndex.createBeginningOfFormIndex()
            }

            controller.jumpToIndex(formHierarchyViewModel.screenIndex)

            // Now test again. This should be true at this point or we're at the beginning.
            if (controller.isDisplayableGroup(controller.getFormIndex())) {
                formHierarchyViewModel.contextGroupRef = controller.getFormIndex()!!.reference
                controller.stepToNextEvent(STEP_INTO_GROUP)
            } else {
                // Let contextGroupRef be null.
            }
        }

        menuHost.invalidateMenu()
    }

    /**
     * Returns true if the event is a displayable group or the start of the form.
     * See [FormController.stepToOuterScreenEvent] for more context.
     */
    private fun isScreenEvent(
        formController: FormController,
        index: FormIndex?
    ): Boolean {
        println("5194c: isScreenEvent")
        // Beginning of form.
        if (index == null) {
            return true
        }

        return formController.isDisplayableGroup(index)
    }

    /**
     * Navigates "up" in the form hierarchy.
     */
    private fun goUpLevel() {
        println("5194c: goUpLevel")

        // If `repeatGroupPickerIndex` is set it means we're currently displaying
        // a list of repeat instances. If we unset `repeatGroupPickerIndex`,
        // we will go back up to the previous screen.
        if (formHierarchyViewModel.shouldShowRepeatGroupPicker(10)) {
            // Exit the picker.
            formHierarchyViewModel.repeatGroupPickerIndex = null
        } else {
            // Enter the picker if coming from a repeat group.
            val event = controller.getEvent(formHierarchyViewModel.screenIndex)
            if (event == FormEntryController.EVENT_REPEAT
                || event == FormEntryController.EVENT_PROMPT_NEW_REPEAT
            ) {
                formHierarchyViewModel.repeatGroupPickerIndex =
                    formHierarchyViewModel.screenIndex
            }

            controller.stepToOuterScreenEvent()
        }

        refreshView(true)
    }

    private val currentPath: CharSequence
        /**
         * Returns a string representing the 'path' of the current screen.
         * Each level is separated by `>`.
         */
        get() {
            println("5194c: currentPath")
            var index = formHierarchyViewModel.screenIndex

            val groups: MutableList<FormEntryCaption?> = ArrayList()

            if (formHierarchyViewModel.shouldShowRepeatGroupPicker(6)) {
                groups.add(controller.getCaptionPrompt(formHierarchyViewModel.repeatGroupPickerIndex))
            }

            while (index != null) {
                groups.add(0, controller.getCaptionPrompt(index))
                index = FormIndexUtils.getPreviousLevel(index)
            }

            // If the repeat picker is showing, don't show an item number for the current index.
            val hideLastMultiplicity =
                formHierarchyViewModel.shouldShowRepeatGroupPicker(7)

            return ODKView.getGroupsPath(
                groups.toTypedArray<FormEntryCaption?>(),
                hideLastMultiplicity
            )
        }

    /**
     * Handles clicks on a specific row in the hierarchy view.
     */
    private fun onElementClick(item: HierarchyItem) {
        println("5194c: onElementClick")
        val index = item.formIndex

        when (item.hierarchyItemType) {
            HierarchyItemType.QUESTION -> onQuestionClicked(index)
            HierarchyItemType.REPEATABLE_GROUP -> {
                // Show the picker.
                formHierarchyViewModel.repeatGroupPickerIndex = index
                refreshView()
            }

            HierarchyItemType.VISIBLE_GROUP, HierarchyItemType.REPEAT_INSTANCE -> {
                // Hide the picker.
                formHierarchyViewModel.repeatGroupPickerIndex = null
                controller.jumpToIndex(index)
                requireActivity().setResult(Activity.RESULT_OK)
                refreshView()
            }
        }
    }

    /**
     * Handles clicks on a question. Jumps to the form filling view with the selected question shown.
     * If the selected question is in a field list, show the entire field list.
     */
    private fun onQuestionClicked(index: FormIndex?) {
        println("5194c: onQuestionClicked")
        if (viewOnly) {
            return
        }

        controller.jumpToIndex(index)
        if (controller.indexIsInFieldList()) {
            try {
                controller.stepToPreviousScreenEvent()
            } catch (e: JavaRosaException) {
                Timber.d(e)
                createErrorDialog(e.cause!!.message)
                return
            }
        }
        requireActivity().setResult(Activity.RESULT_OK)
        requireActivity().finish()
    }

    /**
     * Creates and displays dialog with the given errorMsg.
     */
    private fun createErrorDialog(errorMsg: String?) {
        println("5194c: createErrorDialog")
        val alertDialog = MaterialAlertDialogBuilder(requireContext()).create()

        alertDialog.setTitle(getString(org.odk.collect.strings.R.string.error_occured))
        alertDialog.setMessage(errorMsg)
        val errorListener =
            DialogInterface.OnClickListener { _, i ->
                when (i) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        val formController = controller
                        formController.jumpToIndex(formHierarchyViewModel.currentIndex)
                    }
                }
            }
        alertDialog.setCancelable(false)
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            getString(org.odk.collect.strings.R.string.ok),
            errorListener
        )
        alertDialog.show()
    }

    private val isDisplayingSingleGroup: Boolean
        /**
         * Returns true if there's only one item being displayed, and it's a group.
         * Groups like this are often used to display a label in the hierarchy path.
         */
        get() {
            println("5194c: isDisplayingSingleGroup")
            return (formHierarchyViewModel.elementsToDisplay?.size == 1
                    && formHierarchyViewModel.elementsToDisplay!![0]
                .hierarchyItemType == HierarchyItemType.VISIBLE_GROUP)
        }

    private fun configureButtons(
        binding: FormHierarchyLayoutBinding,
        formController: FormController
    ) {
        println("5194c: configureButtons")
        val exitButton: Button = binding.exitButton
        val jumpBeginningButton: Button = binding.jumpBeginningButton
        val jumpEndButton: Button = binding.jumpEndButton

        if (viewOnly) {
            exitButton.setOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            exitButton.visibility = View.VISIBLE
            jumpBeginningButton.visibility = View.GONE
            jumpEndButton.visibility = View.GONE
        } else {
            jumpBeginningButton.setOnClickListener {
                formController.getAuditEventLogger()!!.flush()
                formController.jumpToIndex(FormIndex.createBeginningOfFormIndex())

                requireActivity().setResult(Activity.RESULT_OK)
                requireActivity().finish()
            }

            jumpEndButton.setOnClickListener {
                formController.getAuditEventLogger()!!.flush()
                formController.jumpToIndex(FormIndex.createEndOfFormIndex())

                requireActivity().setResult(Activity.RESULT_OK)
                requireActivity().finish()
            }
        }
    }

    /**
     * After having deleted the current index,
     * returns true if the current index was the only item in the repeat group.
     */
    private fun didDeleteLastRepeatItem(): Boolean {
        println("5194c: didDeleteLastRepeatItem")
        val index = controller.getFormIndex()
        val event = controller.getEvent(index)

        // If we're on item 0, but we will be prompted to add another item next,
        // it must be the last remaining item.
        return event == FormEntryController.EVENT_PROMPT_NEW_REPEAT
                && index!!.elementMultiplicity == 0
    }

    private fun didDeleteFirstRepeatItem(): Boolean {
        println("5194c: didDeleteFirstRepeatItem")
        return controller.getFormIndex()
            ?.elementMultiplicity == 0
    }

    private fun onRepeatDeleted() {
        println("5194c: onRepeatDeleted")
        if (didDeleteLastRepeatItem()) {
            // goUpLevel would put us in a weird state after deleting the last item;
            // just go back one event instead.
            //
            // TODO: This works well in most cases, but if there are 2 repeats in a row,
            //   and you delete an item from the second repeat, it will send you into the
            //   first repeat instead of going back a level as expected.
            goToPreviousEvent()
        } else if (didDeleteFirstRepeatItem()) {
            goUpLevel()
        } else {
            goToPreviousEvent()
            goUpLevel()
        }
    }

    /**
     * Similar to [.goUpLevel], but makes a less significant step backward.
     * This is only used when the caller knows where to go back to,
     * e.g. after deleting the final remaining item in a repeat group.
     */
    private fun goToPreviousEvent() {
        println("5194c: goToPreviousEvent")
        try {
            controller.stepToPreviousScreenEvent()
        } catch (e: JavaRosaException) {
            Timber.d(e)
            createErrorDialog(e.cause!!.message)
            return
        }

        refreshView()
    }

    private fun navigateToTheLastRelevantIndex(formController: FormController) {
        println("5194c: navigateToTheLastRelevantIndex")
        val fec = FormEntryController(FormEntryModel(formController.getFormDef()))
        formController.jumpToIndex(startIndex)

        // startIndex might no longer exist if it was a part of repeat group that has been removed
        while (true) {
            val isBeginningOfFormIndex = formController.getFormIndex()!!.isBeginningOfFormIndex
            val isEndOfFormIndex = formController.getFormIndex()!!.isEndOfFormIndex
            val isIndexRelevant = isBeginningOfFormIndex
                    || isEndOfFormIndex
                    || fec.model.isIndexRelevant(formController.getFormIndex())
            val isPromptNewRepeatEvent =
                formController.getEvent() == FormEntryController.EVENT_PROMPT_NEW_REPEAT

            val shouldNavigateBack = !isIndexRelevant || isPromptNewRepeatEvent

            if (shouldNavigateBack) {
                formController.stepToPreviousEvent()
            } else {
                break
            }
        }
    }

    private class FormHiearchyMenuProvider(
        private val formEntryViewModel: FormEntryViewModel,
        private val formHierarchyViewModel: FormHierarchyViewModel,
        private val viewOnly: Boolean,
        private val onClickListener: OnClickListener
    ) :
        MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            println("5194c: onCreateMenu")
            menuInflater.inflate(R.menu.form_hierarchy_menu, menu)
        }

        override fun onPrepareMenu(menu: Menu) {
            println("5194c: onPrepareMenu")
            val screenIndex = formHierarchyViewModel.screenIndex
            val repeatGroupPickerIndex = formHierarchyViewModel.repeatGroupPickerIndex

            val isAtBeginning = screenIndex!!.isBeginningOfFormIndex
                    && !formHierarchyViewModel.shouldShowRepeatGroupPicker(0)
            val shouldShowPicker = formHierarchyViewModel.shouldShowRepeatGroupPicker(9)
            val formController = formEntryViewModel.formController as JavaRosaFormController
            val isInRepeat = formController.indexContainsRepeatableGroup(screenIndex)
            val uniqueRepeat = isInRepeat && formController.isRepeatUnique(screenIndex)
            val isGroupSizeLocked =
                if (shouldShowPicker) isGroupSizeLocked(repeatGroupPickerIndex)
                else isGroupSizeLocked(screenIndex)
            val delete = isInRepeat && !shouldShowPicker && !isGroupSizeLocked && !viewOnly

            menu.findItem(R.id.menu_add_repeat).setVisible(
                shouldShowPicker && !isGroupSizeLocked && !viewOnly
            )
            menu.findItem(R.id.menu_delete_child).setVisible(
                delete && !uniqueRepeat
            )
            menu.findItem(R.id.menu_go_up).setVisible(!isAtBeginning)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            println("5194c: onMenuItemSelected")
            when (menuItem.itemId) {
                R.id.menu_delete_child -> {
                    onClickListener.onDeleteRepeatClicked()
                    return true
                }

                R.id.menu_add_repeat -> {
                    onClickListener.onAddRepeatClicked()
                    return true
                }

                R.id.menu_go_up -> {
                    onClickListener.onGoUpClicked()
                    return true
                }

                else -> {
                    return false
                }
            }
        }

        fun isGroupSizeLocked(index: FormIndex?): Boolean {
            println("5194c: isGroupSizeLocked")
            val formController = formEntryViewModel.formController
            val element = formController!!.getCaptionPrompt(index)!!.formElement
            return element is GroupDef && element.noAddRemove
        }

        interface OnClickListener {
            fun onGoUpClicked()

            fun onAddRepeatClicked()

            fun onDeleteRepeatClicked()
        }
    }

    private class FormHierarchyViewModel : ViewModel() {
        var contextGroupRef: TreeReference? = null
        var screenIndex: FormIndex? = null
        var repeatGroupPickerIndex: FormIndex? = null
        var currentIndex: FormIndex? = null
        var elementsToDisplay: List<HierarchyItem>? = null
        var startIndex: FormIndex? = null

        fun shouldShowRepeatGroupPicker(count: Int): Boolean {
            println("5194c: shouldShowRepeatGroupPicker $count")
            return repeatGroupPickerIndex != null
        }
    }
}


