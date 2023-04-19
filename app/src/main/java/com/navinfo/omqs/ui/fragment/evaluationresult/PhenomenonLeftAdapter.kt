package com.navinfo.omqs.ui.fragment.evaluationresult

import android.view.LayoutInflater
import android.view.ViewGroup
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.TextItemSelectBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

class PhenomenonLeftAdapter(private var itemListener: ((Int, String) -> Unit?)? = null) :
    BaseRecyclerViewAdapter<String>() {
    private var selectTitle = ""

    override fun getItemViewRes(position: Int): Int {
        return R.layout.text_item_select
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            TextItemSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val bd = holder.viewBinding as TextItemSelectBinding
        val title = data[position]
        bd.itemId.text = title
        if (selectTitle == title) {
            bd.itemId.setBackgroundColor(holder.viewBinding.root.context.getColor(R.color.cv_gray_153))
        } else {
            bd.itemId.setBackgroundColor(holder.viewBinding.root.context.getColor(R.color.white))
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