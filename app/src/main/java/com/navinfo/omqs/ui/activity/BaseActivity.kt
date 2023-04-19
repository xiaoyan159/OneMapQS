package com.navinfo.omqs.ui.activity

import android.app.Dialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 基类
 */
open class BaseActivity : AppCompatActivity() {
    private var loadingDialog: AlertDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE//横屏
        super.onCreate(savedInstanceState)
    }

    /**
     * 显示loading dialog
     */
    fun showLoadingDialog(message: String) {
        loadingDialog?.dismiss()
        loadingDialog = MaterialAlertDialogBuilder(
            this@BaseActivity, R.style.MaterialAlertDialog_Material3).setMessage(message).setCancelable(false).show()
    }

    /**
     * 隐藏loading dialog
     * */
    fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
}