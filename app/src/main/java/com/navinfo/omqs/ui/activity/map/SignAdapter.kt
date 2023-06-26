package com.navinfo.omqs.ui.activity.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.databinding.AdapterSignBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

interface OnSignAdapterClickListener {
    fun onItemClick(signBean: SignBean)
    fun onMoreInfoClick(selectTag: String, tag: String, signBean: SignBean)
    fun onErrorClick(signBean: SignBean)
    fun onHideMoreInfoView()
}

class SignAdapter(private var listener: OnSignAdapterClickListener?) :
    BaseRecyclerViewAdapter<SignBean>() {
    /**
     * 选中的详细信息按钮的tag标签
     */
    private var selectMoreInfoTag: String = ""
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
        if (item.iconId != 0) bd.signMainIconBg.setImageResource(item.iconId)
        bd.signMainIcon.text = item.iconText
        bd.signBottomText.text = item.name
        holder.tag = item.name + position
        //点击错误按钮
        bd.signMainFastError.setOnClickListener {
            listener?.onErrorClick(item)
        }
        bd.signBottomRightText.text = item.bottomRightText

        bd.root.setOnClickListener {
            listener?.onItemClick(item)
        }
        if (item.moreText.isNotEmpty()) {
            bd.signMainInfo.visibility = View.VISIBLE
            //点击更多信息按钮
            bd.signMainInfo.setOnClickListener {
                listener?.onMoreInfoClick(selectMoreInfoTag, holder.tag, item)
                selectMoreInfoTag = holder.tag
            }
        } else bd.signMainInfo.visibility = View.GONE

    }

    override fun refreshData(newData: List<SignBean>) {
        super.refreshData(newData)
        for (i in newData.indices) {
            if (selectMoreInfoTag == newData[i].name + i) {
                return
            }
        }
        listener?.onHideMoreInfoView()
    }

}