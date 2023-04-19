package com.navinfo.omqs.ui.fragment.evaluationresult

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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

@AndroidEntryPoint
class PhenomenonFragment :
    BaseFragment() {
    private var _binding: FragmentPhenomenonBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EvaluationResultViewModel by shareViewModels("QsRecode")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhenomenonBinding.inflate(inflater, container, false)
        Log.e("jingo", "PhenomenonFragment onCreateView ${hashCode()}")
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //左侧菜单
        binding.phenomenonLeftRecyclerview.setHasFixedSize(true)
        binding.phenomenonLeftRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        val leftAdapter = PhenomenonLeftAdapter { _, text ->
            viewModel.getProblemTypeList(text)
        }
        binding.phenomenonLeftRecyclerview.adapter = leftAdapter
        //左侧菜单查询结果监听
        viewModel.liveDataClassTypeList.observe(viewLifecycleOwner) {
            leftAdapter.refreshData(it)
        }

        //右侧菜单
        binding.phenomenonRightRecyclerview.setHasFixedSize(true)
        var rightLayoutManager = LinearLayoutManager(requireContext())

        binding.phenomenonRightRecyclerview.layoutManager = rightLayoutManager
        val rightAdapter = PhenomenonRightGroupHeaderAdapter { _, bean ->
            viewModel.setPhenomenonMiddleBean(bean)
        }
        binding.phenomenonRightRecyclerview.adapter = rightAdapter
        //右侧菜单增加组标题
        binding.phenomenonRightRecyclerview.addItemDecoration(
            PhenomenonRightGroupHeaderDecoration(
                requireContext()
            )
        )
        //右侧菜单查询数据监听
        viewModel.liveDataPhenomenonRightList.observe(viewLifecycleOwner) {
            rightAdapter.refreshData(it)
        }

        val middleAdapter = PhenomenonMiddleAdapter { _, title ->
            rightLayoutManager.scrollToPositionWithOffset(rightAdapter.getGroupTopIndex(title), 0)

        }

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


        //中间菜单
        binding.phenomenonMiddleRecyclerview.setHasFixedSize(true)
        binding.phenomenonMiddleRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.phenomenonMiddleRecyclerview.adapter = middleAdapter
        //中间侧菜单查询结果监听
        viewModel.liveDataProblemTypeList.observe(viewLifecycleOwner) {
            middleAdapter.refreshData(it)
        }
        binding.phenomenonDrawer.setOnClickListener {
            when (binding.group.visibility) {
                View.INVISIBLE, View.GONE ->
                    binding.group.visibility = View.VISIBLE
                else ->
                    binding.group.visibility = View.GONE
            }
        }

        viewModel.getClassTypeList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.e("jingo", "PhenomenonFragment onDestroyView ${hashCode()}")
    }

    override fun onResume() {
        super.onResume()
    }
}