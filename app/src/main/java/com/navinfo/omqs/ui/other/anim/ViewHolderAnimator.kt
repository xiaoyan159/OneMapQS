package com.navinfo.omqs.ui.other.anim

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

object ViewHolderAnimator {
    fun ofItemViewHeight(holder: RecyclerView.ViewHolder): Animator? {
        val parent = holder.itemView.parent as View
            ?: throw IllegalStateException("Cannot animate the layout of a view that has no parent")
        val start = holder.itemView.measuredHeight
        holder.itemView.measure(
            View.MeasureSpec.makeMeasureSpec(
                parent.measuredWidth,
                View.MeasureSpec.AT_MOST
            ), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val end = holder.itemView.measuredHeight
        val animator = LayoutAnimator.ofHeight(holder.itemView, start, end)
        animator!!.addListener(ViewHolderAnimatorListener(holder))
        animator.addListener(
            LayoutParamsAnimatorListener(
                holder.itemView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        return animator
    }

    class ViewHolderAnimatorListener(private val _holder: RecyclerView.ViewHolder) :
        AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator) {
            _holder.setIsRecyclable(false)
        }

        override fun onAnimationEnd(animation: Animator) {
            _holder.setIsRecyclable(true)
        }

        override fun onAnimationCancel(animation: Animator) {
            _holder.setIsRecyclable(true)
        }
    }

    class LayoutParamsAnimatorListener(
        private val _view: View,
        private val _paramsWidth: Int,
        private val _paramsHeight: Int
    ) : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            val params = _view.layoutParams
            params.width = _paramsWidth
            params.height = _paramsHeight
            _view.layoutParams = params
        }
    }
}