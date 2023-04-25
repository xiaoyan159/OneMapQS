package com.navinfo.omqs.ui.fragment.tasklist

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.TaskBean
import com.navinfo.omqs.databinding.AdapterQsRecordListBinding
import com.navinfo.omqs.databinding.AdapterTaskListBinding
import com.navinfo.omqs.http.taskdownload.TaskDownloadManager
import com.navinfo.omqs.tools.FileManager.Companion.FileDownloadStatus
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
class QsRecordListAdapter(
    private val context: Context
) : BaseRecyclerViewAdapter<QsRecordBean>() {

    private var itemClickListener: IKotlinItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterQsRecordListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)
        //页面滑动时会用holder重构页面，但是对进度条的监听回调会一直返回，扰乱UI，所以当当前holder去重构的时候，移除监听
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val binding: AdapterQsRecordListBinding =
            holder.viewBinding as AdapterQsRecordListBinding
        val qsRecordBean = data[position]
        //tag 方便onclick里拿到数据
        holder.tag = qsRecordBean.id.toString()
        // 点击事件
        holder.itemView.setOnClickListener {
            itemClickListener!!.onItemClickListener(position)
        }
        changeViews(binding, qsRecordBean)
    }

    private fun changeViews(binding: AdapterQsRecordListBinding, qsRecordBean: QsRecordBean) {
        binding.qsRecordClassType.text = qsRecordBean.classType
        binding.qsRecordProblemType.text = qsRecordBean.problemType
        binding.qsRecordPhenomenon.text = qsRecordBean.phenomenon
        binding.qsRecordProblemLink.text = qsRecordBean.problemLink
    }

    override fun getItemViewRes(position: Int): Int {
        return R.layout.adapter_qs_record_list
    }

    // 提供set方法
    fun setOnKotlinItemClickListener(itemClickListener: IKotlinItemClickListener) {
        this.itemClickListener = itemClickListener
    }

    //自定义接口
    interface IKotlinItemClickListener {
        fun onItemClickListener(position: Int)
    }
}


