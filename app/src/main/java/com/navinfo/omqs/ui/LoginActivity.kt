package com.navinfo.omqs.ui

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.ActivityLoginBinding

class LoginActivity : PermissionsActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel by viewModels<LoginActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.activity_login, null, false)
        binding.lifecycleOwner = this
        setContentView(binding.root)
    }

    override fun onPermissionsGranted() {
        initView()
    }

    private fun initView() {

    }

    override fun onPermissionsDenied() {
    }

}