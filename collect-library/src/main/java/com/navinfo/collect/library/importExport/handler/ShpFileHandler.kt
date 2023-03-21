package com.navinfo.collect.library.importExport.handler

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.navinfo.collect.library.data.DataConversion
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.entity.TileElement
import com.navinfo.collect.library.utils.GeometryToolsKt
import com.navinfo.collect.library.utils.StringUtil
import io.realm.RealmSet
import org.gdal.gdal.gdal
import org.gdal.ogr.*
import org.gdal.osr.SpatialReference
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread

open class ShpFileHandler(context: Context, dataBase: MapLifeDataBase) :
    ImportExportBaseHandler(context, dataBase) {
    val strDriverName = "ESRI Shapefile"

    init {
        Log.e("jingo", "ShpFileHandler 初始化！！！！！")
        ogr.RegisterAll()
        gdal.AllRegister()
        gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES")
        // 为了使属性表字段支持中文，请添加下面这句
        gdal.SetConfigOption("SHAPE_ENCODING", "CP936")
    }

    fun getImportShpFileInfo(path: String, callback: (bSuccess: Boolean, message: String) -> Unit) {
        try {
            val file: File = File(path);
            if (file.exists()) {
                //创建一个文件，根据strDriverName扩展名自动判断驱动类型
                val oDriver: org.gdal.ogr.Driver = ogr.GetDriverByName(strDriverName)
                if (oDriver == null) {
                    Log.e("jingo", "$strDriverName 驱动不可用！");
                    callback(false, "$strDriverName 驱动不可用！")
                }
                val dataSource: DataSource = oDriver.Open(path)
                val layer: Layer = dataSource.GetLayer(0)
//                for (i in 0 until dataSource.GetLayerCount()) {
                val layerItem = dataSource.GetLayer(0);
//                Log.e("jingo", "图层名称：${layerItem.GetName()}")
                //空间参考坐标系
                // val spatialReference:SpatialReference = layerItem.GetSpatialRef()
                //图层范围
                // val layerExtent: DoubleArray = layerItem.GetExtent()
//                }
                val geomType = getGeomType(layerItem.GetGeomType())
                Log.e("jingo", "图层数据类型：${layerItem.GetGeomType()}")
                //获取图层信息
                val featureDefn: FeatureDefn = layer.GetLayerDefn()
                val fieldCount = featureDefn.GetFieldCount()
                val stringBuffer = StringBuffer()
                for (i in 0 until fieldCount) {
                    val fieldDefn: FieldDefn = featureDefn.GetFieldDefn(i)
                    //得到属性字段类型
//                    val fieldType = fieldDefn.GetFieldType()
//                    val fieldTypeName = fieldDefn.GetFieldTypeName(fieldType)
                    //得到属性字段名称
                    val fieldName = fieldDefn.GetName()
                    if (stringBuffer.isNotEmpty()) {
                        stringBuffer.append(";")
                    }
                    stringBuffer.append(fieldName)
                }

                val jsonString =
                    "{\"layerName\":\"${layerItem.GetName()}\",\"geomType\":$geomType,\"keys\":\"$stringBuffer\"}"
                callback(true, jsonString)
//                //读取空间信息及属性列表
//                for (i in 0 until fieldCount) {
//                    val feature: Feature = layer.GetFeature(i.toLong())
//                    for ((key, value) in map) {
//                        val fvalue = feature.GetFieldAsString(value)
//                        Log.e("jingo", "属性名称：$value 属性值：$fvalue")
//                    }
//                }
            }
        } catch (e: Exception) {
            callback(false, "${e.message}")
        }
    }

    private fun getGeomType(type: Int): Int {
        when (type) {
            ogr.wkbPoint -> return 0
            ogr.wkbPolygon -> return 2
        }
        return -1
    }

    fun importShpData(
        path: String,
        mapLayerId: String,
        callback: (bSuccess: Boolean, message: String) -> Unit
    ) {
        thread(start = true) {
            try {
                val mapLayer = mDataBase.layerManagerDao.findLayerManager(mapLayerId)
                if (mapLayer != null) {

                    val mapLayerItemList = DataConversion.jsonToLayerItemsList(mapLayer.bundle)
                    var mainName = ""
//                    for (item in mapLayerItemList) {
//                        if (item.isMainName) {
//                            mainName = item.key
//                            break
//                        }
//                    }
                    val file = File(path);
                    if (file.exists()) {
                        val oDriver: org.gdal.ogr.Driver = ogr.GetDriverByName(strDriverName)
                        if (oDriver == null) {
                            Log.e("jingo", "$strDriverName 驱动不可用！");
                            callback(false, "$strDriverName 驱动不可用！")
                        }
                        val dataSource: DataSource = oDriver.Open(path)
                        val layer: Layer = dataSource.GetLayer(0)
                        val layerItem = dataSource.GetLayer(0)
                        //获取图层信息
                        val featureDefn: FeatureDefn = layer.GetLayerDefn()
                        val fieldCount = featureDefn.GetFieldCount()
                        val db = mDataBase.openHelper.readableDatabase
                        db.beginTransaction()
                        for (i in 0 until layer.GetFeatureCount()) {
                            val feature = layer.GetFeature(i)
                            val geometry = feature.GetGeometryRef()
                            val stringNameBuffer = StringBuffer()
                            val stringValueBuffer = StringBuffer()
                            var displayText = ""
                            for (i in 0 until fieldCount) {
                                val fieldDefn: FieldDefn = featureDefn.GetFieldDefn(i)
                                //得到属性字段名称
                                val fieldName = fieldDefn.GetName()
                                val fieldValue = feature.GetFieldAsString(fieldName)
                                stringNameBuffer.append(",'")
                                stringNameBuffer.append(fieldName)
                                stringNameBuffer.append("'")

                                stringValueBuffer.append(",'")
                                stringValueBuffer.append(fieldValue)
                                stringValueBuffer.append("'")
                                if (mainName == fieldName) {
                                    displayText = fieldValue
                                }
                            }
                            val uuid = StringUtil.createUUID()
                            val wkt = geometry.ExportToWkt()
                            val nowTime = StringUtil.getYYYYMMDDHHMMSSS();
                            val masterPoint = GeometryToolsKt.getMasterPoint(wkt)
                            var jsonObject = JSONObject(mapLayer.style)
                            jsonObject.put(
                                "masterPoint",
                                masterPoint
                            )
                            val style = jsonObject.toString()
                            val sql =
                                "insert into '$mapLayerId' ('uuid'${stringNameBuffer}) values ('$uuid'${stringValueBuffer})"
                            val sql2 =
                                "insert into 'element' ('layer_id','display_style','display_text','visibility','export_time','t_lifecycle','t_status','geometry','uuid','start_level','end_level','zindex','operation_time') values ('$mapLayerId','$style','$displayText','0','$nowTime','2','1','${wkt}','$uuid',0,0,0,'$nowTime')"
                            Log.e("jingo",sql)
                            Log.e("jingo",sql2)
                            db.execSQL(sql)
                            db.execSQL(sql2)
                            if (geometry != null) {
                                val tileX = RealmSet<Int>()
                                GeometryToolsKt.getTileXByGeometry(wkt, tileX)
                                val tileY = RealmSet<Int>()
                                GeometryToolsKt.getTileYByGeometry(wkt, tileY)

                                //遍历存储tile对应的x与y的值
                                tileX.forEach { x ->

                                    tileY.forEach { y ->
                                        val sql3 =
                                            "insert into 'tileElement' ('tilex','tiley','element_uuid','uuid') values ('$x','$y','$uuid','${StringUtil.createUUID()}')"
                                        db.execSQL(sql3)
                                    }
                                }
                            }
                        }
                        db.setTransactionSuccessful()
                        db.endTransaction()
                        Handler(Looper.getMainLooper()).post {
                            callback.invoke(true, "SHP导入成功")
                        }
                    }
                }else{
                    Handler(Looper.getMainLooper()).post {
                        callback.invoke(false, "没有对应的图层")
                    }
                }
            } catch (e: Exception) {
                Log.e("导入数据出错", e.toString());
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(false, "SHP导入失败")
                }
            }
        }
    }
}