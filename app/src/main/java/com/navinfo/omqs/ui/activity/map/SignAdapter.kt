package com.navinfo.omqs.ui.activity.map

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.databinding.AdapterSignBinding
import com.navinfo.omqs.databinding.AdapterSignLaneinfoBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder
import com.navinfo.omqs.ui.widget.SignUtil

interface OnSignAdapterClickListener {
    fun onItemClick(signBean: SignBean)
    fun onMoreInfoClick(selectTag: String, tag: String, signBean: SignBean)
    fun onErrorClick(signBean: SignBean)
}

data class LaneInfoItem(val id: Int, val type: Int)

class SignAdapter(private var listener: OnSignAdapterClickListener?) :
    BaseRecyclerViewAdapter<SignBean>() {
    /**
     * 选中的详细信息按钮的tag标签
     */
    private var selectMoreInfoTag: String = ""

    override fun getItemViewType(position: Int): Int {
        if (data.isNotEmpty() && data[position].renderEntity.code == 4601) {
            return 4601
        }
        return 0
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return if (viewType == 4601) {
            val viewBinding =
                AdapterSignLaneinfoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            BaseViewHolder(viewBinding)
        } else {
            val viewBinding =
                AdapterSignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            BaseViewHolder(viewBinding)
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val context = holder.viewBinding.root.context
        val item = data[position]
        if (holder.viewBinding is AdapterSignBinding) {
            val bd = holder.viewBinding

            if (item.iconId != 0) bd.signMainIconBg.setImageResource(item.iconId)
            bd.signMainIcon.text = item.iconText
            bd.signBottomText.text = item.name
            //点击错误按钮
            bd.signMainFastError.setOnClickListener {
                listener?.onErrorClick(item)
            }
            bd.signBottomRightText.text = item.bottomRightText
            if (item.isMoreInfo) {
                bd.signMainInfo.visibility = View.VISIBLE
                bd.signMainInfo.setOnClickListener {
                    listener?.onMoreInfoClick(selectMoreInfoTag, holder.tag, item)
                    selectMoreInfoTag = holder.tag
                }
            } else {
                bd.signMainInfo.visibility = View.GONE
            }
            bd.signSecondIcon.text = ""
            if (item.renderEntity.code == 4002) {
                val minSpeed = SignUtil.getSpeedLimitMinText(item.renderEntity)
                if (minSpeed != "0") {
                    bd.signSecondIcon.text = minSpeed
                }
            }
        } else if (holder.viewBinding is AdapterSignLaneinfoBinding) {
            val bd = holder.viewBinding
            bd.signMoreIconsLayout.removeAllViews()
            bd.signBottomText.text = item.name
            bd.signBottomRightText.text = item.distance.toString()
            val list = SignUtil.getLineInfoIcons(item.renderEntity)
            val lineViewS = View(context)
            lineViewS.layoutParams = ViewGroup.LayoutParams(24, 80)
            lineViewS.background = context.getDrawable(R.drawable.shape_vertical_dashed_line)
            bd.signMoreIconsLayout.addView(lineViewS, lineViewS.layoutParams)
            for (i in list.indices) {
                val laneInfo = list[i]
                val imageView = ImageView(context)
                val drawable = context.getDrawable(laneInfo.id)
                var color = when (laneInfo.type) {
                    1 -> bd.root.resources.getColor(R.color.lane_info_1)
                    2 -> bd.root.resources.getColor(R.color.lane_info_2)
                    else -> bd.root.resources.getColor(R.color.white)
                }
                // 创建 PorterDuffColorFilter 对象
                val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
                // 将 PorterDuffColorFilter 设置给 Drawable
                drawable!!.colorFilter = colorFilter
                // 将 Drawable 设置给 ImageView
                imageView.background = drawable
                // 将 ImageView 的颜色设置为红色
                imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                imageView.layoutParams = ViewGroup.LayoutParams(35, 100)
                bd.signMoreIconsLayout.addView(imageView, imageView.layoutParams)
                if (i < list.size - 1) {
                    val lineView = View(context)
                    lineView.layoutParams = ViewGroup.LayoutParams(24, 80)
                    lineView.background = context.getDrawable(R.drawable.shape_vertical_dashed_line)
                    bd.signMoreIconsLayout.addView(lineView, lineView.layoutParams)
                }
            }
            val lineViewE = View(context)
            lineViewE.layoutParams = ViewGroup.LayoutParams(24, 80)
            lineViewE.background = context.getDrawable(R.drawable.shape_vertical_dashed_line)
            bd.signMoreIconsLayout.addView(lineViewE, lineViewE.layoutParams)
        }
        holder.viewBinding.root.setOnClickListener {
            listener?.onItemClick(item)
        }
        holder.tag = item.name + position
    }

    override fun refreshData(newData: List<SignBean>) {
        super.refreshData(newData)
        for (i in newData.indices) {
            if (selectMoreInfoTag == newData[i].name + i) {
                return
            }
        }
    }

}