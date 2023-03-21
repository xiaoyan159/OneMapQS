package com.navinfo.collect.library.data.dao.impl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.navinfo.collect.library.data.entity.CheckManager

@Dao
interface ICheckManagerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg check: CheckManager?)

    @Query("SELECT * FROM CheckManager where id =:id")
    fun findCheckManagerById(id: Long): CheckManager?

    @Query("SELECT * FROM CheckManager")
    fun findList(): List<CheckManager>
}