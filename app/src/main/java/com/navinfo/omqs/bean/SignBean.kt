package com.navinfo.omqs.bean

import android.os.Parcelable
import com.navinfo.collect.library.data.entity.RenderEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class SignBean(
    //图标ID
    var iconId: Int = 0,
    //定位点到目标距离
    val distance: Int = 0,
    //左上图标中的文字
    val iconText: String = "",
    //绑定的linkid
    val linkId: String,
    //名称
    val name: String,
    //是否要展示详细信息
    val isMoreInfo: Boolean = false,
    //底部右侧文字
    val bottomRightText: String = "",
    //捕捉数据
    val renderEntity: RenderEntity,
    //道路信息排序用的字段
    val index: Int = 0
) : Parcelable