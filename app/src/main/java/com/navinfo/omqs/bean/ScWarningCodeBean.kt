package com.navinfo.omqs.bean

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "ScWarningCode")
@Parcelize
data class ScWarningCodeBean(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    /**
     * code
     * 编码
     */
    @ColumnInfo("CODE")
    val code: String = "",
    /**
     * 描述
     */
    @ColumnInfo("DESCRIBE")
    val describe: String = "",

) : Parcelable