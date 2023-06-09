package com.navinfo.omqs.ui.activity.console

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.transition.AutoTransition
import androidx.transition.Scene
import androidx.transition.TransitionManager
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.ActivityConsoleBinding
import com.navinfo.omqs.ui.activity.BaseActivity
import com.navinfo.omqs.ui.fragment.offlinemap.OfflineMapFragment


class ConsoleActivity : BaseActivity(), OnClickListener {

    private var _binding: ActivityConsoleBinding? = null
    private val binding get() = _binding!!
    private var sceneFlag = true
    private val aTransition = AutoTransition()
    private val bTransition = AutoTransition()
    private var mFragment: Fragment? = null

    // 创建a场景
    private val aScene by lazy {
        Scene.getSceneForLayout(
            binding.consoleRoot, R.layout.console_on, this
        )
    }

    // 创建b场景
    private val bScene by lazy {
        Scene.getSceneForLayout(
            binding.consoleRoot, R.layout.console_off, this
        )
    }

//    private val mTransitionAManager: TransitionManager by lazy {
//        TransitionInflater.from(this)
//            .inflateTransitionManager(R.transition.transitionmanager_console, binding.consoleRoot)
//    }


    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        _binding = ActivityConsoleBinding.inflate(layoutInflater)
        setContentView(_binding!!.root)
//        mTransitionAManager.setTransition(bScene, transition)

        aTransition.addListener(object : androidx.transition.Transition.TransitionListener {
            override fun onTransitionStart(transition: androidx.transition.Transition) {
                Log.e("jingo", "动画开始")
            }

            override fun onTransitionEnd(transition: androidx.transition.Transition) {
                initOnClickListener()
            }

            override fun onTransitionCancel(transition: androidx.transition.Transition) {
            }

            override fun onTransitionPause(transition: androidx.transition.Transition) {
            }

            override fun onTransitionResume(transition: androidx.transition.Transition) {
            }

        })
        bTransition.addListener(object : androidx.transition.Transition.TransitionListener {
            override fun onTransitionStart(transition: androidx.transition.Transition) {
            }

            override fun onTransitionEnd(transition: androidx.transition.Transition) {
                initOnClickListener()
            }

            override fun onTransitionCancel(transition: androidx.transition.Transition) {
            }

            override fun onTransitionPause(transition: androidx.transition.Transition) {
            }

            override fun onTransitionResume(transition: androidx.transition.Transition) {
            }

        })
        initOnClickListener()
    }

    /**
     * 设置点击事件
     */
    private fun initOnClickListener() {
        // 添加点击事件，切换不同的场景
        binding.consoleRoot.findViewById<View>(R.id.console_map_icon_bg)?.setOnClickListener(
            this
        )
        // 添加点击事件，切换不同的场景
        binding.consoleRoot.findViewById<View>(R.id.console_on_map_icon_bg)
            ?.setOnClickListener(this)
        binding.consoleRoot.findViewById<View>(R.id.console_map_bg)?.setOnClickListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.console_map_bg, R.id.console_map_icon_bg -> {
                    sceneFlag = if (sceneFlag) {
                        TransitionManager.go(bScene, bTransition)
                        false
                    } else {
                        TransitionManager.go(aScene, aTransition)
                        true
                    }
                }
                R.id.console_offline_map_bg -> {
                    if (sceneFlag) {
                        mFragment = OfflineMapFragment()
                        sceneFlag = false
                        TransitionManager.go(bScene, bTransition)
                    }
                }
            }
        }
    }

}