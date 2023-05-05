package com.navinfo.omqs.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SignBean(
    //图标ID
    val iconId: Int,
    val distance: Int = 0,
    val iconText: String = "",
    val elementId: String = "",
    val linkId: String,
    val geometry: String,
) : Parcelable