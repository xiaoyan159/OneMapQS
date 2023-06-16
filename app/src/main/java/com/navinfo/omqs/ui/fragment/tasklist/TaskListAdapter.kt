package com.navinfo.omqs.ui.fragment.tasklist

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.AdapterTaskListBinding
import com.navinfo.omqs.http.taskdownload.TaskDownloadManager
import com.navinfo.omqs.http.taskupload.TaskUploadManager
import com.navinfo.omqs.tools.FileManager.Companion.FileDownloadStatus
import com.navinfo.omqs.tools.FileManager.Companion.FileUploadStatus
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

/**
 * 离线地图城市列表 RecyclerView 适配器
 *
 * 在 RecycleView 的 ViewHolder 中监听 ViewModel 的 LiveData，然后此时传递的 lifecycleOwner 是对应的 Fragment。由于 ViewHolder 的生命周期是比 Fragment 短的，所以当 ViewHolder 销毁时，由于 Fragment 的 Lifecycle 还没有结束，此时 ViewHolder 会发生内存泄露（监听的 LiveData 没有解绑）
 *   这种场景下有两种解决办法：
 *使用 LiveData 的 observeForever 然后在 ViewHolder 销毁前手动调用 removeObserver
 *使用 LifecycleRegistry 给 ViewHolder 分发生命周期(这里使用了这个)
 */
