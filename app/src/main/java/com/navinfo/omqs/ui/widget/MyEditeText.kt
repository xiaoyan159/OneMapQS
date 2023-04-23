package com.navinfo.omqs.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText
import com.navinfo.omqs.R

class MyEditeText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle  //不这样写可能有些属性用不了
) : AppCompatEditText(context, attrs, defStyleAttr) {

    init {
        //Edittext通知父控件自己处理自己的滑动事件
        setOnTouchListener { v, event ->
            if (canVerticalScroll(this)) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                if (event.action == MotionEvent.ACTION_UP) {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    /**
     * EditText竖直方向是否可以滚动
     *
     * @param //editText需要判断的EditText
     * @return true：可以滚动   false：不可以滚动
     */
    private fun canVerticalScroll(contentEt: EditText): Boolean {
        //滚动的距离
        val scrollY = contentEt.scrollY
        //控件内容的总高度
        val scrollRange = contentEt.layout.height
        //控件实际显示的高度
        val scrollExtent =
            contentEt.height - contentEt.compoundPaddingTop - contentEt.compoundPaddingBottom
        //控件内容总高度与实际显示高度的差值
        val scrollDifference = scrollRange - scrollExtent

        if (scrollDifference == 0) {
            return false
        }
        return (scrollY > 0) || (scrollY < scrollDifference - 1)
    }

}