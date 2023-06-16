package com.navinfo.omqs.ui.fragment.console

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.AutoTransition
import androidx.transition.Scene
import androidx.transition.TransitionManager
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentConsoleBinding
import com.navinfo.omqs.ui.activity.map.MainActivity
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.fragment.evaluationresult.EvaluationResultFragment
import com.navinfo.omqs.ui.fragment.layermanager.LayerManagerFragment
import com.navinfo.omqs.ui.fragment.offlinemap.OfflineMapFragment
import com.navinfo.omqs.ui.fragment.personalcenter.PersonalCenterFragment
import com.navinfo.omqs.ui.fragment.qsrecordlist.QsRecordListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConsoleFragment : BaseFragment(), OnClickListener {

    private var _binding: FragmentConsoleBinding? = null
    private val binding get() = _binding!!
    private var sceneFlag = true
    private val aTransition = AutoTransition()
    private val bTransition = AutoTransition()
    private var mFragment: Fragment? = null
    private val fragmentId = R.id.console_fragment

    // 创建a场景
    private val aScene by lazy {
        Scene.getSceneForLayout(
            binding.consoleRoot, R.layout.console_on, requireContext()
        )
    }

    // 创建b场景
    private val bScene by lazy {
        Scene.getSceneForLayout(
            binding.consoleRoot, R.layout.console_off, requireContext()
        )
    }

//    private val mTransitionAManager: TransitionManager by lazy {
//        TransitionInflater.from(this)
//            .inflateTransitionManager(R.transition.transitionmanager_console, binding.consoleRoot)
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConsoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        aTransition.addListener(object : androidx.transition.Transition.TransitionListener {
            override fun onTransitionStart(transition: androidx.transition.Transition) {
                sceneFlag = true
                if (mFragment != null) {
                    childFragmentManager.beginTransaction().remove(mFragment!!).commit()
                    mFragment = null
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
        bTransition.addListener(object : androidx.transition.Transition.TransitionListener {
            override fun onTransitionStart(transition: androidx.transition.Transition) {
                sceneFlag = false
                if (mFragment != null) {
                    childFragmentManager.beginTransaction().replace(fragmentId, mFragment!!)
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
        binding.consoleRoot.findViewById<View>(R.id.console_evaluation_bg)?.setOnClickListener(this)
        /**
         * 评测任务
         */
        binding.consoleRoot.findViewById<View>(R.id.console_task_bg)?.setOnClickListener(this)
        binding.consoleRoot.findViewById<View>(R.id.console_task_icon_bg)?.setOnClickListener(this)
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
                    activity?.let { a ->
                        a.supportFragmentManager.beginTransaction().remove(this).commit()
                    }
                }
                /**
                 * 离线地图点击
                 */
                R.id.console_offline_map_icon_bg, R.id.console_offline_map_bg -> {
                    activity?.let { a ->
                        a.supportFragmentManager.beginTransaction().remove(this).commit()
                        (a as MainActivity).onClickOfflineMapFragment()
                    }

                }
                /**
                 * 个人中心点击
                 */
                R.id.console_personal_center_bg, R.id.console_personal_center_icon_bg -> {
                    if (sceneFlag) {
                        mFragment = PersonalCenterFragment {
                            TransitionManager.go(aScene, aTransition)
                        }
                        sceneFlag = false
                        TransitionManager.go(bScene, bTransition)
                    } else {
                        if (mFragment !is PersonalCenterFragment) {
                            mFragment = PersonalCenterFragment {
                                TransitionManager.go(aScene, aTransition)
                            }
                            childFragmentManager.beginTransaction().replace(fragmentId, mFragment!!)
                                .commit()
                        }
                        return
                    }
                }
                /**
                 * 图层设置
                 */
                R.id.console_layer_setting_bg, R.id.console_layer_setting_icon_bg -> {
                    if (sceneFlag) {
                        mFragment = LayerManagerFragment {
                            TransitionManager.go(aScene, aTransition)
                        }
                        sceneFlag = false
                        TransitionManager.go(bScene, bTransition)
                    } else {
                        if (mFragment !is LayerManagerFragment) {
                            mFragment = LayerManagerFragment {
                                TransitionManager.go(aScene, aTransition)
                            }
                            childFragmentManager.beginTransaction().replace(fragmentId, mFragment!!)
                                .commit()
                        }
                        return
                    }
                }
                /**
                 * 测评结果列表
                 */
                R.id.console_evaluation_icon_bg, R.id.console_evaluation_bg -> {
                    activity?.let { a ->
                        a.supportFragmentManager.beginTransaction().remove(this).commit()
                        (a as MainActivity).onClickResFragment()
                    }
                }
                R.id.console_task_icon_bg, R.id.console_task_bg -> {
                    activity?.let { a ->
                        a.supportFragmentManager.beginTransaction().remove(this).commit()
                        (a as MainActivity).onClickTaskFragment()
                    }

                }
                else -> {}
            }
        }
    }

}