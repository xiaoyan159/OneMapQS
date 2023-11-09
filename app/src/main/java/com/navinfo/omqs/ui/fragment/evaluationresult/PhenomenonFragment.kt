package com.navinfo.omqs.ui.fragment.evaluationresult

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentPhenomenonBinding
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels
import dagger.hilt.android.AndroidEntryPoint

/**
 * 问题现象页面
 */
@AndroidEntryPoint
class PhenomenonFragment :
    BaseFragment() {
    private var _binding: FragmentPhenomenonBinding? = null
    private val binding get() = _binding!!

    /**
     * 和[PhenomenonFragment],[ProblemLinkFragment],[EvaluationResultFragment]共用同一个viewModel
     */
    private val viewModel: EvaluationResultViewModel by shareViewModels("QsRecode")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhenomenonBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //左侧菜单
        binding.phenomenonLeftRecyclerview.setHasFixedSize(true)
        binding.phenomenonLeftRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        /**
         * 监听左侧栏的点击事件
         */
        val leftAdapter = LeftAdapter { _, text ->
            viewModel.getProblemTypeList(text)
        }
        binding.phenomenonLeftRecyclerview.adapter = leftAdapter
        //左侧菜单查询结果监听
        viewModel.liveDataLeftTypeList.observe(viewLifecycleOwner) {
            leftAdapter.setSelectTitle(viewModel.liveDataQsRecordBean.value!!.classType)
            leftAdapter.refreshData(it)
        }

        //右侧菜单
        binding.phenomenonRightRecyclerview.setHasFixedSize(true)
        var rightLayoutManager = LinearLayoutManager(requireContext())

        binding.phenomenonRightRecyclerview.layoutManager = rightLayoutManager
        /**
         * 监听右侧栏的点击事件
         */
        val rightAdapter = RightGroupHeaderAdapter { _, bean ->
            viewModel.setPhenomenonMiddleBean(bean)
            if (activity != null) {
                requireActivity().findNavController(R.id.main_activity_middle_fragment).navigateUp()
            }
        }
        binding.phenomenonRightRecyclerview.adapter = rightAdapter
        //右侧菜单增加组标题
        binding.phenomenonRightRecyclerview.addItemDecoration(
            RightGroupHeaderDecoration(
                requireContext()
            )
        )
        //右侧菜单查询数据监听
        viewModel.liveDataRightTypeList.observe(viewLifecycleOwner) {
            rightAdapter.setSelectTitle(viewModel.liveDataQsRecordBean.value!!.phenomenon)
            rightAdapter.refreshData(it)
        }

        /**
         * 监听中间栏的点击事件
         */
        val middleAdapter = MiddleAdapter { _, title ->
            rightLayoutManager.scrollToPositionWithOffset(rightAdapter.getGroupTopIndex(title), 0)
        }

        /**
         * 监控右侧滚动，更新左侧
         */
        binding.phenomenonRightRecyclerview.addOnScrollListener(object :
            OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            //findLastVisibleItemPosition() ：最后一个可见位置
//            findFirstVisibleItemPosition() ：第一个可见位置
//            findLastCompletelyVisibleItemPosition() ：最后一个完全可见位置
//            findFirstCompletelyVisibleItemPosition() ：第一个完全可见位置
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstIndex = rightLayoutManager.findFirstVisibleItemPosition()
                middleAdapter.setRightTitle(rightAdapter.data[firstIndex].title)
            }
        })


//        //中间菜单
//        binding.phenomenonMiddleRecyclerview.setHasFixedSize(true)
//        binding.phenomenonMiddleRecyclerview.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
//
//        binding.phenomenonMiddleRecyclerview.adapter = middleAdapter
        //中间侧菜单查询结果监听
//        viewModel.liveDataMiddleTypeList.observe(viewLifecycleOwner) {
//            middleAdapter.refreshData(it)
//        }
//        binding.phenomenonDrawer.setOnClickListener {
//            when (binding.group.visibility) {
//                View.INVISIBLE, View.GONE ->
//                    binding.group.visibility = View.VISIBLE
//                else ->
//                    binding.group.visibility = View.GONE
//            }
//        }

        viewModel.getClassTypeList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}