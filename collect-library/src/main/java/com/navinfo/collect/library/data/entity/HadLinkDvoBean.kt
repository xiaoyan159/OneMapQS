package com.navinfo.collect.library.data.entity

import io.realm.RealmObject

open class HadLinkDvoBean @JvmOverloads constructor(
    /**
     * 图幅号
     */
    var mesh: String = "",
    /**
     * linkPid
     */
    var linkPid: String = "",
    /**
     * (几何)加偏后
     */
    var geometry: String = "",

    /**
     * 不作业原因
     */
    var reason: String = ""

) : RealmObject()