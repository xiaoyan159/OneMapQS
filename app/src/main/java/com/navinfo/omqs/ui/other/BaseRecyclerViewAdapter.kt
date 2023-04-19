package com.navinfo.omqs.ui.other

import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.navinfo.omqs.R

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

    abstract fun getItemViewRes(position: Int): Int

    override fun getItemViewType(position: Int): Int {
        return getItemViewRes(position)
    }

    override fun getItemCount(): Int {
        return data.size
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

    //        this.recyclerView = recyclerView
//        super.onAttachedToRecyclerView(recyclerView)
//    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    open fun refreshData(newData: List<T>) {
        this.data = newData
        this.notifyDataSetChanged()
    }
//    }
//
//    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
//        super.onDetachedFromRecyclerView(recyclerView)
//        this.recyclerView = null
//    }
}