package com.navinfo.omqs.db.dao

import androidx.room.*
import com.navinfo.omqs.bean.ScProblemTypeBean


@Dao
interface ScProblemTypeDao {
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insert(bean: ScProblemTypeBean): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: List<ScProblemTypeBean>)


    @Query("delete from ScProblemType")
    suspend fun deleteAll()

    /**
     * 更新整个数据库表，由于没有
     */
    @Transaction
    suspend fun insertOrUpdateList(list: List<ScProblemTypeBean>) {
        //先删除
        deleteAll()
        //后插入
        insertList(list)
    }

    /**
     * 获取问题分类，并去重
     */
    @Query("select DISTINCT CLASS_TYPE from ScProblemType order by CLASS_TYPE")
    suspend fun findClassTypeList(): List<String>?

    @Query("select DISTINCT CLASS_TYPE from ScProblemType where ELEMENT_CODE=:code")
    suspend fun findClassTypeByCode(code: Int): String?

    /**
     * 获取问题类型，并去重
     */
    @Query("select * from ScProblemType where CLASS_TYPE=:type order by TYPE")
    suspend fun findProblemTypeList(type: String): List<ScProblemTypeBean>?

//    /**
//     *
//     */
//    @Query("select PHENOMENON from ScProblemType where CLASS_TYPE=:classType and TYPE=:type order by PHENOMENON")
//    suspend fun getPhenomenonList(classType: String, type: String): List<String>

}