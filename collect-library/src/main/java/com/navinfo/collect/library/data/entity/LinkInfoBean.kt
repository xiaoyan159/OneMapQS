package com.navinfo.collect.library.data.entity

import io.realm.RealmObject

/**
 * 道路信息
 */
open class LinkInfoBean @JvmOverloads constructor(
    /**
     * 种别
     */
    var kind: Int = 0,
    /**
     * 功能等级
     */
    var functionLevel: Int = 0,
    /**
     * 数据的等级
     */
    var dataLevel: Int = 0,
    /**
     * 长度（米）
     */
    var length: Double = 0.000,
    /**
     * 备注信息
     */
    var description: String = ""
) : RealmObject()
