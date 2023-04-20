package com.navinfo.collect.library.map.handler

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.navinfo.collect.library.R
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.layers.NIPolygonLayer
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.StringUtil
import org.oscim.android.canvas.AndroidBitmap
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

open class PolygonHandler(context: AppCompatActivity, mapView: NIMapView) :
    BaseHandler(context, mapView), Map.UpdateListener {

    private var editIndex: Int = -1;
    private val mPathMakers: MutableList<MarkerItem> = mutableListOf()

    //绘制线 引导线样式
    private val newTempStyle: Style

    //绘制线 样式
    private val lineStyle: Style

    //线型编辑时的样式
    private val editTempStyle: Style

    //新增线数据引线
    private lateinit var mPathLayerTemp: PathLayer

    //线路端点图标
    private var mPathMarkerBitmap: Bitmap

    //线路端点marker
    private lateinit var mEndpointLayer: ItemizedLayer

    private var bDrawPolygon = false

    private var mPolygonLayer: NIPolygonLayer

    init {
        mMapView.vtmMap.events.bind(this)

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
//        mMapView.layerManager.addLayer(
//            "defaultPolygonLayer",
//            mPolygonLayer,
//            NIMapView.LAYER_GROUPS.VECTOR.ordinal
//        )
//
//        mPathLayerTemp = if (mMapView.layerManager.containsLayer("guideLineLayer")) {
//            mMapView.layerManager.getLayer("guideLineLayer") as PathLayer
//        } else {
//            PathLayer(mMapView.vtmMap, newTempStyle)
//        }
//        mMapView.layerManager.addLayer(
//            "guideLineLayer",
//            mPathLayerTemp,
//            NIMapView.LAYER_GROUPS.VECTOR.ordinal
//        )

        mPathMarkerBitmap = AndroidBitmap(
            BitmapFactory.decodeResource(
                mContext.resources,
                R.mipmap.icon_path_maker
            )
        )

        val markerSymbol = MarkerSymbol(mPathMarkerBitmap, MarkerSymbol.HotspotPlace.CENTER)

//        mEndpointLayer = if (mMapView.layerManager.containsLayer("endpointLayer")) {
//            mMapView.layerManager.getLayer("endpointLayer") as ItemizedLayer
//        } else
//        //新增marker图层
//            ItemizedLayer(
//                mMapView.vtmMap,
//                java.util.ArrayList<MarkerInterface>(),
//                markerSymbol,
//                null
//            )
//        mMapView.layerManager.addLayer(
//            "endpointLayer",
//            mEndpointLayer,
//            NIMapView.LAYER_GROUPS.VECTOR.ordinal
//        )

//        mEndpointLayer.setOnItemGestureListener(object : OnItemGestureListener<MarkerInterface> {
//            override fun onItemSingleTapUp(index: Int, item: MarkerInterface): Boolean {
//                if (bDrawPolygon) {
//                    for (i in mPathMakers.indices) {
//                        val item1 = mPathMakers[i]
//                        if (item === item1) {
//                            mMapView.vtmMap.animator().animateTo(
//                                GeoPoint(
//                                    item.getPoint().latitude,
//                                    item.getPoint().longitude
//                                )
//                            )
//                            editIndex = i
//                            mPathLayerTemp.setStyle(editTempStyle)
//                            val list: MutableList<GeoPoint> = mutableListOf()
//                            if (editIndex == 0 || editIndex == mPathMakers.size - 1) {
//                                list.add(item.getPoint())
//                                list.add(item.getPoint())
//                            } else {
//                                list.add(mPathMakers[editIndex - 1].geoPoint)
//                                list.add(item.getPoint())
//                                list.add(mPathMakers[editIndex + 1].geoPoint)
//                            }
//                            mPathLayerTemp.setPoints(list)
//                            return false
//                        }
//                    }
//                }
//                return false
//            }
//
//            override fun onItemLongPress(index: Int, item: MarkerInterface): Boolean {
//                return false
//            }
//        })
    }

    fun addDrawPolygonPoint(geoPoint: GeoPoint): List<GeoPoint> {
        if (!bDrawPolygon) {
            mPolygonLayer.isEnabled = true
            mPathLayerTemp.isEnabled = true
            mEndpointLayer.isEnabled = true
            bDrawPolygon = true
        }
        //编辑点
        if (editIndex > -1) {
            if (mPolygonLayer.points.size > 0) {
                val list: MutableList<GeoPoint> = mutableListOf()
                list.addAll(mPolygonLayer.points)
                if (list.size > 3) {
                    val newList: MutableList<GeoPoint> = mutableListOf()
                    if (editIndex == 0) {
                        newList.add(list[list.size - 1])
                        newList.add(geoPoint)
                        newList.add(list[editIndex + 1])
                    } else if (editIndex == list.size - 1) {
                        newList.add(list[0])
                        newList.add(geoPoint)
                        newList.add(list[editIndex - 1])
                    } else {
                        newList.add(list[editIndex - 1])
                        newList.add(geoPoint)
                        newList.add(list[editIndex + 1])
                    }
                    val newList2: MutableList<GeoPoint> = mutableListOf()
                    for (i in editIndex + 1 until list.size) {
                        newList2.add(list[i])
                    }
                    for (i in 0 until editIndex) {
                        newList2.add(list[i])
                    }
                    if (GeometryTools.isLineStringCrosses(newList, newList2)) {
                        Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                        return mPolygonLayer.points
                    }
                }
                if (editIndex < list.size) {
                    list.removeAt(editIndex)
                    val list2: MutableList<GeoPoint> = mutableListOf()
                    list2.addAll(list)
                    list2.add(editIndex, geoPoint)
                    mPolygonLayer.setPoints(list2)
                }
            }
            if (mPathMakers.size > editIndex) {
                mEndpointLayer.removeItem(mPathMakers[editIndex])
                mPathMakers.removeAt(editIndex)
                val markerItem = MarkerItem(StringUtil.createUUID(), "", "", geoPoint)
                mEndpointLayer.addItem(markerItem)
                mPathMakers.add(editIndex, markerItem)
                mPathLayerTemp.setStyle(newTempStyle)
                val list: MutableList<GeoPoint> = mutableListOf()
                if (mPathMakers.size > 1) {
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
            val points: MutableList<GeoPoint> = mutableListOf()
            points.addAll(mPolygonLayer.points)
            if (points.size > 2) {
                val list: MutableList<GeoPoint> = mutableListOf()
                points.add(points[0])
                list.add(points[0])
                list.add(geoPoint)
                list.add(mPolygonLayer.points[mPolygonLayer.points.size - 1])
                if (GeometryTools.isPolygonCrosses(points, list)) {
                    Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                    return mPolygonLayer.points
                }
            }
            mPolygonLayer.addPoint(geoPoint)
            val markerItem = MarkerItem(StringUtil.createUUID(), "", "", geoPoint)
            mEndpointLayer.addItem(markerItem)
            mPathMakers.add(markerItem)
        }
        return mPolygonLayer.points
    }

    fun addDrawPolygon(list: List<GeoPoint>) {
        for (item in list) {
            addDrawPolygonPoint(item)
        }
    }

    override fun onMapEvent(e: Event, mapPosition: MapPosition) {
        if (!bDrawPolygon)
            return
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
            if (mPolygonLayer.points.size > 0) {
                if (editIndex > -1) {
                    val list: MutableList<GeoPoint> =
                        mutableListOf()
                    if (editIndex == 0 || editIndex == mPathMakers.size - 1) {
                        list.add(mPathMakers[editIndex].geoPoint)
                        list.add(
                            GeoPoint(
                                mapPosition.latitude,
                                mapPosition.longitude
                            )
                        )
                    } else {
                        list.add(mPathMakers[editIndex - 1].geoPoint)
                        list.add(
                            GeoPoint(
                                mapPosition.latitude,
                                mapPosition.longitude
                            )
                        )
                        list.add(mPathMakers[editIndex + 1].geoPoint)
                    }
                    mPathLayerTemp.setPoints(list)
//                    crossText.setText("")
                } else {
                    val list: MutableList<GeoPoint> =
                        mutableListOf()
                    if (mPolygonLayer.points.size > 1) {
                        list.add(mPolygonLayer.points[0])
                        list.add(
                            GeoPoint(
                                mapPosition.latitude,
                                mapPosition.longitude
                            )
                        )
                        list.add(
                            mPolygonLayer.points[mPolygonLayer.points.size - 1]
                        )
                    } else {
                        list.add(
                            mPolygonLayer.points[mPolygonLayer.points.size - 1]
                        )
                        list.add(
                            GeoPoint(
                                mapPosition.latitude,
                                mapPosition.longitude
                            )
                        )
                    }
                    mPathLayerTemp.setPoints(list)
                    if (mPolygonLayer.points.size > 1) {
                        val list1: MutableList<GeoPoint> =
                            mutableListOf()
                        list1.addAll(mPolygonLayer.points)
                        list1.add(
                            GeoPoint(
                                mapPosition.latitude,
                                mapPosition.longitude
                            )
                        )
                        list1.add(mPolygonLayer.points[0])
////                        val area: Double =
////                            GeometryTools.PlanarPolygonAreaMeters2(list1)
////                        if (area < 1000000) crossText.setText(
////                            area as Int.toString
////                            () + "平方米"
////                        ) else if (area < 10000000000.0) {
////                            val d = area / 1000000.0
////                            val bg = BigDecimal(d)
////                            val f1 =
////                                bg.setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
////                            crossText.setText(f1.toString() + "平方公里")
////                        } else {
////                            val d = area / 10000000000.0
////                            val bg = BigDecimal(d)
////                            val f1 =
////                                bg.setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
////                            crossText.setText(f1.toString() + "万平方公里")
////                        }
//                    } else {
////                        crossText.setText("")
//                    }
                    }
                }
            }
        }
    }

    fun clean() {
        mPolygonLayer.clearPath()
        mPolygonLayer.isEnabled = false
        mPathLayerTemp.clearPath()
        mPathLayerTemp.isEnabled = false
        mEndpointLayer.removeAllItems()
        mEndpointLayer.isEnabled = false
        mPathMakers.clear()
        editIndex = -1
        bDrawPolygon = false
    }
}