package com.navinfo.omqs.ui.fragment.signMoreInfo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.AdapterLaneBoundaryBinding
import com.navinfo.omqs.databinding.AdapterTwoItemBinding
import com.navinfo.omqs.ui.other.BaseViewHolder
import com.navinfo.omqs.ui.other.anim.ExpandableViewHoldersUtil
import com.navinfo.omqs.ui.other.anim.ExpandableViewHoldersUtil.Expandable


data class LaneBoundaryItem(
    val title: String, val text: String?, val itemList: MutableList<TwoItemAdapterItem>?
)

class LaneBoundaryAdapter : RecyclerView.Adapter<ViewHolder>() {
    private val keepOne: ExpandableViewHoldersUtil.KeepOneH<ExpandViewHolder> =
        ExpandableViewHoldersUtil.KeepOneH()

    private var dataList = mutableListOf<LaneBoundaryItem>()

    class ExpandViewHolder(
        val viewBinding: AdapterLaneBoundaryBinding,
        private val keepOne: ExpandableViewHoldersUtil.KeepOneH<ExpandViewHolder>
    ) : ViewHolder(viewBinding.root),
        View.OnClickListener, Expandable {

        init {
            viewBinding.root.setOnClickListener(this)
        }

        fun bind(pos: Int, laneBoundaryItem: LaneBoundaryItem) {
            viewBinding.contactName.text = laneBoundaryItem.title
            if (laneBoundaryItem.itemList != null) {
                for (item in laneBoundaryItem.itemList) {
                    var view = LayoutInflater.from(viewBinding.root.context)
                        .inflate(R.layout.adapter_two_item, null, false)
                    view.findViewById<TextView>(R.id.title).text = item.title
                    view.findViewById<TextView>(R.id.text).text = item.text
                    viewBinding.infos.addView(view)
                }
            }
            keepOne.bind(this, pos)
        }

        override fun onClick(v: View) {
            keepOne.toggle(this,viewBinding.expandIcon)
        }

        override val expandView: View
            get() = viewBinding.infos

    }

    override

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == 1) {
            val viewBinding = AdapterLaneBoundaryBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ExpandViewHolder(viewBinding, keepOne)
        } else {
            val viewBinding =
                AdapterTwoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            BaseViewHolder(viewBinding)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is ExpandViewHolder) {
            holder.bind(position, dataList[position])
        } else if (holder is BaseViewHolder) {
            val binding: AdapterTwoItemBinding = holder.viewBinding as AdapterTwoItemBinding
            val item = dataList[position]
            binding.title.text = item.title
            binding.text.text = item.text
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (dataList[position].itemList != null) 1
        else 0
    }

    fun refreshData(list: List<LaneBoundaryItem>) {
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }
}



