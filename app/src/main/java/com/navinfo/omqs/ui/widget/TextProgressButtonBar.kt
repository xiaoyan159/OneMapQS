package com.navinfo.omqs.ui.widget

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.navinfo.omqs.R

class TextProgressButtonBar : View {
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context!!, attrs!!)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs) {
        init(context!!, attrs!!)
    }

    private lateinit var fm: Paint.FontMetrics
    private var progress = 0
    private var textColor: Int = Color.WHITE
    private var paint: Paint? = null
    private var textSize: Float = 10f
    private var foreground = 0
    private var backgroundcolor = 0
    private var text: String? = null
    private var max = 100
    private val corner = 30 // 圆角的弧度
    private val mStartColor = resources.getColor(R.color.default_button_blue_color)
    private val mEndColor = resources.getColor(R.color.ripple_end_color)
    private val mValueAnimator = ValueAnimator.ofInt(
        mEndColor,
        mStartColor
    )
    private var mCurrentColor = mEndColor
//    private var buttonClickListener: OnProgressButtonClickListener? = null


    fun init(
        context: Context, attrs: AttributeSet
    ) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressButton)
        backgroundcolor = typedArray.getInteger(
            R.styleable.ProgressButton_backgroundcolor, Color.parseColor("#C6C6C6")
        )
        foreground = typedArray.getInteger(
            R.styleable.ProgressButton_foreground, Color.rgb(20, 131, 214)
        )
        textColor = typedArray.getInteger(
            R.styleable.ProgressButton_textcolor, Color.WHITE
        )
        max = typedArray.getInteger(R.styleable.ProgressButton_max, 100)
        progress = typedArray.getInteger(R.styleable.ProgressButton_progress, 0)
        text = typedArray.getString(R.styleable.ProgressButton_text)
        textSize = typedArray.getDimension(R.styleable.ProgressButton_textSize, 20f)
        typedArray.recycle()

        mValueAnimator.duration = 1000
        mValueAnimator.repeatCount = ValueAnimator.INFINITE
        mValueAnimator.repeatMode = ValueAnimator.REVERSE

        // 为 ValueAnimator 对象添加 ArgbEvaluator
        mValueAnimator.setEvaluator(ArgbEvaluator());
        // 添加动画监听器，在动画值改变时更新当前颜色值并重绘 View
        mValueAnimator.addUpdateListener { animation ->
            mCurrentColor = animation.animatedValue as Int
            invalidate();
        };

    }

    fun startAnimator() {
        if (!mValueAnimator.isStarted) {
            progress = max
            mValueAnimator.start()
        }
    }

    fun stopAnimator() {
        if (mValueAnimator.isRunning || mValueAnimator.isStarted) {
            mValueAnimator.cancel()
            mCurrentColor = mEndColor
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 在 View 从窗口中移除时停止动画
        mValueAnimator.cancel()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint = Paint()
        paint?.let {
            it.isAntiAlias = true
            it.strokeWidth = 5f
            it.style = Paint.Style.STROKE
            it.color = textColor
            /**
             * 绘制背景
             */
            var oval = RectF(0F, 0F, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(oval, corner.toFloat(), corner.toFloat(), it)
            it.style = Paint.Style.FILL
            it.color = this.backgroundcolor
            canvas.drawRoundRect(oval, corner.toFloat(), corner.toFloat(), it)

            if (progress <= corner) {
                oval = RectF(
                    0F,
                    (corner - progress).toFloat(),
                    (width * progress / max).toFloat(),
                    (height - corner + progress).toFloat()
                )
                /***
                 * 绘制进度值
                 */

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val shader = LinearGradient(
                        oval.left,
                        oval.top,
                        oval.right,
                        oval.bottom,
                        mStartColor,
                        mCurrentColor,
                        Shader.TileMode.MIRROR
                    )
                    it.shader = shader
                }
                canvas.drawRoundRect(oval, progress.toFloat(), progress.toFloat(), it)
            } else {
                oval = RectF(
                    0F, 0F, (width * progress / max).toFloat(), height.toFloat()
                )
                /***
                 * 绘制进度值
                 */

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val shader = LinearGradient(
                        oval.left,
                        oval.top,
                        oval.right,
                        oval.bottom,
                        mStartColor,
                        mCurrentColor,
                        Shader.TileMode.CLAMP
                    )
                    it.shader = shader
                }
                canvas.drawRoundRect(oval, corner.toFloat(), corner.toFloat(), it)
            }
            /***
             * 绘制文本
             */
            it.shader = null
            if ("" == text || text == null) {
                return
            }
            it.textSize = textSize
            fm = it.fontMetrics
            it.color = textColor
            val textCenterVerticalBaselineY = height / 2 - fm.descent + (fm.descent - fm.ascent) / 2
            canvas.drawText(
                text!!, (measuredWidth - it.measureText(text)) / 2, textCenterVerticalBaselineY, it
            )
        }

    }

    /**
     * 设置最大值
     *
     * @param max
     */
    fun setMax(max: Int) {
        this.max = max
    }

    /**
     * 设置文本提示信息
     *
     * @param text
     */
    fun setText(text: String?) {
        this.text = text
        postInvalidate()
    }

    /**
     * 设置进度条的颜色值
     *
     * @param color
     */
    fun setForeground(color: Int) {
        foreground = color
    }

    /**
     * 设置进度条的背景色
     */
    override fun setBackgroundColor(color: Int) {
        this.backgroundcolor = color
    }

    /***
     * 设置文本的大小
     */
    fun setTextSize(size: Int) {
        textSize = size.toFloat()
    }

    /**
     * 设置文本的颜色值
     *
     * @param color
     */
    fun setTextColor(color: Int) {
        textColor = color
    }

    /**
     * 设置进度值
     *
     * @param progress
     */
    fun setProgress(progress: Int) {
        if (progress > max) {
            return
        }
        this.progress = progress
        //设置进度之后，要求UI强制进行重绘
        postInvalidate()
    }

    fun getMax(): Int {
        return max
    }

    fun getProgress(): Int {
        return progress
    }

//    @SuppressLint("ClickableViewAccessibility")
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        when (event.action) {
//            MotionEvent.ACTION_UP -> buttonClickListener?.onClickListener(this)
//            else -> {
//            }
//        }
//        return true
//    }
//
//    fun setOnProgressButtonClickListener(clickListener: OnProgressButtonClickListener) {
//        buttonClickListener = clickListener
//    }
//
//
//    interface OnProgressButtonClickListener {
//        fun onClickListener(view: View)
//    }

}