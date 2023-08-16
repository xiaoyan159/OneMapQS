package com.navinfo.collect.library.data.entity

import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import io.realm.RealmDictionary
import io.realm.RealmObject
import io.realm.RealmSet
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import org.locationtech.jts.geom.Geometry
import java.util.*

/**
 * 渲染要素对应的实体
 * */
open class ReferenceEntity() : RealmObject() {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString() // id
    var renderEntityId: String = "" // 参考的renderEntity的Id
    lateinit var name: String //要素名
    lateinit var table: String //要素表名
    var code: String = "0" // 要素编码
    var zoomMin: Int = 18 //显示最小级别
    var zoomMax: Int = 23 //显示最大级别
    var taskId: Int = 0 //任务ID
    var enable:Int = 0 // 默认0不是显示 1为渲染显示

    var geometry: String = "" // 要素渲染参考的geometry，该数据可能会在导入预处理环节被修改，原始geometry会保存在properties的geometry字段下
        get() {
            wkt = GeometryTools.createGeometry(field)
            return field
        }
        set(value) {
            field = value
            // 根据geometry自动计算当前要素的x-tile和y-tile
            GeometryToolsKt.getTileXByGeometry(value, tileX)
            GeometryToolsKt.getTileYByGeometry(value, tileY)
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
    var properties: RealmDictionary<String> = RealmDictionary()
    var tileX: RealmSet<Int> = RealmSet() // x方向的tile编码
    var tileY: RealmSet<Int> = RealmSet()  // y方向的tile编码

    constructor(name: String): this() {
        this.name = name
    }
}