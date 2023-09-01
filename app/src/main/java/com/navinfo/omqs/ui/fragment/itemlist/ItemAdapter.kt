package com.navinfo.omqs.ui.fragment.itemlist

import android.view.LayoutInflater
import android.view.ViewGroup
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.omqs.databinding.AdapterItemBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder

class ItemAdapter(private var itemListener: ((Int, RenderEntity) -> Unit?)? = null) :
    BaseRecyclerViewAdapter<RenderEntity>() {

    var selectPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val binding = holder.viewBinding as AdapterItemBinding
        var renderEntity = data[position]
        binding.root.isSelected = selectPosition == position
        binding.itemIndex.text = (position+1).toString()
        binding.name.text = DataCodeEnum.findTableNameByCode(renderEntity.code)
        binding.root.setOnClickListener {
            if (selectPosition != position) {
                notifyItemChanged(selectPosition)
                selectPosition = position
                notifyItemChanged(position)
            }

            if (itemListener != null) {
                itemListener!!.invoke(position, renderEntity)
            }
        }
    }
}