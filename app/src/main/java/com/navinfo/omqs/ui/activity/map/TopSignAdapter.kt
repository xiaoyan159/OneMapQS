package com.navinfo.omqs.ui.activity.map

import android.view.LayoutInflater
import android.view.ViewGroup
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.databinding.AdapterTopSignBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

class TopSignAdapter(private var itemListener: ((Int, SignBean) -> Unit?)? = null) :
    BaseRecyclerViewAdapter<SignBean>() {
    override fun getItemViewRes(position: Int): Int {
        return R.layout.adapter_top_sign
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterTopSignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val bd = holder.viewBinding as AdapterTopSignBinding
        val item = data[position]
        if (item.iconId != 0)
            bd.topSignText.background = holder.viewBinding.root.context.getDrawable(item.iconId)
        bd.topSignName.text = item.name
        bd.topSignText.text = item.iconText
        bd.root.setOnClickListener {
            itemListener?.invoke(position, item)
        }
    }
}