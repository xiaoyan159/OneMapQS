package com.navinfo.collect.library.data.entity

import android.os.Parcelable
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.navinfo.collect.library.system.Constant
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import com.navinfo.collect.library.utils.StrZipUtil
import io.realm.RealmDictionary
import io.realm.RealmObject
import io.realm.RealmSet
import io.realm.annotations.Ignore
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.oscim.core.MercatorProjection
import java.util.*
import java.util.zip.GZIPInputStream

/**
 * 渲染要素对应的实体
 * */
@Parcelize
open class RenderEntity() : RealmObject(), Parcelable {
    //    @PrimaryKey
//    var id: String = UUID.randomUUID().toString() // id
    lateinit var name: String //要素名
    lateinit var table: String //要素表名
    var code: String = "0" // 要素编码
    var propertiesDb: String = ""
    var geometry: String =
        "" // 要素渲染参考的geometry，该数据可能会在导入预处理环节被修改，原始geometry会保存在properties的geometry字段下
        get() {
            wkt = GeometryTools.createGeometry(field)
            return field
        }
        //        get() {
//            if (geometryDb != null && geometryDb.isNotEmpty() && field.isEmpty()) {
//                field = StrZipUtil.uncompress(geometryDb)
//            }
//            return field
//        }
        set(value) {
            field = value
//            geometryDb = StrZipUtil.compress(value)
            // 根据geometry自动计算当前要素的x-tile和y-tile
            GeometryToolsKt.getTileXByGeometry(value, tileX)

            tileXMin = tileX.min()
            tileXMax = tileX.max()

            GeometryToolsKt.getTileYByGeometry(value, tileY)

            tileYMin = tileY.min()
            tileYMax = tileY.max()

            // 根据传入的geometry文本，自动转换为Geometry对象
            try {
                wkt = GeometryTools.createGeometry(value)
            } catch (e: Exception) {

            }
        }

    @Ignore
    var wkt: Geometry? = null
        get() {
            if (field == null || field!!.isEmpty) {
                try {
                    field = GeometryTools.createGeometry(geometry)
                } catch (e: Exception) {
                    Log.e("jingo","RenderEntity 转 wkt失败 $e")
                }
            }
            return field
        }

    @Ignore
    var properties: RealmDictionary<String> = RealmDictionary()
        get() {
            if (propertiesDb != null && propertiesDb.isNotEmpty() && field.isEmpty()) {
                try {
                    val gson = Gson()
                    val type = object : TypeToken<RealmDictionary<String>>() {}.type
                    field = gson.fromJson(StrZipUtil.uncompress(propertiesDb), type)
                } catch (e: Exception) {
                    Log.e("jingo","RenderEntity 转 properties $e")
                }
            }
            return field
        }


    @Ignore
    var tileX: RealmSet<Int> = RealmSet() // x方向的tile编码

    @Ignore
    var tileY: RealmSet<Int> = RealmSet()  // y方向的tile编码
    var tileXMin: Int = 0
    var tileXMax: Int = 0
    var tileYMin: Int = 0
    var tileYMax: Int = 0
    var taskId: Int = 0 //任务ID
    var zoomMin: Int = 18 //显示最小级别
    var zoomMax: Int = 23 //显示最大级别
    var enable: Int = 0 // 默认0不是显示 1为渲染显示 2为常显
    var catchEnable: Int = 0 // 0不捕捉 1捕捉

    var linkPid: String = "" // RenderEntity关联的linkPid集合(可能会关联多个)
    var linkRelation: LinkRelation? = null

    constructor(name: String) : this() {
        this.name = name
    }
}