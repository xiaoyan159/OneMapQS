package com.navinfo.omqs.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.navinfo.omqs.R


/**
 * 带文字提示的进度条
 */
class MyProgressBar : ProgressBar {
    private lateinit var mPaint: Paint
    private var text: String = ""
    private var rate = 0f
    private lateinit var bar: ProgressBar

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
//        LayoutInflater.from(context).inflate(
//            R.layout.my_projressbar, this,
//            true
//        );
        initView()
    }

    constructor(context: Context?) : super(context) {
        initView()
    }

    private fun initView() {
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.color = Color.BLUE
    }


    @Synchronized
    override fun setProgress(progress: Int) {
        setText(progress)
        super.setProgress(progress)
    }

    private fun setText(progress: Int) {
        rate = progress * 1.0f / this.max
        val i = (rate * 100).toInt()
        text = "$i%"
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = Rect()
        mPaint.getTextBounds(text, 0, text.length, rect)
        // int x = (getWidth()/2) - rect.centerX();
        // int y = (getHeight()/2) - rect.centerY();
        var x = (width * rate).toInt()
        if (x == width) {
            // 如果为百分之百则在左边绘制。
            x = width - rect.right
        }
        mPaint.textSize = 24f
        val y: Int = 10 - rect.top

        canvas.drawText(text, x.toFloat(), y.toFloat(), mPaint)
    }
}