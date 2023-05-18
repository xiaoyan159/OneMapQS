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
import com.navinfo.omqs.http.taskupload.TaskUploadManager
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TaskListFragment : BaseFragment() {

    @Inject
    lateinit var downloadManager: TaskDownloadManager

    @Inject
    lateinit var uploadManager: TaskUploadManager
    private var _binding: FragmentTaskListBinding? = null

    /**
     * 和[TaskManagerFragment],[TaskListFragment],[TaskFragment]共用同一个viewModel
     */
    private val viewModel by shareViewModels<TaskViewModel>("Task")
    private val binding get() = _binding!!
    private val adapter: TaskListAdapter by lazy {
        TaskListAdapter(
            downloadManager, uploadManager
        ) { position, taskBean ->
            viewModel.setSelectTaskBean(taskBean)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        downloadManager.init(requireContext())
        uploadManager.init(requireContext())
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(context)
        //// 设置 RecyclerView 的固定大小，避免在滚动时重新计算视图大小和布局，提高性能
        binding.taskListRecyclerview.setHasFixedSize(true)
        binding.taskListRecyclerview.layoutManager = layoutManager
        binding.taskListRecyclerview.adapter = adapter
        viewModel.liveDataTaskList.observe(viewLifecycleOwner) {
            adapter.refreshData(it)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}