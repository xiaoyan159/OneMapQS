package com.navinfo.collect.library.data.entity

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class NavInfo @JvmOverloads constructor(
    @PrimaryKey
    var id: Int = 0,
    /**
     * 起点link
     */
    var naviStartLinkId: String = "",

    /**
     * 终点link
     */
    var naviEndLinkId: String = "",

    /**
     * 起点NodeId
     */
    var naviStartNode: String = "",

    var naviEndNode: String = "",

    ) : RealmObject() {
}