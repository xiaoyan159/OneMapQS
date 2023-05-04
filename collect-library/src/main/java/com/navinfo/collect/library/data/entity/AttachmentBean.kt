package com.navinfo.collect.library.data.entity

import io.realm.RealmObject

open class AttachmentBean @JvmOverloads constructor(
    /**
     * 文件名称
     */
    var name: String = "",
    /**
     * 默认0  照片 录音1
     */
    var type: Int = 0

) : RealmObject()