package com.navinfo.omqs.ui.fragment.evaluationresult

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.TextItemSelectBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

class LeftAdapter(private var itemListener: ((Int, String) -> Unit?)? = null) :
    BaseRecyclerViewAdapter<String>() {
    private var selectTitle = ""


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            TextItemSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val bd = holder.viewBinding as TextItemSelectBinding
        val title = data[position]
        bd.itemId.text = title
        holder.viewBinding.root.isSelected = selectTitle == title
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
        notifyDataSetChanged()
    }

    fun setSelectTitle(title: String) {
        if (title != selectTitle) {
            selectTitle = title
        }
    }
}