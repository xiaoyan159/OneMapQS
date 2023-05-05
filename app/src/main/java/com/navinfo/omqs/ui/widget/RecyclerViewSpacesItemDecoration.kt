package com.navinfo.omqs.ui.widget

import android.graphics.Rect
import android.view.View
import androidx.annotation.StringDef
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * 用来设置recyclerView 要素间的间隔
 */
class RecyclerViewSpacesItemDecoration : ItemDecoration {
    @StringDef(TOP_DECORATION, BOTTOM_DECORATION, LEFT_DECORATION, RIGHT_DECORATION)
    @Retention(
        RetentionPolicy.SOURCE
    )
    private annotation class Decoration

    private var rightSpace = 0 //右边间距
    private var topSpace = 0 //上边边间距
    private var leftSpace = 0 //左边间距
    private var bottomSpace = 0 //下边间距

    /**
     * @param bottomSpace 下间距
     */
    constructor(bottomSpace: Int) {
        this.bottomSpace = bottomSpace
    }

    /**
     * 指定某一个属性
     *
     * @param decoration decoration
     * @param space      间距
     */
    constructor(@Decoration decoration: String?, space: Int) {
        when (decoration) {
            RIGHT_DECORATION -> rightSpace = space
            TOP_DECORATION -> topSpace = space
            LEFT_DECORATION -> leftSpace = space
            BOTTOM_DECORATION -> bottomSpace = space
        }
    }

    /**
     * @param rightSpace  右间距
     * @param topSpace    上间距
     * @param leftSpace   左间距
     * @param bottomSpace 下间距
     */
    constructor(rightSpace: Int, topSpace: Int, leftSpace: Int, bottomSpace: Int) {
        this.rightSpace = rightSpace
        this.topSpace = topSpace
        this.leftSpace = leftSpace
        this.bottomSpace = bottomSpace
    }

    /**
     * @param outRect Item的矩边界
     * @param view    ItemView
     * @param parent  RecyclerView
     * @param state   RecyclerView的状态
     */
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.top = topSpace
        outRect.left = leftSpace
        outRect.right = rightSpace
        outRect.bottom = bottomSpace
    }

    companion object {
        const val TOP_DECORATION = "top_decoration"
        const val BOTTOM_DECORATION = "bottom_decoration"
        const val LEFT_DECORATION = "left_decoration"
        const val RIGHT_DECORATION = "right_decoration"
    }
}