package com.navinfo.collect.library.data.entity

import android.os.Parcelable
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import io.realm.RealmDictionary
import io.realm.RealmObject
import io.realm.RealmSet
import io.realm.annotations.Ignore
import io.realm.annotations.Index
import kotlinx.parcelize.Parcelize
import org.locationtech.jts.geom.Geometry

/**
 * 渲染要素对应的实体
 * */
@Parcelize
open class RenderEntity1() : RealmObject(), Parcelable {
    lateinit var name: String //要素名
    lateinit var table: String //要素表名
    var code: String = "0" // 要素编码
    var geometry: String = ""
    var propertiesDb: String = ""
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