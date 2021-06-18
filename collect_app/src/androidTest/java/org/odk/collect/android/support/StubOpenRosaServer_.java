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

    private boolean noHttpPostResult;
    private File submissionFile;

    public void setNoHttpPostResult(boolean on) {
        noHttpPostResult=on;
    }

    @NotNull
    private HttpPostResult newErrorResult() {
        return new HttpPostResult("", 500, "");
    }

    @NonNull
    @Override
    public HttpPostResult uploadSubmissionAndFiles(@NonNull File submissionFile,
                                                   @NonNull List<File> fileList,
                                                   @NonNull URI uri,
                                                   @Nullable HttpCredentialsInterface credentials,
                                                   @NonNull long contentLength) throws Exception {
        if(noHttpPostResult){
            this.submissionFile = submissionFile;
            int timeOutMs=1000;
            int timeOuts=60;
            Timber.i("sleeping for %s sec",timeOutMs*timeOuts/1000);
            for(int timeOut=1;timeOut<=timeOuts;timeOut++) {
                Thread.sleep(timeOutMs);
                Timber.i("slept for %s ms",timeOut* timeOutMs);
            }
        }else if(this.submissionFile.equals(submissionFile)){
            return newErrorResult();
        }
        return super.uploadSubmissionAndFiles(submissionFile,fileList,uri,credentials,contentLength);

    }

}
