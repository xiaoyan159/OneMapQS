package com.navinfo.collect.library.map.handler

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.navinfo.collect.library.R
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.cluster.ClusterMarkerItem
import com.navinfo.collect.library.map.cluster.ClusterMarkerRenderer
import com.navinfo.collect.library.map.layers.MyItemizedLayer
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.StringUtil
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.locationtech.jts.geom.Geometry
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.backend.CanvasAdapter
import org.oscim.backend.canvas.Bitmap
import org.oscim.backend.canvas.Paint
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.*
import org.oscim.layers.marker.ItemizedLayer.OnItemGestureListener
import org.oscim.map.Map
import java.util.*
import kotlin.collections.ArrayList

/**
 * marker 操作
 */
class MarkHandler(context: AppCompatActivity, mapView: NIMapView) :
    BaseHandler(context, mapView) {

    //    //默认marker图层
    private var mDefaultMarkerLayer: ItemizedLayer

    /**
     * 默认文字颜色
     */
    private val mDefaultTextColor = "#4E55AF"

    /**
     * 文字画笔
     */

    private lateinit var paint: Paint

    //画布
    private lateinit var canvas: org.oscim.backend.canvas.Canvas
    private lateinit var itemizedLayer: MyItemizedLayer
    private lateinit var markerRendererFactory: MarkerRendererFactory
    private var resId = R.mipmap.map_icon_point_add
    private var itemListener: OnQsRecordItemClickListener? = null

    /**
     * 文字大小
     */
    private val NUM_13 = 13

    init {
        //新增marker图标样式
        val mDefaultBitmap =
            AndroidBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.marker));

        val markerSymbol = MarkerSymbol(
            mDefaultBitmap,
            MarkerSymbol.HotspotPlace.BOTTOM_CENTER
        );
        //新增marker图层
        mDefaultMarkerLayer = ItemizedLayer(
            mapView.vtmMap,
            ArrayList<MarkerInterface>(),
            markerSymbol,
            object : OnItemGestureListener<MarkerInterface> {
                override fun onItemSingleTapUp(index: Int, item: MarkerInterface?): Boolean {
                    return false
                }

                override fun onItemLongPress(index: Int, item: MarkerInterface?): Boolean {
                    return false
                }

            }
        )

        //初始化之间数据图层
        initQsRecordDataLayer()
        addLayer(mDefaultMarkerLayer, NIMapView.LAYER_GROUPS.OPERATE);
        // 设置矢量图层均在12级以上才显示
        mMapView.vtmMap.events.bind(Map.UpdateListener { e, mapPosition ->
            if (e == Map.SCALE_EVENT) {
                itemizedLayer.isEnabled = mapPosition.getZoomLevel() >= 12
            }
        })
        mMapView.updateMap()
    }

    fun setOnQsRecordItemClickListener(listener: OnQsRecordItemClickListener?) {
        itemListener = listener
    }

    //增加marker
    fun addMarker(
        geoPoint: GeoPoint,
        title: String?,
        description: String? = ""
    ) {
        var marker: MarkerItem? = null
        for (e in mDefaultMarkerLayer.itemList) {
            if (e is MarkerItem && e.title == title) {
                marker = e
                break
            }
        }
        if (marker == null) {
            var tempTitle = title;
            if (tempTitle.isNullOrBlank()) {
                tempTitle = StringUtil.createUUID();
            }
            val marker = MarkerItem(
                tempTitle,
                description,
                geoPoint
            )
            mDefaultMarkerLayer.addItem(marker);
            mMapView.vtmMap.updateMap(true)
        } else {
            marker.description = description
            marker.geoPoint = geoPoint
            mDefaultMarkerLayer.removeItem(marker)
            mDefaultMarkerLayer.addItem(marker)
            mMapView.vtmMap.updateMap(true)
        }
    }

    fun removeMarker(title: String) {
        var marker: MarkerItem? = null
        for (e in mDefaultMarkerLayer.itemList) {
            if (e is MarkerItem && e.title == title) {
                marker = e
                break
            }
        }
        if (marker != null) {
            mDefaultMarkerLayer.removeItem(marker)
            mMapView.vtmMap.updateMap(true)
        }
    }


    /**
     * 增加或更新marker
     */
    suspend fun addOrUpdateQsRecordMark(data: QsRecordBean) {
        for (item in itemizedLayer.itemList) {
            if (item is MarkerItem) {
                if (item.title == data.id) {
                    itemizedLayer.itemList.remove(item)
                    break
                }
            }
        }
        createMarkerItem(data)
        withContext(Dispatchers.Main) {
            mMapView.updateMap(true)
        }

    }


    /**
     * 删除marker
     */
    suspend fun removeQsRecordMark(data: QsRecordBean) {
        for (item in itemizedLayer.itemList) {
            if (item is MarkerItem) {
                if (item.title == data.id) {
                    itemizedLayer.itemList.remove(item)
                    itemizedLayer.populate()
                    return
                }
            }
        }
    }

    /**
     * 初始话质检数据图层
     */
    private fun initQsRecordDataLayer() {

        canvas = CanvasAdapter.newCanvas()
        paint = CanvasAdapter.newPaint()
        paint.setTypeface(Paint.FontFamily.DEFAULT, Paint.FontStyle.NORMAL)
        paint.setTextSize(NUM_13 * CanvasAdapter.getScale())
        paint.strokeWidth = 2 * CanvasAdapter.getScale()
        paint.color = Color.parseColor(mDefaultTextColor)
        val bitmapPoi: Bitmap = AndroidBitmap(
            BitmapFactory.decodeResource(
                mContext.resources,
                R.mipmap.map_icon_blue2
            )
        )
        val symbol = MarkerSymbol(bitmapPoi, MarkerSymbol.HotspotPlace.BOTTOM_CENTER)
        markerRendererFactory = MarkerRendererFactory { markerLayer ->
            object : ClusterMarkerRenderer(
                mContext,
                markerLayer,
                symbol,
                ClusterStyle(
                    org.oscim.backend.canvas.Color.WHITE,
                    org.oscim.backend.canvas.Color.BLUE
                )
            ) {
//                override fun getClusterBitmap(size: Int): Bitmap? {
//                    return super.getclusterbitmap(size)
//                }
            }
        }

        itemizedLayer =
            MyItemizedLayer(
                mMapView.vtmMap,
                mutableListOf(),
                markerRendererFactory,
                object : MyItemizedLayer.OnItemGestureListener {
                    override fun onItemSingleTapUp(
                        list: MutableList<Int>,
                        nearest: Int
                    ): Boolean {
                        itemListener?.let {
                            val idList = mutableListOf<String>()
                            if (list.size == 0) {
                            } else {
                                for (i in list) {
                                    val markerInterface: MarkerInterface =
                                        itemizedLayer.itemList[i]
                                    if (markerInterface is MarkerItem) {
                                        idList.add(markerInterface.title)
                                    }
                                }
                                it.onQsRecordList(idList.distinct().toMutableList())
                            }
                        }
                        return true
                    }

                    override fun onItemLongPress(
                        list: MutableList<Int>?,
                        nearest: Int
                    ): Boolean {
                        return true
                    }
                })
        addLayer(itemizedLayer, NIMapView.LAYER_GROUPS.OPERATE)
        mContext.lifecycleScope.launch(Dispatchers.IO) {
            var list = mutableListOf<QsRecordBean>()
            val realm = Realm.getDefaultInstance()
            Log.e("jingo", "realm hashCOde ${realm.hashCode()}")
            realm.executeTransaction {
                val objects = realm.where<QsRecordBean>().findAll()
                list = realm.copyFromRealm(objects)
            }
//            realm.close()

            for (item in list) {
                createMarkerItem(item)
            }
        }

    }

    private suspend fun createMarkerItem(item: QsRecordBean) {
        val bitmap: Bitmap = createTextMarkerBitmap(mContext, item.description, resId)
        if (item.t_lifecycle != 2) {
            val geometry: Geometry? = GeometryTools.createGeometry(item.geometry)
            if (geometry != null) {
                var geoPoint: org.oscim.core.GeoPoint? = null
                if (geometry.geometryType != null) {
                    when (geometry.geometryType.uppercase(Locale.getDefault())) {
                        "POINT" -> geoPoint =
                            org.oscim.core.GeoPoint(geometry.coordinate.y, geometry.coordinate.x)
//                                "LINESTRING" -> {
//                                    val lineString = geometry as LineString
//                                    if (lineString != null && lineString.coordinates.size > 0) {
//                                        geoPoint = GeoPoint(
//                                            lineString.coordinates[0].y,
//                                            lineString.coordinates[0].x
//                                        )
//                                    }
//                                    val drawableLine: Drawable =
//                                        convertGeometry2Drawable(geometry, lineStyle)
//                                    if (drawableLine != null) {
//                                        dataVectorLayer.add(drawableLine)
//                                    }
//                                }
//                                "POLYGON" -> {
//                                    val polygon = geometry as Polygon
//                                    if (polygon != null && polygon.coordinates.size > 0) {
//                                        geoPoint = GeoPoint(
//                                            polygon.coordinates[0].y,
//                                            polygon.coordinates[0].x
//                                        )
//                                    }
//                                    val drawablePolygon: Drawable =
//                                        convertGeometry2Drawable(geometry, polygonStyle)
//                                    if (drawablePolygon != null) {
//                                        dataVectorLayer.add(drawablePolygon)
//                                    }
//                                }
                    }
                }
                if (geoPoint != null) {
                    var geoMarkerItem: MarkerItem
//                            if (item.getType() === 1) {
                    geoMarkerItem = ClusterMarkerItem(
                        1, item.id, item.description, geoPoint
                    )
//                            } else {
//                                geoMarkerItem = MarkerItem(
//                                    ePointTemp.getType(),
//                                    ePointTemp.getId(),
//                                    ePointTemp.getStyleText(),
//                                    geoPoint
//                                )
//                            }
                    val markerSymbol =
                        MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.CENTER)
                    geoMarkerItem.marker = markerSymbol
                    itemizedLayer.itemList.add(geoMarkerItem)
                }
            }
        }
        itemizedLayer.populate()
    }


    /**
     * 文字和图片拼装，文字换行
     *
     * @param context
     * @param text
     * @param resId
     * @return
     */
    private fun createTextMarkerBitmap(context: Context, text: String, resId: Int): Bitmap {
        var text: String? = text
        return if (text == null || text.trim { it <= ' ' }.isEmpty()) {
            val drawable = ResourcesCompat.getDrawable(context.resources, resId, null)
            val originBitmap = android.graphics.Bitmap.createBitmap(
                drawable!!.intrinsicWidth,
                drawable.intrinsicHeight * 2,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val androidCanvas = Canvas(originBitmap)
            val startX = (originBitmap.width - drawable.intrinsicWidth) / 2
            drawable.setBounds(
                startX, 0, startX + drawable.intrinsicWidth, drawable.intrinsicHeight
            )
            drawable.draw(androidCanvas)
            val bitmap: Bitmap = AndroidBitmap(originBitmap)
            canvas.setBitmap(bitmap)
            bitmap
        } else {
            val drawable = ResourcesCompat.getDrawable(context.resources, resId, null)
            val textList: MutableList<String> = java.util.ArrayList()
            val fontSize: Float = NUM_13 * CanvasAdapter.getScale()
            paint.setTextSize(fontSize)
            var maxWidth = 0f
            //最多4行，一行7个
            if (text.trim { it <= ' ' }.length > 24) {
                val size = (drawable!!.intrinsicHeight / 4).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                if (text.trim { it <= ' ' }.length > 28) text = text.substring(0, 26) + "..."
                val temp1 = text.substring(0, 7)
                textList.add(temp1)
                text = text.substring(7)
                maxWidth = paint.getTextWidth(temp1)
                val temp2 = text.substring(0, 7)
                textList.add(temp2)
                text = text.substring(7)
                var newWidth = paint.getTextWidth(temp2)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                val temp3 = text.substring(0, 7)
                textList.add(temp3)
                text = text.substring(7)
                newWidth = paint.getTextWidth(temp3)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                textList.add(text)
                newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 21) {
                val size = (drawable!!.intrinsicHeight / 4).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 6)
                textList.add(temp1)
                text = text.substring(6)
                maxWidth = paint.getTextWidth(temp1)
                val temp2 = text.substring(0, 6)
                textList.add(temp2)
                text = text.substring(6)
                var newWidth = paint.getTextWidth(temp2)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                val temp3 = text.substring(0, 6)
                textList.add(temp3)
                text = text.substring(6)
                newWidth = paint.getTextWidth(temp3)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                textList.add(text)
                newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 18) {
                val size = (drawable!!.intrinsicHeight / 3).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 7)
                textList.add(temp1)
                text = text.substring(7)
                maxWidth = paint.getTextWidth(temp1)
                val temp2 = text.substring(0, 7)
                textList.add(temp2)
                text = text.substring(7)
                var newWidth = paint.getTextWidth(temp2)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                textList.add(text)
                newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 18) {
                val size = (drawable!!.intrinsicHeight / 3).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 7)
                textList.add(temp1)
                text = text.substring(7)
                maxWidth = paint.getTextWidth(temp1)
                val temp2 = text.substring(0, 7)
                textList.add(temp2)
                text = text.substring(7)
                var newWidth = paint.getTextWidth(temp2)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                textList.add(text)
                newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 14) {
                val size = (drawable!!.intrinsicHeight / 3).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 6)
                textList.add(temp1)
                text = text.substring(6)
                maxWidth = paint.getTextWidth(temp1)
                val temp2 = text.substring(0, 6)
                textList.add(temp2)
                text = text.substring(6)
                var newWidth = paint.getTextWidth(temp2)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
                textList.add(text)
                newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 12) {
                val size = (drawable!!.intrinsicHeight / 2).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 7)
                textList.add(temp1)
                text = text.substring(7)
                maxWidth = paint.getTextWidth(temp1)
                textList.add(text)
                val newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 10) {
                val size = (drawable!!.intrinsicHeight / 2).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 6)
                textList.add(temp1)
                text = text.substring(6)
                maxWidth = paint.getTextWidth(temp1)
                textList.add(text)
                val newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else if (text.trim { it <= ' ' }.length > 7) {
                val size = (drawable!!.intrinsicHeight / 2).toFloat()
                if (size < fontSize) paint.setTextSize(size)
                val temp1 = text.substring(0, 5)
                textList.add(temp1)
                text = text.substring(5)
                maxWidth = paint.getTextWidth(temp1)
                textList.add(text)
                val newWidth = paint.getTextWidth(text)
                if (newWidth > maxWidth) {
                    maxWidth = newWidth
                }
            } else {
                val size = drawable!!.intrinsicHeight.toFloat()
                if (size < fontSize) paint.setTextSize(size)
                textList.add(text)
                maxWidth = paint.getTextWidth(text)
            }
            paint.color = Color.parseColor(mDefaultTextColor)
            val originBitmap = android.graphics.Bitmap.createBitmap(
                if (drawable.intrinsicWidth > maxWidth) drawable.intrinsicWidth else maxWidth.toInt(),
                drawable.intrinsicHeight * 2,
                android.graphics.Bitmap.Config.ARGB_4444
            )
            val androidCanvas = Canvas(originBitmap)
            val startX = (originBitmap.width - drawable.intrinsicWidth) / 2
            drawable.setBounds(
                startX, 0, startX + drawable.intrinsicWidth, drawable.intrinsicHeight
            )
            drawable.draw(androidCanvas)
            val bitmap: Bitmap = AndroidBitmap(originBitmap)
            canvas.setBitmap(bitmap)
            var startHeight = (drawable.intrinsicHeight + paint.getTextHeight(text)).toInt()
            for (txt in textList) {
                canvas.drawText(
                    txt, (bitmap.width - paint.getTextWidth(txt)) / 2, startHeight.toFloat(), paint
                )
                startHeight += paint.getTextHeight(txt).toInt()
            }
            bitmap
        }
    }
}

interface OnQsRecordItemClickListener {
    fun onQsRecordList(list: MutableList<String>)
}
