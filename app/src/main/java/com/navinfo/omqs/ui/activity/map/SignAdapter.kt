package com.navinfo.omqs.ui.activity.map

import android.view.LayoutInflater
import android.view.ViewGroup
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.databinding.AdapterSignBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

class SignAdapter(private var itemListener: ((Int, SignBean) -> Unit?)? = null) :
    BaseRecyclerViewAdapter<SignBean>() {
    override fun getItemViewRes(position: Int): Int {
        return R.layout.adapter_sign
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterSignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val bd = holder.viewBinding as AdapterSignBinding
        val item = data[position]
        bd.signMainIcon.background = holder.viewBinding.root.context.getDrawable(item.iconId)
        bd.signMainIcon.text = item.iconText
        bd.signBottomText.text = item.bottomText
        bd.root.setOnClickListener {
            itemListener?.invoke(position, item)
        }
    }
}