package com.navinfo.omqs.bean

import io.realm.RealmObject
import io.realm.annotations.RealmClass

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
    var geometry: String = ""

) : RealmObject()