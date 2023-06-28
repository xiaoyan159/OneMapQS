package com.navinfo.omqs.ui.fragment.offlinemap

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.AdapterOfflineMapCityBinding
import com.navinfo.omqs.bean.OfflineMapCityBean
import com.navinfo.omqs.http.offlinemapdownload.OfflineMapDownloadManager
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.tools.FileManager.Companion.FileDownloadStatus
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder
import javax.inject.Inject

/**
 * 离线地图城市列表 RecyclerView 适配器
 *
 * 在 RecycleView 的 ViewHolder 中监听 ViewModel 的 LiveData，然后此时传递的 lifecycleOwner 是对应的 Fragment。由于 ViewHolder 的生命周期是比 Fragment 短的，所以当 ViewHolder 销毁时，由于 Fragment 的 Lifecycle 还没有结束，此时 ViewHolder 会发生内存泄露（监听的 LiveData 没有解绑）
 *   这种场景下有两种解决办法：
 *使用 LiveData 的 observeForever 然后在 ViewHolder 销毁前手动调用 removeObserver
 *使用 LifecycleRegistry 给 ViewHolder 分发生命周期(这里使用了这个)
 */
class OfflineMapCityListAdapter(
    private val downloadManager: OfflineMapDownloadManager, private val context: Context
) : BaseRecyclerViewAdapter<OfflineMapCityBean>() {


    private val downloadBtnClick = View.OnClickListener() {
        if (it.tag != null) {
            val cityBean = data[it.tag as Int]
            when (cityBean.status) {
                FileDownloadStatus.NONE, FileDownloadStatus.UPDATE, FileDownloadStatus.PAUSE, FileDownloadStatus.ERROR -> {
                    downloadManager.start(cityBean.id)
                }
                FileDownloadStatus.LOADING, FileDownloadStatus.WAITING -> {
                    downloadManager.pause(cityBean.id)
                }
                else -> {
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterOfflineMapCityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)
        //页面滑动时会用holder重构页面，但是对进度条的监听回调会一直返回，扰乱UI，所以当当前holder去重构的时候，移除监听
        downloadManager.removeObserver(holder.tag)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val binding: AdapterOfflineMapCityBinding =
            holder.viewBinding as AdapterOfflineMapCityBinding
        //牺牲性能立刻刷新UI，解决闪烁 这里不用
//        binding.executePendingBindings()
        val cityBean = data[position]
        //tag 方便onclick里拿到数据
        holder.tag = cityBean.id

        changeViews(binding, cityBean)
        downloadManager.addTask(cityBean)
        downloadManager.observer(cityBean.id, holder, DownloadObserver(cityBean.id, holder))
        binding.offlineMapDownloadBtn.tag = position
        binding.offlineMapDownloadBtn.setOnClickListener(downloadBtnClick)
        binding.offlineMapCityName.text = cityBean.name
        binding.offlineMapCitySize.text = cityBean.getFileSizeText()
    }

    inner class DownloadObserver(val id: String, val holder: BaseViewHolder) :
        Observer<OfflineMapCityBean> {
        override fun onChanged(offlineMapCityBean: OfflineMapCityBean?) {
            offlineMapCityBean?.let { bean ->
                if (id == holder.tag) {
                    val binding: AdapterOfflineMapCityBinding =
                        holder.viewBinding as AdapterOfflineMapCityBinding
                    changeViews(binding, bean)
                }
            }

        }
    }


    private fun changeViews(binding: AdapterOfflineMapCityBinding, cityBean: OfflineMapCityBean) {
        binding.offlineMapProgress.progress =
            (cityBean.currentSize * 100 / cityBean.fileSize).toInt()
        when (cityBean.status) {
            FileDownloadStatus.NONE -> {
                if (binding.offlineMapProgress.visibility == View.VISIBLE) binding.offlineMapProgress.visibility =
                    View.INVISIBLE
                binding.offlineMapDownloadBtn.text = "下载"
            }
            FileDownloadStatus.WAITING -> {
                if (binding.offlineMapProgress.visibility != View.VISIBLE) binding.offlineMapProgress.visibility =
                    View.VISIBLE
                binding.offlineMapDownloadBtn.text = "等待中"
            }
            FileDownloadStatus.LOADING -> {
                if (binding.offlineMapProgress.visibility != View.VISIBLE) binding.offlineMapProgress.visibility =
                    View.VISIBLE
                binding.offlineMapDownloadBtn.text = "暂停"
            }
            FileDownloadStatus.PAUSE -> {
                if (binding.offlineMapProgress.visibility != View.VISIBLE) binding.offlineMapProgress.visibility =
                    View.VISIBLE
                binding.offlineMapDownloadBtn.text = "继续"
            }
            FileDownloadStatus.ERROR -> {
                if (binding.offlineMapProgress.visibility != View.VISIBLE) binding.offlineMapProgress.visibility =
                    View.VISIBLE
                binding.offlineMapDownloadBtn.text = "重试"
            }
            FileDownloadStatus.DONE -> {
                if (binding.offlineMapProgress.visibility == View.VISIBLE) binding.offlineMapProgress.visibility =
                    View.INVISIBLE
                binding.offlineMapDownloadBtn.text = "已完成"
            }
            FileDownloadStatus.UPDATE -> {
                if (binding.offlineMapProgress.visibility == View.VISIBLE) binding.offlineMapProgress.visibility =
                    View.INVISIBLE
                binding.offlineMapDownloadBtn.text = "更新"
            }
        }
    }
}


