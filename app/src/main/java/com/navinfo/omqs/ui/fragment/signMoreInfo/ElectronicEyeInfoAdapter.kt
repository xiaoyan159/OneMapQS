package com.navinfo.omqs.ui.fragment.signMoreInfo

import android.view.LayoutInflater
import android.view.ViewGroup
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.AdapterElectronicEyeBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

data class ElectronicEyeMoreInfoAdapterItem(
    val title: String,
    val text: String
)

class ElectronicEyeInfoAdapter : BaseRecyclerViewAdapter<ElectronicEyeMoreInfoAdapterItem>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterElectronicEyeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val binding: AdapterElectronicEyeBinding =
            holder.viewBinding as AdapterElectronicEyeBinding
        val item = data[position]
        binding.title.text = item.title
        binding.text.text = item.text
    }
}