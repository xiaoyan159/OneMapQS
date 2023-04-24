package com.navinfo.omqs.ui.fragment.tasklist

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.TaskBean
import com.navinfo.omqs.databinding.AdapterTaskListBinding
import com.navinfo.omqs.http.taskdownload.TaskDownloadManager
import com.navinfo.omqs.tools.FileManager
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
    private val downloadManager: TaskDownloadManager, private val context: Context
) : BaseRecyclerViewAdapter<TaskBean>() {


    private val downloadBtnClick = View.OnClickListener() {
        if (it.tag != null) {
            val taskBean = data[it.tag as Int]
            when (taskBean.status) {
                FileManager.Companion.FileDownloadStatus.NONE, FileManager.Companion.FileDownloadStatus.UPDATE, FileManager.Companion.FileDownloadStatus.PAUSE, FileManager.Companion.FileDownloadStatus.ERROR -> {
                    Log.e("jingo", "开始下载 ${taskBean.status}")
                    downloadManager.start(taskBean.id)
                }
                FileManager.Companion.FileDownloadStatus.LOADING, FileManager.Companion.FileDownloadStatus.WAITING -> {
                    Log.e("jingo", "暂停 ${taskBean.status}")
                    downloadManager.pause(taskBean.id)
                }
                else -> {
                    Log.e("jingo", "暂停 ${taskBean.status}")
                }
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
        downloadManager.observer(taskBean.id, holder, DownloadObserver(taskBean.id, binding))
        binding.taskDownloadBtn.tag = position
        binding.taskDownloadBtn.setOnClickListener(downloadBtnClick)
        binding.taskName.text = taskBean.evaluationTaskName
//        binding.offlineMapCitySize.text = cityBean.getFileSizeText()
    }

    inner class DownloadObserver(val id: Int, val binding: AdapterTaskListBinding) :
        Observer<TaskBean> {
        override fun onChanged(t: TaskBean?) {
            if (id == t?.id)
                changeViews(binding, t)
        }
    }


    private fun changeViews(binding: AdapterTaskListBinding, cityBean: TaskBean) {
        binding.taskProgress.progress =
            (cityBean.currentSize * 100 / cityBean.fileSize).toInt()
        when (cityBean.status) {
            FileManager.Companion.FileDownloadStatus.NONE -> {
                if (binding.taskProgress.visibility == View.VISIBLE) binding.taskProgress.visibility =
                    View.INVISIBLE
                binding.taskDownloadBtn.text = "下载"
            }
            FileManager.Companion.FileDownloadStatus.WAITING -> {
                if (binding.taskProgress.visibility != View.VISIBLE) binding.taskProgress.visibility =
                    View.VISIBLE
                binding.taskDownloadBtn.text = "等待中"
            }
            FileManager.Companion.FileDownloadStatus.LOADING -> {
                if (binding.taskProgress.visibility != View.VISIBLE) binding.taskProgress.visibility =
                    View.VISIBLE
                binding.taskDownloadBtn.text = "暂停"
            }
            FileManager.Companion.FileDownloadStatus.PAUSE -> {
                if (binding.taskProgress.visibility != View.VISIBLE) binding.taskProgress.visibility =
                    View.VISIBLE
                binding.taskDownloadBtn.text = "继续"
            }
            FileManager.Companion.FileDownloadStatus.ERROR -> {
                if (binding.taskProgress.visibility != View.VISIBLE) binding.taskProgress.visibility =
                    View.VISIBLE
                binding.taskDownloadBtn.text = "重试"
            }
            FileManager.Companion.FileDownloadStatus.DONE -> {
                if (binding.taskProgress.visibility == View.VISIBLE) binding.taskProgress.visibility =
                    View.INVISIBLE
                binding.taskDownloadBtn.text = "已完成"
            }
            FileManager.Companion.FileDownloadStatus.UPDATE -> {
                if (binding.taskProgress.visibility == View.VISIBLE) binding.taskProgress.visibility =
                    View.INVISIBLE
                binding.taskDownloadBtn.text = "更新"
            }
        }
    }

    override fun getItemViewRes(position: Int): Int {
        return R.layout.adapter_offline_map_city
    }
}


