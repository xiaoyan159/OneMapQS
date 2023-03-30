package com.navinfo.omqs.ui.fragment.offlinemap

import androidx.databinding.ViewDataBinding
import com.navinfo.omqs.R
import com.navinfo.omqs.BR
import com.navinfo.omqs.bean.OfflineMapCityBean
import com.navinfo.omqs.databinding.AdapterOfflineMapCityBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder
import javax.inject.Inject

/**
 * 离线地图城市列表 RecyclerView 适配器
 */

class OfflineMapCityListAdapter @Inject constructor() :
    BaseRecyclerViewAdapter<OfflineMapCityBean>() {
    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        var binding: ViewDataBinding = holder.dataBinding
        //立刻刷新UI，解决闪烁
//        binding.executePendingBindings()
        binding.setVariable(BR.cityBean, data[position])
        (binding as AdapterOfflineMapCityBinding).offlineMapDownloadBtn.setOnClickListener {

        }

    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.adapter_offline_map_city
    }

}