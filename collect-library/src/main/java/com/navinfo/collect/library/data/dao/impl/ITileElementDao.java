package com.navinfo.collect.library.data.dao.impl;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.navinfo.collect.library.data.entity.LayerManager;
import com.navinfo.collect.library.data.entity.TileElement;
import java.util.List;

@Dao
public interface ITileElementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TileElement... tileElements);

    @Update
    int update(TileElement[] tileElements);

    @Delete
    int delete(TileElement tileElements);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserts(TileElement[] tileElements);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertList(List<TileElement> tileElementList);

    @Query("DELETE FROM tileElement")
    void deleteAll();

    @Query("DELETE FROM tileElement where element_uuid =:elementId")
    void deleteElementId(String elementId);

    @Query("DELETE FROM tileElement where rowId>:start and rowId<:end")
    void deleteAll(int start,int end);

    @Query("SELECT * FROM tileElement where element_uuid =:elementId")
    List<TileElement> getElementIdList(String elementId);
}
