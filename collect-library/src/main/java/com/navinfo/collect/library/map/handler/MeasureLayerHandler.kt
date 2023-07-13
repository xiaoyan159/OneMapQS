package com.navinfo.collect.library.map.handler

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.navinfo.collect.library.R
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.layers.NIPolygonLayer
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
import org.oscim.layers.vector.geometries.Style
import org.oscim.map.Map
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.ceil

@RequiresApi(Build.VERSION_CODES.M)
open class MeasureLayerHandler(context: AppCompatActivity, mapView: NIMapView) :
    BaseHandler(context, mapView), Map.UpdateListener {

    private var editIndex: Int = -1;//线修型的时候，用来表示是不是正在修型，修的第几个点
    private val mPathMakers: MutableList<MarkerItem> = mutableListOf()
    private var bDrawLine = false
    private val bDrawPoint = false

    /**
     * 加上虚线的总长度
     */
    val tempLineDistanceLiveData = MutableLiveData("")

    /**
     * 实际绘制的总长度
     */
    val lineLenghtLiveData = MutableLiveData<Double>(0.000)

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

    private val mAreaLayer: ItemizedLayer by lazy {
        val markerSymbol = MarkerSymbol(mPathMarkerBitmap, MarkerSymbol.HotspotPlace.CENTER)
        ItemizedLayer(mMapView.vtmMap, ArrayList(), markerSymbol, null)
    }

    //绘制线 样式
    private val lineStyle: Style by lazy {
        //新增线数据图层和线样式
        Style.builder().scaleZoomLevel(20).buffer(1.0)
            .stippleColor(context.resources.getColor(R.color.draw_line_blue1_color, null))
            .strokeWidth(4f)
            .fillColor(context.resources.getColor(R.color.draw_line_blue2_color, null))
            .fillAlpha(0.5f)
            .strokeColor(context.resources.getColor(R.color.draw_line_blue2_color, null))
            .fillColor(context.resources.getColor(R.color.draw_line_red_color, null))
            .stippleWidth(4f).fixed(true).build()
    }

    private val newTempStyle: Style by lazy {
        Style.builder().stippleColor(context.resources.getColor(R.color.transparent, null))
            .stipple(30).stippleWidth(30f).strokeWidth(4f)
            .strokeColor(context.resources.getColor(R.color.draw_line_blue2_color, null))
            .fixed(true).randomOffset(false).build()
    }

    //线型编辑时的样式
    private val editTempStyle: Style by lazy {
        Style.builder().stippleColor(context.resources.getColor(R.color.transparent, null))
            .stipple(30).stippleWidth(30f).strokeWidth(8f)
            .strokeColor(context.resources.getColor(R.color.draw_line_red_color, null)).fixed(true)
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

    private var bDrawPolygon = false

    val mPolygonLayer: NIPolygonLayer by lazy {
        val layer = NIPolygonLayer(mMapView.vtmMap, lineStyle)
        addLayer(layer, NIMapView.LAYER_GROUPS.OPERATE_LINE)
        layer
    }


    init {
        mMapView.vtmMap.events.bind(this)
    }

    open fun drawLineOrPolygon(isPolygon: Boolean) {
        bDrawLine = true

        //画面
        if (isPolygon && !mPolygonLayer.isEnabled) {
            mPolygonLayer.isEnabled = true
        } else if (!mPathLayer.isEnabled) {
            mPathLayer.isEnabled = true
        }
        //上一个点的引线
        if (!mPathLayerTemp.isEnabled) {
            mPathLayerTemp.isEnabled = true
        }
        val geoPoint = GeoPoint(
            mMapView.vtmMap.mapPosition.latitude, mMapView.vtmMap.mapPosition.longitude
        )

        //编辑点
        if (editIndex > -1) {
            if (mPathMakers.size > editIndex) {
                markerLayer.removeItem(editIndex)
                mPathMakers.removeAt(editIndex)
                val markerItem = MarkerItem(createUUID(), "", "", geoPoint)
                markerLayer.addItem(markerItem)
                mPathMakers.add(editIndex, markerItem)
                if (mPathLayer.points.size > 0) {
                    val list: MutableList<GeoPoint> = mPathLayer.points
                    if (editIndex < list.size) {
                        list.removeAt(editIndex)
                        val list2: MutableList<GeoPoint> = java.util.ArrayList(list)
                        list2.add(editIndex, geoPoint)
                        mPathLayer.setPoints(list2)
                    }
                } else if (mPolygonLayer.points.size > 0) {
                    val list = mPolygonLayer.points
                    if (editIndex < list.size) {
                        list.removeAt(editIndex)
                        val list2: MutableList<GeoPoint> = java.util.ArrayList(list)
                        list2.add(editIndex, geoPoint)
                        mPolygonLayer.setPoints(list2)
                    }
                }
                mPathLayerTemp.setStyle(newTempStyle)
                val list: MutableList<GeoPoint> = java.util.ArrayList<GeoPoint>()
                if (isPolygon && mPathMakers.size > 1) {
                    list.add(mPathMakers[0].geoPoint)
                    list.add(geoPoint)
                    list.add(mPathMakers[mPathMakers.size - 1].geoPoint)
                } else {
                    list.add(mPathMakers[mPathMakers.size - 1].geoPoint)
                    list.add(geoPoint)
                }
                mPathLayerTemp.setPoints(list)
            }
            editIndex = -1
        } else { //新增点
            if (isPolygon) {
                val points: MutableList<GeoPoint> = java.util.ArrayList(mPolygonLayer.points)
                if (points.size > 2) {
                    val list: MutableList<GeoPoint> = java.util.ArrayList()
                    points.add(points[0])
                    list.add(points[0])
                    list.add(geoPoint)
                    list.add(mPolygonLayer.points[mPolygonLayer.points.size - 1])
                    val bCross = GeometryTools.isPolygonCrosses(points, list)
                    if (bCross) {
                        Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                mPolygonLayer.addPoint(geoPoint)
            } else {
                val points: List<GeoPoint> = mPathLayer.points
                if (points.size > 2) {
                    val list: MutableList<GeoPoint> = java.util.ArrayList()
                    list.add(geoPoint)
                    list.add(points[points.size - 1])
                    val bCross = GeometryTools.isLineStringCrosses(points, list)
                    if (bCross) {
                        Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                mPathLayer.addPoint(geoPoint)
                if (mPathLayer.points.size > 1) {
                    val distance: Double = GeometryTools.getDistance(mPathLayer.points)
                    val bg = BigDecimal(distance)
                    val f1 = bg.setScale(3, BigDecimal.ROUND_HALF_UP).toDouble()
                    lineLenghtLiveData.value = f1
                }
            }
            val markerItem = MarkerItem(createUUID(), "", "", geoPoint)
            markerLayer.addItem(markerItem)
            mPathMakers.add(markerItem)
        }
        showAreaLayer()
    }

    open fun drawLineBackspace() {
//        if (mPathLayer != null && mPathLayer.getPoints().size > 0) {
//            val list: MutableList<GeoPoint> = java.util.ArrayList<GeoPoint>(mPathLayer.getPoints())
//            val point = list[mPathLayer.getPoints().size - 1]
//            list.remove(point)
//            mPathLayer.setPoints(list)
//            mMapView.layerManager.jumpToPosition(point.longitude, point.latitude, 0)
//        } else if (mPolygonLayer != null && mPolygonLayer.points.size > 0) {
//            val list: MutableList<GeoPoint> = java.util.ArrayList<GeoPoint>(mPolygonLayer.points)
//            val point = list[mPolygonLayer.points.size - 1]
//            list.remove(point)
//            mPolygonLayer.setPoints(list)
//            mMapView.layerManager.jumpToPosition(point!!.longitude, point.latitude, 0)
//        }
//        if (mPathMakers.size > 0) {
//            var item: MarkerItem? = mPathMakers[mPathMakers.size - 1]
//            mMapView.layerManager.removeMarker(item, NILayerManager.MARQUEE_MARKER_LAYER)
//            mPathMakers.remove(item)
//            item = null
//        }
//        if (mPathMakers.size == 0 && mPathLayerTemp != null) {
//            mPathLayerTemp.clearPath()
//        }
//        editIndex = -1
//        if (mPathLayerTemp != null) {
//            mPathLayerTemp.setStyle(newTempStyle)
//        }
    }


    /**
     * 隐藏面积计算
     */
    open fun hideAreaLayer() {
        mAreaLayer.removeAllItems()
        mMapView.vtmMap.layers().remove(mAreaLayer)
    }


    /**
     * 显示面积计算
     */
    open fun showAreaLayer() {
        mAreaLayer.removeAllItems()
        mMapView.vtmMap.layers().remove(mAreaLayer)
        if (mPolygonLayer.points.size > 2) {
            val list: MutableList<GeoPoint> = ArrayList(mPolygonLayer.points)
            val area = DistanceUtil.planarPolygonAreaMeters2(list)
            val areaString = if (area < 1000000) {
                area.toString() + "平方米"
            } else if (area < 10000000000.0) {
                val d = area / 1000000.0
                val bg = BigDecimal(d)
                val f1 = bg.setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
                f1.toString() + "平方公里"
            } else {
                val d = area / 10000000000.0
                val bg = BigDecimal(d)
                val f1 = bg.setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
                f1.toString() + "万平方公里"
            }
            val textPaint = TextPaint()
            textPaint.textSize = 13 * CanvasAdapter.getScale()
            textPaint.color = Color.BLUE
            val width = ceil(textPaint.measureText(areaString).toDouble()).toInt()
            val fontMetrics = textPaint.fontMetrics
            val height = ceil((abs(fontMetrics.bottom) + abs(fontMetrics.top)).toDouble()).toInt()
            val bitmap = android.graphics.Bitmap.createBitmap(
                width, height, android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawText(areaString, 0f, abs(fontMetrics.ascent), textPaint)
//            val bitmap2: Bitmap = AndroidBitmap(bitmap)
//            val markerSymbol = MarkerSymbol(bitmap2, MarkerSymbol.HotspotPlace.CENTER)
//            mAreaLayer = ItemizedLayer(mMapView.vtmMap, ArrayList(), markerSymbol, null)
//            mMapView.vtmMap.layers().add(mAreaLayer)
            list.add(list[0])
            val polygon = GeometryTools.createPolygon(list)
            val point = polygon.centroid
            val item = MarkerItem(areaString, "", GeoPoint(point.coordinate.y, point.coordinate.x))
            mAreaLayer.removeAllItems()
            mAreaLayer.addItem(item)
            mAreaLayer.update()
        }
    }

    open fun removeLine() {
        bDrawLine = false
        editIndex = -1
        markerLayer.removeAllItems()
        mPathMakers.clear()
        mPathLayer.clearPath()
        mPathLayer.isEnabled = false
        mPolygonLayer.clearPath()
        mPolygonLayer.isEnabled = false
        mPathLayerTemp.clearPath()
        mPathLayerTemp.isEnabled = false
        mPathLayerTemp.setStyle(newTempStyle)
        hideAreaLayer()
    }

    fun clear() {
        removeLine()
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
            if (mPathLayer.points.size > 0) {
                if (editIndex > -1) {
                    val list: MutableList<GeoPoint> = mutableListOf()
                    if (editIndex == 0 || editIndex == mPathMakers.size - 1) {
                        list.add(mPathMakers[editIndex].geoPoint)
                        list.add(
                            GeoPoint(
                                mapPosition.latitude, mapPosition.longitude
                            )
                        )
                    } else {
                        list.add(mPathMakers[editIndex - 1].geoPoint)
                        list.add(
                            GeoPoint(
                                mapPosition.latitude, mapPosition.longitude
                            )
                        )
                        list.add(mPathMakers[editIndex + 1].geoPoint)
                    }
                    mPathLayerTemp.setPoints(list)
//                    crossText.setText("")
                } else {
                    val list: MutableList<GeoPoint> = mutableListOf()
                    list.add(mPathLayer.points[mPathLayer.points.size - 1])
                    val nowPoint = GeoPoint(
                        mapPosition.latitude, mapPosition.longitude
                    )
                    list.add(
                        nowPoint
                    )
                    mPathLayerTemp.setPoints(list)
                    if (mPathLayer.points.size > 0) {
                        val dList = mPathLayer.points.toMutableList()
                        dList.add(nowPoint)
                        val distance: Double =
                            GeometryTools.getDistance(dList)
                        if (distance < 1000) {
                            tempLineDistanceLiveData.value = "${distance.toInt()}米"
                        } else {
                            try {
                                val d = distance / 1000.0
                                val bg = BigDecimal(d)
                                val f1 =
                                    bg.setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
                                tempLineDistanceLiveData.value = "${f1}公里"
                            } catch (e: Exception) {
                                Log.e("jingo",e.toString() + "$distance")
                            }
                        }
                    }
                }
            }
        }
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