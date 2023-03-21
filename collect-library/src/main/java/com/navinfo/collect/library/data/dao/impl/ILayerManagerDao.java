package com.navinfo.collect.library.data.dao.impl;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.navinfo.collect.library.data.entity.Element;
import com.navinfo.collect.library.data.entity.LayerManager;

import java.util.List;
import java.util.Set;

@Dao
public interface ILayerManagerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LayerManager... layerManagers);

    @Update
    int updates(LayerManager[] layerManagers);

    @Update
    int update(LayerManager layerManagers);

    @Delete
    int delete(LayerManager layerManagers);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserts(LayerManager[] layerManagers);

    @Query("DELETE FROM layerManager")
    void deleteAll();

    @Query("DELETE FROM layerManager where rowId>:start and rowId<:end")
    void deleteAll(int start, int end);

    @Query("SELECT * FROM layerManager where visibility =:visable")
    List<LayerManager> findList(int visable);

    @Query("SELECT * FROM layerManager")
    List<LayerManager> findList();

    @Query("SELECT * FROM layerManager where project_id in(:projectIds)")
    List<LayerManager> findListByProject(Set<String> projectIds);

    @Query("SELECT * FROM layerManager where uuid =:id")
    LayerManager findLayerManager(String id);

}