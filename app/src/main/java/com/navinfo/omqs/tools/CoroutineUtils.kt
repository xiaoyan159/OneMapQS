package com.navinfo.omqs.tools
import android.app.ProgressDialog
import android.content.Context
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

object CoroutineUtils {
    fun <T> launchWithLoading(
        context: Context,
        coroutineContext: CoroutineContext = Dispatchers.Main,
        loadingMessage: String? = null,
        task: suspend CoroutineScope.() -> T
    ): Job {
        val progressDialog = MaterialAlertDialogBuilder(
            context, R.style.MaterialAlertDialog_Material3).setMessage(loadingMessage).setCancelable(false).show()

        return CoroutineScope(coroutineContext).launch {
            try {
                withContext(Dispatchers.IO) {
                    task.invoke(this)
                }
            } finally {
                progressDialog.dismiss()
            }
        }
    }
}