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
     * 问题分类
     */
    @ColumnInfo("CLASS_TYPE")
    var classType: String = "",
    /**
     * 问题类型
     */
    @ColumnInfo("TYPE")
    var problemType: String = "",
    /**
     * 问题现象
     */
    @ColumnInfo("PHENOMENON")
    var phenomenon: String = ""

) : Parcelable