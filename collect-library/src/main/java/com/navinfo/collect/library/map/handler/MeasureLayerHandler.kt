package com.navinfo.collect.library.map.maphandler

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.text.TextPaint
import android.widget.Toast
import com.navinfo.collect.library.R
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.handler.BaseHandler
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

open class MeasureLayerHandler(context: Context, mapView: NIMapView) :
    BaseHandler(context, mapView), Map.UpdateListener {

    private var editIndex: Int = -1;//线修型的时候，用来表示是不是正在修型，修的第几个点
    private val mPathMakers: MutableList<MarkerItem> = mutableListOf()
    private var bDrawLine = false
    private val bDrawPoint = false

    private var mAreaLayer: ItemizedLayer

    //绘制线 样式
    private val lineStyle: Style

    private val newTempStyle: Style

    //线型编辑时的样式
    private val editTempStyle: Style

    //新增线数据引线
    private var mPathLayerTemp: PathLayer

    //新增线数据
    private var mPathLayer: PathLayer

    //线路端点图标
    private var mPathMarkerBitmap: Bitmap

    private var bDrawPolygon = false

    private var mPolygonLayer: NIPolygonLayer

    init {

        //新增线数据图层和线样式
        lineStyle = Style.builder().scaleZoomLevel(20).buffer(1.0)
            .stippleColor(context.resources.getColor(R.color.draw_line_blue1_color, null))
            .strokeWidth(4f)
            .fillColor(context.resources.getColor(R.color.draw_line_blue2_color, null))
            .fillAlpha(0.5f)
            .strokeColor(context.resources.getColor(R.color.draw_line_blue2_color, null))
            .fillColor(context.resources.getColor(R.color.draw_line_red_color, null))
            .stippleWidth(4f)
            .fixed(true)
            .build()

        newTempStyle = Style.builder()
            .stippleColor(context.resources.getColor(R.color.transparent, null))
            .stipple(30)
            .stippleWidth(30f)
            .strokeWidth(4f)
            .strokeColor(context.resources.getColor(R.color.draw_line_blue2_color, null))
            .fixed(true)
            .randomOffset(false)
            .build()

        editTempStyle = Style.builder()
            .stippleColor(context.resources.getColor(R.color.transparent, null))
            .stipple(30)
            .stippleWidth(30f)
            .strokeWidth(8f)
            .strokeColor(context.resources.getColor(R.color.draw_line_red_color, null))
            .fixed(true)
            .randomOffset(false)
            .build()

        mPolygonLayer = NIPolygonLayer(
            mMapView.vtmMap,
            lineStyle
        )
//        addLayer(mPolygonLayer, NIMapView.LAYER_GROUPS.OPERATE)

        mPathLayerTemp = PathLayer(mMapView.vtmMap, newTempStyle)
        //        addLayer(mPathLayerTemp, NIMapView.LAYER_GROUPS.OPERATE)

        mPathMarkerBitmap = AndroidBitmap(
            BitmapFactory.decodeResource(
                context.resources,
                R.mipmap.icon_path_maker
            )
        )

        val markerSymbol = MarkerSymbol(mPathMarkerBitmap, MarkerSymbol.HotspotPlace.CENTER)

        mAreaLayer = ItemizedLayer(mMapView.vtmMap, ArrayList(), markerSymbol, null)

        mPathLayer = PathLayer(mMapView.vtmMap, lineStyle)

        addLayer(mPathLayer, NIMapView.LAYER_GROUPS.OPERATE)

    }

    open fun drawLineOrPolygon(type: Int) {
        bDrawLine = true

//        //画面
//        if (type == 3) {
//            if (mPolygonLayer == null) {
//                mPolygonLayer = NIPolygonLayer(mMapView.vtmMap, lineStyle)
//                addLayer(mPolygonLayer, NIMapView.LAYER_GROUPS.OPERATE)
//            } else if (!mPolygonLayer.isEnabled) {
//                mPolygonLayer.isEnabled = true
//            }
//        } else {
//            if (mPathLayer == null) {
//                mPathLayer = PathLayer(mMapView.vtmMap, lineStyle)
//                addLayer(mPathLayer, NIMapView.LAYER_GROUPS.OPERATE)
//            } else if (!mPathLayer.isEnabled()) {
//                mPathLayer.setEnabled(true)
//            }
//        }
//        //上一个点的引线
//        if (mPathLayerTemp == null) {
//            mPathLayerTemp = PathLayer(mMapView.vtmMap, newTempStyle)
//            addLayer(mPathLayerTemp, NIMapView.LAYER_GROUPS.OPERATE)
//        } else if (!mPathLayerTemp.isEnabled) {
//            mPathLayerTemp.isEnabled = true
//        }
//        val geoPoint: GeoPoint =
//            GeoPoint(
//                mMapView.vtmMap.getMapPosition().getLatitude(),
//                mMapView.vtmMap.getMapPosition().getLongitude()
//            )
//
//        //编辑点
//        if (editIndex > -1) {
//            if (mPathMakers.size > editIndex) {
//                mMapView.layerManager.removeMarker(
//                    mPathMakers[editIndex],
//                    NILayerManager.MARQUEE_MARKER_LAYER
//                )
//                mPathMakers.removeAt(editIndex)
//                if (mPathMarkerBitmap == null) {
//                    mPathMarkerBitmap = AndroidBitmap(
//                        BitmapFactory.decodeResource(
//                            mContext.getResources(),
//                            R.mipmap.icon_path_maker
//                        )
//                    )
//                }
//                val markerItem = MarkerItem(createUUID(), "", "", geoPoint)
//                val markerSymbol = MarkerSymbol(mPathMarkerBitmap, MarkerSymbol.HotspotPlace.CENTER)
//                markerItem.marker = markerSymbol
//                mMapView.layerManager.addMarker2MarkerLayer(
//                    markerItem,
//                    mPathMarkerBitmap,
//                    NILayerManager.MARQUEE_MARKER_LAYER,
//                    NIMapView.LAYER_GROUPS.OTHER.ordinal,
//                    itemGestureListener
//                )
//                mPathMakers.add(editIndex, markerItem)
//                if (mPathLayer != null && mPathLayer.getPoints().size > 0) {
//                    val list: MutableList<GeoPoint> = mPathLayer.getPoints()
//                    if (editIndex < list.size) {
//                        list.removeAt(editIndex)
//                        val list2: MutableList<GeoPoint> = java.util.ArrayList(list)
//                        list2.add(editIndex, geoPoint)
//                        mPathLayer.setPoints(list2)
//                    }
//                } else if (mPolygonLayer != null && mPolygonLayer.points.size > 0) {
//                    val list = mPolygonLayer.points
//                    if (editIndex < list.size) {
//                        list.removeAt(editIndex)
//                        val list2: MutableList<GeoPoint> = java.util.ArrayList(list)
//                        list2.add(editIndex, geoPoint)
//                        mPolygonLayer.setPoints(list2)
//                    }
//                }
//                if (mPathLayerTemp != null) {
//                    mPathLayerTemp.setStyle(newTempStyle)
//                    val list: MutableList<GeoPoint> = java.util.ArrayList<GeoPoint>()
//                    if (type == 3 && mPathMakers.size > 1) {
//                        list.add(mPathMakers[0].geoPoint)
//                        list.add(geoPoint)
//                        list.add(mPathMakers[mPathMakers.size - 1].geoPoint)
//                    } else {
//                        list.add(mPathMakers[mPathMakers.size - 1].geoPoint)
//                        list.add(geoPoint)
//                    }
//                    mPathLayerTemp.setPoints(list)
//                }
//            }
//            editIndex = -1
//        } else { //新增点
//            if (type == 3) {
//                val points: MutableList<GeoPoint> = java.util.ArrayList(mPolygonLayer.points)
//                if (points.size > 2) {
//                    val list: MutableList<GeoPoint> = java.util.ArrayList()
//                    points.add(points[0])
//                    list.add(points[0])
//                    list.add(geoPoint)
//                    list.add(mPolygonLayer.points[mPolygonLayer.points.size - 1])
//                    val bCross = GeometryTools.isPolygonCrosses(points, list)
//                    if (bCross == true) {
//                        Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
//                        return
//                    }
//                }
//                mPolygonLayer.addPoint(geoPoint)
//            } else {
//                val points: List<GeoPoint> = mPathLayer.getPoints()
//                if (points.size > 2) {
//                    val list: MutableList<GeoPoint> = java.util.ArrayList()
//                    list.add(geoPoint)
//                    list.add(points[points.size - 1])
//                    val bCross = GeometryTools.isLineStringCrosses(points, list)
//                    if (bCross == true) {
//                        Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
//                        return
//                    }
//                }
//                mPathLayer.addPoint(geoPoint)
//            }
//            if (mPathMarkerBitmap == null) {
//                mPathMarkerBitmap = AndroidBitmap(
//                    BitmapFactory.decodeResource(
//                        mContext.getResources(),
//                        R.mipmap.icon_path_maker
//                    )
//                )
//            }
//            val markerItem = MarkerItem(createUUID(), "", "", geoPoint)
//            val markerSymbol = MarkerSymbol(mPathMarkerBitmap, MarkerSymbol.HotspotPlace.CENTER)
//            markerItem.marker = markerSymbol
//            mMapView.layerManager.addMarker2MarkerLayer(
//                markerItem,
//                mPathMarkerBitmap,
//                NILayerManager.MARQUEE_MARKER_LAYER,
//                NIMapView.LAYER_GROUPS.OTHER.ordinal,
//                itemGestureListener
//            )
//            mPathMakers.add(markerItem)
//        }
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
        if (mAreaLayer != null) {
            mAreaLayer.removeAllItems()
            mMapView.vtmMap.layers().remove(mAreaLayer)
        }
    }


    /**
     * 显示面积计算
     */
    open fun showAreaLayer() {
        if (mAreaLayer != null) {
            mAreaLayer.removeAllItems()
            mMapView.vtmMap.layers().remove(mAreaLayer)
        }
        if (mPolygonLayer != null && mPolygonLayer.points.size > 2) {
            val list: MutableList<GeoPoint> = ArrayList(mPolygonLayer.points)
            val area = DistanceUtil.planarPolygonAreaMeters2(list)
            var areaString: String
            if (area < 1000000) {
                areaString = area.toString() + "平方米"
            } else if (area < 10000000000.0) {
                val d = area / 1000000.0
                val bg = BigDecimal(d)
                val f1 = bg.setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
                areaString = f1.toString() + "平方公里"
            } else {
                val d = area / 10000000000.0
                val bg = BigDecimal(d)
                val f1 = bg.setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
                areaString = f1.toString() + "万平方公里"
            }
            val textPaint = TextPaint()
            textPaint.textSize = 13 * CanvasAdapter.getScale()
            textPaint.color = Color.BLUE
            val width = Math.ceil(textPaint.measureText(areaString).toDouble()).toInt()
            val fontMetrics = textPaint.fontMetrics
            val height =
                Math.ceil((Math.abs(fontMetrics.bottom) + Math.abs(fontMetrics.top)).toDouble())
                    .toInt()
            val bitmap = android.graphics.Bitmap.createBitmap(
                width,
                height,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawText(areaString, 0f, Math.abs(fontMetrics.ascent), textPaint)
            val bitmap2: Bitmap = AndroidBitmap(bitmap)
            val markerSymbol = MarkerSymbol(bitmap2, MarkerSymbol.HotspotPlace.CENTER)
            mAreaLayer = ItemizedLayer(mMapView.vtmMap, ArrayList(), markerSymbol, null)
            mMapView.vtmMap.layers().add(mAreaLayer)
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
//        bDrawLine = false
//        editIndex = -1
//        for (item in mPathMakers) {
//            mMapView.layerManager.removeMarker(item, NILayerManager.MARQUEE_MARKER_LAYER)
//        }
//        mPathMakers.clear()
//        if (mPathLayer != null) {
//            mPathLayer.clearPath()
//            mPathLayer.isEnabled = false
//        }
//        if (mPolygonLayer != null) {
//            mPolygonLayer.clearPath()
//            mPolygonLayer.isEnabled = false
//        }
//        if (mPathLayerTemp != null) {
//            mPathLayerTemp.clearPath()
//            mPathLayerTemp.isEnabled = false
//            mPathLayerTemp.setStyle(newTempStyle)
//        }
        hideAreaLayer()
    }

    fun clean() {
        removeLine()
        mMapView.vtmMap.updateMap(true)
    }

    override fun onMapEvent(e: Event?, mapPosition: MapPosition?) {

    }

    private val itemGestureListener: OnItemGestureListener<*> =
        object : OnItemGestureListener<MarkerInterface> {
            override fun onItemSingleTapUp(index: Int, item: MarkerInterface): Boolean {
                if (bDrawLine) {
//                    for (i in mPathMakers.indices) {
//                        val item1 = mPathMakers[i]
//                        if (item === item1) {
//                            mMapView.layerManager.jumpToPosition(
//                                item.getPoint().longitude,
//                                item.getPoint().latitude,
//                                0
//                            )
//                            editIndex = i
//                            if (mPathLayerTemp != null) {
//                                mPathLayerTemp.setStyle(editTempStyle)
//                                val list: MutableList<GeoPoint> = java.util.ArrayList<GeoPoint>()
//                                if (editIndex == 0 || editIndex == mPathMakers.size - 1) {
//                                    list.add(item.geoPoint as Nothing)
//                                    list.add(item.geoPoint as Nothing)
//                                } else {
//                                    list.add(mPathMakers[editIndex - 1].geoPoint as Nothing)
//                                    list.add(item.geoPoint as Nothing)
//                                    list.add(mPathMakers[editIndex + 1].geoPoint as Nothing)
//                                }
//                                mPathLayerTemp.setPoints(list)
//                            }
//                            return true
//                        }
//                    }
                }
                return false
            }

            override fun onItemLongPress(index: Int, item: MarkerInterface): Boolean {
                return false
            }
        }
}