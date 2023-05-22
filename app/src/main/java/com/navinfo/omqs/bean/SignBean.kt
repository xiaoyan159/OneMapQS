package com.navinfo.omqs.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SignBean(
    //图标ID
    val iconId: Int,
    //定位点到目标距离
    val distance: Int = 0,
    //图表中的问题
    val iconText: String = "",
    //绑定的要素id
    val elementId: String = "",
    //绑定的linkid
    val linkId: String,
    //坐标
    val geometry: String,
    //底部文字
    val bottomText: String,
    //底部右侧文字
    val bottomRightText: String,
    //要素code类型
    val elementCode: Int
) : Parcelable