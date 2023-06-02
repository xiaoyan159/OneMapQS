package com.navinfo.omqs.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SignBean(
    //图标ID
    var iconId: Int = 0,
    //定位点到目标距离
    val distance: Int = 0,
    //左上图标中的文字
    val iconText: String = "",
    //绑定的要素id
    val elementId: String = "",
    //绑定的linkid
    val linkId: String,
    //坐标
    val geometry: String,
    //名称
    val name: String,
    //底部右侧文字
    val bottomRightText: String = "",
    //要素code类型
    val elementCode: Int,
    //需要展示更多的内容
    val moreText: String = "",
    //左上角信息
    val topRightText: String = ""
) : Parcelable