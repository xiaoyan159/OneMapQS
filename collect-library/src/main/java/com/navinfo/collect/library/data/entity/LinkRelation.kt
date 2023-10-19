package com.navinfo.collect.library.data.entity

import android.os.Parcelable
import com.navinfo.collect.library.system.Constant
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import io.realm.RealmDictionary
import io.realm.RealmObject
import io.realm.RealmSet
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.oscim.core.MercatorProjection
import java.util.*

/**
 * 渲染要素对应的实体
 * */
@Parcelize
open class LinkRelation() : RealmObject(), Parcelable {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString() // id
    var sNodeId: String? = null
    var eNodeId: String? = null
    var linkName: String? = null
    var direct: Int = 0

    constructor(direct: Int) : this() {
        this.direct = direct
    }
}