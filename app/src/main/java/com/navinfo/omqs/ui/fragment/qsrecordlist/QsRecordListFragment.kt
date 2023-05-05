package com.navinfo.omqs.ui.fragment.qsrecordlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentQsRecordListBinding
import com.navinfo.omqs.ui.activity.map.MainActivity
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.fragment.tasklist.QsRecordListAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QsRecordListFragment : BaseFragment(){
    private var _binding: FragmentQsRecordListBinding? = null
    private val viewModel by viewModels<QsRecordListViewModel>()
    private val binding get() = _binding!!
    private val adapter: QsRecordListAdapter by lazy {
        QsRecordListAdapter(
            requireContext()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQsRecordListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(context)
        //// 设置 RecyclerView 的固定大小，避免在滚动时重新计算视图大小和布局，提高性能
        binding.qsRecyclerview.setHasFixedSize(true)
        binding.qsRecyclerview.layoutManager = layoutManager
        binding.qsRecyclerview.adapter = adapter
        viewModel.liveDataQSList.observe(viewLifecycleOwner) {
            adapter.refreshData(it)
        }
        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        itemDecoration.setDrawable(resources.getDrawable(R.drawable.separator))
        binding.qsRecyclerview.addItemDecoration(itemDecoration)
        viewModel.getList(requireContext())
        // itemClick
        adapter.setOnKotlinItemClickListener(object : QsRecordListAdapter.IKotlinItemClickListener {
            override fun onItemClickListener(position: Int) {
                viewModel.onItemClickListener(activity as MainActivity,position)
                findNavController().popBackStack()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.getList(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}