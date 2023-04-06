package com.navinfo.omqs.ui.other

import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView 适配器基础类
 */
abstract class BaseRecyclerViewAdapter<T>(var data: List<T> = listOf()) :
    RecyclerView.Adapter<BaseViewHolder>() {
    //    private var recyclerView: RecyclerView? = null
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
//
//
//
//        return BaseViewHolder(
//            DataBindingUtil.inflate(
//                LayoutInflater.from(parent.context),
//                viewType,
//                parent,
//                false
//            )
//        )
//    }


    override fun getItemCount(): Int {
        return data.size
    }

    fun refreshData(newData: List<T>) {
        this.data = newData
        this.notifyDataSetChanged()
    }


    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onStart()
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.apply {
            onStop()
        }
    }
//
//    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
//        super.onAttachedToRecyclerView(recyclerView)
//        this.recyclerView = recyclerView
//    }
//
//    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
//        super.onDetachedFromRecyclerView(recyclerView)
//        this.recyclerView = null
//    }
}