package com.navinfo.collect.library.map.handler

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.navinfo.collect.library.R
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.utils.DistanceUtil
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.StringUtil.Companion.createUUID
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.backend.CanvasAdapter
import org.oscim.backend.canvas.Bitmap
import org.oscim.core.GeoPoint
import org.oscim.core.MapPosition
import org.oscim.event.Event
import org.oscim.layers.marker.ItemizedLayer
import org.oscim.layers.marker.ItemizedLayer.OnItemGestureListener
import org.oscim.layers.marker.MarkerInterface
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol
import org.oscim.layers.vector.PathLayer
import org.oscim.layers.vector.geometries.PolygonDrawable
import org.oscim.layers.vector.geometries.Style
import org.oscim.map.Map
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.ceil


open class MeasureLayerHandler(context: AppCompatActivity, mapView: NIMapView) :
    BaseHandler(context, mapView), Map.UpdateListener {

    private var editIndex: Int = -1;//线修型的时候，用来表示是不是正在修型，修的第几个点
    private val mPathMakers: MutableList<MarkerItem> = mutableListOf()
    private var bDrawLine = false
    private var measureType = MEASURE_TYPE.DISTANCE

    /**
     * 测量类型
     */
    enum class MEASURE_TYPE {
        DISTANCE,//距离
        AREA,//面积
        ANGLE,//角度
    }

    data class MeasureValueBean(
        var value: Double = 0.00,
        var valueString: String = "0.00",
        var unit: String = ""
    )


    /**
     * 加上虚线的总长度
     */
    val tempMeasureValueLiveData = MutableLiveData<MeasureValueBean>()

    /**
     * 实际绘制的总长度
     */
    val measureValueLiveData = MutableLiveData<MeasureValueBean>()


    private val markerLayer: ItemizedLayer by lazy {

        val markerSymbol = MarkerSymbol(
            mPathMarkerBitmap, MarkerSymbol.HotspotPlace.CENTER
        )
        //新增marker图层
        val layer = ItemizedLayer(
            mapView.vtmMap, mutableListOf<MarkerInterface>(), markerSymbol, itemGestureListener
        )

        addLayer(layer, NIMapView.LAYER_GROUPS.OPERATE_MARKER)
        layer
    }

    /**
     * 显示面积文字
     */
    private var mAreaTextLayer: ItemizedLayer? = null

    /**
     * 面积的渲染面
     */
    private var areaPolygon: PolygonDrawable? = null

    /**
     * 面积的渲染样式
     */
    private val areaStyle: Style by lazy {
        Style.builder().scaleZoomLevel(20).buffer(1.0)
            .fillColor(context.resources.getColor(R.color.draw_area_color)).fillAlpha(0.5f)
            .fixed(true).build()
    }

    //绘制线 样式
    private val lineStyle: Style by lazy {
        //新增线数据图层和线样式
        Style.builder().scaleZoomLevel(20).buffer(1.0)
            .stippleColor(context.resources.getColor(R.color.draw_line_blue1_color)).strokeWidth(4f)
            .fillColor(context.resources.getColor(R.color.draw_line_blue2_color)).fillAlpha(0.5f)
            .strokeColor(context.resources.getColor(R.color.draw_line_blue2_color)).stippleWidth(4f)
            .fixed(true).build()
    }

    private val newTempStyle: Style by lazy {
        Style.builder().stippleColor(context.resources.getColor(R.color.transparent)).stipple(30)
            .stippleWidth(30f).strokeWidth(4f)
            .strokeColor(context.resources.getColor(R.color.draw_line_blue2_color)).fixed(true)
            .randomOffset(false).build()
    }

    //线型编辑时的样式
    private val editTempStyle: Style by lazy {
        Style.builder().stippleColor(context.resources.getColor(R.color.transparent)).stipple(30)
            .stippleWidth(30f).strokeWidth(8f)
            .strokeColor(context.resources.getColor(R.color.draw_line_red_color)).fixed(true)
            .randomOffset(false).build()

    }

    //新增线数据引线
    private val mPathLayerTemp: PathLayer by lazy {
        val layer = PathLayer(mMapView.vtmMap, newTempStyle)
        addLayer(layer, NIMapView.LAYER_GROUPS.OPERATE_LINE)
        layer
    }

    //新增线数据
    val mPathLayer: PathLayer by lazy {
        val layer = PathLayer(mMapView.vtmMap, lineStyle)
        addLayer(layer, NIMapView.LAYER_GROUPS.OPERATE_LINE)
        layer
    }

    //线路端点图标
    private val mPathMarkerBitmap: Bitmap by lazy {
        AndroidBitmap(
            BitmapFactory.decodeResource(
                context.resources, R.mipmap.icon_path_maker
            )
        )
    }

    init {
        mMapView.vtmMap.events.bind(this)
    }

    open fun addPoint(type: MEASURE_TYPE, selectPoint: GeoPoint? = null) {

        bDrawLine = true
        measureType = type
        if (!mPathLayer.isEnabled) {
            mPathLayer.isEnabled = true
        }
        if (!markerLayer.isEnabled) {
            markerLayer.isEnabled = true
        }
        //上一个点的引线
        if (!mPathLayerTemp.isEnabled) {
            mPathLayerTemp.isEnabled = true
        }

        val geoPoint = selectPoint ?: GeoPoint(
            mMapView.vtmMap.mapPosition.latitude, mMapView.vtmMap.mapPosition.longitude
        )

        //编辑点
        if (editIndex > -1) {
            Log.e("jingo", "移除marker $editIndex")
            val marker = mPathMakers[editIndex]
            markerLayer.removeItem(marker)
            mPathMakers.removeAt(editIndex)
            val markerItem = MarkerItem(createUUID(), "", "", geoPoint)
            markerLayer.addItem(markerItem)
            mPathMakers.add(editIndex, markerItem)
            if (mPathLayer.points.size > 0) {
                val list: MutableList<GeoPoint> = mPathLayer.points
                list.removeAt(editIndex)
                val list2: MutableList<GeoPoint> = ArrayList(list)
                list2.add(editIndex, geoPoint)
                mPathLayer.setPoints(list2)
            }
            mPathLayerTemp.setStyle(newTempStyle)
            mMapView.vtmMap.animator().animateTo(mPathLayer.points[mPathLayer.points.size - 1])
            editIndex = -1
        } else { //新增点
            if (type == MEASURE_TYPE.ANGLE && mPathLayer.points.size == 3) {
                return
            }
            val points: List<GeoPoint> = mPathLayer.points
            if (points.size > 2) {
                val list = mutableListOf<GeoPoint>()
                list.add(geoPoint)
                list.add(points[points.size - 1])
                var bCross = GeometryTools.isLineStringCrosses(points, list)
                if (bCross) {
                    Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                    return
                }
                if (measureType == MEASURE_TYPE.AREA) {
                    list.clear()
                    list.add(geoPoint)
                    list.add(points[0])
                    bCross = GeometryTools.isLineStringCrosses(points, list)
                    if (bCross) {
                        Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
            }
            mPathLayer.addPoint(geoPoint)
//            }
            val markerItem = MarkerItem(createUUID(), "", "", geoPoint)
            markerLayer.addItem(markerItem)
            mPathMakers.add(markerItem)
            markerLayer.update()
        }

        //
        when (type) {
            MEASURE_TYPE.AREA -> {
                showAreaLayer()
            }
            MEASURE_TYPE.DISTANCE -> {
                if (mPathLayer.points.size > 1) {
                    measureValueLiveData.value = calculatedDistance(mPathLayer.points)
                } else {
                    measureValueLiveData.value = MeasureValueBean(unit = "米")
                }
            }
            MEASURE_TYPE.ANGLE -> {
                if (mPathLayer.points.size == 3) {
                    val bean = calculatedAngle(mPathLayer.points)
                    measureValueLiveData.value = bean
                } else {
                    measureValueLiveData.value = MeasureValueBean(unit = "度")
                }
            }
        }
        //点击选点让地图动一下，好出发mapEvent
        if (selectPoint != null) {
            mMapView.vtmMap.animator().animateTo(selectPoint)
        }
        mPathLayer.update()
    }

    /**
     * 绘制线回退点
     */
    open fun backspacePoint() {
        if (mPathLayer.points.size > 0) {
            val list: MutableList<GeoPoint> = java.util.ArrayList<GeoPoint>(mPathLayer.points)
            val point = list[mPathLayer.points.size - 1]
            list.remove(point)
            mPathLayer.setPoints(list)
            mMapView.vtmMap.animator().animateTo(
                GeoPoint(
                    point.latitude, point.longitude
                )
            )
        }
        if (mPathMakers.size > 0) {
            val item: MarkerItem = mPathMakers[mPathMakers.size - 1]
            markerLayer.removeItem(item)
            mPathMakers.remove(item)
        }
        if (mPathMakers.size == 0) {
            mPathLayerTemp.clearPath()
        }
        editIndex = -1
        mPathLayerTemp.setStyle(newTempStyle)
        when (measureType) {
            MEASURE_TYPE.AREA -> {
                showAreaLayer()
            }
            MEASURE_TYPE.DISTANCE -> {
                if (mPathLayer.points.size > 1) {
                    measureValueLiveData.value = calculatedDistance(mPathLayer.points)
                } else {
                    val bean = MeasureValueBean(unit = "米")
                    measureValueLiveData.value = bean
                    tempMeasureValueLiveData.value = bean
                }
            }
            MEASURE_TYPE.ANGLE -> {
                val bean = MeasureValueBean(unit = "度")
                measureValueLiveData.value = bean
                tempMeasureValueLiveData.value = bean
            }
        }
    }

    /**
     * 隐藏面积计算
     */
    open fun hideAreaLayer() {
        mAreaTextLayer?.let {
            it.removeAllItems()
            removeLayer(it)
        }

    }

    /**
     * 显示面积计算
     */
    open fun showAreaLayer() {
        if (mAreaTextLayer != null) {
            mAreaTextLayer!!.removeAllItems()
            removeLayer(mAreaTextLayer!!)
        }
        if (areaPolygon != null) mPathLayer.remove(areaPolygon)

        if (mPathLayer.points.size > 2) {
            val list: MutableList<GeoPoint> = mPathLayer.points.toMutableList()
            val valueBean = calculatedArea(list)
            val textPaint = TextPaint()
            textPaint.textSize = 13 * CanvasAdapter.getScale()
            textPaint.color = Color.BLUE
            val width = ceil(
                textPaint.measureText("${valueBean.valueString}${valueBean.unit}").toDouble()
            ).toInt()
            val fontMetrics = textPaint.fontMetrics
            val height = ceil((abs(fontMetrics.bottom) + abs(fontMetrics.top)).toDouble()).toInt()
            val bitmap = android.graphics.Bitmap.createBitmap(
                width, height, android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawText(
                "${valueBean.valueString}${valueBean.unit}",
                0f,
                abs(fontMetrics.ascent),
                textPaint
            )
            val bitmap2: Bitmap = AndroidBitmap(bitmap)
            val markerSymbol = MarkerSymbol(bitmap2, MarkerSymbol.HotspotPlace.CENTER)
            mAreaTextLayer = ItemizedLayer(
                mMapView.vtmMap, java.util.ArrayList(), markerSymbol, null
            )
            addLayer(mAreaTextLayer!!, NIMapView.LAYER_GROUPS.OPERATE_MARKER)
            list.add(list[0])
            val polygon = GeometryTools.createPolygon(list)
            val point = polygon.centroid
            val item = MarkerItem(
                "${valueBean.valueString}${valueBean.unit}",
                "",
                GeoPoint(point.coordinate.y, point.coordinate.x)
            )
            mAreaTextLayer!!.removeAllItems()
            mAreaTextLayer!!.addItem(item)
            mAreaTextLayer!!.update()
            areaPolygon = PolygonDrawable(polygon, areaStyle)
            mPathLayer.add(areaPolygon)

            measureValueLiveData.postValue(valueBean)
        }

    }

    /**
     * 计算角度
     */
    private fun calculatedAngle(list: MutableList<GeoPoint>): MeasureValueBean {
        val bean = MeasureValueBean(unit = "度")
        if (list.size == 3) {
            var angle = DistanceUtil.angle(
                list[0],
                list[1],
                list[2]
            )
            if (angle > 180)
                angle = 360 - angle
            val bg = BigDecimal(angle)
            val f1 = bg.setScale(2, BigDecimal.ROUND_HALF_UP)
            bean.value = angle
            bean.valueString = f1.toString()
        }
        return bean
    }

    /**
     * 计算距离
     */
    private fun calculatedDistance(list: MutableList<GeoPoint>): MeasureValueBean {
        val bean = MeasureValueBean(unit = "米")
        if (list.size < 2) {
            return bean
        }
        val distance: Double = GeometryTools.getDistance(list)
        bean.value = distance
        try {
            if (distance < 1000) {
                val bg = BigDecimal(distance)
                val f1 = bg.setScale(2, BigDecimal.ROUND_HALF_UP)
                bean.valueString = f1.toString()
                bean.unit = "米"
            } else if (distance < 10000000) {

                val d = distance / 1000.0
                val bg = BigDecimal(d)
                val f1 = bg.setScale(2, BigDecimal.ROUND_HALF_UP)
                bean.valueString = f1.toString()
                bean.unit = "公里"

            } else {
                val d = distance / 10000000.0
                val bg = BigDecimal(d)
                val f1 = bg.setScale(2, BigDecimal.ROUND_HALF_UP)
                bean.valueString = f1.toString()
                bean.unit = "万公里"
            }
        } catch (e: Exception) {
            Log.e("jingo", e.toString() + "$distance")
        }
        return bean
    }

    /**
     * 计算面积
     */
    private fun calculatedArea(list: MutableList<GeoPoint>): MeasureValueBean {
        val bean = MeasureValueBean(unit = "平方米")

        val area = DistanceUtil.planarPolygonAreaMeters2(list)
        bean.value = area
        if (area == 0.0) {
            "0"
        } else if (area < 10000) {
            val bg = BigDecimal(area)
            val f1 = bg.setScale(2, BigDecimal.ROUND_HALF_UP)
            bean.valueString = f1.toString()
            bean.unit = "平方米"
        } else if (area < 1000000) {
            val d = area / 10000.0
            val bg = BigDecimal(d)
            val f1 = bg.setScale(2, BigDecimal.ROUND_HALF_UP)
            bean.valueString = f1.toString()
            bean.unit = "万平方米"
        } else if (area < 10000000000.0) {
            val d = area / 1000000.0
            val bg = BigDecimal(d)
            val f1 = bg.setScale(2, BigDecimal.ROUND_HALF_UP)
            bean.valueString = f1.toString()
            bean.unit = "平方公里"
        } else {
            val d = area / 10000000000.0
            val bg = BigDecimal(d)
            val f1 = bg.setScale(2, BigDecimal.ROUND_HALF_UP)
            bean.valueString = f1.toString()
            bean.unit = "万平方公里"
        }
        return bean
    }

    /**
     * 清除所有
     */
    fun clear() {
        bDrawLine = false
        editIndex = -1
        markerLayer.removeAllItems()
        markerLayer.isEnabled = false
        mPathMakers.clear()
        mPathLayer.clearPath()
        if (areaPolygon != null) {
            mPathLayer.remove(areaPolygon)
            areaPolygon = null
        }
        mPathLayer.isEnabled = false
        mPathLayerTemp.clearPath()
        mPathLayerTemp.isEnabled = false
        mPathLayerTemp.setStyle(newTempStyle)
        hideAreaLayer()
        measureValueLiveData.value = MeasureValueBean()
        tempMeasureValueLiveData.value = MeasureValueBean()
        mMapView.vtmMap.updateMap(true)
    }

    override fun onMapEvent(e: Event, mapPosition: MapPosition) {
        if (!bDrawLine) return
//        if (mMapView.centerPixel[1] > mMapView.vtmMap.height / 2) {
//            val geoPoint =
//                mMapView.vtmMap.viewport()
//                    .fromScreenPoint(
//                        mMapView.centerPixel[0],
//                        mMapView.vtmMap.height - mMapView.centerPixel[1]
//                    )
//            mapPosition.setPosition(geoPoint)
//        }
        if (e === Map.POSITION_EVENT) {
            mPathLayerTemp.clearPath()
            //测角度，3个点后不渲染引导线

            if (mPathLayer.points.size > 0) {
                if (editIndex > -1) {
                    val list = mutableListOf<GeoPoint>()
                    val tempList = mutableListOf<GeoPoint>()
                    //编辑起点
                    if (editIndex == 0) {
                        //如果现在只有一个点，显示当前点要移动到的位置
                        if (mPathLayer.points.size == 1) {
                            list.add(mPathLayer.points[0])
                            list.add(
                                GeoPoint(
                                    mapPosition.latitude, mapPosition.longitude
                                )
                            )
                        } else {
                            //当前有多个点，红线链接第二个点
                            list.add(mPathLayer.points[1])
                            list.add(
                                GeoPoint(
                                    mapPosition.latitude, mapPosition.longitude
                                )
                            )
                            if (measureType == MEASURE_TYPE.AREA) {
                                list.add(mPathLayer.points[mPathLayer.points.size - 1])
                            }
                            tempList.add(
                                GeoPoint(
                                    mapPosition.latitude, mapPosition.longitude
                                )
                            )
                            for (i in 1 until mPathLayer.points.size) {
                                tempList.add(mPathLayer.points[i])
                            }
                        }
                    } else if (editIndex == mPathLayer.points.size - 1) {
                        //如果当前编辑的是最后一个点
                        list.add(mPathLayer.points[editIndex - 1])
                        list.add(
                            GeoPoint(
                                mapPosition.latitude, mapPosition.longitude
                            )
                        )
                        for (i in 0 until mPathLayer.points.size - 1) {
                            tempList.add(mPathLayer.points[i])
                        }
                        tempList.add(
                            GeoPoint(
                                mapPosition.latitude, mapPosition.longitude
                            )
                        )
                        if (measureType == MEASURE_TYPE.AREA) {
                            list.add(mPathLayer.points[0])
                        }
                    } else {
                        list.add(mPathLayer.points[editIndex - 1])
                        list.add(
                            GeoPoint(
                                mapPosition.latitude, mapPosition.longitude
                            )
                        )
                        list.add(mPathLayer.points[editIndex + 1])
                        for (i in 0 until editIndex) {
                            tempList.add(mPathLayer.points[i])
                        }
                        tempList.add(
                            GeoPoint(
                                mapPosition.latitude, mapPosition.longitude
                            )
                        )
                        for (i in editIndex + 1 until mPathLayer.points.size) {
                            tempList.add(mPathLayer.points[i])
                        }
                    }
                    mPathLayerTemp.setPoints(list)
                    when (measureType) {
                        MEASURE_TYPE.DISTANCE -> {
                            tempMeasureValueLiveData.value = calculatedDistance(tempList)
                        }
                        MEASURE_TYPE.AREA -> {
                            tempMeasureValueLiveData.value = calculatedArea(tempList)
                        }
                        MEASURE_TYPE.ANGLE -> {
                            tempMeasureValueLiveData.value = calculatedAngle(tempList)
                        }
                    }
                } else {
                    val list: MutableList<GeoPoint> = mutableListOf()
                    list.add(mPathLayer.points[mPathLayer.points.size - 1])
                    val nowPoint = GeoPoint(
                        mapPosition.latitude, mapPosition.longitude
                    )
                    list.add(
                        nowPoint
                    )
                    when (measureType) {
                        MEASURE_TYPE.DISTANCE -> {
                            mPathLayerTemp.setPoints(list)
                            val dList = mPathLayer.points.toMutableList()
                            dList.add(nowPoint)
                            tempMeasureValueLiveData.value = calculatedDistance(dList)
                        }
                        MEASURE_TYPE.AREA -> {
                            if (mPathLayer.points.size == 1) {
                                mPathLayerTemp.setPoints(list)
                            } else if (mPathLayer.points.size > 1) {
                                list.add(mPathLayer.points[0])
                                mPathLayerTemp.setPoints(list)
                                val dList = mPathLayer.points.toMutableList()
                                dList.add(nowPoint)
                                dList.add(mPathLayer.points[0])
                                tempMeasureValueLiveData.value = calculatedArea(dList)
                            } else {
                                tempMeasureValueLiveData.value = MeasureValueBean(unit = "平方米")
                            }

                        }
                        MEASURE_TYPE.ANGLE -> {
                            mPathLayerTemp.setPoints(list)
                            val dList = mPathLayer.points.toMutableList()
                            if(dList.size < 3) {
                                dList.add(nowPoint)
                            }
                            tempMeasureValueLiveData.value = calculatedAngle(dList)
                        }
                    }
                }
            } else {
                tempMeasureValueLiveData.value = MeasureValueBean(unit = "平方米")
            }
        }
    }

    /**
     * 初始化线数据， 用来二次编辑
     */
    fun initPathLine(geometry: String) {
        bDrawLine = true
        mPathLayer.isEnabled = true
        mPathLayerTemp.isEnabled = true
        val pointList = GeometryTools.getGeoPoints(geometry)
        mPathLayer.setPoints(pointList)
        for (point in pointList) {
            val markerItem = MarkerItem(createUUID(), "", "", point)
            markerLayer.addItem(markerItem)
            mPathMakers.add(markerItem)
        }
        if (mPathLayer.points.size > 1) {
            val bean = calculatedDistance(mPathLayer.points)
            measureValueLiveData.value = bean
            tempMeasureValueLiveData.value = bean
        } else {
            measureValueLiveData.value = MeasureValueBean(unit = "米")
        }
        mMapView.updateMap(true)
    }

    private val itemGestureListener: OnItemGestureListener<MarkerInterface> =
        object : OnItemGestureListener<MarkerInterface> {
            override fun onItemSingleTapUp(index: Int, item: MarkerInterface): Boolean {
                if (bDrawLine) {
                    for (i in mPathMakers.indices) {
                        val item1 = mPathMakers[i]
                        if (item === item1) {
                            mMapView.vtmMap.animator().animateTo(
                                GeoPoint(
                                    item.getPoint().latitude, item.getPoint().longitude
                                )
                            )
                            editIndex = i
                            mPathLayerTemp.setStyle(editTempStyle)
                            val list: MutableList<GeoPoint> = mutableListOf()
                            if (editIndex == 0 || editIndex == mPathMakers.size - 1) {
                                list.add(item.getPoint())
                                list.add(item.getPoint())
                            } else {
                                list.add(mPathMakers[editIndex - 1].geoPoint)
                                list.add(item.getPoint())
                                list.add(mPathMakers[editIndex + 1].geoPoint)
                            }
                            mPathLayerTemp.setPoints(list)
                            return true
                        }
                    }
                }
                return false
            }

            override fun onItemLongPress(index: Int, item: MarkerInterface): Boolean {
                return false
            }
        }
}