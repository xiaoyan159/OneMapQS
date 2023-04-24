package com.navinfo.omqs.ui.fragment.tasklist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.navinfo.omqs.databinding.FragmentTaskListBinding
import com.navinfo.omqs.http.taskdownload.TaskDownloadManager
import com.navinfo.omqs.ui.fragment.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TaskListFragment : BaseFragment(){
    @Inject
    lateinit var downloadManager: TaskDownloadManager
    private var _binding: FragmentTaskListBinding? = null
    private val viewModel by viewModels<TaskListViewModel>()
    private val binding get() = _binding!!
    private val adapter: TaskListAdapter by lazy {
        TaskListAdapter(
            downloadManager,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(context)
        //// 设置 RecyclerView 的固定大小，避免在滚动时重新计算视图大小和布局，提高性能
        binding.taskRecyclerview.setHasFixedSize(true)
        binding.taskRecyclerview.layoutManager = layoutManager
        binding.taskRecyclerview.adapter = adapter
        viewModel.liveDataTaskList.observe(viewLifecycleOwner) {
            adapter.refreshData(it)
        }
        viewModel.getTaskList(requireContext())
        binding.taskBack.setOnClickListener{
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}