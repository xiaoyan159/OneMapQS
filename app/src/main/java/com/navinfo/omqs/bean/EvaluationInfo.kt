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
    val linkStatus: Int = 0,//Link状态

    @SerializedName("markId")
    val markId: String = "",//Link状态

    @SerializedName("trackPhotoNumber")
    val trackPhotoNumber: String = "",//轨迹照片编号 多个分号隔开

    @SerializedName("markGeometry")
    val markGeometry: String = "",//MARK_几何坐标

    @SerializedName("featureName")
    val featureName: String = "",//问题类型

    @SerializedName("problemType")
    val problemType: String = "",//问题现象 0错误 1多余 2遗漏  服务字段定义为Integer，使用包装类，对应无值情况为空

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
    val evaluationWay: String = "2",//测评方式 1生产测评 2现场测评 服务字段定义为Integer，使用包装类，对应无值情况为空

    @SerializedName("roadClassfcation")
    val roadClassfcation: String = "",//道路种别

    @SerializedName("roadFunctionGrade")
    val roadFunctionGrade: String = "",//道路功能等级

    @SerializedName("noEvaluationreason")
    val noEvaluationreason: String = "",//未测评原因

    @SerializedName("linkLength")
    val linkLength: Double = 0.0,//link长度(m 保留3位小数)

    @SerializedName("dataLevel")
    val dataLevel: String = "",//数据级别

    @SerializedName("linstringLength")
    val linstringLength: Double = 0.0,//错误要素长度（m）
) : Parcelable

