package com.navinfo.omqs.bean

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class EvaluationInfo(
    @SerializedName("evaluationTaskId")
    val evaluationTaskId: String = "",//测评任务id
    @SerializedName("linkPid")
    val linkPid: String = "",//Link号

    @SerializedName("linkStatus")
    val linkStatus: String = "",//Link状态

    @SerializedName("markId")
    val markId: String = "",//Link状态

    @SerializedName("trackPhotoNumber")
    val trackPhotoNumber: String = "",//轨迹照片编号 多个分号隔开

    @SerializedName("markGeometry")
    val markGeometry: String = "",//MARK_几何坐标

    @SerializedName("featureName")
    val featureName: String = "",//问题类型

    @SerializedName("problemType")
    val problemType: String = "",//问题现象

    @SerializedName("problemPhenomenon")
    val problemPhenomenon: String = "",//问题现象

    @SerializedName("problemDesc")
    val problemDesc: String = "",//问题描述

    @SerializedName("problemLink")
    val problemLink: String = "",//问题环节

    @SerializedName("problemReason")
    val problemReason: String = "",//问题原因

    @SerializedName("evaluatorName")
    val evaluatorName: String = "",//测评人名称

    @SerializedName("evaluationDate")
    val evaluationDate: String = "",//测评日期(yyyy-mm-dd)

    @SerializedName("evaluationWay")
    val evaluationWay: String = "现场测评"//测评方式
) : Parcelable

