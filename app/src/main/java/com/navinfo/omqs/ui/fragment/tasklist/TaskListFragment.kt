package com.navinfo.omqs.ui.fragment.tasklist

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.databinding.FragmentTaskListBinding
import com.navinfo.omqs.http.taskdownload.TaskDownloadManager
import com.navinfo.omqs.http.taskupload.TaskUploadManager
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.ui.dialog.FirstDialog
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels
import dagger.hilt.android.AndroidEntryPoint
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.M)
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
            downloadManager, uploadManager,binding.taskListRecyclerview
        ) { _, status, taskBean ->
            if(taskBean.hadLinkDvoList.isEmpty()){
                Toast.makeText(context, "数据错误，无Link数据！", Toast.LENGTH_SHORT).show()
            }
            if(status==TaskListAdapter.Companion.ItemClickStatus.ITEM_LAYOUT_CLICK){
                viewModel.setSelectTaskBean(taskBean as TaskBean)
            }else if(status==TaskListAdapter.Companion.ItemClickStatus.DELETE_LAYOUT_CLICK){
                context?.let { viewModel.removeTask(it, taskBean as TaskBean) }
            }else if(status==TaskListAdapter.Companion.ItemClickStatus.UPLOAD_LAYOUT_CLICK){
                showLoadingDialog("正在校验")
                Toast.makeText(context, "正在校验", Toast.LENGTH_SHORT).show()
                viewModel.checkUploadTask(binding.root.context,taskBean)
            } else {

            }
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

        //监听并调用上传
        viewModel.liveDataTaskUpload.observe(viewLifecycleOwner){
            for ((key, value) in it) {
                if(value){
                    adapter.uploadTask(key)
                }
            }
            hideLoadingDialog()
        }

        binding.taskListSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                viewModel.filterTaskList(s.toString())
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}