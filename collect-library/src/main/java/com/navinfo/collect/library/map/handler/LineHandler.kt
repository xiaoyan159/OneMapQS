package com.navinfo.collect.library.map.handler

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import com.navinfo.collect.library.R
import com.navinfo.collect.library.map.NIMapView
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

open class LineHandler(context: Context, mapView: NIMapView) :
    BaseHandler(context, mapView), Map.UpdateListener {

    private var editIndex: Int = -1;
    private val mPathMakers: MutableList<MarkerItem> = mutableListOf()

    //绘制线 引导线样式
    private val newTempStyle: Style

    //绘制线 样式
    private val lineStyle: Style

    //线型编辑时的样式
    private val editTempStyle: Style

    //新增线数据
    private val mPathLayer: PathLayer

    //新增线数据引线
    private val mPathLayerTemp: PathLayer

    //线路端点图标
    private val mPathMarkerBitmap: Bitmap

    //线路端点marker
    private val mEndpointLayer: ItemizedLayer

    private var bDrawLine = false

    init {
        mMapView.vtmMap.events.bind(this)

        //新增线数据图层和线样式
        lineStyle = Style.builder()
            .stippleColor(context.resources.getColor(R.color.draw_line_blue1_color, null))
            .strokeWidth(4f)
            .fillColor(context.resources.getColor(R.color.draw_line_blue2_color, null))
            .fillAlpha(0.5f)
            .strokeColor(context.resources.getColor(R.color.draw_line_blue2_color, null))
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

        mPathLayer = PathLayer(mMapView.vtmMap, lineStyle)
//        addLayer(mPathLayer, NIMapView.LAYER_GROUPS.OPERATE)

        mPathLayerTemp = PathLayer(mMapView.vtmMap, newTempStyle)
        //        addLayer(mPathLayerTemp, NIMapView.LAYER_GROUPS.OPERATE)

        mPathMarkerBitmap = AndroidBitmap(
            BitmapFactory.decodeResource(
                mContext.resources,
                R.mipmap.icon_path_maker
            )
        )
        val markerSymbol = MarkerSymbol(mPathMarkerBitmap, MarkerSymbol.HotspotPlace.CENTER)
        //新增marker图层
        mEndpointLayer = ItemizedLayer(
            mMapView.vtmMap, ArrayList<MarkerInterface>(), markerSymbol, null
        )
        //        addLayer(mEndpointLayer, NIMapView.LAYER_GROUPS.OPERATE)
        mEndpointLayer.setOnItemGestureListener(object : OnItemGestureListener<MarkerInterface> {
            override fun onItemSingleTapUp(index: Int, item: MarkerInterface): Boolean {
                if (bDrawLine) {
                    for (i in mPathMakers.indices) {
                        val item1 = mPathMakers[i]
                        if (item === item1) {
                            mMapView.vtmMap.animator().animateTo(
                                GeoPoint(
                                    item.getPoint().latitude,
                                    item.getPoint().longitude
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
                            return false
                        }
                    }
                }
                return false
            }

            override fun onItemLongPress(index: Int, item: MarkerInterface): Boolean {
                return false
            }
        })
    }

    fun addDrawLinePoint(geoPoint: GeoPoint): List<GeoPoint> {

        if (!bDrawLine) {
            mPathLayer.isEnabled = true
            mPathLayerTemp.isEnabled = true
            mEndpointLayer.isEnabled = true
            bDrawLine = true
        }
        //编辑点
        if (editIndex > -1) {
            if (mPathLayer.points.size > 0) {
                var list: MutableList<GeoPoint> = mutableListOf<GeoPoint>()
                list.addAll(mPathLayer.points)
                if (list.size > 3) {
                    if (editIndex == 0) {
                        val listNew = mutableListOf<GeoPoint>()
                        listNew.add(geoPoint)
                        listNew.add(list[1])
                        list.removeAt(0)
                        if (GeometryTools.isLineStringCrosses(list, listNew)) {
                            Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                            return mPathLayer.points
                        }
                    } else if (editIndex == list.size - 1) {
                        val listNew = mutableListOf<GeoPoint>()
                        listNew.add(geoPoint)
                        listNew.add(list[editIndex - 1])
                        list.removeAt(editIndex)
                        if (GeometryTools.isLineStringCrosses(list, listNew)) {
                            Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                            return mPathLayer.points
                        }
                    } else if (editIndex == 1) {
                        val listNew = mutableListOf<GeoPoint>()
                        listNew.add(list[0])
                        listNew.add(geoPoint)
                        listNew.add(list[2])
                        list = list.subList(2, list.size)
                        if (GeometryTools.isLineStringCrosses(list, listNew)) {
                            Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                            return mPathLayer.points
                        }
                    } else if (editIndex == list.size - 2) {
                        val listNew = mutableListOf<GeoPoint>()
                        listNew.add(list[list.size - 1])
                        listNew.add(geoPoint)
                        listNew.add(list[editIndex - 1])
                        list = list.subList(0, list.size - 2)
                        if (GeometryTools.isLineStringCrosses(list, listNew)) {
                            Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                            return mPathLayer.points
                        }
                    } else {
                        val listNew = mutableListOf<GeoPoint>()
                        listNew.add(list[editIndex - 1])
                        listNew.add(geoPoint)
                        listNew.add(list[editIndex + 1])
                        val list1: MutableList<GeoPoint> = mutableListOf();
                        list1.addAll(list.subList(0, editIndex))
                        val list2: MutableList<GeoPoint> = mutableListOf()
                        list2.addAll(list.subList(editIndex + 1, list.size))
                        if (GeometryTools.isLineStringCrosses(list1, listNew)) {
                            Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                            return mPathLayer.points
                        }
                        if (GeometryTools.isLineStringCrosses(list2, listNew)) {
                            Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                            return mPathLayer.points
                        }
                    }
                }
                if (editIndex < mPathLayer.getPoints().size) {
                    mPathLayer.points.removeAt(editIndex)
                    val list2: MutableList<GeoPoint> = mutableListOf<GeoPoint>()
                    list2.addAll(mPathLayer.points)
                    list2.add(editIndex, geoPoint)
                    mPathLayer.setPoints(list2)
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
//                    if (NMPFragmentManager.getInstance()
//                            .getNewDataBottomType() === 3 && mPathMakers.size > 1
//                    ) {
//                        list.add(mPathMakers[0].geoPoint)
//                        list.add(geoPoint)
//                        list.add(mPathMakers[mPathMakers.size - 1].geoPoint)
//                    } else {
                list.add(mPathMakers[mPathMakers.size - 1].geoPoint)
                list.add(geoPoint)
//                    }
                mPathLayerTemp.setPoints(list)
            }
            editIndex = -1
        } else { //新增点
            val points: List<GeoPoint> = mPathLayer.points
            if (points.size > 2) {
                val list: MutableList<GeoPoint> = mutableListOf()
                list.add(geoPoint)
                list.add(points[points.size - 1])
                val bCross = GeometryTools.isLineStringCrosses(points, list)
                if (bCross) {
                    Toast.makeText(mContext, "不能交叉", Toast.LENGTH_SHORT).show()
                    return mPathLayer.points
                }
            }
            mPathLayer.addPoint(geoPoint)
            val markerItem = MarkerItem(StringUtil.createUUID(), "", "", geoPoint)
            mEndpointLayer.addItem(markerItem)
            mPathMakers.add(markerItem)
        }

        return mPathLayer.points
    }

    fun addDrawLine(list: List<GeoPoint>) {
        for (item in list) {
            addDrawLinePoint(item)
        }
    }


    override fun onMapEvent(e: Event, mapPosition: MapPosition) {
        if (!bDrawLine)
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
            if (mPathLayer.points.size > 0) {
                if (editIndex > -1) {
                    val list: MutableList<GeoPoint> = mutableListOf()
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
                    val list: MutableList<GeoPoint> = mutableListOf()
                    list.add(mPathLayer.points[mPathLayer.points.size - 1])
                    list.add(
                        GeoPoint(
                            mapPosition.latitude,
                            mapPosition.longitude
                        )
                    )
                    mPathLayerTemp.setPoints(list)
                    if (mPathLayer.points.size > 0) {
                        val listDis: MutableList<GeoPoint> = mutableListOf()
                        listDis.add(
                            GeoPoint(
                                mapPosition.latitude,
                                mapPosition.longitude
                            )
                        )
//                        val distance: Double =
//                            GeometryTools.getDistance(listDis)
//                        if (distance < 1000) crossText.setText(
//                            distance as Int.toString() + "米"
//                        ) else {
//                            val d = distance / 1000.0
//                            val bg = BigDecimal(d)
//                            val f1 =
//                                bg.setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
////                            crossText.setText(f1.toString() + "公里")
//                        }
                    } else {
//                        crossText.setText("")
                    }
                }
            }
        }
    }

    fun clean() {
        mPathLayer.clearPath()
        mPathLayer.isEnabled = false
        mPathLayerTemp.clearPath()
        mPathLayerTemp.isEnabled = false
        mEndpointLayer.removeAllItems()
        mEndpointLayer.isEnabled = false
        mPathMakers.clear()
        editIndex = -1
        bDrawLine = false
    }
}