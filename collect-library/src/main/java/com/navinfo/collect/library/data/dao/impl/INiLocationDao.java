package com.navinfo.collect.library.data.dao.impl;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.navinfo.collect.library.data.entity.Element;
import com.navinfo.collect.library.data.entity.NiLocation;
import java.util.List;
import java.util.Set;

@Dao
public interface INiLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NiLocation... niLocations);

    @Update
    int update(NiLocation niLocation);

    @Update
    int updateList(NiLocation[] niLocations);

    @Delete
    int delete(NiLocation niLocations);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserts(NiLocation[] niLocations);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertList(List<NiLocation> niLocationList);

    @Query("DELETE FROM niLocation")
    void deleteAll();

    @Query("SELECT * FROM niLocation where uuid=:id ")
    NiLocation find(String id);

    @Query("SELECT * FROM niLocation where tilex>=:minx and tilex<=:maxx and tiley>=:miny and tiley <=:maxy")
    List<NiLocation> findList(int minx, int maxx, int miny, int maxy);

    @Query("SELECT * FROM niLocation where tilex>=:minx and tilex<=:maxx and tiley>=:miny and tiley <=:maxy and time>=:startTime and time<=:endTime")
    List<NiLocation> timeTofindList(int minx, int maxx, int miny, int maxy,long startTime,long endTime);

    @Query("SELECT * FROM niLocation")
    List<NiLocation> findAll();
}
