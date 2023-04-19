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