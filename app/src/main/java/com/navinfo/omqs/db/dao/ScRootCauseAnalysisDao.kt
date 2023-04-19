package com.navinfo.omqs.db.dao

import androidx.room.*
import com.navinfo.omqs.bean.ScRootCauseAnalysisBean


@Dao
interface ScRootCauseAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: List<ScRootCauseAnalysisBean>)


    @Query("delete from ScRootCauseAnalysis")
    suspend fun deleteAll()

    @Transaction
    suspend fun insertOrUpdateList(list: List<ScRootCauseAnalysisBean>) {
        //先删除
        deleteAll()
        //后插入
        insertList(list)
    }

    /**
     * 获取问题环节数据
     */
    @Query("select * from ScRootCauseAnalysis order by PROBLEM_LINK")
    suspend fun findAllData(): List<ScRootCauseAnalysisBean>?

}