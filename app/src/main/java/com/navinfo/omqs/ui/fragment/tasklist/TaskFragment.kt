package com.navinfo.omqs.ui.fragment.tasklist

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentTaskBinding
import com.navinfo.omqs.databinding.FragmentTaskListBinding
import com.navinfo.omqs.http.taskdownload.TaskDownloadManager
import com.navinfo.omqs.http.taskupload.TaskUploadManager
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 当前任务的道路列表
 */
@AndroidEntryPoint
class TaskFragment : BaseFragment() {

    private var _binding: FragmentTaskBinding? = null

    /**
     * 和[TaskManagerFragment],[TaskListFragment],[TaskFragment]共用同一个viewModel
     */
    private val viewModel by shareViewModels<TaskViewModel>("Task")
    private val binding get() = _binding!!
    private val adapter: TaskAdapter by lazy {
        TaskAdapter(object : TaskAdapterCallback {
            override fun itemOnClick(bean: HadLinkDvoBean) {
                viewModel.showCurrentLink(bean)
            }

            override fun editOnclick(position: Int, bean: HadLinkDvoBean) {
                showLinkEditDialog(position, bean)
            }
        })
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(context)
        //// 设置 RecyclerView 的固定大小，避免在滚动时重新计算视图大小和布局，提高性能
        binding.taskRecyclerview.setHasFixedSize(true)
        binding.taskRecyclerview.layoutManager = layoutManager
        binding.taskRecyclerview.adapter = adapter
        viewModel.liveDataTaskLinks.observe(viewLifecycleOwner) {
            adapter.resetSelect()
            adapter.refreshData(it)
        }
        viewModel.getTaskList(requireContext())

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 显示link编辑dialog
     */
    private fun showLinkEditDialog(position: Int, bean: HadLinkDvoBean) {
        val view = this.layoutInflater.inflate(R.layout.dialog_view_edittext, null)
        val inputDialog = MaterialAlertDialogBuilder(
            requireContext()
        ).setTitle("标记原因").setView(view)
        var editText = view.findViewById<EditText>(R.id.dialog_edittext)
        editText.setText(bean.reason)
        inputDialog.setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
        }
        inputDialog.setPositiveButton("确定") { dialog, _ ->
            lifecycleScope.launch {
                val text = editText.text.toString()
                viewModel.saveLinkReason(bean, text)
                adapter.notifyItemChanged(position)
                dialog.dismiss()
            }

        }
        inputDialog.show()
    }
}