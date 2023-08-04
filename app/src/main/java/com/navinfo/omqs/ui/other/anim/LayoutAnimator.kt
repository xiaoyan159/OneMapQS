package com.navinfo.omqs.ui.other.anim

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View

object LayoutAnimator {
    fun ofHeight(view: View, start: Int, end: Int): Animator {
        val animator = ValueAnimator.ofInt(start, end)
        animator.addUpdateListener(LayoutHeightUpdateListener(view))
        return animator
    }

    class LayoutHeightUpdateListener(private val _view: View) :
        ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val lp = _view.layoutParams
            lp.height = animation.animatedValue as Int
            _view.layoutParams = lp
        }
    }
}