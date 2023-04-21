package com.navinfo.collect.library.data.entity

import com.navinfo.collect.library.system.Constant
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import io.realm.RealmDictionary
import io.realm.RealmObject
import io.realm.RealmSet
import io.realm.annotations.PrimaryKey
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.oscim.core.MercatorProjection
import java.util.*

/**
 * 渲染要素对应的实体
 * */
open class RenderEntity(): RealmObject() {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString() // id
    lateinit var name: String //要素名
    lateinit var table: String //要素表名
    var code: Int = 0 // 要素编码
    var geometry: String = ""
        get() = field
        set(value) {
            field = value
            // 根据geometry自动计算当前要素的x-tile和y-tile
            GeometryToolsKt.getTileXByGeometry(value, tileX)
            GeometryToolsKt.getTileYByGeometry(value, tileY)
        }
    var properties: RealmDictionary<String?> = RealmDictionary()
    val tileX: RealmSet<Int> = RealmSet() // x方向的tile编码
    val tileY: RealmSet<Int> = RealmSet()  // y方向的tile编码

    constructor(name: String, properties: RealmDictionary<String?>): this() {
        this.name = name
        this.properties = properties
    }
}