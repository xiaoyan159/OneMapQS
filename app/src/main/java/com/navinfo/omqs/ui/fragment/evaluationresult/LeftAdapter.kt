package com.navinfo.omqs.ui.fragment.evaluationresult

import android.view.LayoutInflater
import android.view.ViewGroup
import com.navinfo.omqs.bean.ScProblemTypeBean
import com.navinfo.omqs.databinding.TextItemSelectBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

class LeftAdapter(private var itemListener: ((Int, ScProblemTypeBean) -> Unit?)? = null) :
    BaseRecyclerViewAdapter<ScProblemTypeBean>() {
    private var selectTitle = ""


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            TextItemSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val bd = holder.viewBinding as TextItemSelectBinding
        val title = data[position]
        bd.itemId.text = title.classType
        holder.viewBinding.root.isSelected = selectTitle == title.classType
        bd.root.setOnClickListener {
            if (selectTitle != title.classType) {
                selectTitle = title.classType
                notifyDataSetChanged()
            }
            itemListener?.invoke(position, title)
        }
    }

    override fun refreshData(newData: List<ScProblemTypeBean>) {
        data = newData
        notifyDataSetChanged()
    }

    fun setSelectTitle(title: String) {
        if (title != selectTitle) {
            selectTitle = title
        }
    }
}