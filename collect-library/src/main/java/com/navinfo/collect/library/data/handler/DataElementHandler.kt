package com.navinfo.collect.library.data.handler

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CONFLICT_NONE
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.navinfo.collect.library.data.DataConversion
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.entity.*
import com.navinfo.collect.library.data.entity.DataLayerItemType.*
import com.navinfo.collect.library.data.search.OnGetSearchDataResultListener
import com.navinfo.collect.library.data.search.SearchDataOption
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import io.realm.RealmSet
import org.oscim.core.MercatorProjection
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread

/**
 * 数据库操作
 */


open class
DataElementHandler(context: Context, dataBase: MapLifeDataBase) :
    BaseDataHandler(context, dataBase) {
    private var mListener: OnGetSearchDataResultListener? = null

    private var lastSearchTime = 0
    private var timer: Timer? = null

    fun setListener(listener: OnGetSearchDataResultListener) {
        this.mListener = listener
    }

    /**
     * 保存数据
     */
    fun saveData(
        element: Element,
        map: Map<String, Any>?,
        callback: (res: Boolean, errorString: String) -> Unit
    ) {
        thread(start = true) {
            try {
                if (map != null) {
                    val cursor =
                        mDataBase.openHelper.writableDatabase.query(
                            "Select * from \"${element.layerId}\" where uuid=?",
                            arrayOf(element.id)
                        )

                    val contentValues = ContentValues()  //存储信息
                    for ((key, value) in map) {
                        when (value) {
                            is String -> contentValues.put(key, value)
                            is Long -> contentValues.put(key, value)
                            is Int -> contentValues.put(key, value)
                            is Double -> contentValues.put(key, value)
                            is ByteArray -> contentValues.put(key, value)
                            is Boolean -> contentValues.put(key, value)
                            is Float -> contentValues.put(key, value)
                            is Short -> contentValues.put(key, value)
                            is Byte -> contentValues.put(key, value)
                        }
                    }
                    contentValues.put("uuid", element.id);
                    cursor.moveToFirst()
                    if (cursor.count > 0) {
                        mDataBase.openHelper.writableDatabase.update(
                            "'${element.layerId}'",
                            CONFLICT_NONE,
                            contentValues,
                            "uuid = ? ",
                            arrayOf(element.id)
                        )
                    } else {
                        mDataBase.openHelper.writableDatabase.insert(
                            "'${element.layerId}'",//element.layerId,
                            CONFLICT_NONE,
                            contentValues
                        ).toInt()
                    }
                    cursor.close()
                }

                //resetCoordinate(element)
                element.tLifecycle = 2

                //优先删除已有数据，下面会重新计算最新的tile
                mDataBase.tileElementDao.deleteElementId(element.id)

                val list = ArrayList<TileElement>()

                try {

                    if (element.geometry != null) {
                        val tileX = RealmSet<Int>()
                        GeometryToolsKt.getTileXByGeometry(element.geometry, tileX)
                        val tileY = RealmSet<Int>()
                        GeometryToolsKt.getTileYByGeometry(element.geometry, tileY)

                        //遍历存储tile对应的x与y的值
                        tileX.forEach { x ->

                            tileY.forEach { y ->
                                val tile = TileElement()
                                tile.elementId = element.id
                                tile.tilex = x
                                tile.tiley = y
                                list.add(tile)
                            }

                        }

                    }

                    mDataBase.tileElementDao.insertList(list)

                    mDataBase.elementDao.insert(element)

                } catch (e: java.lang.Exception) {

                }

                Handler(Looper.getMainLooper()).post {
                    callback.invoke(true, "")
                }

            } catch (e: Throwable) {
                e.message?.let { Log.e("jingo", it) }
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(false, "${e.message}")
                }
            }

        }
    }

    /**
     * 删除数据
     */

    fun deleteData(element: Element, callback: (res: Boolean, errorString: String) -> Unit) {
        thread(start = true) {
            try {
                mDataBase.openHelper.writableDatabase.delete(
                    "'${element.layerId}'",
                    "uuid=?",
                    arrayOf("'${element.id}'")
                )
                mDataBase.elementDao.delete(element);
            } catch (e: Throwable) {
                Log.e("jingo", "删除数据报错 ${e.message}");
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(false, "${e.message}")
                }
            }
            Handler(Looper.getMainLooper()).post {
                callback.invoke(true, "")
            }
        }
    }


