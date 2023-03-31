package com.navinfo.omqs.bean

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey


open class OfflineMapCityRealmObject(): RealmObject() {
    @PrimaryKey
    var id: String = ""
    var fileName: String=""
    var name: String = ""
    var url: String = ""
    var version: Long = 0
    var fileSize: Long = 0
    var currentSize:Long = 0
    var status:Int = 0
}