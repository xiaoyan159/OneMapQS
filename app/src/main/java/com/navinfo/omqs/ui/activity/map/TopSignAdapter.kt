package com.navinfo.omqs.ui.activity.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.databinding.AdapterTopSignBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

class TopSignAdapter(private var itemListener: ((Int, SignBean) -> Unit?)? = null) :
    BaseRecyclerViewAdapter<SignBean>() {

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
        when (item.renderEntity.code) {
            DataCodeEnum.OMDB_CON_ACCESS.code,
            DataCodeEnum.OMDB_MULTI_DIGITIZED.code,
            DataCodeEnum.OMDB_TUNNEL.code,
            DataCodeEnum.OMDB_ROUNDABOUT.code,
            DataCodeEnum.OMDB_VIADUCT.code,
            -> bd.topSignName.text = "形态"
            else -> bd.topSignName.text = item.name
        }

        bd.topSignText.text = item.iconText
        if (data.size == 1) {
            bd.topSignLeftLine.visibility = View.GONE
            bd.topSignRightLine.visibility = View.GONE
            bd.topSignName.background =
                holder.viewBinding.root.context.getDrawable(R.drawable.shape_road_info_top_bg)
            bd.topSignText.background =
                holder.viewBinding.root.context.getDrawable(R.drawable.shape_road_info_bottom_bg)
        } else if (position == 0) {
            bd.topSignLeftLine.visibility = View.GONE
            bd.topSignRightLine.visibility = View.VISIBLE
            bd.topSignName.background =
                holder.viewBinding.root.context.getDrawable(R.drawable.shape_road_info_left_top_bg)
            bd.topSignText.background =
                holder.viewBinding.root.context.getDrawable(R.drawable.shape_road_info_left_bottom_bg)
        } else if (position == data.size - 1) {
            bd.topSignLeftLine.visibility = View.VISIBLE
            bd.topSignRightLine.visibility = View.GONE
            bd.topSignName.background =
                holder.viewBinding.root.context.getDrawable(R.drawable.shape_road_info_right_top_bg)
            bd.topSignText.background =
                holder.viewBinding.root.context.getDrawable(R.drawable.shape_road_info_right_bottom_bg)
        } else {
            bd.topSignLeftLine.visibility = View.VISIBLE
            bd.topSignRightLine.visibility = View.VISIBLE
            bd.topSignName.background =
                holder.viewBinding.root.context.getDrawable(R.drawable.shape_road_info_middle_top_bg)
            bd.topSignText.background =
                holder.viewBinding.root.context.getDrawable(R.drawable.shape_road_info_middle_bottom_bg)
        }

        bd.root.setOnClickListener {
            itemListener?.invoke(position, item)
        }
    }
}