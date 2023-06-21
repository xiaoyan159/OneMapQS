package com.navinfo.omqs.ui.fragment.sign

import android.view.LayoutInflater
import android.view.ViewGroup
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.RoadNameBean
import com.navinfo.omqs.databinding.AdapterRoadNameBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

class RoadNameInfoAdapter : BaseRecyclerViewAdapter<RoadNameBean>() {
    override fun getItemViewRes(position: Int): Int {
        return R.layout.adapter_road_name
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterRoadNameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val binding: AdapterRoadNameBinding =
            holder.viewBinding as AdapterRoadNameBinding
        val bean = data[position]
        binding.title.text = bean.getNameClassStr()
        binding.name.text = bean.name
        binding.type.text = bean.getTypeStr()
    }
}