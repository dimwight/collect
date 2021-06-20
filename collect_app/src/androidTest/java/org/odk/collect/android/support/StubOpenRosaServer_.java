package org.odk.collect.android.support;

import android.content.res.AssetManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jetbrains.annotations.NotNull;
import org.odk.collect.android.openrosa.CaseInsensitiveEmptyHeaders;
import org.odk.collect.android.openrosa.CaseInsensitiveHeaders;
import org.odk.collect.android.openrosa.HttpCredentialsInterface;
import org.odk.collect.android.openrosa.HttpGetResult;
import org.odk.collect.android.openrosa.HttpHeadResult;
import org.odk.collect.android.openrosa.HttpPostResult;
import org.odk.collect.android.openrosa.OpenRosaHttpInterface;
import org.odk.collect.shared.strings.Md5;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

import static java.util.Arrays.asList;

final public class StubOpenRosaServer_ extends StubOpenRosaServer {

    private static final String HOST = "server.example.com";

    private String formListPath = "/formList";
    private String submissionPath = "/submission";

    private String username;
    private String password;
    private boolean alwaysReturnError;
    private boolean fetchingFormsError;
    private boolean noHashInFormList;
    private boolean noHttpPostResult;
    private File submissionFile;

    public void setNoHttpPostResult(boolean on) {
        noHttpPostResult=on;
    }

    @NotNull
    private HttpPostResult newErrorResult() {
        return new HttpPostResult("", 500, "");
    }


   }
