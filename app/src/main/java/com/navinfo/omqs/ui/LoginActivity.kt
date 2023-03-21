package com.navinfo.omqs.ui

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.viewModels
import com.navinfo.omqs.databinding.ActivityLoginBinding

class LoginActivity : PermissionsActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel by viewModels<LoginActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
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