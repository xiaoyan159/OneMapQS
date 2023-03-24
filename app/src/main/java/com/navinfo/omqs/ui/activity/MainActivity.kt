package com.navinfo.omqs.ui.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.ActivityMainBinding

/**
 * 地图主页面
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        viewModel.initMap(this, binding.mainActivityMap)
        lifecycle.addObserver(viewModel)
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
    }

}