package com.navinfo.omqs.ui.fragment.signMoreInfo

import android.view.LayoutInflater
import android.view.ViewGroup
import com.navinfo.omqs.databinding.AdapterTwoItemBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

data class TwoItemAdapterItem(
    val title: String,
    val text: String
)

class TwoItemAdapter : BaseRecyclerViewAdapter<TwoItemAdapterItem>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterTwoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val binding: AdapterTwoItemBinding =
            holder.viewBinding as AdapterTwoItemBinding
        val item = data[position]
        binding.title.text = item.title
        binding.text.text = item.text
    }
}