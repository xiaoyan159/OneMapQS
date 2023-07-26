package com.navinfo.omqs.ui.fragment.evaluationresult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentProblemLinkBinding
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels

class ProblemLinkFragment : BaseFragment() {
    private var _binding: FragmentProblemLinkBinding? = null
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
        _binding = FragmentProblemLinkBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //右侧菜单
        binding.linkRightRecyclerview.setHasFixedSize(true)
        var rightLayoutManager = LinearLayoutManager(requireContext())

        binding.linkRightRecyclerview.layoutManager = rightLayoutManager
        val rightAdapter = RightGroupHeaderAdapter { _, bean ->
            viewModel.setProblemLinkMiddleBean(bean)
            if (activity != null) {
                requireActivity().findNavController(R.id.main_activity_middle_fragment).navigateUp()
            }
        }
        binding.linkRightRecyclerview.adapter = rightAdapter
        //右侧菜单增加组标题
        binding.linkRightRecyclerview.addItemDecoration(
            RightGroupHeaderDecoration(
                requireContext()
            )
        )
        //右侧菜单查询数据监听
        viewModel.liveDataRightTypeList.observe(viewLifecycleOwner) {
            rightAdapter.setSelectTitle(viewModel.liveDataQsRecordBean.value!!.cause)
            rightAdapter.refreshData(it)
        }

        val middleAdapter = MiddleAdapter { _, title ->
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


        viewModel.getProblemLinkList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}