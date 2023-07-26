package com.navinfo.omqs.ui.fragment.note

import android.content.Context
import android.graphics.*
import android.media.ThumbnailUtils
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.omqs.R
import com.navinfo.omqs.ui.other.BaseToast
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author zhjch
 * @version V1.0
 * @ClassName: CanvasView
 * @Date 2016/5/10
 * @Description: ${TODO}(画板)
 */
class CanvasView @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(
    mContext, attrs, defStyle
) {
    /**
     * 画笔类型
     */
    enum class CanvasStyle {
        FREE_LINE,  //自由线
        STRAIGHT_LINE,  //直线
        RECT_LINE,  //框
        RAILWAY_LINE,  // 铁路线
        GREENLAND_LINE,  //绿地线
        WATER_LINE,  //水系线
        BUILDING_LINE,  //建筑物
        POLY_LINE,  //折线
        CIRCULAR_POINT,  //圆点
        ELLIPSE_LINE,  //椭圆
        PARKING_LINE
        //停车场
    }

    /**
     * 画布
     */
    private var mCanvas: Canvas? = null

    /**
     * 控制线型
     */
    private var mPath: Path? = null

    /**
     * 画布背景
     */
    private var mBitmapPaint: Paint? = null

    /**
     * 画布前景
     */
    private var mBitmap: Bitmap? = null

    /**
     * 普通画笔
     */
    private val mPaint: Paint?

    /**
     * 圆形画笔
     */
    private val mPaintCircular: Paint

    /**
     * 铁路画笔1 底色
     */
    private val mPaintRailway1: Paint

    /**
     * 铁路画笔2 前景色
     */
    private val mPaintRailway2: Paint

    /**
     * 水系画笔1 边框
     */
    private val mPaintWater: Paint

    /**
     * 水系画笔2 填充
     */
    private val mPaintWater1: Paint

    /**
     * 绿地画笔1 边框
     */
    private val mPaintGreenland: Paint

    /**
     * 绿地画笔2 填充
     */
    private val mPaintGreenland1: Paint

    /**
     * 停车场1 边框
     */
    private val mPaintParking: Paint

    /**
     * 停车场 填充
     */
    private val mPaintParking1: Paint

    /**
     * 准星图片
     */
    private val mStarBitmap: Bitmap

    /**
     * 铁路栅格线样式
     */
    private val pathEffect1 = DashPathEffect(floatArrayOf(16f, 16f), 4f)
    //    private DashPathEffect pathEffect2 = new DashPathEffect(new float[] { 12,12 }, 12);
    /**
     * 需不需要记录画笔轨迹点
     */
    private var isSavePoint: Boolean

    /**
     * 是否在move 中
     */
    private var isMove = false

    /**
     * 是否打开橡皮擦
     */
    private var isEraser = false

    /**
     * 绘制轨迹路径
     */
    private var mDrawPath: DrawPath? = null

    /**
     * 画笔宽度
     */
    private var mPaintWidth = 5

    /**
     * 画笔颜色
     */
    private var mPaintColor = 0xFF5052

    /**
     * 圆形图案半径
     */
    private val mCircularP = 150
    /**
     * 获取当前画笔样式
     *
     * @return
     */
    /**
     * 线型
     */
    var canvasStyle = CanvasStyle.FREE_LINE
        private set

    /**
     * 当前画布上的每一笔
     */
    private var mCurrentPaths: MutableList<DrawPath> = mutableListOf()

    /**
     * 被撤销的每一笔
     */
    private val mDeletePaths: MutableList<DrawPath?>?

    /**
     * 画布 宽度
     */
    private var viewWidth = 10

    /**
     * 画布 高度
     */
    private var viewHeight = 10

    /**
     * 画笔第一笔点下去的位置
     */
    private var mDownX = 0f
    private var mDownY = 0f

    /**
     * 画笔移动的位置
     */
    private var mMoveX = 0f
    private var mMoveY = 0f

    /**
     * 圆点图案移动矩阵
     */
    private val mMatrixCircular = Matrix()
    private var mListener: OnCanvasChangeListener? = null

    /**
     * 用来回馈上层页面
     */
    interface OnCanvasChangeListener {
        /**
         * 画布上有绘制操作
         */
        fun onDraw()
    }

    fun setOnCanvasChangeListener(listener: OnCanvasChangeListener?) {
        mListener = listener
    }

    //路径对象
    class DrawPath(
        startPoint: Point?,
        var path: Path,
        var width: Int,
        var color: Int,
        var style: CanvasStyle
    ) {
        var pointList: MutableList<Point>?
        var isOver = true

        /**
         * 外截矩形
         */
        var rect: Rect? = null

        /**
         * @param startPoint 起点
         * @param path       路径
         * @param width      宽度
         * @param color      颜色
         * @param style      线型
         */
        init {
            pointList = ArrayList()
            if (startPoint != null) {
                rect = Rect(startPoint.x, startPoint.y, startPoint.x + 1, startPoint.y + 1)
            }
        }

        /**
         * 获取画笔
         *
         * @param paint
         * @return
         */
        fun getPaint(paint: Paint?): Paint? {
            paint!!.strokeWidth = width.toFloat()
            paint.color = color
            return paint
        }
    }

    /**
     * 初始化画布
     */
    private fun initCanvas() {
        //画布大小
        if (mBitmap != null) {
            mBitmap!!.recycle()
        }
        mBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap!!) //所有mCanvas画的东西都被保存在了mBitmap中
        mPath = Path()
        mBitmapPaint = Paint(Paint.DITHER_FLAG)
    }

    /**
     * 结束折线类型的操作
     */
    private fun setPolyLineOver() {
        if (mCurrentPaths.size > 0) {
            val path = mCurrentPaths[mCurrentPaths.size - 1]
            if (path.style == CanvasStyle.RAILWAY_LINE
                || path.style == CanvasStyle.POLY_LINE
            ) {
                //切换类型时，线少于2个点的不要
                if (path.pointList == null || path.pointList!!.size == 1) {
                    back() //只有一个点时会遗留一个起始圆点，先移除掉。然后清除
                    mDeletePaths!!.clear()
                    if (path === mDrawPath) {
                        mDrawPath = null
                        //                        mDrawPath.pointList.clear();
//                        mPath.reset();
                    }
                    return
                }
            } else if (path.style == CanvasStyle.GREENLAND_LINE || path.style == CanvasStyle.WATER_LINE || path.style == CanvasStyle.PARKING_LINE) {
                //切换类型时，面少于3个点的不要
                if (path.pointList == null || path.pointList!!.size < 4) {
                    mCurrentPaths.removeAt(mCurrentPaths.size - 1)
                    initCanvas()
                }
                invalidate()
                return
            }
            path.isOver = true
        }
    }

    /**
     * 切换线型
     *
     * @param style
     */
    fun setStyle(style: CanvasStyle) {
        canvasStyle = style
        setPolyLineOver()
    }

    init {
        mCurrentPaths = ArrayList()
        mDeletePaths = ArrayList()
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.isDither = true
        mPaint.color = mPaintColor
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = mPaintWidth.toFloat()
        mPaintRailway1 = Paint()
        mPaintRailway1.isAntiAlias = true
        mPaintRailway1.isDither = true
        mPaintRailway1.color = -0xff8f40
        mPaintRailway1.style = Paint.Style.STROKE
        mPaintRailway1.strokeWidth = 8f
        mPaintRailway2 = Paint()
        mPaintRailway2.isAntiAlias = true
        mPaintRailway2.isDither = true
        mPaintRailway2.color = Color.WHITE
        mPaintRailway2.style = Paint.Style.STROKE
        mPaintRailway2.strokeWidth = 4f
        mPaintRailway2.pathEffect = pathEffect1
        mPaintWater = Paint()
        mPaintWater.isAntiAlias = true
        mPaintWater.isDither = true
        mPaintWater.color = -0x543501
        mPaintWater.style = Paint.Style.FILL
        mPaintWater.strokeWidth = 4f
        mPaintWater1 = Paint()
        mPaintWater1.isAntiAlias = true
        mPaintWater1.isDither = true
        mPaintWater1.color = -0xff4fb0
        mPaintWater1.style = Paint.Style.STROKE
        mPaintWater1.strokeWidth = 4f
        mPaintGreenland = Paint()
        mPaintGreenland.isAntiAlias = true
        mPaintGreenland.isDither = true
        mPaintGreenland.color = -0x321c54
        mPaintGreenland.style = Paint.Style.FILL
        mPaintGreenland.strokeWidth = 4f
        mPaintGreenland1 = Paint()
        mPaintGreenland1.isAntiAlias = true
        mPaintGreenland1.isDither = true
        mPaintGreenland1.color = -0xff4fb0
        mPaintGreenland1.style = Paint.Style.STROKE
        mPaintGreenland1.strokeWidth = 4f
        mPaintParking = Paint()
        mPaintParking.isAntiAlias = true
        mPaintParking.isDither = true
        mPaintParking.color = -0x168
        mPaintParking.style = Paint.Style.FILL
        mPaintParking.strokeWidth = 4f
        mPaintParking1 = Paint()
        mPaintParking1.isAntiAlias = true
        mPaintParking1.isDither = true
        mPaintParking1.color = -0x59595a
        mPaintParking1.style = Paint.Style.STROKE
        mPaintParking1.strokeWidth = 4f
        mPaintCircular = Paint()
        mPaintCircular.style = Paint.Style.FILL
        mPaintCircular.color = mPaintColor
        mStarBitmap = BitmapFactory.decodeResource(resources, R.drawable.home_map_center)
        val a = mContext.obtainStyledAttributes(attrs, R.styleable.CanvasView)
        isSavePoint = a.getBoolean(R.styleable.CanvasView_isSavePoint, true)
        a.recycle()
    }

    /**
     * 手指移动的处理
     *
     * @param x 坐标
     * @param y 坐标
     */
    private fun touch_move_line(x: Float, y: Float) {
        if (x == mDownX && y == mDownY) return
        val dx = abs(x - mDownX)
        val dy = abs(y - mDownY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            //增加非空判断
            if (mPath != null) mPath!!.quadTo(
                mDownX,
                mDownY,
                (x + mDownX) / 2,
                (y + mDownY) / 2
            ) //源代码是这样写的，可是我没有弄明白，为什么要这样？
            mDownX = x
            mDownY = y
        }
        setBuilder(x.toInt(), y.toInt())
    }

    /**
     * 多边形起点点下时的处理
     *
     * @param x 坐标
     * @param y 坐标
     */
    private fun touch_start_poly(x: Int, y: Int) {
        if (canvasStyle == CanvasStyle.GREENLAND_LINE || canvasStyle == CanvasStyle.WATER_LINE || canvasStyle == CanvasStyle.PARKING_LINE) {
            if (mCurrentPaths.size > 0 && mCurrentPaths[0].isOver) {
                BaseToast.makeText(mContext, "不允许存在多个！", BaseToast.LENGTH_SHORT).show()
                return
            }
            if (mCurrentPaths.size > 0) {
                val drawPath = mCurrentPaths[mCurrentPaths!!.size - 1]
                if (drawPath.pointList!!.size > 3) {
                    if (!GeometryTools.isSimplePolygon(drawPath.pointList, Point(x, y))) {
                        BaseToast.makeText(mContext, "面不允许自相交图形!", BaseToast.LENGTH_SHORT).show()
                        return
                    }
                }
            }
        }
        if (mDeletePaths != null && mDeletePaths.size > 0) {
            mDeletePaths.clear()
        }
        if (mCurrentPaths.size > 0) {
            val drawPath = mCurrentPaths[mCurrentPaths.size - 1]
            if (!drawPath.isOver) {
                mDrawPath = drawPath
                if (drawPath.style == CanvasStyle.GREENLAND_LINE || drawPath.style == CanvasStyle.WATER_LINE || drawPath.style == CanvasStyle.PARKING_LINE) {
                    if (drawPath.pointList!!.size > 1) {
                        drawPath.pointList!!.removeAt(drawPath.pointList!!.size - 1)
                    }
                    setBuilder(x, y)
                    if (drawPath.pointList!!.size < 3) {
                        drawPath.path.lineTo(x.toFloat(), y.toFloat())
                    } else {
                        initCanvas()
                        drawPath.path.reset()
                        for (i in drawPath.pointList!!.indices) {
                            if (i == 0) {
                                drawPath.path.moveTo(
                                    drawPath.pointList!![0].x.toFloat(),
                                    drawPath.pointList!![0].y.toFloat()
                                )
                            } else {
                                drawPath.path.lineTo(
                                    drawPath.pointList!![i].x.toFloat(),
                                    drawPath.pointList!![i].y.toFloat()
                                )
                            }
                        }
                        drawPath.path.lineTo(
                            drawPath.pointList!![0].x.toFloat(),
                            drawPath.pointList!![0].y.toFloat()
                        )
                    }
                    drawPath.pointList!!.add(drawPath.pointList!![0])
                } else {
                    drawPath.path.lineTo(x.toFloat(), y.toFloat())
                    setBuilder(x, y)
                }
                if (drawPath.style == CanvasStyle.POLY_LINE) {
                    mCanvas!!.drawPath(drawPath.path, drawPath.getPaint(mPaint)!!)
                } else if (drawPath.style == CanvasStyle.RAILWAY_LINE) {
                    mCanvas!!.drawPath(drawPath.path, mPaintRailway1)
                    mCanvas!!.drawPath(drawPath.path, mPaintRailway2)
                } else if (drawPath.style == CanvasStyle.GREENLAND_LINE) {
                    mCanvas!!.drawPath(drawPath.path, mPaintGreenland)
                    mCanvas!!.drawPath(drawPath.path, mPaintGreenland1)
                } else if (drawPath.style == CanvasStyle.WATER_LINE) {
                    mCanvas!!.drawPath(drawPath.path, mPaintWater)
                    mCanvas!!.drawPath(drawPath.path, mPaintWater1)
                } else if (drawPath.style == CanvasStyle.PARKING_LINE) {
                    mCanvas!!.drawPath(drawPath.path, mPaintParking)
                    mCanvas!!.drawPath(drawPath.path, mPaintParking1)
                }
                invalidate()
                return
            }
        }
        mPath = Path()
        mDrawPath = DrawPath(Point(x, y), mPath!!, mPaintWidth, mPaintColor, canvasStyle)
        mDrawPath!!.isOver = false
        mPaintCircular.color = mPaintColor
        mCanvas!!.drawCircle(x.toFloat(), y.toFloat(), 2f, mPaintCircular)
        mPath!!.moveTo(x.toFloat(), y.toFloat())
        setBuilder(x, y)
        if (canvasStyle == CanvasStyle.WATER_LINE || canvasStyle == CanvasStyle.GREENLAND_LINE || canvasStyle == CanvasStyle.PARKING_LINE) {
            setBuilder(x, y)
        }
        mCurrentPaths.add(mDrawPath!!)
        if (mListener != null) {
            mListener!!.onDraw()
        }
        invalidate()
        return
    }

    /**
     * 手指按下的第一个点处理
     *
     * @param x 坐标
     * @param y 坐标
     */
    private fun touch_start(x: Float, y: Float) {
        mPath!!.reset() //清空path
        mPath!!.moveTo(x, y)
        mDownX = x
        mDownY = y
        if (canvasStyle != CanvasStyle.ELLIPSE_LINE) setBuilder(x.toInt(), y.toInt())
        if (mDeletePaths != null && mDeletePaths.size > 0) {
            mDeletePaths.clear()
        }
    }

    /**
     * 手指离开屏幕时的处理
     *
     * @param x 坐标
     * @param y 坐标
     */
    private fun touch_up(x: Float, y: Float) {
        try {
            if (canvasStyle == CanvasStyle.POLY_LINE) {
                val Y = y - mCircularP + mStarBitmap.height / 2
                mDrawPath!!.width = mPaintWidth
                mDrawPath!!.color = mPaintColor
                if (mDrawPath!!.pointList!!.size > 0) {
                    mDrawPath = mCurrentPaths[mCurrentPaths.size - 1]
                    mPath = mDrawPath!!.path
                    mPath!!.lineTo(x, Y)
                    mCanvas!!.drawPath(mPath!!, mPaint!!)
                } else {
                    mCurrentPaths.add(mDrawPath!!)
                    if (mListener != null) {
                        mListener!!.onDraw()
                    }
                    mPath!!.moveTo(x, Y)
                    mPaintCircular.color = mPaintColor
                    mPaintCircular.strokeWidth = 4f
                    mCanvas!!.drawCircle(x, Y, 4f, mPaintCircular)
                }
                setBuilder(x.toInt(), Y.toInt())
                return
            } else if (canvasStyle == CanvasStyle.STRAIGHT_LINE) {
                val Y = y - mCircularP + mStarBitmap.height / 2
                setBuilder(x.toInt(), Y.toInt())
                mDrawPath!!.width = mPaintWidth
                mDrawPath!!.color = mPaintColor
                if (mDrawPath!!.pointList!!.size > 1) {
                    mDrawPath!!.isOver = true
                    mCurrentPaths.add(mDrawPath!!)
                    if (mListener != null) {
                        mListener!!.onDraw()
                    }
                    mPath!!.lineTo(x, Y)
                    mCanvas!!.drawPath(mPath!!, mPaint!!)
                    mPath = null
                } else {
                    mPath!!.moveTo(x, Y)
                    mPaintCircular.color = mPaintColor
                    mPaintCircular.strokeWidth = 4f
                    mCanvas!!.drawCircle(x, Y, 4f, mPaintCircular)
                }
                return
            } else if (canvasStyle == CanvasStyle.CIRCULAR_POINT) {
                mPaintCircular.color = mPaintColor
                val Y = y - mCircularP + mStarBitmap.height / 2
                mCanvas!!.drawCircle(x, Y, (mPaintWidth + 5).toFloat(), mPaintCircular)
                mDrawPath!!.pointList!!.add(Point(x.toInt(), Y.toInt()))
                mDrawPath!!.rect = Rect(
                    (x - mPaintWidth - 20).toInt(),
                    (Y - mPaintWidth - 20).toInt(),
                    (x + mPaintWidth + 20).toInt(),
                    (Y + mPaintWidth + 20).toInt()
                )
                mCurrentPaths.add(mDrawPath!!)
                if (mListener != null) {
                    mListener!!.onDraw()
                }
            } else if (canvasStyle == CanvasStyle.RECT_LINE) {
                if (mDownX == x || mDownY == y) return
                mPath!!.lineTo(x, mDownY)
                setBuilder(x.toInt(), mDownY.toInt())
                mPath!!.lineTo(x, y)
                setBuilder(x.toInt(), y.toInt())
                mPath!!.lineTo(mDownX, y)
                setBuilder(mDownX.toInt(), y.toInt())
                mPath!!.lineTo(mDownX, mDownY)
                setBuilder(mDownX.toInt(), mDownY.toInt())
            } else if (canvasStyle == CanvasStyle.ELLIPSE_LINE) {
                if (mDownX == x || mDownY == y) {
                    return
                }
                val minX = if (x < mDownX) x else mDownX
                val maxX = if (x > mDownX) x else mDownX
                val minY = if (y < mDownY) y else mDownY
                val maxY = if (y > mDownY) y else mDownY
                mPath!!.moveTo(minX, minY)
                mPath!!.addOval(RectF(minX, minY, maxX, maxY), Path.Direction.CW)
                var a = 0.0
                val xR = ((maxX - minX) / 2).toDouble()
                val yR = ((maxY - minY) / 2).toDouble()
                var tempX = (xR * cos(a) + xR + minX).toInt()
                val tempY = (yR * sin(a) + yR + minY).toInt()
                val firstX = tempX
                setBuilder(tempX, tempY)
                var bLeft = false
                var bRight = false
                while (!bLeft || !bRight) {
                    a += 0.1
                    val x1 = (xR * Math.cos(a) + xR + minX).toInt()
                    val y1 = (yR * Math.sin(a) + yR + minY).toInt()
                    if (!bLeft && x1 > tempX) {
                        bLeft = true
                    }
                    if (!bRight && bLeft && x1 <= tempX) {
                        bRight = true
                        setBuilder(firstX, tempY)
                    } else {
                        tempX = x1
                        setBuilder(x1, y1)
                    }
                }
            } else {
                if (!contains(
                        mDownX.toInt(),
                        mDownY.toInt(),
                        x.toInt(),
                        y.toInt(),
                        ChouXiBanJing
                    )
                ) {
                    mPath!!.lineTo(x, y)
                    setBuilder(x.toInt(), y.toInt())
                }
                if (canvasStyle == CanvasStyle.GREENLAND_LINE || canvasStyle == CanvasStyle.WATER_LINE || canvasStyle == CanvasStyle.PARKING_LINE) {
                    val point = mDrawPath!!.pointList!![0]
                    mPath!!.lineTo(point.x.toFloat(), point.y.toFloat())
                    setBuilder(point.x, point.y)
                }
            }
            if (mDrawPath!!.pointList!!.size > 1) {
                if (canvasStyle == CanvasStyle.RAILWAY_LINE) {
                    mCanvas!!.drawPath(mPath!!, mPaintRailway1)
                    mCanvas!!.drawPath(mPath!!, mPaintRailway2)
                } else if (canvasStyle == CanvasStyle.GREENLAND_LINE) {
                    mCanvas!!.drawPath(mPath!!, mPaintGreenland)
                    mCanvas!!.drawPath(mPath!!, mPaintGreenland1)
                } else if (canvasStyle == CanvasStyle.WATER_LINE) {
                    mCanvas!!.drawPath(mPath!!, mPaintWater)
                    mCanvas!!.drawPath(mPath!!, mPaintWater1)
                } else if (canvasStyle == CanvasStyle.PARKING_LINE) {
                    mCanvas!!.drawPath(mPath!!, mPaintParking)
                    mCanvas!!.drawPath(mPath!!, mPaintParking1)
                } else {
                    mCanvas!!.drawPath(mPath!!, mPaint!!)
                }
                mCurrentPaths.add(mDrawPath!!)
                if (mListener != null) {
                    mListener!!.onDraw()
                }
            }
            mPath = null
        } catch (e: Exception) {
        }
    }

    /**
     * 记录每一个点，并计算外截矩形
     *
     * @param x
     * @param y
     */
    private fun setBuilder(x: Int, y: Int) {
        if (!isSavePoint || mDrawPath == null) return
        if (mDrawPath!!.pointList == null) mDrawPath!!.pointList = ArrayList()
        if (mDrawPath!!.rect == null) {
            mDrawPath!!.rect = Rect(x, y, x + 1, y + 1)
        }
        if (x < mDrawPath!!.rect!!.left) {
            mDrawPath!!.rect!!.left = x
        } else if (x > mDrawPath!!.rect!!.right) {
            mDrawPath!!.rect!!.right = x
        }
        if (y < mDrawPath!!.rect!!.top) {
            mDrawPath!!.rect!!.top = y
        } else if (y > mDrawPath!!.rect!!.bottom) {
            mDrawPath!!.rect!!.bottom = y
        }
        mDrawPath!!.pointList!!.add(Point(x, y))
    }

    /**
     * 记录手指滑动中的坐标
     *
     * @param x
     * @param y
     */
    private fun touch_move(x: Float, y: Float) {
        mMoveX = x
        mMoveY = y
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        initCanvas()
        if (mCurrentPaths.isNotEmpty()) {
            for (dp in mCurrentPaths) {
                if (dp.style == CanvasStyle.RAILWAY_LINE) {
                    mCanvas!!.drawPath(dp.path, mPaintRailway1)
                    mCanvas!!.drawPath(dp.path, mPaintRailway2)
                } else if (dp.style == CanvasStyle.GREENLAND_LINE) {
                    mCanvas!!.drawPath(dp.path, mPaintGreenland)
                    mCanvas!!.drawPath(dp.path, mPaintGreenland1)
                } else if (dp.style == CanvasStyle.WATER_LINE) {
                    mCanvas!!.drawPath(dp.path, mPaintWater)
                    mCanvas!!.drawPath(mPath!!, mPaintWater1)
                } else if (dp.style == CanvasStyle.PARKING_LINE) {
                    mCanvas!!.drawPath(dp.path, mPaintParking)
                    mCanvas!!.drawPath(mPath!!, mPaintParking1)
                } else {
                    mCanvas!!.drawPath(dp.path, dp.getPaint(mPaint)!!)
                }
            }
        }
    }

    /**
     * 撤销上一步
     */
    fun back(): Boolean {
        if (mCurrentPaths.size > 0) {
            //调用初始化画布函数以清空画布
            initCanvas()
            //将路径保存列表中的最后一个元素删除 ,并将其保存在路径删除列表中
            val drawPath = mCurrentPaths[mCurrentPaths.size - 1]
            //如果是正在绘制的折线
            if ((drawPath.style == CanvasStyle.POLY_LINE
                        || drawPath.style == CanvasStyle.RAILWAY_LINE)
                && !drawPath.isOver
            ) {
                val drawPathCopy: DrawPath?
                if (mDeletePaths!!.size == 0 || mDeletePaths[mDeletePaths.size - 1]!!.style != drawPath.style || mDeletePaths[mDeletePaths.size - 1]!!.isOver) {
                    drawPathCopy = DrawPath(
                        null,
                        drawPath.path,
                        drawPath.width,
                        drawPath.color,
                        drawPath.style
                    )
                    drawPathCopy.rect = drawPath.rect
                    drawPathCopy.isOver = false
                    mDeletePaths.add(drawPathCopy)
                } else {
                    drawPathCopy = mDeletePaths[mDeletePaths.size - 1]
                }
                val point = drawPath.pointList!![drawPath.pointList!!.size - 1]
                drawPathCopy!!.pointList!!.add(point)
                if (drawPath.pointList!!.size == 1) {
                    mCurrentPaths.removeAt(mCurrentPaths.size - 1)
                } else {
                    drawPath.pointList!!.remove(point)
                }
            } else if ((drawPath.style == CanvasStyle.WATER_LINE || drawPath.style == CanvasStyle.GREENLAND_LINE || drawPath.style == CanvasStyle.PARKING_LINE)
                && !drawPath.isOver
            ) {
                val drawPathCopy: DrawPath?
                if (mDeletePaths!!.size == 0 || mDeletePaths[mDeletePaths.size - 1]!!.style != drawPath.style || mDeletePaths[mDeletePaths.size - 1]!!.isOver) {
                    drawPathCopy = DrawPath(
                        null,
                        drawPath.path,
                        drawPath.width,
                        drawPath.color,
                        drawPath.style
                    )
                    drawPathCopy.rect = drawPath.rect
                    drawPathCopy.isOver = false
                    mDeletePaths.add(drawPathCopy)
                } else {
                    drawPathCopy = mDeletePaths[mDeletePaths.size - 1]
                }
                val point = drawPath.pointList!![drawPath.pointList!!.size - 2]
                drawPathCopy!!.pointList!!.add(point)
                if (drawPath.pointList!!.size < 3) {
                    mCurrentPaths.removeAt(mCurrentPaths.size - 1)
                } else {
                    drawPath.pointList!!.removeAt(drawPath.pointList!!.size - 1)
                    drawPath.pointList!!.remove(point)
                    drawPath.pointList!!.add(drawPath.pointList!![0])
                }
            } else {
                mDeletePaths!!.add(drawPath)
                mCurrentPaths.removeAt(mCurrentPaths.size - 1)
            }
            if (mCurrentPaths.isNotEmpty()) {
                //将路径保存列表中的路径重绘在画布上
                for (dp in mCurrentPaths) {
                    if (dp.style == CanvasStyle.POLY_LINE) {
                        dp.path.reset()
                        for (i in dp.pointList!!.indices) {
                            val point = dp.pointList!![i]
                            mPaintCircular.color = dp.color
                            if (i == 0) {
                                mCanvas!!.drawCircle(
                                    point.x.toFloat(),
                                    point.y.toFloat(),
                                    2f,
                                    mPaintCircular
                                )
                                dp.path.moveTo(point.x.toFloat(), point.y.toFloat())
                            } else {
                                dp.path.lineTo(point.x.toFloat(), point.y.toFloat())
                            }
                        }
                        mCanvas!!.drawPath(dp.path, dp.getPaint(mPaint)!!)
                    } else if (dp.style == CanvasStyle.RAILWAY_LINE) {
                        dp.path.reset()
                        for (i in dp.pointList!!.indices) {
                            val point = dp.pointList!![i]
                            if (i == 0) {
                                mCanvas!!.drawCircle(
                                    point.x.toFloat(),
                                    point.y.toFloat(),
                                    2f,
                                    mPaintCircular
                                )
                                dp.path.moveTo(point.x.toFloat(), point.y.toFloat())
                            } else {
                                dp.path.lineTo(point.x.toFloat(), point.y.toFloat())
                            }
                        }
                        mCanvas!!.drawPath(dp.path, mPaintRailway1)
                        mCanvas!!.drawPath(dp.path, mPaintRailway2)
                    } else if (dp.style == CanvasStyle.GREENLAND_LINE) {
                        dp.path.reset()
                        for (i in dp.pointList!!.indices) {
                            val point = dp.pointList!![i]
                            if (i == 0) {
                                mCanvas!!.drawCircle(
                                    point.x.toFloat(),
                                    point.y.toFloat(),
                                    2f,
                                    mPaintCircular
                                )
                                dp.path.moveTo(point.x.toFloat(), point.y.toFloat())
                            } else {
                                dp.path.lineTo(point.x.toFloat(), point.y.toFloat())
                            }
                        }
                        mCanvas!!.drawPath(dp.path, mPaintGreenland)
                        mCanvas!!.drawPath(dp.path, mPaintGreenland1)
                    } else if (dp.style == CanvasStyle.WATER_LINE) {
                        dp.path.reset()
                        for (i in dp.pointList!!.indices) {
                            val point = dp.pointList!![i]
                            if (i == 0) {
                                mCanvas!!.drawCircle(
                                    point.x.toFloat(),
                                    point.y.toFloat(),
                                    2f,
                                    mPaintCircular
                                )
                                dp.path.moveTo(point.x.toFloat(), point.y.toFloat())
                            } else {
                                dp.path.lineTo(point.x.toFloat(), point.y.toFloat())
                            }
                        }
                        mCanvas!!.drawPath(dp.path, mPaintWater)
                        mCanvas!!.drawPath(dp.path, mPaintWater1)
                    } else if (dp.style == CanvasStyle.PARKING_LINE) {
                        dp.path.reset()
                        for (i in dp.pointList!!.indices) {
                            val point = dp.pointList!![i]
                            if (i == 0) {
                                mCanvas!!.drawCircle(
                                    point.x.toFloat(),
                                    point.y.toFloat(),
                                    2f,
                                    mPaintCircular
                                )
                                dp.path.moveTo(point.x.toFloat(), point.y.toFloat())
                            } else {
                                dp.path.lineTo(point.x.toFloat(), point.y.toFloat())
                            }
                        }
                        mCanvas!!.drawPath(dp.path, mPaintParking)
                        mCanvas!!.drawPath(dp.path, mPaintParking1)
                    } else if (dp.style == CanvasStyle.CIRCULAR_POINT) {
                        mPaintCircular.color = dp.color
                        mCanvas!!.drawCircle(
                            dp.pointList!![0].x.toFloat(),
                            dp.pointList!![0].y.toFloat(),
                            dp.width.toFloat(),
                            mPaintCircular
                        )
                    } else {
                        mCanvas!!.drawPath(dp.path, dp.getPaint(mPaint)!!)
                    }
                }
            }
            invalidate() // 刷新
        }
        if (mCurrentPaths.size == 0) {
            mDrawPath = null
        }
        return mCurrentPaths.size != 0
    }

    /**
     * 恢复撤销的上一步
     */
    fun forward() {
        if (mDeletePaths!!.size > 0) {
            //将删除的路径列表中的最后一个，也就是最顶端路径取出（栈）,并加入路径保存列表中
            val dp = mDeletePaths[mDeletePaths.size - 1]
            if ((dp!!.style == CanvasStyle.POLY_LINE
                        || dp.style == CanvasStyle.RAILWAY_LINE)
                && !dp.isOver
            ) {
                val dp2: DrawPath?
                if (mCurrentPaths.size > 0 && mCurrentPaths[mCurrentPaths.size - 1].style == dp.style && !mCurrentPaths[mCurrentPaths.size - 1].isOver) {
                    dp2 = mCurrentPaths[mCurrentPaths.size - 1]
                } else {
                    dp2 = DrawPath(null, dp.path, dp.width, dp.color, dp.style)
                    dp2.isOver = false
                    dp2.rect = dp.rect
                    mCurrentPaths.add(dp2)
                    if (mListener != null) {
                        mListener!!.onDraw()
                    }
                }
                val point = dp.pointList!![dp.pointList!!.size - 1]
                dp2.pointList!!.add(point)
                dp.pointList!!.remove(point)
            } else if ((dp.style == CanvasStyle.WATER_LINE || dp.style == CanvasStyle.GREENLAND_LINE || dp.style == CanvasStyle.PARKING_LINE)
                && !dp.isOver
            ) {
                val dp2: DrawPath?
                if (mCurrentPaths.size > 0 && mCurrentPaths[mCurrentPaths.size - 1].style == dp.style && !mCurrentPaths[mCurrentPaths.size - 1].isOver) {
                    dp2 = mCurrentPaths[mCurrentPaths.size - 1]
                } else {
                    dp2 = DrawPath(null, dp.path, dp.width, dp.color, dp.style)
                    dp2.isOver = false
                    dp2.rect = dp.rect
                    mCurrentPaths.add(dp2)
                    if (mListener != null) {
                        mListener!!.onDraw()
                    }
                }
                val point = dp.pointList!![dp.pointList!!.size - 1]
                if (dp2.pointList!!.size > 1) {
                    dp2.pointList!!.removeAt(dp2.pointList!!.size - 1)
                }
                dp2.pointList!!.add(point)
                dp2.pointList!!.add(dp2.pointList!![0])
                dp.pointList!!.remove(point)
            } else {
                mCurrentPaths.add(dp)
                if (mListener != null) {
                    mListener!!.onDraw()
                }
            }
            //将取出的路径重绘在画布上
            if (dp.style == CanvasStyle.POLY_LINE) {
                val dp2 = mCurrentPaths[mCurrentPaths.size - 1]
                dp2.path.reset()
                if (dp2.pointList != null && dp2.pointList!!.size > 0) {
                    for (i in dp2.pointList!!.indices) {
                        val point = dp2.pointList!![i]
                        mPaintCircular.color = dp2.color
                        if (i == 0) {
                            mCanvas!!.drawCircle(
                                point.x.toFloat(),
                                point.y.toFloat(),
                                2f,
                                mPaintCircular
                            )
                            dp2.path.moveTo(point.x.toFloat(), point.y.toFloat())
                        } else {
                            dp2.path.lineTo(point.x.toFloat(), point.y.toFloat())
                        }
                    }
                }
                mCanvas!!.drawPath(dp2.path, dp2.getPaint(mPaint)!!)
            } else if (dp.style == CanvasStyle.RAILWAY_LINE) {
                val dp2 = mCurrentPaths[mCurrentPaths.size - 1]
                dp2.path.reset()
                if (dp2.pointList != null && dp2.pointList!!.size > 0) {
                    for (i in dp2.pointList!!.indices) {
                        val point = dp2.pointList!![i]
                        if (i == 0) {
                            dp2.path.moveTo(point.x.toFloat(), point.y.toFloat())
                        } else {
                            dp2.path.lineTo(point.x.toFloat(), point.y.toFloat())
                        }
                    }
                }
                mCanvas!!.drawPath(dp2.path, mPaintRailway1)
                mCanvas!!.drawPath(dp2.path, mPaintRailway2)
            } else if (dp.style == CanvasStyle.GREENLAND_LINE) {
                val dp2 = mCurrentPaths[mCurrentPaths.size - 1]
                initCanvas()
                dp2.path.reset()
                if (dp2.pointList != null && dp2.pointList!!.size > 0) {
                    for (i in dp2.pointList!!.indices) {
                        val point = dp2.pointList!![i]
                        if (i == 0) {
                            mCanvas!!.drawCircle(
                                point.x.toFloat(),
                                point.y.toFloat(),
                                2f,
                                mPaintCircular
                            )
                            dp2.path.moveTo(point.x.toFloat(), point.y.toFloat())
                        } else {
                            dp2.path.lineTo(point.x.toFloat(), point.y.toFloat())
                        }
                    }
                }
                mCanvas!!.drawPath(dp2.path, mPaintGreenland)
                mCanvas!!.drawPath(dp2.path, mPaintGreenland1)
            } else if (dp.style == CanvasStyle.WATER_LINE) {
                val dp2 = mCurrentPaths[mCurrentPaths.size - 1]
                initCanvas()
                dp2.path.reset()
                if (dp2.pointList != null && dp2.pointList!!.size > 0) {
                    for (i in dp2.pointList!!.indices) {
                        val point = dp2.pointList!![i]
                        if (i == 0) {
                            mCanvas!!.drawCircle(
                                point.x.toFloat(),
                                point.y.toFloat(),
                                2f,
                                mPaintCircular
                            )
                            dp2.path.moveTo(point.x.toFloat(), point.y.toFloat())
                        } else {
                            dp2.path.lineTo(point.x.toFloat(), point.y.toFloat())
                        }
                    }
                }
                mCanvas!!.drawPath(dp2.path, mPaintWater)
                mCanvas!!.drawPath(dp2.path, mPaintWater1)
            } else if (dp.style == CanvasStyle.PARKING_LINE) {
                val dp2 = mCurrentPaths[mCurrentPaths.size - 1]
                initCanvas()
                dp2!!.path.reset()
                if (dp2.pointList != null && dp2.pointList!!.size > 0) {
                    for (i in dp2.pointList!!.indices) {
                        val point = dp2.pointList!![i]
                        if (i == 0) {
                            mCanvas!!.drawCircle(
                                point.x.toFloat(),
                                point.y.toFloat(),
                                2f,
                                mPaintCircular
                            )
                            dp2.path.moveTo(point.x.toFloat(), point.y.toFloat())
                        } else {
                            dp2.path.lineTo(point.x.toFloat(), point.y.toFloat())
                        }
                    }
                }
                mCanvas!!.drawPath(dp2.path, mPaintParking)
                mCanvas!!.drawPath(dp2.path, mPaintParking1)
            } else if (dp.style == CanvasStyle.CIRCULAR_POINT) {
                mPaintCircular.color = dp.color
                mCanvas!!.drawCircle(
                    dp.pointList!![0].x.toFloat(),
                    dp.pointList!![0].y.toFloat(),
                    dp.width.toFloat(),
                    mPaintCircular
                )
            } else {
                mCanvas!!.drawPath(dp.path, dp.getPaint(mPaint)!!)
            }
            //将该路径从删除的路径列表中去除
            if ((dp.style == CanvasStyle.POLY_LINE
                        || dp.style == CanvasStyle.RAILWAY_LINE)
                && !dp.isOver
            ) {
                if (dp.pointList!!.size == 0) {
                    mDeletePaths.remove(dp)
                }
            } else if ((dp.style == CanvasStyle.WATER_LINE || dp.style == CanvasStyle.GREENLAND_LINE || dp.style == CanvasStyle.PARKING_LINE)
                && !dp.isOver
            ) {
                if (dp.pointList!!.size == 0) {
                    mDeletePaths.remove(dp)
                }
            } else {
                mDeletePaths.removeAt(mDeletePaths.size - 1)
            }
            invalidate()
        }
    }

    /*
         * 清空的主要思想就是初始化画布
         * 将保存路径的两个List清空
         * */
    fun removeAllPaint() {
        //调用初始化画布函数以清空画布
        initCanvas()
        invalidate() //刷新
        mPath = null
        mDrawPath = null
        mCurrentPaths.clear()
        mDeletePaths!!.clear()
    }

    /**
     * 设置画笔粗细
     *
     * @param paintWidth
     */
    fun setPaintWidth(paintWidth: Int) {
        mPaintWidth = paintWidth
        if (mPaint != null) {
            mPaint.strokeWidth = mPaintWidth.toFloat()
        }
    }

    /**
     * 设置画笔颜色
     *
     * @param color
     */
    fun setPaintColor(color: Int) {
        mPaintColor = color
        if (mPaint != null) {
            mPaint.color = mPaintColor
        }
    }

    /**
     * 将图形数据绘制在画布上
     *
     * @param value
     */
    fun setDrawPathList(value: MutableList<DrawPath>) {
        mPath = null
        mCurrentPaths = value
        if (mListener != null) {
            mListener!!.onDraw()
        }
        mDeletePaths!!.clear()
        if (mCanvas != null) {
            if (mCurrentPaths.size > 0) {
                for (dp in mCurrentPaths) {
                    if (dp.style == CanvasStyle.RAILWAY_LINE) {
                        dp.path.reset()
                        for (i in dp.pointList!!.indices) {
                            if (i == 0) {
                                dp.path.moveTo(
                                    dp.pointList!![i].x.toFloat(),
                                    dp.pointList!![i].y.toFloat()
                                )
                            } else {
                                dp.path.lineTo(
                                    dp.pointList!![i].x.toFloat(),
                                    dp.pointList!![i].y.toFloat()
                                )
                            }
                        }
                        mCanvas!!.drawPath(dp.path, mPaintRailway1)
                        mCanvas!!.drawPath(dp.path, mPaintRailway2)
                    } else if (dp.style == CanvasStyle.GREENLAND_LINE) {
                        mCanvas!!.drawPath(dp.path, mPaintGreenland)
                        mCanvas!!.drawPath(dp.path, mPaintGreenland1)
                    } else if (dp.style == CanvasStyle.WATER_LINE) {
                        mCanvas!!.drawPath(dp.path, mPaintWater)
                        mCanvas!!.drawPath(dp.path, mPaintWater1)
                    } else if (dp.style == CanvasStyle.PARKING_LINE) {
                        mCanvas!!.drawPath(dp.path, mPaintParking)
                        mCanvas!!.drawPath(dp.path, mPaintParking1)
                    } else if (dp.style == CanvasStyle.CIRCULAR_POINT) {
                        mPaintCircular.color = dp.color
                        mCanvas!!.drawCircle(
                            dp.pointList!![0].x.toFloat(),
                            dp.pointList!![0].y.toFloat(),
                            dp.width.toFloat(),
                            mPaintCircular
                        )
                    } else if (dp.style == CanvasStyle.POLY_LINE) {
                        mPaintCircular.color = dp.color
                        dp.path.reset()
                        for (i in dp.pointList!!.indices) {
                            if (i == 0) {
                                mCanvas!!.drawCircle(
                                    dp.pointList!![i].x.toFloat(),
                                    dp.pointList!![i].y.toFloat(),
                                    2f,
                                    mPaintCircular
                                )
                                dp.path.moveTo(
                                    dp.pointList!![i].x.toFloat(),
                                    dp.pointList!![i].y.toFloat()
                                )
                            } else {
                                dp.path.lineTo(
                                    dp.pointList!![i].x.toFloat(),
                                    dp.pointList!![i].y.toFloat()
                                )
                            }
                        }
                        mCanvas!!.drawPath(dp.path, dp.getPaint(mPaint)!!)
                    } else {
                        mCanvas!!.drawPath(dp.path, dp.getPaint(mPaint)!!)
                    }
                    canvasStyle = dp.style
                }
            }
            mPaint!!.color = mPaintColor
        }
        invalidate() // 刷新
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mBitmap!!, 0f, 0f, mBitmapPaint) //显示旧的画布
        if (mPath != null) {
            // 实时的显示
            if (isMove) {
                if (canvasStyle == CanvasStyle.CIRCULAR_POINT) {
                    mMatrixCircular.reset()
                    mMatrixCircular.postTranslate(
                        mMoveX - mStarBitmap.width / 2,
                        mMoveY - mCircularP
                    )
                    canvas.drawBitmap(mStarBitmap, mMatrixCircular, null)
                } else if (canvasStyle == CanvasStyle.STRAIGHT_LINE) {
                    if (mDrawPath != null && mDrawPath!!.pointList!!.size > 0) {
                        val x = mDrawPath!!.pointList!![0].x
                        val y = mDrawPath!!.pointList!![0].y
                        val Y2 = mMoveY - mCircularP + mStarBitmap.height / 2
                        canvas.drawLine(x.toFloat(), y.toFloat(), mMoveX, Y2, mPaint!!)
                    }
                    mMatrixCircular.reset()
                    mMatrixCircular.postTranslate(
                        mMoveX - mStarBitmap.width / 2,
                        mMoveY - mCircularP
                    )
                    canvas.drawBitmap(mStarBitmap, mMatrixCircular, null)
                } else if (canvasStyle == CanvasStyle.POLY_LINE) {
                    if (mDrawPath != null && mDrawPath!!.pointList!!.size > 0) {
                        val x = mDrawPath!!.pointList!![mDrawPath!!.pointList!!.size - 1].x
                        val y = mDrawPath!!.pointList!![mDrawPath!!.pointList!!.size - 1].y
                        val Y2 = mMoveY - mCircularP + mStarBitmap.height / 2
                        canvas.drawLine(x.toFloat(), y.toFloat(), mMoveX, Y2, mPaint!!)
                    }
                    mMatrixCircular.reset()
                    mMatrixCircular.postTranslate(
                        mMoveX - mStarBitmap.width / 2,
                        mMoveY - mCircularP
                    )
                    canvas.drawBitmap(mStarBitmap, mMatrixCircular, null)
                } else if (canvasStyle == CanvasStyle.RECT_LINE) {
                    if (mDownX > mMoveX && mDownY > mMoveY) {
                        canvas.drawRect(mMoveX, mMoveY, mDownX, mDownY, mPaint!!)
                    } else if (mDownX < mMoveX && mDownY > mMoveY) {
                        canvas.drawRect(mDownX, mMoveY, mMoveX, mDownY, mPaint!!)
                    } else if (mDownX > mMoveX && mDownY < mMoveY) {
                        canvas.drawRect(mMoveX, mDownY, mDownX, mMoveY, mPaint!!)
                    } else {
                        canvas.drawRect(mDownX, mDownY, mMoveX, mMoveY, mPaint!!)
                    }
                } else if (canvasStyle == CanvasStyle.ELLIPSE_LINE) {
                    if (mDownX > mMoveX && mDownY > mMoveY) {
                        canvas.drawOval(RectF(mMoveX, mMoveY, mDownX, mDownY), mPaint!!)
                    } else if (mDownX < mMoveX && mDownY > mMoveY) {
                        canvas.drawOval(RectF(mDownX, mMoveY, mMoveX, mDownY), mPaint!!)
                    } else if (mDownX > mMoveX && mDownY < mMoveY) {
                        canvas.drawOval(RectF(mMoveX, mDownY, mDownX, mMoveY), mPaint!!)
                    } else {
                        canvas.drawOval(RectF(mDownX, mDownY, mMoveX, mMoveY), mPaint!!)
                    }
                } else {
                    if (canvasStyle == CanvasStyle.RAILWAY_LINE) {
                        canvas.drawPath(mPath!!, mPaintRailway1)
                        canvas.drawPath(mPath!!, mPaintRailway2)
                    } else if (canvasStyle == CanvasStyle.GREENLAND_LINE) {
                        canvas.drawPath(mPath!!, mPaintGreenland1)
                    } else if (canvasStyle == CanvasStyle.WATER_LINE) {
                        canvas.drawPath(mPath!!, mPaintWater1)
                    } else if (canvasStyle == CanvasStyle.PARKING_LINE) {
                        canvas.drawPath(mPath!!, mPaintParking1)
                    } else {
                        canvas.drawPath(mPath!!, mPaint!!)
                    }
                }
            } else {


//                if (mStyle == CanvasStyle.RAILWAY_LINE) {
//                    canvas.drawPath(mPath, mPaintRailway1);
//                    canvas.drawPath(mPath, mPaintRailway2);
//                } else {
//                    canvas.drawPath(mPath, mPaint);
//                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> if (isEraser) {
                clearLine(x.toInt(), y.toInt())
            } else {
//                    if (mCurrentPaths != null && mCurrentPaths.size() > 0) {
//                        for (DrawPath dp : mCurrentPaths) {
//                            if (dp.style == CanvasStyle.GREENLAND_LINE || dp.style == CanvasStyle.WATER_LINE) {
//                                return true;
//                            }
//                        }
//                    }
                isMove = false
                if (canvasStyle == CanvasStyle.RAILWAY_LINE || canvasStyle == CanvasStyle.GREENLAND_LINE || canvasStyle == CanvasStyle.WATER_LINE || canvasStyle == CanvasStyle.PARKING_LINE) {
                    touch_start_poly(x.toInt(), y.toInt())
                    return true
                } else if (canvasStyle == CanvasStyle.CIRCULAR_POINT) {
                    mPath = Path()
                    mDrawPath = DrawPath(null, mPath!!, mPaintWidth + 5, mPaintColor, canvasStyle)
                    return true
                } else if (canvasStyle == CanvasStyle.POLY_LINE) {
                    if (mDeletePaths != null && mDeletePaths.size > 0) {
                        mDeletePaths.clear()
                    }
                    if (mDrawPath != null && mDrawPath!!.style == CanvasStyle.POLY_LINE && !mDrawPath!!.isOver) {
                        return true
                    }
                    mPath = Path()
                    mDrawPath = DrawPath(
                        Point(x.toInt(), y.toInt()),
                        mPath!!,
                        mPaintWidth,
                        mPaintColor,
                        canvasStyle
                    )
                    mDrawPath!!.isOver = false
                    return true
                } else if (canvasStyle == CanvasStyle.STRAIGHT_LINE) {
                    if (mDeletePaths != null && mDeletePaths.size > 0) {
                        mDeletePaths.clear()
                    }
                    if (mDrawPath != null && mDrawPath!!.style == CanvasStyle.STRAIGHT_LINE && !mDrawPath!!.isOver) {
                        return true
                    }
                    mPath = Path()
                    mDrawPath = DrawPath(
                        Point(x.toInt(), y.toInt()),
                        mPath!!,
                        mPaintWidth,
                        mPaintColor,
                        canvasStyle
                    )
                    mDrawPath!!.isOver = false
                    return true
                }
                mPath = Path()
                mDrawPath = DrawPath(
                    Point(x.toInt(), y.toInt()),
                    mPath!!,
                    mPaintWidth,
                    mPaintColor,
                    canvasStyle
                )
                touch_start(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (isEraser) {
                    return true
                }
                if (canvasStyle == CanvasStyle.RAILWAY_LINE || canvasStyle == CanvasStyle.GREENLAND_LINE || canvasStyle == CanvasStyle.WATER_LINE || canvasStyle == CanvasStyle.PARKING_LINE) return true

//                if (mCurrentPaths != null && mCurrentPaths.size() > 0) {
//                    for (DrawPath dp : mCurrentPaths) {
//                        if (dp.style == CanvasStyle.GREENLAND_LINE || dp.style == CanvasStyle.WATER_LINE) {
//                            return true;
//                        }
//                    }
//                }
                isMove = true
                if (x == mDownX && y == mDownY) return false
                if (canvasStyle == CanvasStyle.POLY_LINE || canvasStyle == CanvasStyle.STRAIGHT_LINE || canvasStyle == CanvasStyle.RECT_LINE || canvasStyle == CanvasStyle.ELLIPSE_LINE || canvasStyle == CanvasStyle.CIRCULAR_POINT) {
                    touch_move(x, y)
                } else {
                    if (contains(
                            mDownX.toInt(),
                            mDownY.toInt(),
                            x.toInt(),
                            y.toInt(),
                            ChouXiBanJing
                        )
                    ) {
                        return true
                    }
                    touch_move_line(x, y)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (isEraser) {
                    return true
                }
                if (canvasStyle == CanvasStyle.RAILWAY_LINE || canvasStyle == CanvasStyle.GREENLAND_LINE || canvasStyle == CanvasStyle.WATER_LINE || canvasStyle == CanvasStyle.PARKING_LINE) return true
                //                if (mCurrentPaths != null && mCurrentPaths.size() > 0) {
//                    for (DrawPath dp : mCurrentPaths) {
//                        if (dp.style == CanvasStyle.GREENLAND_LINE || dp.style == CanvasStyle.WATER_LINE) {
//                            return true;
//                        }
//                    }
//                }
                touch_up(x, y)
                invalidate()
                isMove = false
            }
        }
        return true
    }

    /**
     * 橡皮擦捕捉清除
     *
     * @param x
     * @param y
     */
    private fun clearLine(x: Int, y: Int) {
        if (mCurrentPaths.size == 0) return
        val rect = Rect(x - EraserWight, y - EraserWight, x + EraserWight, y + EraserWight)
        for (i in mCurrentPaths.size - 1 downTo -1 + 1) {
            val path = mCurrentPaths[i]
            if (path.rect != null && path.rect!!.intersects(
                    rect.left,
                    rect.top,
                    rect.right,
                    rect.bottom
                )
            ) {
                if (LineIntersectRect(path.pointList, rect)) {
                    initCanvas()
                    mDeletePaths!!.add(path)
                    mCurrentPaths.remove(path)
                    for (dp in mCurrentPaths) {
                        if (dp.style == CanvasStyle.RAILWAY_LINE) {
                            dp.path.reset()
                            if (dp.pointList != null && dp.pointList!!.size > 0) {
                                for (k in dp.pointList!!.indices) {
                                    if (k == 0) {
                                        dp.path.moveTo(
                                            dp.pointList!![k].x.toFloat(),
                                            dp.pointList!![k].y.toFloat()
                                        )
                                    } else {
                                        dp.path.lineTo(
                                            dp.pointList!![k].x.toFloat(),
                                            dp.pointList!![k].y.toFloat()
                                        )
                                    }
                                }
                            }
                            mCanvas!!.drawPath(dp.path, mPaintRailway1)
                            mCanvas!!.drawPath(dp.path, mPaintRailway2)
                        } else if (dp.style == CanvasStyle.GREENLAND_LINE) {
                            mCanvas!!.drawPath(dp.path, mPaintGreenland)
                            mCanvas!!.drawPath(dp.path, mPaintGreenland1)
                        } else if (dp.style == CanvasStyle.WATER_LINE) {
                            mCanvas!!.drawPath(dp.path, mPaintWater)
                            mCanvas!!.drawPath(dp.path, mPaintWater1)
                        } else if (dp.style == CanvasStyle.PARKING_LINE) {
                            mCanvas!!.drawPath(dp.path, mPaintParking)
                            mCanvas!!.drawPath(dp.path, mPaintParking1)
                        } else if (dp.style == CanvasStyle.CIRCULAR_POINT) {
                            mPaintCircular.color = dp.color
                            mCanvas!!.drawCircle(
                                dp.pointList!![0].x.toFloat(),
                                dp.pointList!![0].y.toFloat(),
                                dp.width.toFloat(),
                                mPaintCircular
                            )
                        } else if (dp.style == CanvasStyle.POLY_LINE) {
                            mPaintCircular.color = dp.color
                            dp.path.reset()
                            if (dp.pointList != null && dp.pointList!!.size > 0) {
                                for (k in dp.pointList!!.indices) {
                                    if (k == 0) {
                                        mCanvas!!.drawCircle(
                                            dp.pointList!![k].x.toFloat(),
                                            dp.pointList!![k].y.toFloat(),
                                            2f,
                                            mPaintCircular
                                        )
                                        dp.path.moveTo(
                                            dp.pointList!![k].x.toFloat(),
                                            dp.pointList!![k].y.toFloat()
                                        )
                                    } else {
                                        dp.path.lineTo(
                                            dp.pointList!![k].x.toFloat(),
                                            dp.pointList!![k].y.toFloat()
                                        )
                                    }
                                }
                            }
                            mCanvas!!.drawPath(dp.path, dp.getPaint(mPaint)!!)
                        } else {
                            mCanvas!!.drawPath(dp.path, dp.getPaint(mPaint)!!)
                        }
                    }
                    invalidate() // 刷新
                    return
                }
            }
        }
        invalidate()
    }

    /**
     * 整条线段是否穿过矩形
     *
     * @param pointList
     * @param rect
     * @return
     */
    private fun LineIntersectRect(pointList: List<Point>?, rect: Rect?): Boolean {
        if (pointList != null && pointList.size > 1 && rect != null && !rect.isEmpty) {
            for (i in 0 until pointList.size - 1) {
                if (CheckRectLine(pointList[i], pointList[i + 1], rect)) return true
            }
        } else if (pointList != null && pointList.size == 1) {
            val point = pointList[0]
            if (point.x >= rect!!.left && point.x <= rect.right && point.y >= rect.top && point.y <= rect.bottom) {
                return true
            }
        }
        return false
    }

    /**
     * 获取画布上的图形数据
     *
     * @return
     */
    val paths: List<DrawPath>?
        get() {
            setPolyLineOver()
            if (mCurrentPaths.size > 0) {
                var i = 0
                while (i < mCurrentPaths.size) {
                    val drawPath = mCurrentPaths[i]
                    if (drawPath.style == CanvasStyle.POLY_LINE
                        || drawPath.style == CanvasStyle.RAILWAY_LINE
                    ) {
                        if (drawPath.pointList!!.size < 2) {
                            mCurrentPaths.remove(drawPath)
                            i--
                        }
                    } else if (drawPath.style == CanvasStyle.WATER_LINE || drawPath.style == CanvasStyle.GREENLAND_LINE || drawPath.style == CanvasStyle.PARKING_LINE) {
                        if (drawPath.pointList!!.size < 4) {
                            mCurrentPaths.remove(drawPath)
                            i--
                        }
                    }
                    i++
                }
            }
            return mCurrentPaths
        }

    /**
     * 得到缩略图
     *
     * @return
     */
    val thumbnail: Bitmap?
        get() {
            if (mCurrentPaths == null || mCurrentPaths!!.size == 0) {
                return null
            }
            val whiteBgBitmap = Bitmap.createBitmap(
                mBitmap!!.width,
                mBitmap!!.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(whiteBgBitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(mBitmap!!, 0f, 0f, null)
            return ThumbnailUtils.extractThumbnail(whiteBgBitmap, 100, 100)
        }

    /**
     * 设置是否保存中间点信息
     *
     * @param b
     */
    fun setIsSavePoint(b: Boolean) {
        isSavePoint = b
    }

    /**
     * 设置橡皮擦按钮是否开启或关闭
     *
     * @param b
     */
    fun setEraser(b: Boolean) {
        isEraser = b
        if (b) {
            setPolyLineOver()
        }
    }

    /**
     * 判断线与垂直线相交
     *
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     * @param x0
     * @param y1
     * @param y2
     * @return
     */
    private fun CheckRectLineV(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        x0: Float,
        y1: Float,
        y2: Float
    ): Boolean {
        if (x0 < startX && x0 < endX) return false
        if (x0 > startX && x0 > endX) return false
        if (startX == endX) {
            return if (x0 == startX) {
                if (startY < y1 && endY < y1) return false
                !(startY > y2 && endY > y2)
            } else {
                false
            }
        }
        val y = (endY - startY) * (x0 - startX) / (endX - startX) + startY
        return y in y1..y2
    }

    /**
     * 判断线与水平线相交
     *
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     * @param y0
     * @param x1
     * @param x2
     * @return
     */
    private fun CheckRectLineH(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        y0: Float,
        x1: Float,
        x2: Float
    ): Boolean {
        //直线在点的上方
        if (y0 < startY && y0 < endY) return false
        //直线在点的下方
        if (y0 > startY && y0 > endY) return false
        //水平直线
        if (startY == endY) {
            //水平直线与点处于同一水平。
            return if (y0 == startY) {
                //直线在点的左边
                if (startX < x1 && endX < x1) return false
                //直线在x2垂直线右边
                !(startX > x2 && endX > x2)
                //直线的部分或者全部处于点与x2垂直线之间
            } else  //水平直线与点不处于同一水平。
            {
                false
            }
        }
        //斜线
        val x = (endX - startX) * (y0 - startY) / (endY - startY) + startX
        return x in x1..x2
    }

    /**
     * 直线是否穿过矩形
     *
     * @param start
     * @param end
     * @param rect
     * @return
     */
    private fun CheckRectLine(start: Point, end: Point, rect: Rect): Boolean {
        var result = false
        if (rect.contains(start.x, start.y) || rect.contains(end.x, end.y)) result = true else {
            if (CheckRectLineH(
                    start.x.toFloat(),
                    start.y.toFloat(),
                    end.x.toFloat(),
                    end.y.toFloat(),
                    rect.top.toFloat(),
                    rect.left.toFloat(),
                    rect.right.toFloat()
                )
            ) return true
            if (CheckRectLineH(
                    start.x.toFloat(),
                    start.y.toFloat(),
                    end.x.toFloat(),
                    end.y.toFloat(),
                    rect.bottom.toFloat(),
                    rect.left.toFloat(),
                    rect.right.toFloat()
                )
            ) return true
            if (CheckRectLineV(
                    start.x.toFloat(),
                    start.y.toFloat(),
                    end.x.toFloat(),
                    end.y.toFloat(),
                    rect.left.toFloat(),
                    rect.top.toFloat(),
                    rect.bottom.toFloat()
                )
            ) return true
            if (CheckRectLineV(
                    start.x.toFloat(),
                    start.y.toFloat(),
                    end.x.toFloat(),
                    end.y.toFloat(),
                    rect.right.toFloat(),
                    rect.top.toFloat(),
                    rect.bottom.toFloat()
                )
            ) return true
        }
        return result
    }

    /**
     * 两个矩形是否包含
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param dis
     * @return
     */
    private fun contains(x1: Int, y1: Int, x2: Int, y2: Int, dis: Int): Boolean {
        val rect = Rect(x1 - dis, y1 - dis, x1 + dis, y1 + dis)
        return rect.contains(x2, y2)
    }

    companion object {
        /*
        橡皮擦捕捉半径
     */
        private const val EraserWight = 12

        /**
         * 线点 抽稀半径
         */
        private const val ChouXiBanJing = 12 //抽稀半径

        /**
         * 手指滑动的距离，两点之间像素值少于4 的点不要
         */
        private const val TOUCH_TOLERANCE = 4f
    }
}