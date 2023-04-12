package com.navinfo.omqs.ui.fragment.evaluationresult

import android.view.LayoutInflater
import android.view.ViewGroup
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.TextItemSelectBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

class PhenomenonAdapter() : BaseRecyclerViewAdapter<String>() {
    override fun getItemViewRes(position: Int): Int {
        return R.layout.text_item_select
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            TextItemSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        (holder.viewBinding as TextItemSelectBinding).itemId.text = data[position]
    }
}