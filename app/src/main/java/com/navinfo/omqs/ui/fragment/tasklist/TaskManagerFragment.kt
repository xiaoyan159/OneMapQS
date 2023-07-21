package com.navinfo.omqs.ui.fragment.tasklist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.navinfo.omqs.databinding.FragmentTaskManagerBinding
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels
import dagger.hilt.android.AndroidEntryPoint

/**
 * 评测任务viewpager管理页面
 */
@AndroidEntryPoint
class TaskManagerFragment(private var backListener: ((TaskManagerFragment) -> Unit?)? = null) :
    BaseFragment() {
    private var _binding: FragmentTaskManagerBinding? = null

    private val binding get() = _binding!!

    /**
     * 和[TaskManagerFragment],[TaskListFragment],[TaskFragment]共用同一个viewModel
     */
    private val viewModel by shareViewModels<TaskViewModel>("Task")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskManagerBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.liveDataToastMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
        //禁止滑动，因为页面在抽屉里，和抽屉的滑动有冲突
        binding.taskManagerViewpager.isUserInputEnabled = false
        //创建viewpager2的适配器
        binding.taskManagerViewpager.adapter = activity?.let { TaskManagerAdapter(it) }
        binding.taskManagerViewpager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setSelectLink(false)
            }
        })

        //绑定viewpager2与tabLayout
        TabLayoutMediator(
            binding.taskManagerTabLayout,
            binding.taskManagerViewpager
        ) { tab, position ->
            when (position) {
                0 -> tab.text = "当前任务"
                1 -> tab.text = "任务列表"
            }
        }.attach()
        viewModel.getTaskList(requireContext())
        binding.taskBack.setOnClickListener {
            backListener?.invoke(this)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}