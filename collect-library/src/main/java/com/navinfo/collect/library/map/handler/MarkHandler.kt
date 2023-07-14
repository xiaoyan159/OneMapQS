package com.navinfo.collect.library.map.handler

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.navinfo.collect.library.R
import com.navinfo.collect.library.data.dao.impl.TraceDataBase
import com.navinfo.collect.library.data.entity.NiLocation
import com.navinfo.collect.library.data.entity.NoteBean
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.map.NIMapView
import com.navinfo.collect.library.map.cluster.ClusterMarkerItem
import com.navinfo.collect.library.map.cluster.ClusterMarkerRenderer
import com.navinfo.collect.library.map.layers.MyItemizedLayer
import com.navinfo.collect.library.map.layers.NoteLineLayer
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.StringUtil
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.backend.CanvasAdapter
import org.oscim.backend.canvas.Bitmap
import org.oscim.backend.canvas.Paint
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.ItemizedLayer
import org.oscim.layers.marker.ItemizedLayer.OnItemGestureListener
import org.oscim.layers.marker.MarkerInterface
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerRendererFactory
import org.oscim.layers.marker.MarkerSymbol
import org.oscim.layers.vector.geometries.Drawable
import org.oscim.layers.vector.geometries.LineDrawable
import org.oscim.layers.vector.geometries.PointDrawable
import org.oscim.layers.vector.geometries.PolygonDrawable
import org.oscim.layers.vector.geometries.Style
import org.oscim.map.Map
import java.util.Locale

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
    private val canvas: org.oscim.backend.canvas.Canvas by lazy {
        CanvasAdapter.newCanvas()
    }

    /**
     * 默认marker图层
     */
    private val mDefaultMarkerLayer: ItemizedLayer by lazy {
        //新增marker图标样式
        val mDefaultBitmap =
            AndroidBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.marker))

        val markerSymbol = MarkerSymbol(
            mDefaultBitmap,
            MarkerSymbol.HotspotPlace.BOTTOM_CENTER
        )
        val layer = ItemizedLayer(
            mapView.vtmMap,
            ArrayList(),
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
        addLayer(layer, NIMapView.LAYER_GROUPS.OPERATE_MARKER)
        layer
    }

    private lateinit var niLocationItemizedLayer: MyItemizedLayer

    private lateinit var markerRendererFactory: MarkerRendererFactory
    private val resId = R.mipmap.map_icon_report
    private val noteResId = R.drawable.icon_note_marker
    private var itemListener: OnQsRecordItemClickListener? = null
    private var niLocationBitmap: Bitmap? = null
    private var niLocationBitmap1: Bitmap? = null
    private var niLocationBitmap2: Bitmap? = null
    private var niLocationBitmap3: Bitmap? = null

    /**
     * 评测数据marker 图层
     */
    private val qsRecordItemizedLayer: MyItemizedLayer by lazy {
        val layer = MyItemizedLayer(
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
                                    qsRecordItemizedLayer.itemList[i]
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
    private val noteLayer: MyItemizedLayer by lazy {

        val layer = MyItemizedLayer(
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
                                    noteLayer.itemList[i]
                                if (markerInterface is MarkerItem) {
                                    idList.add(markerInterface.title)
                                }
                            }
                            it.onNoteList(idList.distinct().toMutableList())
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


    private val markerRendererFactory: MarkerRendererFactory by lazy {
        val bitmapPoi: Bitmap = AndroidBitmap(
            BitmapFactory.decodeResource(
                mContext.resources,
                R.mipmap.map_icon_blue2
            )
        )
        val symbol = MarkerSymbol(bitmapPoi, MarkerSymbol.HotspotPlace.BOTTOM_CENTER)
        MarkerRendererFactory { markerLayer ->
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
    }
    private val resId = R.mipmap.map_icon_report
    private val noteResId = R.drawable.icon_note_marker
    private var itemListener: OnQsRecordItemClickListener? = null

    /**
     * 文字大小
     */
    private val NUM_13 = 13


    init {
        //新增marker图标样式
        val mDefaultBitmap =
            AndroidBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.marker))
        val markerSymbol = MarkerSymbol(
            mDefaultBitmap,
            MarkerSymbol.HotspotPlace.BOTTOM_CENTER)

        niLocationBitmap = AndroidBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.icon_gps))
        niLocationBitmap1 = AndroidBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.icon_gps_1))
        niLocationBitmap2 = AndroidBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.icon_nilocation))
        niLocationBitmap3 = AndroidBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.icon_nilocation_1))

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
        addLayer(mDefaultMarkerLayer, NIMapView.LAYER_GROUPS.OPERATE_MARKER);
        // 设置矢量图层均在12级以上才显示
        mMapView.vtmMap.events.bind(Map.UpdateListener { e, mapPosition ->
            if (e == Map.SCALE_EVENT) {
                qsRecordItemizedLayer.isEnabled = mapPosition.getZoomLevel() >= 12
            }
        })
        initNoteData()
        //初始化加载轨迹
        initNiLocationDataLayer()
        mMapView.updateMap()
    }

    /**
     * 设置marker 点击回调
     */
    fun setOnQsRecordItemClickListener(listener: OnQsRecordItemClickListener?) {
        itemListener = listener
    }

    /**
     *    增加marker
     */

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
            var tempTitle = title
            if (tempTitle.isNullOrBlank()) {
                tempTitle = StringUtil.createUUID()
            }
            val marker = MarkerItem(
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


//    /**
//     * 初始话质检数据图层
//     */
//    private fun initQsRecordDataLayer() {
//
//        mContext.lifecycleScope.launch(Dispatchers.IO) {
//            var list = mutableListOf<QsRecordBean>()
//            val realm = Realm.getDefaultInstance()
//            realm.executeTransaction {
//                val objects = realm.where<QsRecordBean>().findAll()
//                list = realm.copyFromRealm(objects)
//            }
//            for (item in list) {
//                createMarkerItem(item)
//            }
//        }
//
//    }

    /**
     * 初始化定位图层
     */
    private fun initNiLocationDataLayer() {
        niLocationItemizedLayer =
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
                            val idList = mutableListOf<NiLocation>()
                            if (list.size == 0) {
                            } else {
                                for (i in list) {
                                    val markerInterface: MarkerInterface =
                                        niLocationItemizedLayer.itemList[i]
                                    if (markerInterface is MarkerItem) {
                                        idList.add(markerInterface.uid as NiLocation)
                                    }
                                }
                                it.onNiLocationList(idList.distinct().toMutableList())
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
        addLayer(niLocationItemizedLayer, NIMapView.LAYER_GROUPS.OPERATE_MARKER)
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
                            org.oscim.core.GeoPoint(geometry.coordinate.y, geometry.coordinate.x)
                    }
                }
                if (geoPoint != null) {
                    val geoMarkerItem: MarkerItem
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
                    qsRecordItemizedLayer.itemList.add(geoMarkerItem)
                }
            }
        }
        qsRecordItemizedLayer.populate()
    }

    /**
     * 添加质检数据marker
     */
    public suspend fun addNiLocationMarkerItem(niLocation: NiLocation) {
        val item = MarkerItem(niLocation, niLocation.id, "", GeoPoint(niLocation.latitude, niLocation.longitude))
        var itemizedLayer: ItemizedLayer? = null
        val direction: Double = niLocation.direction
        //角度
        when(niLocation.media){
            0->{
                //角度不为0时需要预先设置marker样式并进行角度设置，否则使用图层默认的sym即可
                //角度不为0时需要预先设置marker样式并进行角度设置，否则使用图层默认的sym即可
                if (direction != 0.0) {
                    val symbolGpsTemp =
                        MarkerSymbol(niLocationBitmap, MarkerSymbol.HotspotPlace.CENTER, false)
                    item.marker = symbolGpsTemp
                    item.setRotation(direction.toFloat())
                }else{
                    val symbolGpsTemp =
                        MarkerSymbol(niLocationBitmap2, MarkerSymbol.HotspotPlace.CENTER, false)
                    item.marker = symbolGpsTemp
                }
                niLocationItemizedLayer.addItem(item)
                itemizedLayer = niLocationItemizedLayer
            }
            1->{
                //角度不为0时需要预先设置marker样式并进行角度设置，否则使用图层默认的sym即可
                //角度不为0时需要预先设置marker样式并进行角度设置，否则使用图层默认的sym即可
                if (direction != 0.0) {
                    val symbolLidarTemp = MarkerSymbol(niLocationBitmap1, MarkerSymbol.HotspotPlace.CENTER, false)
                    item.marker = symbolLidarTemp
                    item.setRotation(direction.toFloat())
                }else{
                    val symbolGpsTemp =
                        MarkerSymbol(niLocationBitmap3, MarkerSymbol.HotspotPlace.CENTER, false)
                    item.marker = symbolGpsTemp
                }
                niLocationItemizedLayer.addItem(item)
                itemizedLayer = niLocationItemizedLayer
            }

        }

        itemizedLayer!!.update()

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

}

interface OnQsRecordItemClickListener {
    fun onQsRecordList(list: MutableList<String>)
    fun onNoteList(list: MutableList<String>)
}
