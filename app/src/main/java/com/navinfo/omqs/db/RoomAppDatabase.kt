package com.navinfo.omqs.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.navinfo.omqs.bean.OfflineMapCityBean
import com.navinfo.omqs.bean.ScProblemTypeBean
import com.navinfo.omqs.bean.ScRootCauseAnalysisBean
import com.navinfo.omqs.db.dao.OfflineMapDao
import com.navinfo.omqs.db.dao.ScProblemTypeDao
import com.navinfo.omqs.db.dao.ScRootCauseAnalysisDao

@Database(
    entities = [OfflineMapCityBean::class, ScProblemTypeBean::class, ScRootCauseAnalysisBean::class],
    version = 1,
    exportSchema = false
)
abstract class RoomAppDatabase : RoomDatabase() {
    abstract fun getOfflineMapDao(): OfflineMapDao
    abstract fun getScProblemTypeDao(): ScProblemTypeDao
    abstract fun getScRootCauseAnalysisDao(): ScRootCauseAnalysisDao
}