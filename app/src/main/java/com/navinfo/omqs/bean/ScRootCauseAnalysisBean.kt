package com.navinfo.omqs.bean

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "ScRootCauseAnalysis")
@Parcelize
data class ScRootCauseAnalysisBean(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    /**
     * 问题环节
     */
    @ColumnInfo("PROBLEM_LINK")
    var problemLink: String = "",
    /**
     * 问题原因
     */
    @ColumnInfo("PROBLEM_CAUSE")
    var problemCause: String = "",
) : Parcelable