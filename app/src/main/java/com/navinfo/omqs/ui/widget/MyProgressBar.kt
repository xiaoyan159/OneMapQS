package com.navinfo.omqs.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.util.AttributeSet
import android.widget.ProgressBar


/**
 * 带文字提示的进度条
 */
class MyProgressBar : ProgressBar {
    private lateinit var mPaint: Paint
    private var text: String = ""
    private var rate = 0f

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
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
        rate = progress * 1.0f / this.getMax()
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
        val y: Int = 0 - rect.top
        mPaint.textSize = 22f
        canvas.drawText(text, x.toFloat(), y.toFloat(), mPaint)
    }
}