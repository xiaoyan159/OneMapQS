package com.navinfo.omqs.ui.other.anim

import android.R.attr.animation
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Build
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.navinfo.omqs.ui.other.anim.ViewHolderAnimator.ViewHolderAnimatorListener


object ExpandableViewHoldersUtil {

    //自定义处理列表中右侧图标，这里是一个旋转动画
    fun rotateExpandIcon(mImage: ImageView, from: Float, to: Float) {
        val valueAnimator: ValueAnimator = ValueAnimator.ofFloat(from, to) //属性动画
        valueAnimator.duration = 500
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.addUpdateListener { valueAnimator ->
            mImage.rotation = valueAnimator.animatedValue as Float
        }
        valueAnimator.start()
    }

    //参数介绍：1、holder对象 2、展开部分的View，由holder.getExpandView()方法获取 3、animate参数为true，则有动画效果
    fun openH(holder: RecyclerView.ViewHolder, expandView: View, animate: Boolean) {
        if (animate) {
            expandView.visibility = View.VISIBLE
            //改变高度的动画
            val animator = ViewHolderAnimator.ofItemViewHeight(holder)
            //扩展的动画，结束后透明度动画开始
            animator!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val alphaAnimator = ObjectAnimator.ofFloat(expandView, View.ALPHA, 1f)
                    alphaAnimator.addListener(ViewHolderAnimatorListener(holder))
                    alphaAnimator.start()
                }
            })
            animator.start()
        } else {//为false时直接显示
            expandView.visibility = View.VISIBLE
            expandView.alpha = 1f
        }
    }

    //类似于打开的方法
    fun closeH(holder: RecyclerView.ViewHolder, expandView: View, animate: Boolean) {
        if (animate) {
            expandView.visibility = View.GONE
            val animator = ViewHolderAnimator.ofItemViewHeight(holder)
            expandView.visibility = View.VISIBLE
            animator!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    expandView.visibility = View.GONE
                    expandView.alpha = 0f
                }

                override fun onAnimationCancel(animation: Animator) {
                    expandView.visibility = View.GONE
                    expandView.alpha = 0f
                }
            })
            animator.start()
        } else {
            expandView.visibility = View.GONE
            expandView.alpha = 0f
        }
    }

    //获取展开部分的View
    interface Expandable {
        val expandView: View
    }

    class KeepOneH<VH> where VH : RecyclerView.ViewHolder, VH : Expandable? {
        //-1表示所有item是关闭状态，opend为pos值的表示pos位置的item为展开的状态
        private var _opened = -1

        /**
         * 此方法是在Adapter的onBindViewHolder()方法中调用
         *
         * @param holder holder对象
         * @param pos    下标
         */
        fun bind(holder: VH, pos: Int) {
            if (pos == _opened) openH(holder, holder!!.expandView, false) else closeH(
                holder,
                holder!!.expandView,
                false
            )
        }

        /**
         * 响应ViewHolder的点击事件
         *
         * @param holder    holder对象
         * @param imageView 这里我传入了一个ImageView对象，为了处理图片旋转的动画，为了处理内部业务
         */
        fun toggle(holder: VH, imageView: ImageView) {
            if (_opened == holder!!.position) {
                _opened = -1
                rotateExpandIcon(imageView, 180F, 0F);
                closeH(holder, holder.expandView, true)
            } else {
                val previous = _opened
                _opened = holder.position
                rotateExpandIcon(imageView, 0F, 180F);
                openH(holder, holder.expandView, true)
                val oldHolder =
                    (holder.itemView.parent as RecyclerView).findViewHolderForPosition(previous) as VH?
                if (oldHolder != null) closeH(oldHolder, oldHolder.expandView, true)
            }
        }
    }
}