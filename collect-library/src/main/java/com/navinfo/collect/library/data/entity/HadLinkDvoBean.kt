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
    var reason: String = "",

    /**
     * 1:源库link， 3：现象新增
     */
    var linkStatus: Int = 1,

    /**
     * link 长度
     */
    var linkLength: Double = 0.000
) : RealmObject()