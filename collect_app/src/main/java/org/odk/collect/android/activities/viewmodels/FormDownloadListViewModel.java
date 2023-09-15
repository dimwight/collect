/*
 * Copyright 2019 Nafundi
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

package org.odk.collect.android.activities.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.odk.collect.android.formmanagement.ServerFormDetails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;

import timber.log.Timber;

public class FormDownloadListViewModel extends ViewModel {

    private HashMap<String, ServerFormDetails> formDetailsByFormId = new HashMap<>();

    /**
     * List of forms from the formList response. The map acts like a DisplayableForm object with
     * values for each component that shows up in the form list UI. See
     * FormDownloadListActivity.formListDownloadingComplete for keys.
     */
    private final ArrayList<HashMap<String, String>> formList = new ArrayList<>();

    private final LinkedHashSet<String> selectedFormIds = new LinkedHashSet<>();

    private String alertTitle;
    private String alertDialogMsg;

    private boolean alertShowing;
    private boolean cancelDialogShowing;
    private boolean shouldExit;
    private boolean loadingCanceled;

    // Variables used when the activity is called from an external app
    private boolean isDownloadOnlyMode;
    private String[] formIdsToDownload;
    private String url;
    private String username;
    private String password;
    private final HashMap<String, Boolean> formResults = new HashMap<>();

    public HashMap<String, ServerFormDetails> getFormDetailsByFormId() {
        Timber.i("5358_C getFormDetailsByFormId %s", 60);
        return formDetailsByFormId;
    }

    public void setFormDetailsByFormId(HashMap<String, ServerFormDetails> formDetailsByFormId) {
        Timber.i("5358_C setFormDetailsByFormId %s", 67);
        this.formDetailsByFormId = formDetailsByFormId;
    }

    public void clearFormDetailsByFormId() {
        Timber.i("5358_C clearFormDetailsByFormId %s", 71);
        formDetailsByFormId.clear();
    }

    public String getAlertTitle() {
        Timber.i("5358_C getAlertTitle %s", 76);
        return alertTitle;
    }

    public void setAlertTitle(String alertTitle) {
        Timber.i("5358_C setAlertTitle %s", 81);
        this.alertTitle = alertTitle;
    }

    public String getAlertDialogMsg() {
        Timber.i("5358_C getAlertDialogMsg %s", 86);
        return alertDialogMsg;
    }

    public void setAlertDialogMsg(String alertDialogMsg) {
        Timber.i("5358_C setAlertDialogMsg %s", 91);
        this.alertDialogMsg = alertDialogMsg;
    }

    public boolean isAlertShowing() {
        Timber.i("5358_C isAlertShowing %s", 96);
        return alertShowing;
    }

    public void setAlertShowing(boolean alertShowing) {
        Timber.i("5358_C setAlertShowing %s", 101);
        this.alertShowing = alertShowing;
    }

    public boolean shouldExit() {
        Timber.i("5358_C shouldExit %s", 106);
        return shouldExit;
    }

    public void setShouldExit(boolean shouldExit) {
        Timber.i("5358_C setShouldExit %s", 111);
        this.shouldExit = shouldExit;
    }

    public ArrayList<HashMap<String, String>> getFormList() {
//        Timber.i("5358_C getFormList %s", 116);
        Timber.i("5358_C getFormList: %s", formList.hashCode() & 0xffff);
        return formList;
    }

    public void clearFormList() {
        Timber.i("5358_C clearFormList %s", 121);
        formList.clear();
    }

    public void addForm(HashMap<String, String> item) {
        Timber.i("5358_C addForm %s", 126);
        formList.add(item);
    }

    public void addForm(int index, HashMap<String, String> item) {
        Timber.i("5358_C addForm %s", 131);
        formList.add(index, item);
    }

    public LinkedHashSet<String> getSelectedFormIds() {
        Timber.i("5358_C getSelectedFormIds %s", 136);
        return selectedFormIds;
    }

    public void addSelectedFormId(String selectedFormId) {
        Timber.i("5358_C addSelectedFormId %s", 141);
        selectedFormIds.add(selectedFormId);
    }

    public void removeSelectedFormId(String selectedFormId) {
        Timber.i("5358_C removeSelectedFormId %s", 146);
        selectedFormIds.remove(selectedFormId);
    }

    public void clearSelectedFormIds() {
        Timber.i("5358_C clearSelectedFormIds %s", 151);
        selectedFormIds.clear();
    }

    public boolean isDownloadOnlyMode() {
        Timber.i("5358_C isDownloadOnlyMode %s", 156);
        return isDownloadOnlyMode;
    }

    public void setDownloadOnlyMode(boolean downloadOnlyMode) {
        Timber.i("5358_C setDownloadOnlyMode %s", 161);
        isDownloadOnlyMode = downloadOnlyMode;
    }

    public HashMap<String, Boolean> getFormResults() {
        Timber.i("5358_C getFormResults %s", 166);
        return formResults;
    }

    public void putFormResult(String formId, boolean result) {
        Timber.i("5358_C putFormResult %s", 171);
        formResults.put(formId, result);
    }

    public String getPassword() {
        Timber.i("5358_C getPassword %s", 176);
        return password;
    }

    public void setPassword(String password) {
        Timber.i("5358_C setPassword %s", 181);
        this.password = password;
    }

    public String getUsername() {
        Timber.i("5358_C getUsername %s", 186);
        return username;
    }

    public void setUsername(String username) {
        Timber.i("5358_C setUsername %s", 191);
        this.username = username;
    }

    public String getUrl() {
        Timber.i("5358_C getUrl %s", 196);
        return url;
    }

    public void setUrl(String url) {
        Timber.i("5358_C setUrl %s", 201);
        this.url = url;
    }

    public String[] getFormIdsToDownload() {
        Timber.i("5358_C getFormIdsToDownload %s", 206);
        return Arrays.copyOf(formIdsToDownload, formIdsToDownload.length);
    }

    public void setFormIdsToDownload(String[] formIdsToDownload) {
        Timber.i("5358_C setFormIdsToDownload %s", 211);
        this.formIdsToDownload = formIdsToDownload;
    }

    public boolean isCancelDialogShowing() {
        Timber.i("5358_C isCancelDialogShowing %s", 216);
        return cancelDialogShowing;
    }

    public void setCancelDialogShowing(boolean cancelDialogShowing) {
        Timber.i("5358_C setCancelDialogShowing %s", 221);
        this.cancelDialogShowing = cancelDialogShowing;
    }

    public boolean wasLoadingCanceled() {
        Timber.i("5358_C wasLoadingCanceled %s", 226);
        return loadingCanceled;
    }

    public void setLoadingCanceled(boolean loadingCanceled) {
        Timber.i("5358_C setLoadingCanceled %s", 231);
        this.loadingCanceled = loadingCanceled;
    }

    public static class Factory implements ViewModelProvider.Factory {

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            Timber.i("5358_C create %s", 242);
            return (T) new FormDownloadListViewModel();
        }
    }
}
