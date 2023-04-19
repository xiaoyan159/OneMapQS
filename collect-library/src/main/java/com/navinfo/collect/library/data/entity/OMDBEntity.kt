package com.navinfo.collect.library.data.entity

import io.realm.RealmDictionary
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class OMDBEntity(): RealmObject() {
    @PrimaryKey
    var id: Long = 0
    lateinit var table: String
    lateinit var properties: RealmDictionary<String?>

    constructor(table: String, properties: RealmDictionary<String?>): this() {
        this.table = table
        this.properties = properties
    }
}

class HAD_LINK() {
    var LINK_PID: String = ""
    var MESH: String = ""
    var S_NODE_PID: String = ""
    var E_NODE_PID: String = ""
    var GEOMETRY: String = ""
}

class HAD_LINK_KIND() {
    var LINK_PID: String = ""
    var MESH: String = ""
    var KIND: Int = 0
    var GEOMETRY: String = ""
}

class HAD_LINK_DIRECT() {
    var LINK_PID: String = ""
    var MESH: String = ""
    var DIRECT: Int = 0
    var GEOMETRY: String = ""
}

class HAD_SPEEDLIMIT() {
    var SPEED_ID: String = ""
    var MESH: String = ""
    var LINK_PID: String = ""
    var GEOMETRY: String = ""
    var DIRECT: Int = 0
    var SPEED_FLAG: Int = 0
    var MAX_SPEED: Int = 0
    var MIN_SPEED: Int = 0
}

class HAD_SPEEDLIMIT_COND() {
    var SPEED_COND_ID: String = ""
    var MESH: String = ""
    var LINK_PID: String = ""
    var GEOMETRY: String = ""
    var DIRECT: Int = 0
    var SPEED_FLAG: Int = 0
    var MAX_SPEED: Int = 0
    var SPEED_DEPENDENT: Int = 0
    var VEHICLE_TYPE: Int = 0
    var VALID_PERIOD: String = ""
}

class HAD_SPEEDLIMIT_VAR() {
    var SPEED_VAR_ID: String = ""
    var MESH: String = ""
    var LINK_PID: String = ""
    var GEOMETRY: String = ""
    var DIRECT: Int = 0
    var LOCATION: String = ""
}
