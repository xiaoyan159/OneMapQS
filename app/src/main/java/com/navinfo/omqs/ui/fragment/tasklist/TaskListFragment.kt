package com.navinfo.omqs.ui.fragment.tasklist

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentTaskListBinding
import com.navinfo.omqs.http.taskdownload.TaskDownloadManager
import com.navinfo.omqs.http.taskupload.TaskUploadManager
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.ui.activity.map.MainActivity
import com.navinfo.omqs.ui.activity.map.MainViewModel
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels
import com.yanzhenjie.recyclerview.SwipeMenuCreator
import com.yanzhenjie.recyclerview.SwipeMenuItem
import dagger.hilt.android.AndroidEntryPoint
import org.videolan.vlc.Util
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
            downloadManager, uploadManager,
        ) { _, status, taskBean ->
            if (taskBean.hadLinkDvoList.isEmpty()) {
                Toast.makeText(context, "数据错误，无Link数据！", Toast.LENGTH_SHORT).show()
            }

            when (status) {
                TaskListAdapter.Companion.ItemClickStatus.ITEM_LAYOUT_CLICK -> {
                    viewModel.setSelectTaskBean(taskBean)
                }
                TaskListAdapter.Companion.ItemClickStatus.DELETE_LAYOUT_CLICK -> {

                }
                TaskListAdapter.Companion.ItemClickStatus.UPLOAD_LAYOUT_CLICK -> {
                    showLoadingDialog("正在校验")
                    Toast.makeText(context, "正在校验", Toast.LENGTH_SHORT).show()
                    viewModel.checkUploadTask(binding.root.context, taskBean)
                }
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

        //注意：使用滑动菜单不能开启滑动删除，否则只有滑动删除没有滑动菜单
        val mSwipeMenuCreator = SwipeMenuCreator { _, rightMenu, _ ->
            //添加菜单自动添加至尾部
            val deleteItem = SwipeMenuItem(context)
            deleteItem.height = Util.convertDpToPx(requireContext(), 60)
            deleteItem.width = Util.convertDpToPx(requireContext(), 80)
            deleteItem.text = "关闭"
            deleteItem.background = requireContext().getDrawable(R.color.red)
            deleteItem.setTextColor(requireContext().resources.getColor(R.color.white))
            rightMenu.addMenuItem(deleteItem)

            val resetDownLoad = SwipeMenuItem(context)
            resetDownLoad.height = Util.convertDpToPx(requireContext(), 60)
            resetDownLoad.width = Util.convertDpToPx(requireContext(), 80)
            resetDownLoad.text = "重新下载"
            resetDownLoad.background = requireContext().getDrawable(R.color.btn_bg_blue)
            resetDownLoad.setTextColor(requireContext().resources.getColor(R.color.white))
            rightMenu.addMenuItem(resetDownLoad)
        }

        val layoutManager = LinearLayoutManager(context)
        //// 设置 RecyclerView 的固定大小，避免在滚动时重新计算视图大小和布局，提高性能
        binding.taskListRecyclerview.setHasFixedSize(true)
        binding.taskListRecyclerview.layoutManager = layoutManager

        //增加侧滑按钮
        binding.taskListRecyclerview.setSwipeMenuCreator(mSwipeMenuCreator)

        //单项点击
        binding.taskListRecyclerview.setOnItemMenuClickListener { menuBridge, position ->
            menuBridge.closeMenu()
            val taskBean = adapter.data[position]
            if(menuBridge.position==0){
                if (taskBean.syncStatus != FileManager.Companion.FileUploadStatus.DONE) {
                    Toast.makeText(context, "数据未上传，不允许关闭！", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    viewModel.removeTask(requireContext(), taskBean)
                }
            }else{
                viewModel.resetDownload(requireContext(), taskBean)
            }
        }


        /**
         * 刷新
         */
        binding.refreshLayout.setOnRefreshListener {
            viewModel.loadNetTaskList(requireContext())
        }

        loadFinish()
        binding.taskListRecyclerview.adapter = adapter

        viewModel.liveDataTaskList.observe(viewLifecycleOwner) {
            loadFinish()
            adapter.initSelectTask(it, viewModel.currentSelectTaskBean?.id)
            var position = adapter.getSelectTaskPosition()
            if(position<0){
                position = 0
            }
            //定位到被选中的任务
            binding.taskListRecyclerview.smoothScrollToPosition(position)
        }

        viewModel.liveDataLoadTask.observe(viewLifecycleOwner){
            when(it){
                TaskLoadStatus.TASK_LOAD_STATUS_BEGIN->{
                    showLoadingDialog("正在切换任务")
                }
                TaskLoadStatus.TASK_LOAD_STATUS_FISISH->{
                    hideLoadingDialog()
                }
            }
        }

        viewModel.liveDataCloseTask.observe(viewLifecycleOwner){
            when(it){
                TaskDelStatus.TASK_DEL_STATUS_BEGIN->{
                    showLoadingDialog("正在重置...")
                }
                TaskDelStatus.TASK_DEL_STATUS_LOADING->{
                    showLoadingDialog("正在重置...")
                }
                TaskDelStatus.TASK_DEL_STATUS_SUCCESS->{
                    hideLoadingDialog()
                    Toast.makeText(context,"成功重置",Toast.LENGTH_LONG).show()

                }
                TaskDelStatus.TASK_DEL_STATUS_FAILED->{
                    hideLoadingDialog()
                }
                TaskDelStatus.TASK_DEL_STATUS_CANCEL->{

                }
            }
        }

        //监听并调用上传
        viewModel.liveDataTaskUpload.observe(viewLifecycleOwner) {
            for ((key, value) in it) {
                if (value) {
                    adapter.uploadTask(key)
                }
            }
            hideLoadingDialog()
        }

        binding.taskListSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                viewModel.filterTaskList(s.toString())
            }
        })
    }

    private fun loadFinish() {
        binding.refreshLayout.isRefreshing = false


        // 第一次加载数据：一定要调用这个方法，否则不会触发加载更多。
        // 第一个参数：表示此次数据是否为空，假如你请求到的list为空(== null || list.size == 0)，那么这里就要true。
        // 第二个参数：表示是否还有更多数据，根据服务器返回给你的page等信息判断是否还有更多，这样可以提供性能，如果不能判断则传true。

        // 第一次加载数据：一定要调用这个方法，否则不会触发加载更多。
        // 第一个参数：表示此次数据是否为空，假如你请求到的list为空(== null || list.size == 0)，那么这里就要true。
        // 第二个参数：表示是否还有更多数据，根据服务器返回给你的page等信息判断是否还有更多，这样可以提供性能，如果不能判断则传true。
        binding.taskListRecyclerview.loadMoreFinish(true, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}