class TaskListAdapter(
    private val downloadManager: TaskDownloadManager,
    private val uploadManager: TaskUploadManager,
    private var itemListener: ((Int, TaskBean) -> Unit?)? = null
) : BaseRecyclerViewAdapter<TaskBean>() {
    private var selectPosition = -1

    private
    val downloadBtnClick = View.OnClickListener() {
        if (it.tag != null) {
            val taskBean = data[it.tag as Int]
            if (taskBean.hadLinkDvoList.isNotEmpty()) {
                when (taskBean.status) {
                    FileDownloadStatus.NONE, FileDownloadStatus.UPDATE, FileDownloadStatus.PAUSE, FileDownloadStatus.IMPORT, FileDownloadStatus.ERROR -> {
                        Log.e("jingo", "开始下载 ${taskBean.status}")
                        downloadManager.start(taskBean.id)
                    }

                    FileDownloadStatus.LOADING, FileDownloadStatus.WAITING -> {
                        Log.e("jingo", "暂停 ${taskBean.status}")
                        downloadManager.pause(taskBean.id)
                    }

                    else -> {
                        Log.e("jingo", "暂停 ${taskBean.status}")
                    }
                }
            } else {
                Toast.makeText(downloadManager.context, "数据错误，无Link信息，无法执行下载！", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val uploadBtnClick = View.OnClickListener() {
        if (it.tag != null) {
            val taskBean = data[it.tag as Int]
            Log.e("jingo", "开始上传 ${taskBean.syncStatus}")
            if (taskBean.hadLinkDvoList.isNotEmpty()) {
                when (taskBean.syncStatus) {
                    FileUploadStatus.NONE, FileUploadStatus.UPLOADING, FileUploadStatus.ERROR, FileUploadStatus.WAITING -> {
                        uploadManager.start(taskBean.id)
                    }
                }
            }else{
                Toast.makeText(uploadManager.context, "数据错误，无Link信息，无法执行同步！", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterTaskListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)
        //页面滑动时会用holder重构页面，但是对进度条的监听回调会一直返回，扰乱UI，所以当当前holder去重构的时候，移除监听
        downloadManager.removeObserver(holder.tag.toInt())
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val binding: AdapterTaskListBinding =
            holder.viewBinding as AdapterTaskListBinding
        val taskBean = data[position]
        //tag 方便onclick里拿到数据
        holder.tag = taskBean.id.toString()
        changeViews(binding, taskBean)
        downloadManager.addTask(taskBean)
        downloadManager.observer(taskBean.id, holder, DownloadObserver(taskBean.id, holder))
        uploadManager.addTask(taskBean)
        uploadManager.observer(taskBean.id, holder, UploadObserver(taskBean.id, binding))
        if (taskBean.status == FileDownloadStatus.NONE) {
            binding.taskDownloadBtn.setBackgroundColor(Color.WHITE)
            binding.taskDownloadBtn.setTextColor(Color.parseColor("#888FB3"))
        } else {
            binding.taskDownloadBtn.setBackgroundColor(Color.parseColor("#888FB3"))
            binding.taskDownloadBtn.setTextColor(Color.WHITE)
        }
        if (taskBean.status == FileDownloadStatus.DONE) {
            binding.taskDownloadBtn.visibility = View.INVISIBLE
            binding.taskUploadBtn.visibility = View.VISIBLE
        } else {
            binding.taskDownloadBtn.visibility = View.VISIBLE
            binding.taskUploadBtn.visibility = View.INVISIBLE
        }
        if (taskBean.syncStatus == FileUploadStatus.DONE) {
            binding.taskUploadBtn.setProgress(0)
            binding.taskUploadBtn.setBackgroundColor(binding.root.resources.getColor(R.color.ripple_end_color))
        } else {
            binding.taskUploadBtn.setProgress(100)
            binding.taskUploadBtn.setBackgroundColor(Color.parseColor("#888FB3"))
        }
        binding.taskDownloadBtn.tag = position
        binding.taskDownloadBtn.setOnClickListener(downloadBtnClick)
        binding.taskUploadBtn.tag = position
        binding.taskUploadBtn.setOnClickListener(uploadBtnClick)
        binding.taskName.text = taskBean.evaluationTaskName
        binding.taskCityName.text = taskBean.cityName
        binding.taskDataVersion.text = "版本号：${taskBean.dataVersion}"
        binding.root.isSelected = selectPosition == position
        binding.root.setOnClickListener {
            val pos = holder.adapterPosition
            if (selectPosition != pos) {
                val lastPos = selectPosition
                selectPosition = pos
                if (lastPos > -1) {
                    notifyItemChanged(lastPos)
                }
                binding.root.isSelected = true
                itemListener?.invoke(position, taskBean)
            }

        }
    }

    inner class DownloadObserver(val id: Int, val holder: BaseViewHolder) :
        Observer<TaskBean> {
        override fun onChanged(taskBean: TaskBean?) {
            taskBean?.let { bean ->
                if (id.toString() == holder.tag) {
                    val binding: AdapterTaskListBinding =
                        holder.viewBinding as AdapterTaskListBinding
                    changeViews(binding, bean)
                }
            }
        }
    }

    inner class UploadObserver(val id: Int, val binding: AdapterTaskListBinding) :
        Observer<TaskBean> {
        override fun onChanged(t: TaskBean?) {
            if (id == t?.id)
                changeUploadTxtViews(binding, t)
        }
    }

    private fun changeUploadTxtViews(binding: AdapterTaskListBinding, taskBean: TaskBean) {
        when (taskBean.syncStatus) {
            FileUploadStatus.DONE -> {
                binding.taskUploadBtn.stopAnimator()
                binding.taskUploadBtn.setText("已上传")
                binding.taskUploadBtn.setProgress(0)
                binding.taskUploadBtn.setBackgroundColor(binding.root.resources.getColor(R.color.ripple_end_color))
            }

            FileUploadStatus.ERROR -> {
                binding.taskUploadBtn.stopAnimator()
                binding.taskUploadBtn.setText("重新同步")
                binding.taskUploadBtn.setProgress(100)
            }

            FileUploadStatus.NONE -> {
                binding.taskUploadBtn.setText("未上传")
                binding.taskUploadBtn.setProgress(0)
            }

            FileUploadStatus.WAITING -> {
                binding.taskUploadBtn.setText("等待同步")
                binding.taskUploadBtn.setProgress(100)
            }

            FileUploadStatus.UPLOADING -> {
                binding.taskUploadBtn.setText("上传中")
                binding.taskUploadBtn.setProgress(100)
                binding.taskUploadBtn.startAnimator()
            }
        }
    }


    private fun changeViews(binding: AdapterTaskListBinding, taskBean: TaskBean) {
        if (taskBean.status == FileDownloadStatus.NONE) {
            binding.taskDownloadBtn.setBackgroundColor(Color.WHITE)
            binding.taskDownloadBtn.setTextColor(Color.parseColor("#888FB3"))
        } else {
            binding.taskDownloadBtn.setBackgroundColor(Color.parseColor("#888FB3"))
            binding.taskDownloadBtn.setTextColor(Color.WHITE)
        }

        if (taskBean.fileSize > 0L) {
            val progress = (taskBean.currentSize * 100 / taskBean.fileSize).toInt()
            binding.taskProgressText.text =
                "$progress%"
            binding.taskDownloadBtn.setProgress(progress)
        }
        when (taskBean.status) {
            FileDownloadStatus.NONE -> {
                if (binding.taskProgressText.visibility == View.VISIBLE) binding.taskProgressText.visibility =
                    View.INVISIBLE
                binding.taskDownloadBtn.setText("下载")
            }

            FileDownloadStatus.WAITING -> {
                if (binding.taskProgressText.visibility != View.VISIBLE) binding.taskProgressText.visibility =
                    View.VISIBLE
                binding.taskDownloadBtn.setText("等待中")
            }

            FileDownloadStatus.LOADING -> {
                if (binding.taskProgressText.visibility != View.VISIBLE) binding.taskProgressText.visibility =
                    View.VISIBLE
                binding.taskDownloadBtn.setText("暂停")
            }

            FileDownloadStatus.PAUSE -> {
                if (binding.taskProgressText.visibility != View.VISIBLE) binding.taskProgressText.visibility =
                    View.VISIBLE
                binding.taskDownloadBtn.setText("继续")
            }

            FileDownloadStatus.ERROR -> {
                if (binding.taskProgressText.visibility != View.VISIBLE) binding.taskProgressText.visibility =
                    View.VISIBLE
                binding.taskDownloadBtn.setText("重试")
            }

            FileDownloadStatus.DONE -> {
                if (binding.taskProgressText.visibility == View.VISIBLE) binding.taskProgressText.visibility =
                    View.INVISIBLE
                binding.taskDownloadBtn.setText("已完成")
                binding.taskDownloadBtn.visibility = View.INVISIBLE
                binding.taskUploadBtn.visibility = View.VISIBLE
            }

            FileDownloadStatus.UPDATE -> {
                if (binding.taskProgressText.visibility == View.VISIBLE) binding.taskProgressText.visibility =
                    View.INVISIBLE
                binding.taskDownloadBtn.setText("更新")
            }

            FileDownloadStatus.IMPORTING -> {
                if (binding.taskProgressText.visibility != View.VISIBLE) binding.taskProgressText.visibility =
                    View.VISIBLE
                binding.taskDownloadBtn.setText("安装中")
                val split = taskBean.message.split("/")
                if (split.size == 2) {
                    try {
                        val index = split[0].toInt()
                        val count = split[1].toInt()
                        binding.taskProgressText.text =
                            "${index * 100 / count}%"
                    } catch (e: Exception) {
                        Log.e("jingo", "更新进度条 $e")
                    }
                } else {
                    binding.taskProgressText.text = "0%"
                }
                val errMsg = taskBean.errMsg
                if (errMsg != null && errMsg.isNotEmpty()) {
                    Toast.makeText(binding.taskProgressText.context, errMsg, Toast.LENGTH_LONG).show()
                }
            }

            FileDownloadStatus.IMPORT -> {
                if (binding.taskProgressText.visibility != View.VISIBLE) binding.taskProgressText.visibility =
                    View.INVISIBLE
                binding.taskDownloadBtn.setText("安装")
            }
        }
    }

    override fun getItemViewRes(position: Int): Int {
        return R.layout.adapter_task_list
    }
}


