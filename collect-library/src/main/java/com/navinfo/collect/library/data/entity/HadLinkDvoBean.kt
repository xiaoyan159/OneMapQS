package com.navinfo.collect.library.data.entity

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class HadLinkDvoBean @JvmOverloads constructor(
    /**
     * 任务id，方便捕捉查询
     */
    var taskId: Int = 0,
    /**
     * 图幅号
     */
    var mesh: String = "",
    /**
     * linkPid
     */
    @PrimaryKey
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
     * 1:源库link，2：选择link 3：现场新增
     */
    var linkStatus: Int = 1,
    /**
     * 详细属性
     */
    var linkInfo: LinkInfoBean? = null,
    /**
     * 长度（米）
     */
    var length: Double = 0.000,
) : RealmObject()