package com.navinfo.collect.library.data.entity

import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class OfflineMapCityRealmObject: RealmModel {
    @PrimaryKey
    var id: String = ""
    var fileName: String=""
    var name: String = ""
    var url: String = ""
    var version: Long = 0
    var fileSize: Long = 0
    var currentSize:Long = 0
    var status:Int = 0

    constructor(){

    }

    constructor(
        id: String,
        fileName: String,
        name: String,
        url: String,
        version: Long,
        fileSize: Long,
        currentSize: Long,
        status: Int
    ) {
        this.id = id
        this.fileName = fileName
        this.name = name
        this.url = url
        this.version = version
        this.fileSize = fileSize
        this.currentSize = currentSize
        this.status = status
    }
}