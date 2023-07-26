package com.navinfo.omqs.ui.fragment.tasklink

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.AdapterElectronicEyeBinding
import com.navinfo.omqs.databinding.AdapterTaskLinkInfoBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

data class TaskLinkInfoAdapterItem(
    val title: String,
    val type: Int
)

class TaskLinkMiddleAdapter(private var itemListener: ((Int, TaskLinkInfoAdapterItem) -> Unit?)? = null) :
    BaseRecyclerViewAdapter<TaskLinkInfoAdapterItem>() {

    private var selectTitle = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterTaskLinkInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val binding = holder.viewBinding as AdapterTaskLinkInfoBinding
        binding.title.text = data[position].title

        if (selectTitle == binding.title.text) {
            binding.title.setBackgroundResource(R.drawable.shape_bg_blue_bg_4_radius)
            binding.title.setTextColor(holder.viewBinding.root.context.resources.getColor(R.color.white))
        } else {
            binding.title.setBackgroundResource(R.drawable.shape_rect_white_2dp_bg)
            binding.title.setTextColor(holder.viewBinding.root.context.resources.getColor(R.color.black))
        }
        binding.root.setOnClickListener {
            if (selectTitle != data[position].title) {
                selectTitle = data[position].title
                notifyDataSetChanged()
            }
            itemListener?.invoke(position, data[position])
        }
    }
}