package com.navinfo.collect.library.data.entity

import android.os.Parcelable
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.navinfo.collect.library.utils.DeflaterUtil
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import com.navinfo.collect.library.utils.StrZipUtil
import io.realm.RealmDictionary
import io.realm.RealmObject
import io.realm.RealmSet
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.locationtech.jts.geom.Geometry
import java.util.*

/**
 * 渲染要素对应的实体
 * */
@Parcelize
open class ReferenceEntity() : RealmObject(), Parcelable {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString() // id
    lateinit var name: String //要素名
    lateinit var table: String //要素表名
    var propertiesDb: String = ""
    var code: String = "0" // 要素编码

    var zoomMin: Int = 18 //显示最小级别

    var zoomMax: Int = 23 //显示最大级别
    var taskId: Int = 0 //任务ID
    var enable: Int = 0 // 默认0不是显示 1为渲染显示
    var tileXMin: Int = 0
    var tileXMax: Int = 0
    var tileYMin: Int = 0
    var tileYMax: Int = 0
    var geometry: String =
        "" // 要素渲染参考的geometry，该数据可能会在导入预处理环节被修改，原始geometry会保存在properties的geometry字段下
        get() {
            wkt = GeometryTools.createGeometry(field)
            return field
        }
        set(value) {
            field = value
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

                }
            }
            return field
        }

    @Ignore
    var properties: RealmDictionary<String> = RealmDictionary()
        get() {
            if (propertiesDb != null && propertiesDb!!.isNotEmpty() && field.isEmpty()) {
                try {
                    val gson = Gson()
                    val type = object : TypeToken<RealmDictionary<String>>() {}.type
                    field = gson.fromJson(DeflaterUtil.unzipString(propertiesDb), type)
                } catch (e: Exception) {
                    Log.e("jingo","ReferenceEntity 转 properties $e")
                }
            }
            return field
        }

    @Ignore
    var tileX: RealmSet<Int> = RealmSet() // x方向的tile编码

    @Ignore
    var tileY: RealmSet<Int> = RealmSet()  // y方向的tile编码

    constructor(name: String) : this() {
        this.name = name
    }
}