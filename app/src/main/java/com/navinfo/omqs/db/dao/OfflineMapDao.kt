package com.navinfo.omqs.db.dao

import androidx.room.*
import com.navinfo.omqs.bean.OfflineMapCityBean

@Dao
interface OfflineMapDao {

    @Insert
    suspend fun insert(message: OfflineMapCityBean): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(message: OfflineMapCityBean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(list: List<OfflineMapCityBean>)

    @Query("select * from OfflineMapCity order by id")
    suspend fun getOfflineMapList(): List<OfflineMapCityBean>

    @Query("select * from OfflineMapCity where status != 0 order by id")
    suspend fun getOfflineMapListWithOutNone(): List<OfflineMapCityBean>
}