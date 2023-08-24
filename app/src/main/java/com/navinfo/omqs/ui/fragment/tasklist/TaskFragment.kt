package com.navinfo.omqs.ui.fragment.tasklist

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentTaskBinding
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels
import com.yanzhenjie.recyclerview.SwipeMenuBridge
import com.yanzhenjie.recyclerview.SwipeMenuCreator
import com.yanzhenjie.recyclerview.SwipeMenuItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.videolan.vlc.Util

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    viewModel.showCurrentLink(bean)
                }
            }

            override fun editOnClick(position: Int, bean: HadLinkDvoBean) {
                showLinkEditDialog(position, bean)
            }

            override fun scrollPosition(position: Int) {
               binding.taskRecyclerview.scrollToPosition(position)
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
        binding.taskAddLink.setOnClickListener {
            viewModel.setSelectLink(!binding.taskAddLink.isSelected)
        }

        viewModel.liveDataSelectNewLink.observe(viewLifecycleOwner) {
            binding.taskAddLink.isSelected = it
        }

        //注意：使用滑动菜单不能开启滑动删除，否则只有滑动删除没有滑动菜单
        val mSwipeMenuCreator = SwipeMenuCreator { _, rightMenu, _ ->
            //添加菜单自动添加至尾部
            val deleteItem = SwipeMenuItem(context)
            deleteItem.height = Util.convertDpToPx(requireContext(), 60)
            deleteItem.width = Util.convertDpToPx(requireContext(), 80)
            deleteItem.text = "删除"
            deleteItem.background = requireContext().getDrawable(R.color.red)
            deleteItem.setTextColor(requireContext().resources.getColor(R.color.white))
            rightMenu.addMenuItem(deleteItem)
        }

        val layoutManager = LinearLayoutManager(context)
        //// 设置 RecyclerView 的固定大小，避免在滚动时重新计算视图大小和布局，提高性能
        binding.taskRecyclerview.setHasFixedSize(true)
        binding.taskRecyclerview.layoutManager = layoutManager

        //增加侧滑按钮
        binding.taskRecyclerview.setSwipeMenuCreator(mSwipeMenuCreator)


        //单项点击
        binding.taskRecyclerview.setOnItemMenuClickListener { menuBridge, position ->
            menuBridge.closeMenu()
            viewModel.deleteTaskLink(requireContext(), adapter.data[position])
        }

        binding.taskRecyclerview.adapter = adapter

        binding.taskSearchClear.setOnClickListener {
            binding.taskSearch.setText("")
        }
        viewModel.liveDataTaskLinks.observe(viewLifecycleOwner) {
            adapter.resetSelect()
            adapter.refreshData(it)
        }
        viewModel.getTaskList(requireContext())
        binding.taskSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                viewModel.filterTask(s.toString())
            }

        })
        viewModel.liveDataSelectLink.observe(viewLifecycleOwner){
            adapter.setSelectTag(it)
        }
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