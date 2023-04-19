package com.navinfo.omqs.ui.fragment.evaluationresult

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.navinfo.omqs.databinding.FragmentProblemLinkBinding
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels

class ProblemLinkFragment : BaseFragment() {
    private var _binding: FragmentProblemLinkBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EvaluationResultViewModel by shareViewModels("QsRecode")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProblemLinkBinding.inflate(inflater, container, false)
        Log.e("jingo", "linkFragment onCreateView ${hashCode()}")
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //右侧菜单
        binding.linkRightRecyclerview.setHasFixedSize(true)
        var rightLayoutManager = LinearLayoutManager(requireContext())

        binding.linkRightRecyclerview.layoutManager = rightLayoutManager
        val rightAdapter = PhenomenonRightGroupHeaderAdapter { _, bean ->
            viewModel.setProblemLinkMiddleBean(bean)
        }
        binding.linkRightRecyclerview.adapter = rightAdapter
        //右侧菜单增加组标题
        binding.linkRightRecyclerview.addItemDecoration(
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

        binding.linkRightRecyclerview.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstIndex = rightLayoutManager.findFirstVisibleItemPosition()
                middleAdapter.setRightTitle(rightAdapter.data[firstIndex].title)
            }
        })

        //中间菜单
        binding.linkMiddleRecyclerview.setHasFixedSize(true)
        binding.linkMiddleRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.linkMiddleRecyclerview.adapter = middleAdapter
        //中间侧菜单查询结果监听
        viewModel.liveDataProblemTypeList.observe(viewLifecycleOwner) {
            middleAdapter.refreshData(it)
        }
        binding.linkDrawer.setOnClickListener {
            when (binding.group.visibility) {
                View.INVISIBLE, View.GONE ->
                    binding.group.visibility = View.VISIBLE
                else ->
                    binding.group.visibility = View.GONE
            }
        }

        viewModel.getProblemLinkList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.e("jingo", "linkFragment onDestroyView ${hashCode()}")
    }

    override fun onResume() {
        super.onResume()
    }
}