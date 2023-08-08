package com.navinfo.omqs.ui.other

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * dataBinding viewHolder 基类
 * LifecycleRegistry 这是一个生命周期注册器，继承自 Lifecycle，LifecycleOwner 通过这个类来分发生命周期事件，并在 getLifecycle() 中返回
 */
open class BaseViewHolder(val viewBinding: ViewBinding) :
    RecyclerView.ViewHolder(viewBinding.root), LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    var tag = ""

    private var listener: OnLifecycleStateListener? = null

    init {
//        dataBinding.lifecycleOwner = this
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        itemView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            // View onDetached 的时候回调 onDestroy()
            override fun onViewDetachedFromWindow(v: View) {
                itemView.removeOnAttachStateChangeListener(this)
                onDestroy()
            }

            // View onAttached 的时候回调 onCreate()
            override fun onViewAttachedToWindow(v: View) {
                onStart()
            }
        })
    }

    fun onStart() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED     //
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED     //   ON_RESUME EVENT
        listener?.onState(tag,Lifecycle.State.STARTED)
    }

    fun onStop() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED    //
        lifecycleRegistry.currentState = Lifecycle.State.CREATED    //
//        listener?.onState(tag,Lifecycle.State.STARTED)// ON_STOP EVENT
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED   ///  ON_DESTROY EVENT
        listener?.onState(tag,Lifecycle.State.DESTROYED)
    }


    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    fun addObserver(listener: OnLifecycleStateListener) {
        this.listener = listener
    }
}

/**
 * 生命周期变化
 */
interface OnLifecycleStateListener {
    fun onState(tag:String,state: Lifecycle.State)
}