//    /**
//     * 根据给定的geometry计算其横跨的20级瓦片Y值
//     */
//    private fun getTileYByGeometry(geometry: Geometry, tileYSet: MutableSet<Int?>): Set<Int?>? {
//        var tileYSet: MutableSet<Int?>? = tileYSet
//        val startTime = System.currentTimeMillis()
//        if (tileYSet == null) {
//            tileYSet = RealmSet()
//        }
//        val envelope = geometry.envelope
//        if (envelope != null) {
//            val coordinates = envelope.coordinates
//            // 最小最大x轴坐标，索引0位最小x值，索引1位最大y值
//            if (coordinates != null && coordinates.isNotEmpty()) {
//                val minMaxY = doubleArrayOf(coordinates[0].y, coordinates[0].y)
//                for (coordinate in coordinates) {
//                    // 获取最大和最小y的值
//                    if (coordinate.y < minMaxY[0]) {
//                        minMaxY[0] = coordinate.y
//                    }
//                    if (coordinate.y > minMaxY[1]) {
//                        minMaxY[1] = coordinate.y
//                    }
//                }
//                // 分别计算最大和最小x值对应的tile号
//                val tileY0 = MercatorProjection.latitudeToTileY(minMaxY[0], 20.toByte())
//                val tileY1 = MercatorProjection.latitudeToTileY(minMaxY[1], 20.toByte())
//                val minTileY = if (tileY0 <= tileY1) tileY0 else tileY1
//                val maxTileY = if (tileY0 <= tileY1) tileY1 else tileY0
//                println("getTileYByGeometry$envelope===$minTileY===$maxTileY")
//
//                for (i in minTileY..maxTileY) {
//                    tileYSet.add(i)
//                }
//            }
//        }
//        println("YGeometry-time:" + (System.currentTimeMillis() - startTime))
//        return tileYSet
//    }


    /**
     * 计算数据最大最小坐标
     */
    private fun resetCoordinate(element: Element) {
        when {
            element.geometry.startsWith("POINT") -> {
                var geoPoint = GeometryTools.createGeoPoint(element.geometry)
                val tile = TileElement()
                tile.elementId = element.id
                val minLatitude = Math.max(
                    MercatorProjection.LATITUDE_MIN,
                    MercatorProjection.tileYToLatitude(
                        (geoPoint.latitude.toLong() + 1).toLong(),
                        20
                    )
                )
                val minLongitude = Math.max(
                    -180.0,
                    MercatorProjection.tileXToLongitude(geoPoint.longitude.toLong(), 20)
                )
/*                element.maxx = (geoPoint.longitude * Constant.CONVERSION_FACTOR).toFloat()
                element.minx = element.maxx
                element.maxy = (geoPoint.latitude * Constant.CONVERSION_FACTOR).toFloat()
                element.miny = element.maxy*/
            }
            element.geometry.startsWith("LINESTRING") -> {

            }
            element.geometry.startsWith("POLYGON") -> {

            }
        }
    }


    /**
     * 捕捉数据
     */
    fun snapElementDataList(
        polygon: String,
        callback: (list: List<Element>) -> Unit
    ) {
        thread(start = true) {

            val geometry = GeometryTools.createGeometry(polygon)

            if (geometry != null) {
                //计算tileX号
                val tileX = RealmSet<Int>()
                GeometryToolsKt.getTileXByGeometry(polygon, tileX)

                //计算tileY号
                val tileY = RealmSet<Int>()
                GeometryToolsKt.getTileYByGeometry(polygon, tileY)

                //读取数据库获取数据
                val list = mDataBase.elementDao.findList(tileX, tileY)
                val elements = java.util.ArrayList<Element>()

                //几何遍历判断数据包括或者相交
                list.forEach { element ->
                    if (element != null && element.geometry != null) {
                        val geometryTemp = GeometryTools.createGeometry(element.geometry)
                        if (geometryTemp != null && geometry.contains(geometryTemp) || geometry.intersects(
                                geometryTemp
                            )
                        ) {
                            elements.add(element)
                        }
                    }
                }
                var cursor: Cursor? = null
                try {
                    for (e in elements) {
//                        val layer: LayerManager =
//                            mDataBase.layerManagerDao.findLayerManager(e.layerId)
                        e.values = mutableMapOf<String, String>()
//                        MoshiUtil.fromJson<List<CustomLayerItem>>(layer.bundle)
                        cursor =
                            mDataBase.openHelper.readableDatabase.query("select * from \"${e.layerId}\" where uuid = \"${e.id}\"")
                        while (cursor.moveToNext()) {
                            for (index in 0 until cursor.columnNames.size) {
                                val key = cursor.getColumnName(index)
                                val value = cursor.getString(index)
                                e.values[key] = value
                            }

                        }
                    }
                } catch (e: Throwable) {
                    e.message?.let { Log.e("jingo", it) }
                } finally {
                    cursor?.close()
                }
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(elements)
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(listOfNotNull())
                }
            }
        }
    }

    /**
     * 查询数据深度信息模板
     */
    fun queryElementDeepInfo(
        id: String,
        layerId: String,
        callback: (layer: LayerManager?, itemList: List<CustomLayerItem>?) -> Unit
    ) {
        thread(start = true) {
            val layerManager = mDataBase.layerManagerDao.findLayerManager(layerId)
            var layerItems = listOf<CustomLayerItem>()
            if (layerManager != null) {
                layerItems = DataConversion.jsonToLayerItemsList(layerManager.bundle)
                var cursor: Cursor? = null
                try {
                    cursor =
                        mDataBase.openHelper.readableDatabase.query("select * from \"$layerId\" where uuid = \"$id\"")
                    while (cursor.moveToNext()) {
                        for (layerItem in layerItems) {
                            val index = cursor.getColumnIndex(layerItem.key)
                            when (layerItem.type) {
                                DataLayerItemTypeInput -> {
//                                    layerItem.value = cursor.getString(index)
                                }
                                DataLayerItemTypeInputArray -> TODO()
                                DataLayerItemTypeText -> TODO()
                                DataLayerItemTypeSingleSelection -> {
//                                    layerItem.value = cursor.getString(index)
                                }
                                DataLayerItemTypeMultipleSelection -> TODO()
                            }
                        }
                    }
                } catch (e: Throwable) {
                    e.message?.let { Log.e("jingo", it) }
                } finally {
                    cursor?.close()
                }
            }
            Handler(Looper.getMainLooper()).post {
                callback.invoke(layerManager, layerItems)
            }
        }
    }

