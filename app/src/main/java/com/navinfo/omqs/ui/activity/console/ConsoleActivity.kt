package com.navinfo.omqs.ui.activity.console

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import androidx.fragment.app.Fragment
import androidx.transition.AutoTransition
import androidx.transition.Scene
import androidx.transition.TransitionManager
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.ActivityConsoleBinding
import com.navinfo.omqs.ui.activity.BaseActivity
import com.navinfo.omqs.ui.activity.map.MainActivity
import com.navinfo.omqs.ui.fragment.layermanager.LayermanagerFragment
import com.navinfo.omqs.ui.fragment.offlinemap.OfflineMapFragment
import com.navinfo.omqs.ui.fragment.personalcenter.PersonalCenterFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConsoleActivity : BaseActivity(), OnClickListener {

    private var _binding: ActivityConsoleBinding? = null
    private val binding get() = _binding!!
    private var sceneFlag = true
    private val aTransition = AutoTransition()
    private val bTransition = AutoTransition()
    private var mFragment: Fragment? = null
    private val fragmentId = R.id.console_fragment

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
        super.onCreate(savedInstanceState)
        _binding = ActivityConsoleBinding.inflate(layoutInflater)
        setContentView(_binding!!.root)
//        mTransitionAManager.setTransition(bScene, transition)

        aTransition.addListener(object : androidx.transition.Transition.TransitionListener {
            override fun onTransitionStart(transition: androidx.transition.Transition) {
                if (mFragment != null) {
                    Log.e("jingo", "动画开始B mFragment 不为null")
                    supportFragmentManager.beginTransaction().remove(mFragment!!).commit()
                    mFragment = null
                }
            }

            override fun onTransitionEnd(transition: androidx.transition.Transition) {
                Log.e("jingo", "动画A结束")
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
                if (mFragment != null) {
                    Log.e("jingo", "动画开始A mFragment 不为null")
                    supportFragmentManager.beginTransaction().replace(fragmentId, mFragment!!)
                        .commit()
                }
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
        /**
         *  地图按钮
         */
        binding.consoleRoot.findViewById<View>(R.id.console_map_icon_bg)?.setOnClickListener(
            this
        )
        binding.consoleRoot.findViewById<View>(R.id.console_map_bg)?.setOnClickListener(this)
        /**
         * 离线地图按钮
         */
        binding.consoleRoot.findViewById<View>(R.id.console_offline_map_icon_bg)
            ?.setOnClickListener(this)
        binding.consoleRoot.findViewById<View>(R.id.console_offline_map_bg)
            ?.setOnClickListener(this)
        /**
         * 图层设置按钮
         */
        binding.consoleRoot.findViewById<View>(R.id.console_layer_setting_icon_bg)
            ?.setOnClickListener(this)
        binding.consoleRoot.findViewById<View>(R.id.console_layer_setting_bg)
            ?.setOnClickListener(this)
        /**
         * 个人中心
         */
        binding.consoleRoot.findViewById<View>(R.id.console_personal_center_icon_bg)
            ?.setOnClickListener(this)
        binding.consoleRoot.findViewById<View>(R.id.console_personal_center_bg)
            ?.setOnClickListener(this)
        /**
         * 测评结果列表
         */
        binding.consoleRoot.findViewById<View>(R.id.console_evaluation_icon_bg)
            ?.setOnClickListener(this)
        binding.consoleRoot.findViewById<View>(R.id.console_evaluation_bg)
            ?.setOnClickListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                /**
                 *   地图点击事件
                 */
                R.id.console_map_bg, R.id.console_map_icon_bg -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                /**
                 * 离线地图点击
                 */
                R.id.console_offline_map_icon_bg, R.id.console_offline_map_bg -> {
                    if (sceneFlag) {
                        mFragment = OfflineMapFragment()
                        sceneFlag = false
                        TransitionManager.go(bScene, bTransition)
                    } else {
                        if (mFragment !is OfflineMapFragment) {
                            mFragment = OfflineMapFragment()
                            supportFragmentManager.beginTransaction()
                                .replace(fragmentId, mFragment!!).commit()
                        }
                        return
                    }
                }
                /**
                 * 个人中心点击
                 */
                R.id.console_personal_center_bg, R.id.console_personal_center_icon_bg -> {
                    if (sceneFlag) {
                        mFragment = PersonalCenterFragment()
                        sceneFlag = false
                        TransitionManager.go(bScene, bTransition)
                    } else {
                        if (mFragment !is PersonalCenterFragment) {
                            mFragment = PersonalCenterFragment()
                            supportFragmentManager.beginTransaction()
                                .replace(fragmentId, mFragment!!).commit()
                        }
                        return
                    }
                }
                /**
                 * 图层设置
                 */
                R.id.console_layer_setting_bg, R.id.console_layer_setting_icon_bg -> {
/*                    if (sceneFlag) {
                        mFragment = LayermanagerFragment()
                        sceneFlag = false
                        TransitionManager.go(bScene, bTransition)
                    } else {
                        if (mFragment !is LayermanagerFragment) {
                            mFragment = LayermanagerFragment()
                            supportFragmentManager.beginTransaction()
                                .replace(fragmentId, mFragment!!).commit()
                        }
                        return
                    }*/
                }
                /**
                 * 测评结果列表
                 */
                R.id.console_evaluation_icon_bg,
                R.id.console_evaluation_bg -> {
//                    if (sceneFlag) {
//                        mFragment = LayermanagerFragment()
//                        sceneFlag = false
//                        TransitionManager.go(bScene, bTransition)
//                    } else {
//                        if (mFragment !is LayermanagerFragment) {
//                            mFragment = LayermanagerFragment()
//                            supportFragmentManager.beginTransaction()
//                                .replace(fragmentId, mFragment!!).commit()
//                        }
//                        return
//                    }
                }
                else -> {}
            }
        }
    }

}