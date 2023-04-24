package com.navinfo.omqs.ui.fragment.evaluationresult

import android.view.LayoutInflater
import android.view.ViewGroup
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.TextItemSelectBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

class RightGroupHeaderAdapter(private var itemListener: ((Int, RightBean) -> Unit?)? = null) :
    BaseRecyclerViewAdapter<RightBean>() {
    private var groupTitleList = mutableListOf<String>()
    override fun getItemViewRes(position: Int): Int {
        return R.layout.text_item_select
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            TextItemSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val bd = holder.viewBinding as TextItemSelectBinding
        bd.itemId.text = data[position].text
        bd.root.setOnClickListener {
            itemListener?.invoke(position, data[position])
        }
    }

    /**
     * 判断position对应的Item是否是组的第一项
     *
     * @param position
     * @return
     */
    fun isItemHeader(position: Int): Boolean {
        if (position >= data.size)
            return false
        return if (position == 0) {
            true
        } else {
            val lastGroupName = data[position - 1].title
            val currentGroupName = data[position].title
            //判断上一个数据的组别和下一个数据的组别是否一致，如果不一致则是不同组，也就是为第一项（头部）
            lastGroupName != currentGroupName
        }
    }

    fun isLastGroupTitle(position: Int): Boolean {
        if (groupTitleList.isNotEmpty() && data[position].title == groupTitleList.last()) {
            return true
        }
        return false
    }

    /**
     * 获取position对应的Item组名
     *
     * @param position
     * @return
     */
    fun getGroupName(position: Int): String {
        return data[position].title
    }

    fun getGroupTopIndex(position: Int): Int {
        var nowPosition = position
        val title = data[position].title
        for (i in data.size - 2 downTo 0) {
            if (data[i].title == title) {
                nowPosition = i
            } else {
                break
            }
        }
        return nowPosition
    }

    fun getGroupTopIndex(title: String): Int {
        for (i in data.indices) {
            if (data[i].title == title)
                return i
        }
        return 0
    }

    override fun refreshData(newData: List<RightBean>) {
        super.refreshData(newData)
        groupTitleList.clear()
        for (item in newData) {
            if (groupTitleList.size > 0) {
                if (groupTitleList.last() != item.title) {
                    groupTitleList.add(item.title)
                }
            } else {
                groupTitleList.add(item.title)
            }
        }
    }


}