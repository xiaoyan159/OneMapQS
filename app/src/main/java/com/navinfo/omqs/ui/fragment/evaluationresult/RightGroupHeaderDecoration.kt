package com.navinfo.omqs.ui.fragment.evaluationresult

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.navinfo.omqs.R


/**
 * 自定义装饰器（实现分组+吸顶效果）
 */
@RequiresApi(Build.VERSION_CODES.M)
class RightGroupHeaderDecoration(context: Context) : ItemDecoration() {
    //头部的高
    private val mItemHeaderHeight: Int
    private val mTextPaddingLeft: Int

    //画笔，绘制头部和分割线
    private val mItemHeaderPaint: Paint
    private val mTextPaint: Paint
    private val mLinePaint: Paint
    private val mTextRect: Rect
    private var lastGroupView: View? = null

    init {
        mItemHeaderHeight = dp2px(context, 40f)
        mTextPaddingLeft = dp2px(context, 6f)
        mTextRect = Rect()
        mItemHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mItemHeaderPaint.color = context.getColor(R.color.btn_bg_blue)
        mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint.textSize = 46f
        mTextPaint.color = Color.WHITE
        mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mLinePaint.color = Color.WHITE
    }

    /**
     * 绘制Item的分割线和组头
     *
     * @param c
     * @param parent
     * @param state
     */
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.adapter is RightGroupHeaderAdapter) {
            val adapter = parent.adapter as RightGroupHeaderAdapter
            val count = parent.childCount //获取可见范围内Item的总数
            for (i in 0 until count) {
                val view: View = parent.getChildAt(i)

                val position = parent.getChildLayoutPosition(view)
                val isHeader: Boolean = adapter.isItemHeader(position)
                val left = parent.paddingLeft
                val right = parent.width - parent.paddingRight
                if (isHeader) {
                    c.drawRect(
                        left.toFloat(),
                        (view.top - mItemHeaderHeight).toFloat(),
                        right.toFloat(),
                        view.top.toFloat(),
                        mItemHeaderPaint
                    )
                    val text = adapter.getGroupName(position)
                    mTextPaint.getTextBounds(
                        text,
                        0,
                        text.length,
                        mTextRect
                    )
                    c.drawText(
                        adapter.getGroupName(position),
                        (left + mTextPaddingLeft).toFloat(),
                        (view.top - mItemHeaderHeight + mItemHeaderHeight / 2 + mTextRect.height() / 2).toFloat(),
                        mTextPaint
                    )
                } else {
                    c.drawRect(
                        left.toFloat(),
                        (view.top - 1).toFloat(), right.toFloat(),
                        view.top.toFloat(), mLinePaint
                    )
                }
            }
        }
    }

    /**
     * 绘制Item的顶部布局（吸顶效果）
     *
     * @param c
     * @param parent
     * @param state
     */
    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.adapter is RightGroupHeaderAdapter) {
            val adapter = parent.adapter as RightGroupHeaderAdapter
            val position =
                (parent.layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition()
            parent.findViewHolderForAdapterPosition(position)?.let {
                val view: View = it.itemView
                val isHeader: Boolean = adapter.isItemHeader(position + 1)
                val top = parent.paddingTop
                val left = parent.paddingLeft
                val right = parent.width - parent.paddingRight
                if (isHeader) {
                    val bottom = mItemHeaderHeight.coerceAtMost(view.bottom)
                    c.drawRect(
                        left.toFloat(),
                        (top + view.top - mItemHeaderHeight).toFloat(),
                        right.toFloat(),
                        (top + bottom).toFloat(),
                        mItemHeaderPaint
                    )
                    val text = adapter.getGroupName(position)
                    mTextPaint.getTextBounds(
                        text,
                        0,
                        text.length,
                        mTextRect
                    )
                    c.drawText(
                        adapter.getGroupName(position),
                        (left + mTextPaddingLeft).toFloat(),
                        (top + mItemHeaderHeight / 2 + mTextRect.height() / 2 - (mItemHeaderHeight - bottom)).toFloat(),
                        mTextPaint
                    )
                } else {
                    c.drawRect(
                        left.toFloat(),
                        top.toFloat(), right.toFloat(),
                        (top + mItemHeaderHeight).toFloat(), mItemHeaderPaint
                    )
                    val text = adapter.getGroupName(position)
                    mTextPaint.getTextBounds(
                        text,
                        0,
                        text.length,
                        mTextRect
                    )
                    c.drawText(
                        adapter.getGroupName(position), (left + mTextPaddingLeft).toFloat(),
                        (top + mItemHeaderHeight / 2 + mTextRect.height() / 2).toFloat(), mTextPaint
                    )

                }
            }
            c.save()
        }
    }

    /**
     * 设置Item的间距
     *
     * @param outRect
     * @param view
     * @param parent
     * @param state
     */
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (parent.adapter is RightGroupHeaderAdapter) {
            val adapter = parent.adapter as RightGroupHeaderAdapter
            //获取当前view在整个列表中的位置
            val position = parent.getChildLayoutPosition(view)
            //是不是改组的第一个
            val isHeader: Boolean = adapter.isItemHeader(position)
            if (isHeader) {
                outRect.top = mItemHeaderHeight
                if (adapter.isLastGroupTitle(position)) {
                    lastGroupView = view
                }
            } else if (position == (parent.adapter as RightGroupHeaderAdapter).itemCount - 1) {
                //判断这条是不是最后一条
                //如果是最后一个，找到他所在组的第一个
                lastGroupView?.let {
                    if (it.top > 0) {
                        outRect.bottom = it.top - it.height * 2
                    }
                }
                outRect.top = 1
            } else {
                outRect.top = 1
            }
        }
    }

    /**
     * dp转换成px
     */
    private fun dp2px(context: Context, dpValue: Float): Int {
        val scale: Float = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
}