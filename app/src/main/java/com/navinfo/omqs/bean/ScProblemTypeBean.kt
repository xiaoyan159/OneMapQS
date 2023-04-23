package com.navinfo.omqs.bean

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "ScProblemType")
@Parcelize
data class ScProblemTypeBean(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    /**
     * elementType
     * 要素类型
     */
    @ColumnInfo("ELEMENT_TYPE")
    val elementType: String = "",
    /**
     * 要素代码
     */
    @ColumnInfo("ELEMENT_CODE")
    val elementCode: String = "",
    /**
     * 问题分类
     */
    @ColumnInfo("CLASS_TYPE")
    val classType: String = "",

    /**
     * 问题类型
     */
    @ColumnInfo("TYPE")
    val problemType: String = "",

    /**
     * 问题现象
     */
    @ColumnInfo("PHENOMENON")
    val phenomenon: String = ""

) : Parcelable