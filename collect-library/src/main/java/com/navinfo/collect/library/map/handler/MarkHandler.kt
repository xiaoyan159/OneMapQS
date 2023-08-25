package com.navinfo.collect.library.map.handler

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.navinfo.collect.library.R
import com.navinfo.collect.library.data.entity.NiLocation
import com.navinfo.collect.library.data.entity.NoteBean
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.map.BaseClickListener
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.cluster.ClusterMarkerItem
import com.navinfo.collect.library.map.cluster.ClusterMarkerRenderer
import com.navinfo.collect.library.map.layers.MyItemizedLayer
import com.navinfo.collect.library.map.layers.NoteLineLayer
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.StringUtil
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.backend.CanvasAdapter
import org.oscim.backend.canvas.Bitmap
import org.oscim.backend.canvas.Paint
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.*
import org.oscim.layers.marker.ItemizedLayer.OnItemGestureListener
import org.oscim.layers.vector.geometries.*
import org.oscim.map.Map
import java.util.*

/**
 * marker 操作
 */
class MarkHandler(context: AppCompatActivity, mapView: NIMapView) :
    BaseHandler(context, mapView) {

    /**
     * 默认文字颜色
     */
    private val mDefaultTextColor = "#4E55AF"

    /**
     * 文字画笔
     */
    private val paint: Paint by lazy {
        val p = CanvasAdapter.newPaint()
        p.setTypeface(Paint.FontFamily.DEFAULT, Paint.FontStyle.NORMAL)
        p.setTextSize(NUM_13 * CanvasAdapter.getScale())
        p.strokeWidth = 2 * CanvasAdapter.getScale()
        p.color = Color.parseColor(mDefaultTextColor)
        p
    }

    /**
     *  画布
     */
    private val canvas: org.oscim.backend.canvas.Canvas = CanvasAdapter.newCanvas()

    /**
     * 默认marker图层
     */
    private val mDefaultMarkerLayer: ItemizedLayer by lazy {
        //新增marker图标样式
        val mDefaultBitmap =
            AndroidBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.marker))
        mDefaultBitmap.scaleTo(150, 150)
        val markerSymbol = MarkerSymbol(
            mDefaultBitmap,
            MarkerSymbol.HotspotPlace.CENTER
        )
        val layer = ItemizedLayer(
            mapView.vtmMap,
            markerSymbol,
        )
        addLayer(layer, NIMapView.LAYER_GROUPS.OPERATE_MARKER)
        layer
    }

    private val niLocationBitmap: Bitmap by lazy {
        AndroidBitmap(
            BitmapFactory.decodeResource(
                context.resources,
                R.mipmap.icon_gps
            )
        )
    }
    private val niLocationBitmap1: Bitmap by lazy {
        AndroidBitmap(
            BitmapFactory.decodeResource(
                context.resources,
                R.mipmap.icon_gps_1
            )
        )
    }
    private val niLocationBitmap2: Bitmap by lazy {
        AndroidBitmap(
            BitmapFactory.decodeResource(
                context.resources,
                R.mipmap.icon_nilocation
            )
        )
    }
    private val niLocationBitmap3: Bitmap by lazy {
        AndroidBitmap(
            BitmapFactory.decodeResource(
                context.resources,
                R.mipmap.icon_nilocation_1
            )
        )
    }

    /**
     * 评测数据marker 图层
     */
    private val qsRecordItemizedLayer: MyItemizedLayer by lazy {
        val bitmapPoi: Bitmap = AndroidBitmap(
            BitmapFactory.decodeResource(
                mContext.resources,
                R.mipmap.map_icon_blue2
            )
        )
        val symbol = MarkerSymbol(bitmapPoi, MarkerSymbol.HotspotPlace.BOTTOM_CENTER)
        val markerRendererFactory = MarkerRendererFactory { markerLayer ->
            object : ClusterMarkerRenderer(
                mContext,
                markerLayer,
                symbol,
                ClusterStyle(
                    org.oscim.backend.canvas.Color.WHITE,
                    org.oscim.backend.canvas.Color.BLUE
                )
            ) {
            }
        }
        val layer = MyItemizedLayer(
            mMapView.vtmMap,
            mutableListOf(),
            markerRendererFactory,
            object : MyItemizedLayer.OnItemGestureListener {
                override fun onItemSingleTapUp(
                    list: MutableList<Int>,
                    nearest: Int
                ): Boolean {
                    val tag = mMapView.listenerTagList.last()
                    val listenerList = mMapView.listenerList[tag]
                    if (listenerList != null) {
                        for (listener in listenerList) {
                            if (listener is OnQsRecordItemClickListener) {
                                val idList = mutableListOf<String>()
                                for (i in list) {
                                    val markerInterface: MarkerInterface =
                                        qsRecordItemizedLayer.itemList[i]
                                    if (markerInterface is MarkerItem) {
                                        idList.add(markerInterface.title)
                                    }
                                }
                                listener.onQsRecordList(tag, idList.distinct().toMutableList())
                                break
                            }
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
        addLayer(layer, NIMapView.LAYER_GROUPS.OPERATE_MARKER)
        layer
    }

    /**
     * 评测数据marker 图层
     */
    private val niLocationItemizedLayer: ItemizedLayer by lazy {

        val symbol = MarkerSymbol(niLocationBitmap, MarkerSymbol.HotspotPlace.CENTER)
        val layer = ItemizedLayer(
            mapView.vtmMap,
            symbol,
        )
        layer.setOnItemGestureListener(object : OnItemGestureListener<MarkerInterface> {
            override fun onItemSingleTapUp(index: Int, item: MarkerInterface?): Boolean {
                val tag = mMapView.listenerTagList.last()
                val listenerList = mMapView.listenerList[tag]
                if (listenerList != null) {
                    for (listener in listenerList) {
                        if (listener is OnNiLocationItemListener) {
                            listener.onNiLocation(
                                tag,
                                index,
                                (niLocationItemizedLayer.itemList[index] as MarkerItem).uid as NiLocation
                            )
                            break
                        }
                    }
                }
                return true
            }

            override fun onItemLongPress(index: Int, item: MarkerInterface?): Boolean {
                return true
            }

        })

        addLayer(layer, NIMapView.LAYER_GROUPS.OPERATE_MARKER)
        layer
    }

    /**
     * 便签线图层
     */
    private val noteLineLayer: NoteLineLayer by lazy {
        val layer = NoteLineLayer(mMapView.vtmMap)
        addLayer(layer, NIMapView.LAYER_GROUPS.VECTOR)
        layer
    }

    /**
     * 便签图标图层
     */
    private val noteLayer: ItemizedLayer by lazy {
        val bitmap =
            AndroidBitmap(BitmapFactory.decodeResource(context.resources, noteResId))
        val symbol = MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.CENTER)
        val layer = ItemizedLayer(
            mMapView.vtmMap,
            symbol,
        )
        layer.setOnItemGestureListener(object : OnItemGestureListener<MarkerInterface> {
            override fun onItemSingleTapUp(index: Int, item: MarkerInterface?): Boolean {
                val tag = mMapView.listenerTagList.last()
                val listenerList = mMapView.listenerList[tag]
                if (listenerList != null) {
                    for (listener in listenerList) {
                        if (listener is ONNoteItemClickListener) {
                            val marker = layer.itemList[index]
                            if (marker is MarkerItem)
                                listener.onNote(tag, marker.title)
                            break
                        }
                    }
                }
                return true
            }

            override fun onItemLongPress(index: Int, item: MarkerInterface?): Boolean {
                return true
            }
        })
        addLayer(layer, NIMapView.LAYER_GROUPS.OPERATE_MARKER)
        layer
    }


    private val resId = R.mipmap.map_icon_report
    private val noteResId = R.drawable.icon_note_marker

    /**
     * 文字大小
     */
    private val NUM_13 = 13


    init {
        // 设置矢量图层均在12级以上才显示
        mMapView.vtmMap.events.bind(Map.UpdateListener { e, mapPosition ->
            if (e == Map.SCALE_EVENT) {
                qsRecordItemizedLayer.isEnabled = mapPosition.getZoomLevel() >= 12
                niLocationItemizedLayer.isEnabled = mapPosition.getZoomLevel() >= 12
            }
        })
    }


    /**
     *    增加marker
     */
    fun addMarker(
        geoPoint: GeoPoint,
        title: String?,
        description: String? = "",
        uid: java.lang.Object? = null,
    ) {
        var marker: MarkerItem? = null
        for (e in mDefaultMarkerLayer.itemList) {
            if (e is MarkerItem && e.title == title) {
                marker = e
                break
            }
        }
        if (marker == null) {
            var tempTitle = title
            if (tempTitle.isNullOrBlank()) {
                tempTitle = StringUtil.createUUID()
            }
            val marker = MarkerItem(
                uid,
                tempTitle,
                description,
                geoPoint
            )
            mDefaultMarkerLayer.addItem(marker)
            mMapView.vtmMap.updateMap(true)
        } else {
            marker.description = description
            marker.geoPoint = geoPoint
            mDefaultMarkerLayer.removeItem(marker)
            mDefaultMarkerLayer.addItem(marker)
            mMapView.vtmMap.updateMap(true)
        }
    }

    fun getCurrentMark(): MarkerInterface? {

        if (mDefaultMarkerLayer != null) {
            return mDefaultMarkerLayer.itemList[mDefaultMarkerLayer.itemList.size - 1]
        }
        return null
    }

    /**
     * 移除marker
     */
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
    fun addOrUpdateQsRecordMark(data: QsRecordBean) {
        for (item in qsRecordItemizedLayer.itemList) {
            if (item is MarkerItem) {
                if (item.title == data.id) {
                    qsRecordItemizedLayer.itemList.remove(item)
                    break
                }
            }
        }
        createQsRecordMarker(data)
        mMapView.updateMap(true)
    }


    /**
     * 增加或更新便签
     */
    fun addOrUpdateNoteMark(data: NoteBean) {
        for (item in noteLayer.itemList) {
            if (item is MarkerItem) {
                if (item.title == data.id) {
                    noteLayer.itemList.remove(item)
                    break
                }
            }
        }
        noteLineLayer.removeNoteBeanLines(data)
        createNoteMarkerItem(data)
        mMapView.updateMap(true)
    }


    private fun convertGeometry2Drawable(geometry: Geometry, vectorLayerStyle: Style): Drawable? {
        var resultDrawable: Drawable? = null
        if ("POINT" == geometry.geometryType.uppercase(Locale.getDefault())) {
            val geoPoint = GeoPoint(geometry.coordinate.y, geometry.coordinate.x)
            resultDrawable = PointDrawable(geoPoint, vectorLayerStyle)
        } else if ("LINESTRING" == geometry.geometryType.uppercase(Locale.getDefault())) {
            val lineString = geometry as LineString
            resultDrawable = LineDrawable(lineString, vectorLayerStyle)
        } else if ("POLYGON" == geometry.geometryType.uppercase(Locale.getDefault())) {
            val polygon = geometry as Polygon
            resultDrawable = PolygonDrawable(polygon, vectorLayerStyle)
        }
        return resultDrawable
    }


    /**
     * 删除质检数据
     */
    fun removeQsRecordMark(data: QsRecordBean) {
        for (item in qsRecordItemizedLayer.itemList) {
            if (item is MarkerItem) {
                if (item.title == data.id) {
                    qsRecordItemizedLayer.itemList.remove(item)
                    qsRecordItemizedLayer.populate()
                    return
                }
            }
        }
    }

    /**
     * 删除标签
     */
    fun removeNoteMark(data: NoteBean) {
        for (item in noteLayer.itemList) {
            if (item is MarkerItem) {
                if (item.title == data.id) {
                    noteLayer.itemList.remove(item)
                    noteLineLayer.removeNoteBeanLines(data)
                    noteLayer.populate()
                    mMapView.updateMap(true)
                    return
                }
            }
        }
    }


    /**
     * 添加质检数据marker
     */
    private fun createNoteMarkerItem(item: NoteBean) {
        val bitmap: Bitmap = createTextMarkerBitmap(mContext, item.description, noteResId)
        val geometry: Geometry? = GeometryTools.createGeometry(item.guideGeometry)
        if (geometry != null) {
            var geoPoint: GeoPoint? = null
            if (geometry.geometryType != null) {
                when (geometry.geometryType.uppercase(Locale.getDefault())) {
                    "POINT" -> geoPoint =
                        GeoPoint(geometry.coordinate.y, geometry.coordinate.x)
                }
            }
            if (geoPoint != null) {
                val geoMarkerItem: MarkerItem
                geoMarkerItem = ClusterMarkerItem(
                    1, item.id, item.description, geoPoint
                )
                val markerSymbol =
                    MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.CENTER)
                geoMarkerItem.marker = markerSymbol
                noteLayer.itemList.add(geoMarkerItem)
            }
        }
        noteLineLayer.showNoteBeanLines(item)
        noteLayer.populate()
    }


    /**
     * 添加质检数据marker
     */
    private fun createQsRecordMarker(item: QsRecordBean) {
        val bitmap: Bitmap = createTextMarkerBitmap(mContext, item.description, resId)
        if (item.t_lifecycle != 2) {
            val geometry: Geometry? = GeometryTools.createGeometry(item.geometry)
            if (geometry != null) {
                var geoPoint: GeoPoint? = null
                if (geometry.geometryType != null) {
                    when (geometry.geometryType.uppercase(Locale.getDefault())) {
                        "POINT" -> geoPoint =
                            GeoPoint(geometry.coordinate.y, geometry.coordinate.x)
                    }
                }
                if (geoPoint != null) {

                    val geoMarkerItem: MarkerItem

                    geoMarkerItem = ClusterMarkerItem(1, item.id, item.description, geoPoint)

                    val markerSymbol = MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.CENTER)

                    geoMarkerItem.marker = markerSymbol

                    qsRecordItemizedLayer.itemList.add(geoMarkerItem)
                }
            }
        }
        qsRecordItemizedLayer.populate()
    }

    /**
     * 添加质检数据marker
     */
    fun addNiLocationMarkerItem(niLocation: NiLocation) {
        synchronized(this) {
            var geoMarkerItem = createNILocationBitmap(niLocation)
            niLocationItemizedLayer.addItem(geoMarkerItem)
            niLocationItemizedLayer.update()
        }
    }

    private fun createNILocationBitmap(niLocation: NiLocation): MarkerItem {

        val direction: Double = niLocation.direction

        val geoMarkerItem: MarkerItem = ClusterMarkerItem(
            niLocation,
            niLocation.id,
            niLocation.time,
            GeoPoint(niLocation.latitude, niLocation.longitude)
        )

        //角度
        when (niLocation.media) {
            0 -> {
                //角度不为0时需要预先设置marker样式并进行角度设置，否则使用图层默认的sym即可
                //角度不为0时需要预先设置marker样式并进行角度设置，否则使用图层默认的sym即可
                if (direction > 0.0) {
                    val symbolGpsTemp =
                        MarkerSymbol(niLocationBitmap, MarkerSymbol.HotspotPlace.CENTER, false)
                    geoMarkerItem.marker = symbolGpsTemp
                    geoMarkerItem.setRotation(direction.toFloat())
                } else {
                    val symbolGpsTemp =
                        MarkerSymbol(niLocationBitmap2, MarkerSymbol.HotspotPlace.CENTER, false)
                    geoMarkerItem.marker = symbolGpsTemp
                }
            }

            1 -> {
                //角度不为0时需要预先设置marker样式并进行角度设置，否则使用图层默认的sym即可
                //角度不为0时需要预先设置marker样式并进行角度设置，否则使用图层默认的sym即可
                if (direction > 0.0) {
                    val symbolLidarTemp =
                        MarkerSymbol(niLocationBitmap1, MarkerSymbol.HotspotPlace.CENTER, false)
                    geoMarkerItem.marker = symbolLidarTemp
                    geoMarkerItem.setRotation(direction.toFloat())
                } else {
                    val symbolGpsTemp =
                        MarkerSymbol(niLocationBitmap3, MarkerSymbol.HotspotPlace.CENTER, false)
                    geoMarkerItem.marker = symbolGpsTemp
                }
            }
        }

        return geoMarkerItem
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

    /**
     * 移除轨迹图层
     */
    fun clearNiLocationLayer() {
        niLocationItemizedLayer.removeAllItems()
        niLocationItemizedLayer.update()
    }

    /**
     * 移除所有质检数据
     */
    fun removeAllQsMarker() {
        qsRecordItemizedLayer.removeAllItems()
        mMapView.updateMap(true)
    }

    fun getNILocationItemizedLayerSize(): Int {
        return niLocationItemizedLayer.itemList.size
    }

    fun getNILocation(index: Int): NiLocation? {
        return if (index > -1 && index < getNILocationItemizedLayerSize()) {
            ((niLocationItemizedLayer.itemList[index]) as MarkerItem).uid as NiLocation
        } else {
            null
        }
    }

    fun getNILocationIndex(niLocation: NiLocation): Int? {

        var list = niLocationItemizedLayer.itemList

        if (niLocation != null && list.isNotEmpty()) {

            var index = -1

            list.forEach {

                index += 1

                if (((it as MarkerItem).uid as NiLocation).id.equals(niLocation.id)) {
                    return index
                }
            }
        }

        return -1
    }
}

interface OnQsRecordItemClickListener : BaseClickListener {
    fun onQsRecordList(tag: String, list: MutableList<String>)
}

interface ONNoteItemClickListener : BaseClickListener {
    fun onNote(tag: String, noteId: String)
}

interface OnNiLocationItemListener : BaseClickListener {
    fun onNiLocation(tag: String, index: Int, it: NiLocation)
}