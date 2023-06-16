package com.navinfo.omqs.ui.activity.login

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.ActivityLoginBinding
import com.navinfo.omqs.ui.activity.CheckPermissionsActivity
import com.navinfo.omqs.ui.activity.map.MainActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * 登陆页面
 */

@AndroidEntryPoint
class LoginActivity : CheckPermissionsActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel by viewModels<LoginViewModel>()
    private var loginDialog: AlertDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.loginUserModel = viewModel
        binding.lifecycleOwner = this
        binding.activity = this
        initView()
        Log.e("jingo", getScreenParams())
    }

    private fun getScreenParams(): String {
        val dm = DisplayMetrics();
        windowManager.defaultDisplay.getMetrics(dm);
        val heightPixels = dm.heightPixels;//高的像素
        val widthPixels = dm.widthPixels;//宽的像素
        val densityDpi = dm.densityDpi;//dpi
        val xdpi = dm.xdpi;//xdpi
        val ydpi = dm.ydpi;//ydpi
        val density = dm.density;//density=dpi/160,密度比
        val scaledDensity = dm.scaledDensity;//scaledDensity=dpi/160 字体缩放密度比
        val heightDP = heightPixels / density;//高度的dp
        val widthDP = widthPixels / density;//宽度的dp
        var str = "heightPixels: " + heightPixels + "px";
        str += "\nwidthPixels: " + widthPixels + "px";
        str += "\ndensityDpi: " + densityDpi + "dpi";
        str += "\nxdpi: " + xdpi + "dpi";
        str += "\nydpi: " + ydpi + "dpi";
        str += "\ndensity: " + density;
        str += "\nscaledDensity: " + scaledDensity;
        str += "\nheightDP: " + heightDP + "dp";
        str += "\nwidthDP: " + widthDP + "dp";

        return str;
    }


    /**
     * 观察登录状态，把Observer提出来是为了防止每次数据变化都会有新的observer创建
     * 还有为了方便释放（需不需要手动释放？不清楚）不需要释放，当viewmodel观察到activity/fragment 的生命周期时会自动释放
     * PS:不要在 observer 中修改 LiveData 的值的数据，会影响其他 observer
     */
    private val loginObserve = Observer<LoginStatus> {
        when (it) {
            LoginStatus.LOGIN_STATUS_NET_LOADING -> {
                loginDialog("验证用户信息...")
            }
            LoginStatus.LOGIN_STATUS_FOLDER_INIT -> {
                loginDialog("检查本地数据...")
            }
            LoginStatus.LOGIN_STATUS_FOLDER_FAILURE -> {
                Toast.makeText(this, "文件夹初始化失败", Toast.LENGTH_SHORT).show()
                loginDialog?.dismiss()
                loginDialog = null
            }
            LoginStatus.LOGIN_STATUS_NET_FAILURE -> {
                Toast.makeText(this, "网络访问失败", Toast.LENGTH_SHORT).show()
                loginDialog?.dismiss()
                loginDialog = null
            }
            LoginStatus.LOGIN_STATUS_SUCCESS -> {
                loginDialog?.dismiss()
                loginDialog = null
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            LoginStatus.LOGIN_STATUS_CANCEL -> {
                loginDialog?.dismiss()
                loginDialog = null
            }
            LoginStatus.LOGIN_STATUS_NET_OFFLINE_MAP -> {
                loginDialog("检查离线地图...")
            }
        }
    }

    private fun initView() {
        //登录校验，初始化成功
        viewModel.loginStatus.observe(this, loginObserve)

    }

    /**
     * 登录dialog
     */
    private fun loginDialog(message: String) {
        if (loginDialog == null) {
            loginDialog = MaterialAlertDialogBuilder(
                this, com.google.android.material.R.style.MaterialAlertDialog_Material3
            ).setTitle("登录").setMessage(message).show()
            loginDialog!!.setCanceledOnTouchOutside(false)
            loginDialog!!.setOnCancelListener {
                viewModel.cancelLogin()
            }
        } else {
            loginDialog!!.setMessage(message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * 处理登录按钮
     */
    fun onClickLoginButton() {
        viewModel.onClickLoginBtn(
            this, binding.loginUsername.text.toString(), binding.loginPassword.text.toString()
        )
    }
}