//    fun queryElementByName(
//        name: String,
//        start: Int,
//        total: Int,
//        callback: (list: List<Element>) -> Unit
//    ) {
//        thread(start = true) {
//            if (name.isNotEmpty()) {
//                //读取数据库获取数据
//                val elements = mDataBase.elementDao.findListByKeyword(name, start, total)
//                var cursor: Cursor? = null
//                try {
//                    for (e in elements) {
////                        val layer: LayerManager =
////                            mDataBase.layerManagerDao.findLayerManager(e.layerId)
//                        e.values = mutableMapOf<String, String>()
////                        MoshiUtil.fromJson<List<CustomLayerItem>>(layer.bundle)
//                        cursor =
//                            mDataBase.openHelper.readableDatabase.query("select * from \"${e.layerId}\" where uuid = \"${e.id}\"")
//                        while (cursor.moveToNext()) {
//                            for (index in 0 until cursor.columnNames.size) {
//                                val key = cursor.getColumnName(index)
//                                val value = cursor.getString(index)
//                                e.values[key] = value
//                            }
//
//                        }
//                    }
//                } catch (e: Throwable) {
//                    Log.e("jingo", e.message)
//                } finally {
//                    cursor?.close()
//                }
//                Handler(Looper.getMainLooper()).post {
//                    callback.invoke(elements)
//                }
//            } else {
//                Handler(Looper.getMainLooper()).post {
//                    callback.invoke(listOfNotNull())
//                }
//            }
//        }
//    }

    fun searchData(option: SearchDataOption, callback: (list: List<Element>) -> Unit) {
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastSearchTime < 1500 && timer != null) {
            timer?.cancel()
            timer = null
        }
        timer = fixedRateTimer("", false, 1500, 10000) {
            thread(start = true) {
                var cursor: Cursor? = null
                try {
                    val db = mDataBase.openHelper.readableDatabase;

                    val elementList = mutableListOf<Element>()
                    if (option.layerItemList.isEmpty() && option.projectItemList.isEmpty() && option.fieldItemList.isEmpty()) {
                        elementList.addAll(
                            mDataBase.elementDao.findListByKeyword(
                                option.keyword,
                                option.pageNum * option.pageCapacity,
                                option.pageCapacity
                            )
                        )
                        for (e in elementList) {
                            e.values = mutableMapOf<String, String>()
                            if (cursor != null && !cursor.isClosed) {
                                cursor.close()
                            }
                            cursor =
                                db.query("select * from \"${e.layerId}\" where uuid = \"${e.id}\"")
                            while (cursor.moveToNext()) {
                                for (index in 0 until cursor.columnNames.size) {
                                    val key = cursor.getColumnName(index)
                                    val value = cursor.getString(index)
                                    e.values[key] = value
                                }
                            }
                        }
                    } else {
                        if (option.fieldItemList.isNotEmpty()) {
                            for (item in option.fieldItemList) {
                                val valuesMap = mutableMapOf<String, String>()
                                if (cursor != null && !cursor.isClosed) {
                                    cursor.close()
                                }
                                val sql =
                                    "select * from \"${item.layerId}\" where ${item.fieldName} like '%${option.keyword}%' limit ${option.pageNum * option.pageCapacity},${option.pageCapacity} "
                                cursor = db.query(sql)
                                var uuid = ""
                                while (cursor.moveToNext()) {
                                    for (index in 0 until cursor.columnNames.size) {
                                        val key = cursor.getColumnName(index)
                                        val value = cursor.getString(index)
                                        if (key == "uuid") {
                                            uuid = value;
                                        }
                                        valuesMap[key] = value
                                    }
                                    val e = mDataBase.elementDao.findById(uuid);
                                    e.values = valuesMap
                                    elementList.add(e)
                                    for (layer in option.layerItemList) {
                                        if (layer.layerId == item.layerId) {
                                            option.layerItemList.remove(layer)
                                            break
                                        }
                                    }
                                    for (project in option.projectItemList) {
                                        if (project.projectId == item.projectId) {
                                            var his = false;
                                            for (layer in option.layerItemList) {
                                                if (layer.projectId == project.projectId) {
                                                    his = true
                                                    break
                                                }
                                            }
                                            if (!his) {
                                                option.projectItemList.remove(project)
                                            }
                                            break
                                        }
                                    }
                                }
                            }
                        }

                        if (option.layerItemList.isNotEmpty()) {
                            val layerSet = RealmSet<String>();
                            for (layer in option.layerItemList) {
                                layerSet.add(layer.layerId)
                                for (project in option.projectItemList) {
                                    if (project.projectId == layer.projectId) {
                                        option.projectItemList.remove(project)
                                        break
                                    }
                                }
                            }
                            val elements = mDataBase.elementDao.findListByKeywordLimitLayer(
                                option.keyword,
                                layerSet,
                                option.pageNum * option.pageCapacity,
                                option.pageCapacity
                            )
                            for (e in elements) {
                                e.values = mutableMapOf<String, String>()
                                if (cursor != null && !cursor.isClosed) {
                                    cursor.close()
                                }
                                cursor =
                                    db.query("select * from \"${e.layerId}\" where uuid = \"${e.id}\"")
                                while (cursor.moveToNext()) {
                                    for (index in 0 until cursor.columnNames.size) {
                                        val key = cursor.getColumnName(index)
                                        val value = cursor.getString(index)
                                        e.values[key] = value
                                    }
                                }
                            }
                            elementList.addAll(elements)
                        }
                        if (option.projectItemList.isNotEmpty()) {
                            val projectSet = RealmSet<String>();
                            for (project in option.projectItemList) {
                                projectSet.add(project.projectId)
                            }
                            val elements = mDataBase.elementDao.findListByKeywordLimitProject(
                                option.keyword,
                                projectSet,
                                option.pageNum * option.pageCapacity,
                                option.pageCapacity
                            )
                            elementList.addAll(elements)
                            for (e in elementList) {
                                e.values = mutableMapOf<String, String>()
                                if (cursor != null && !cursor.isClosed) {
                                    cursor.close()
                                }
                                cursor =
                                    db.query("select * from \"${e.layerId}\" where uuid = \"${e.id}\"")
                                while (cursor.moveToNext()) {
                                    for (index in 0 until cursor.columnNames.size) {
                                        val key = cursor.getColumnName(index)
                                        val value = cursor.getString(index)
                                        e.values[key] = value
                                    }
                                }
                            }
                        }
                    }

                    Handler(Looper.getMainLooper()).post {
                        mListener?.onGetElementResult(elementList)
//                        callback.invoke(elementList)
                    }
                } catch (e: Exception) {
                    println(e)
                } finally {
                    cursor?.close()
                }

            }
            timer?.cancel();
            timer = null;
        }

    }

    /**
     * 获取所有检查项标签
     */
    fun queryCheckManagerList(callback: (list: List<CheckManager>) -> Unit) {
        thread(start = true) {
            val list = mDataBase.checkManagerDao.findList()
            Handler(Looper.getMainLooper()).post {
                callback.invoke(list)
            }
        }
    }
}

