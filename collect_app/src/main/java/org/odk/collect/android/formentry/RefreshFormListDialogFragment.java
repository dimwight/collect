package org.odk.collect.android.formentry;

import android.content.Context;

import androidx.annotation.NonNull;

import org.odk.collect.material.MaterialProgressDialogFragment;

import timber.log.Timber;

public class RefreshFormListDialogFragment extends MaterialProgressDialogFragment {

    protected RefreshFormListDialogFragmentListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        Timber.i("5358_D onAttach %s", 14);
        super.onAttach(context);

        if (context instanceof RefreshFormListDialogFragmentListener) {
            listener = (RefreshFormListDialogFragmentListener) context;
        }
        setTitle(getString(org.odk.collect.strings.R.string.downloading_data));
        setMessage(getString(org.odk.collect.strings.R.string.please_wait));
        setCancelable(false);
    }

    @Override
    protected String getCancelButtonText() {
        Timber.i("5358_D getCancelButtonText %s", 29);
        return getString(org.odk.collect.strings.R.string.cancel_loading_form);
    }

    @Override
    protected OnCancelCallback getOnCancelCallback() {
        Timber.i("5358_D getOnCancelCallback %s", 35);
        return () -> {
            listener.onCancelFormLoading();
            dismiss();
            return true;
        };
    }

    public interface RefreshFormListDialogFragmentListener {
            void onCancelFormLoading();
    }
}
