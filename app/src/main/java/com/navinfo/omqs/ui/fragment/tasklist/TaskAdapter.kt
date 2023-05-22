package com.navinfo.omqs.ui.fragment.tasklist

import android.view.LayoutInflater
import android.view.ViewGroup
import com.navinfo.collect.library.data.entity.HadLinkDvoBean
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.AdapterTaskBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder


interface TaskAdapterCallback {
    fun itemOnClick(bean: HadLinkDvoBean)
    fun editOnclick(position: Int, bean: HadLinkDvoBean)
}

/**
 * 当前任务适配器
 */
class TaskAdapter(
    private val callback: TaskAdapterCallback
) : BaseRecyclerViewAdapter<HadLinkDvoBean>() {
    private var selectPosition = -1


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val binding: AdapterTaskBinding =
            holder.viewBinding as AdapterTaskBinding
        val bean = data[position]
        binding.taskLinkPid.text = "PID:${bean.linkPid}"
        binding.taskMesh.text = "mesh:${bean.mesh}"
        binding.root.isSelected = selectPosition == position
        binding.root.setOnClickListener {
            val pos = holder.adapterPosition
            if (selectPosition != pos) {
                val lastPos = selectPosition
                selectPosition = pos
                if (lastPos > -1 && lastPos < itemCount) {
                    notifyItemChanged(lastPos)
                }
                binding.root.isSelected = true
                callback.itemOnClick(bean)
            }
        }
        binding.taskEdit.isSelected = bean.reason != ""
        binding.taskEdit.setOnClickListener {
            callback.editOnclick(position, bean)
        }
    }

    override fun getItemViewRes(position: Int): Int {
        return R.layout.adapter_task
    }

    fun resetSelect() {
        selectPosition = -1
    }
}


