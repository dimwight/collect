package org.odk.collect.android.formentry;

import static org.javarosa.form.api.FormEntryController.EVENT_BEGINNING_OF_FORM;
import static org.odk.collect.android.javarosawrapper.FormIndexUtils.getRepeatGroupIndex;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.actions.recordaudio.RecordAudioActionHandler;
import org.jetbrains.annotations.NotNull;
import org.odk.collect.android.analytics.AnalyticsEvents;
import org.odk.collect.android.analytics.AnalyticsUtils;
import org.odk.collect.android.exception.JavaRosaException;
import org.odk.collect.android.formentry.audit.AuditEvent;
import org.odk.collect.android.javarosawrapper.FormController;
import org.odk.collect.androidshared.livedata.MutableNonNullLiveData;
import org.odk.collect.androidshared.livedata.NonNullLiveData;
import org.odk.collect.utilities.Clock;

import java.util.Objects;

public class FormEntryViewModel extends ViewModel implements RequiresFormController {

    private final Clock clock;

    private final MutableLiveData<FormError> error = new MutableLiveData<>(null);
    private final MutableNonNullLiveData<Boolean> hasBackgroundRecording = new MutableNonNullLiveData<>(false);

    @Nullable
    private FormController formController;

    @Nullable
    private FormIndex jumpBackIndex;

    @SuppressWarnings("WeakerAccess")
    public FormEntryViewModel(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void formLoaded(@NotNull FormController formController) {
        this.formController = formController;

        boolean hasBackgroundRecording = formController.getFormDef().hasAction(RecordAudioActionHandler.ELEMENT_NAME);
        this.hasBackgroundRecording.setValue(hasBackgroundRecording);

        if (hasBackgroundRecording) {
            AnalyticsUtils.logFormEvent(AnalyticsEvents.REQUESTS_BACKGROUND_AUDIO);
        }
    }

    public boolean isFormControllerSet() {
        return formController != null;
    }

    @Nullable
    public FormIndex getCurrentIndex() {
        if (formController != null) {
            return formController.getFormIndex();
        } else {
            return null;
        }
    }

    public LiveData<FormError> getError() {
        return error;
    }

    @SuppressWarnings("WeakerAccess")
    public void promptForNewRepeat() {
        if (formController == null) {
            return;
        }

        jumpBackIndex = formController.getFormIndex();
        jumpToNewRepeat();
    }

    public void jumpToNewRepeat() {
        formController.jumpToNewRepeatPrompt();
    }

    public void addRepeat() {
        if (formController == null) {
            return;
        }

        jumpBackIndex = null;

        try {
            formController.newRepeat();
        } catch (RuntimeException e) {
            error.setValue(new NonFatal(e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }

        if (!formController.indexIsInFieldList()) {
            try {
                formController.stepToNextScreenEvent();
            } catch (JavaRosaException e) {
                error.setValue(new NonFatal(e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }
        }
    }

    public void cancelRepeatPrompt() {
        if (formController == null) {
            return;
        }

        if (jumpBackIndex != null) {
            formController.jumpToIndex(jumpBackIndex);
            jumpBackIndex = null;
        } else {
            try {
                this.formController.stepToNextScreenEvent();
            } catch (JavaRosaException exception) {
                error.setValue(new NonFatal(exception.getCause().getMessage()));
            }
        }
    }

    public void errorDisplayed() {
        error.setValue(null);
    }

    public boolean canAddRepeat() {
        if (formController != null && formController.indexContainsRepeatableGroup()) {
            FormDef formDef = formController.getFormDef();
            FormIndex repeatGroupIndex = getRepeatGroupIndex(formController.getFormIndex(), formDef);
            return !((GroupDef) formDef.getChild(repeatGroupIndex)).noAddRemove;
        } else {
            return false;
        }
    }

    public void moveForward() {
        try {
            formController.stepToNextScreenEvent();
        } catch (JavaRosaException e) {
            error.setValue(new NonFatal(e.getCause().getMessage()));
            return;
        }

        formController.getAuditEventLogger().flush(); // Close events waiting for an end time
    }

    public void moveBackward() {
         try {
             //Never scan past current index
             FormIndex activeStopIndex = formController.getFormIndex();
             //Back up to previous screen
             int event = formController.stepToPreviousScreenEvent();
             // If we are the beginning of the form we need to move forward
             // to the first actual screen - most unlikely?
             if (event == EVENT_BEGINNING_OF_FORM) {
                 formController.stepToNextScreenEvent();
                 //#3027 Field lists?
             } else if (true ||
                     !formController.indexIsInFieldList()) {
                 return;
             }
             //Record start of preceding list
             FormIndex listBeforeIndex = formController.getFormIndex();
             //Scan to last question in list
             FormIndex activeIndex = null;
             String _activeIndex;
             while (formController.indexIsInFieldList()) {
                 activeIndex = formController.getFormIndex();
                 _activeIndex = activeIndex.toString();
                 formController.stepToNextEvent(true);
                 //Don't overshoot starting point
                 FormIndex formIndex = formController.getFormIndex();
                 if (formIndex.equals(activeStopIndex)) {
                     break;
                 }
             }
             //Record for focus setting
             formController.setFieldListActiveIndex(activeIndex);
             //Ready to display preceding list
             formController.jumpToIndex(listBeforeIndex);
         } catch (JavaRosaException e) {
            error.setValue(new NonFatal(e.getCause().getMessage()));
            return;
        }

        formController.getAuditEventLogger().flush(); // Close events waiting for an end time
    }

    public void openHierarchy() {
        formController.getAuditEventLogger().logEvent(AuditEvent.AuditEventType.HIERARCHY, true, clock.getCurrentTime());
    }

    public void logFormEvent(String event) {
        AnalyticsUtils.logFormEvent(event);
    }

    public NonNullLiveData<Boolean> hasBackgroundRecording() {
        return hasBackgroundRecording;
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Clock clock;

        public Factory(Clock clock) {
            this.clock = clock;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new FormEntryViewModel(clock);
        }
    }

    public abstract static class FormError {

    }

    public static class NonFatal extends FormError {

        private final String message;

        public NonFatal(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            NonFatal nonFatal = (NonFatal) o;
            return Objects.equals(message, nonFatal.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(message);
        }
    }
}
