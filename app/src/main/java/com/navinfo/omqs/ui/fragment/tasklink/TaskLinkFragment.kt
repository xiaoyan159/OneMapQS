package com.navinfo.omqs.ui.fragment.tasklink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentTaskLinkBinding
import com.navinfo.omqs.ui.activity.map.MainActivity
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TaskLinkFragment : BaseFragment(), View.OnClickListener {
    private var _binding: FragmentTaskLinkBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var mapController: NIMapController

    private val viewModel by shareViewModels<TaskLinkViewModel>("taskLink")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskLinkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        arguments?.let {
            val id = it.getString("TaskLinkId")
            if (id != null && id.isNotEmpty()) {
                viewModel.initData(id)
            }
        }

        binding.taskLinkAddPoint.setOnClickListener(this)
        binding.taskLinkKind.setOnClickListener(this)
        binding.taskLinkFunctionalLevel.setOnClickListener(this)
        binding.taskLinkDataLevel.setOnClickListener(this)
        binding.taskLinkBarCancel.setOnClickListener(this)
        binding.taskLinkBarSave.setOnClickListener(this)
        binding.taskLinkBack.setOnClickListener(this)
        binding.taskLinkClear.setOnClickListener(this)
        binding.taskLinkBarDelete.setOnClickListener(this)

        /**
         * 数据操作结束
         */
        viewModel.liveDataFinish.observe(viewLifecycleOwner) {
            if (it)
                onBackPressed()
        }
        /**
         * 种别
         */
        viewModel.liveDataSelectKind.observe(viewLifecycleOwner) {
            binding.taskLinkKind.text = it?.title
        }

        /**
         * 功能等级
         */
        viewModel.liveDataSelectFunctionLevel.observe(viewLifecycleOwner) {
            binding.taskLinkFunctionalLevel.text = it?.title
        }

        /**
         * 数据等级
         */
        viewModel.liveDataSelectDataLevel.observe(viewLifecycleOwner) {
            binding.taskLinkDataLevel.text = it?.title
        }

        /**
         * 当前选中任务
         */
        viewModel.liveDataTaskBean.observe(viewLifecycleOwner) {
            binding.taskLinkTaskName.text = it?.evaluationTaskName
        }

        /**
         * viewModel 返回的文字信息
         */
        viewModel.liveDataToastMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
        /**
         * 线长度
         */
        mapController.measureLayerHandler.measureValueLiveData.observe(viewLifecycleOwner) {
            binding.taskLinkLength.text = "${it.valueString}${it.unit}"
        }
        mapController.measureLayerHandler.tempMeasureValueLiveData.observe(viewLifecycleOwner) {
            (activity as MainActivity).setHomeCenterText("${it.valueString}${it.unit}")
        }
    }

    override fun onStart() {
        super.onStart()
        /**
         * 显示地图准星
         */
        activity?.let {
            (activity as MainActivity).measuringToolOff()
            (activity as MainActivity).setHomeCenterVisibility(View.VISIBLE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        /**
         * 隐藏地图准星
         */
        requireActivity().findNavController(R.id.main_activity_middle_fragment).navigateUp()
        activity?.let {
            (activity as MainActivity).setHomeCenterVisibility(View.GONE)
        }
    }

    override fun onClick(v: View) {
        when (v) {
            binding.taskLinkAddPoint -> {
                viewModel.addPoint()
            }
            binding.taskLinkKind -> {
                showMiddleFragment()
                viewModel.setAdapterList(1)
            }
            binding.taskLinkFunctionalLevel -> {
                showMiddleFragment()
                viewModel.setAdapterList(2)
            }
            binding.taskLinkDataLevel -> {
                showMiddleFragment()
                viewModel.setAdapterList(3)
            }
            binding.taskLinkBarCancel -> {
                onBackPressed()
            }
            binding.taskLinkBarSave -> {
                viewModel.saveData()
            }
            binding.taskLinkBack -> {
                viewModel.removeLinkLastPoint()
            }
            binding.taskLinkClear -> {
                viewModel.clearLink()
            }
            binding.taskLinkBarDelete -> {
                viewModel.deleteData(requireContext())
            }
        }
    }

    /**
     * 显示中间面板
     */
    private fun showMiddleFragment() {
        activity?.run {
            val controller = findNavController(
                R.id.main_activity_middle_fragment
            )
            if (controller.currentDestination?.id == R.id.MiddleEmptyFragment)
                controller.navigate(R.id.TaskLinkMiddleFragment)
        }
    }

    override fun onBackPressed(): Boolean {
        findNavController().navigateUp()
        return true
    }
}