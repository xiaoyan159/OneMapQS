package com.navinfo.omqs.ui.other

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

/**
 * dataBinding viewHolder 基类
 */
open class BaseViewHolder(var dataBinding: ViewDataBinding) :
    RecyclerView.ViewHolder(dataBinding.root) {
}