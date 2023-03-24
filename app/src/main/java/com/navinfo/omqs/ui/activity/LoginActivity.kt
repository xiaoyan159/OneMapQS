package com.navinfo.omqs.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.ActivityLoginBinding

/**
 * 登陆页面
 */
class LoginActivity : PermissionsActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel by viewModels<LoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.loginUserModel = viewModel
        binding.lifecycleOwner = this
        binding.activity = this
    }

    override fun onPermissionsGranted() {
    }

    override fun onPermissionsDenied() {
    }

    /**
     * 处理登录按钮
     */
    fun onClickLoginButton() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}