package com.navinfo.omqs.ui.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
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
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
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