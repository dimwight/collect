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

package org.odk.collect.android.tasks;

import android.net.Uri;
import android.os.AsyncTask;

import androidx.core.util.Pair;

import org.odk.collect.android.formmanagement.ServerFormDetails;
import org.odk.collect.android.formmanagement.ServerFormsDetailsFetcher;
import org.odk.collect.android.listeners.FormListDownloaderListener;
import org.odk.collect.android.utilities.WebCredentialsUtils;
import org.odk.collect.forms.FormSourceException;

import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

/**
 * Background task for downloading forms from urls or a formlist from a url. We overload this task
 * a bit so that we don't have to keep track of two separate downloading tasks and it simplifies
 * interfaces. If LIST_URL is passed to doInBackground(), we fetch a form list. If a hashmap
 * containing form/url pairs is passed, we download those forms.
 *
 * @author carlhartung
 */
public class DownloadFormListTask extends AsyncTask<Void, String, Pair<List<ServerFormDetails>, FormSourceException>> {

    private final ServerFormsDetailsFetcher serverFormsDetailsFetcher;

    private FormListDownloaderListener stateListener;
    private WebCredentialsUtils webCredentialsUtils;
    private String url;
    private String username;
    private String password;

    public DownloadFormListTask(ServerFormsDetailsFetcher serverFormsDetailsFetcher) {
        Timber.i("5358_I DownloadFormListTask %s", 50);
        this.serverFormsDetailsFetcher = serverFormsDetailsFetcher;
    }

    @Override
    protected Pair<List<ServerFormDetails>, FormSourceException> doInBackground(Void... values) {
        Timber.i("5358_I doInBackground %s", 58);
        if (webCredentialsUtils != null) {
            setTemporaryCredentials();
        }

        List<ServerFormDetails> formList = null;
        FormSourceException exception = null;

        try {
            formList = serverFormsDetailsFetcher.fetchFormDetails();
        } catch (FormSourceException e) {
            exception = e;
        } finally {
            if (webCredentialsUtils != null) {
                clearTemporaryCredentials();
            }
        }

        Timber.i("5358_I doInBackground- %s", 77);
        return new Pair<>(formList, exception);
    }

    @Override
    protected void onPostExecute(Pair<List<ServerFormDetails>, FormSourceException> result) {
        synchronized (this) {
            Timber.i("5358_I onPostExecute %s", 81);
            if (stateListener != null) {
                if (result.first != null) {
                    HashMap<String, ServerFormDetails> detailsHashMap = new HashMap<>();
                    for (ServerFormDetails details : result.first) {
                        detailsHashMap.put(details.getFormId(), details);
                    }

                    stateListener.formListDownloadingComplete(detailsHashMap, result.second);
                } else {
                    stateListener.formListDownloadingComplete(null, result.second);
                }
            }
            Timber.i("5358_I onPostExecute- %s", 95);
        }
    }

    public void setDownloaderListener(FormListDownloaderListener sl) {
        synchronized (this) {
            stateListener = sl;
        }
    }

    public void setAlternateCredentials(WebCredentialsUtils webCredentialsUtils, String url, String username, String password) {
        this.webCredentialsUtils = webCredentialsUtils;
        serverFormsDetailsFetcher.updateCredentials(webCredentialsUtils);

        this.url = url;
        if (url != null && !url.isEmpty()) {
            serverFormsDetailsFetcher.updateUrl(url);
        }

        this.username = username;
        this.password = password;
    }

    private void setTemporaryCredentials() {
        if (url != null) {
            String host = Uri.parse(url).getHost();

            if (host != null) {
                if (username != null && password != null) {
                    webCredentialsUtils.saveCredentials(url, username, password);
                } else {
                    webCredentialsUtils.clearCredentials(url);
                }
            }
        }
    }

    private void clearTemporaryCredentials() {
        if (url != null) {
            String host = Uri.parse(url).getHost();

            if (host != null) {
                webCredentialsUtils.clearCredentials(url);
            }
        }
    }
}
