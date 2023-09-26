package com.navinfo.omqs.db.dao

import androidx.room.*
import com.navinfo.omqs.bean.ScProblemTypeBean
import com.navinfo.omqs.bean.ScWarningCodeBean


@Dao
interface ScWarningCodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: List<ScWarningCodeBean>)


    @Query("delete from ScWarningCode")
    suspend fun deleteAll()

    /**
     * 更新整个数据库表，由于没有
     */
    @Transaction
    suspend fun insertOrUpdateList(list: List<ScWarningCodeBean>) {
        //先删除
        deleteAll()
        //后插入
        insertList(list)
    }

    @Query("select DESCRIBE from ScWarningCode where CODE=:code")
    suspend fun findScWarningDescribe(code: String): String?

}