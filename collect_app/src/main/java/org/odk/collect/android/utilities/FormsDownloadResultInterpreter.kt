package org.odk.collect.android.utilities

import android.content.Context
import org.odk.collect.android.formmanagement.FormDownloadException
import org.odk.collect.android.formmanagement.FormDownloadExceptionMapper
import org.odk.collect.android.formmanagement.ServerFormDetails
import org.odk.collect.errors.ErrorItem
import org.odk.collect.strings.localization.getLocalizedString

object FormsDownloadResultInterpreter {
    private const val FAILURE_5358 = false
    fun getFailures(result: Map<ServerFormDetails, FormDownloadException?>, context: Context) = result.filter {
        FAILURE_5358 || it.value != null
    }.map {
        val errorItem = if (FAILURE_5358) {
            ErrorItem(
                "formName",
                context.getLocalizedString(
                    org.odk.collect.strings.R.string.form_details,
                    "formId",
                    "formVersion"
                ),
                "getMessage"
            )
        } else
            ErrorItem(
                it.key.formName ?: "",
                context.getLocalizedString(
                    org.odk.collect.strings.R.string.form_details,
                    it.key.formId ?: "",
                    it.key.formVersion ?: ""
                ),
                FormDownloadExceptionMapper(context).getMessage(it.value)
            )
        errorItem
    }

    fun getNumberOfFailures(result: Map<ServerFormDetails, FormDownloadException?>) = result.count {
        FAILURE_5358 || it.value != null
    }

    fun allFormsDownloadedSuccessfully(result: Map<ServerFormDetails, FormDownloadException?>): Boolean {
        return !FAILURE_5358 &&
                result.values.all {
                    it == null
                }
    }
}
