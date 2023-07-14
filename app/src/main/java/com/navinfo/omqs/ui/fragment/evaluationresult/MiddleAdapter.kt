package com.navinfo.omqs.ui.fragment.evaluationresult

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.TextItemSelectBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

class MiddleAdapter(private var itemListener: ((Int, String) -> Unit?)? = null) :
    BaseRecyclerViewAdapter<String>() {
    private var selectTitle = ""


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            TextItemSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val bd = holder.viewBinding as TextItemSelectBinding
        val title = data[position]
        bd.itemId.text = title
        val layoutParams: ViewGroup.LayoutParams = holder.viewBinding.itemLayout.layoutParams
        layoutParams.width = 115
        bd.itemLayout.layoutParams = layoutParams
        if (selectTitle == title) {
            bd.itemId.setBackgroundResource(R.drawable.shape_bg_blue_bg_4_radius)
            bd.itemId.setTextColor(holder.viewBinding.root.context.resources.getColor(R.color.white))
        } else {
            bd.itemId.setBackgroundResource(R.drawable.shape_rect_white_2dp_bg)
            bd.itemId.setTextColor(holder.viewBinding.root.context.resources.getColor(R.color.black))
        }
        bd.root.setOnClickListener {
            if (selectTitle != title) {
                selectTitle = title
                notifyDataSetChanged()
            }
            itemListener?.invoke(position, title)
        }
    }

    override fun refreshData(newData: List<String>) {
        data = newData
        selectTitle = newData[0]
        notifyDataSetChanged()
    }

    fun setRightTitle(title: String) {
        if (title != selectTitle) {
            selectTitle = title
            notifyDataSetChanged()
        }
    }
}