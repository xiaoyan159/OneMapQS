package com.navinfo.collect.library.data.dao.impl;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.navinfo.collect.library.data.entity.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Dao
public interface IElementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Element... elements);

    @Update
    int update(Element[] elements);

    @Delete
    int delete(Element elements);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserts(Element[] elements);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertList(List<Element> elementList);

    @Query("DELETE FROM element")
    void deleteAll();

    @Query("DELETE FROM element where rowId>:start and rowId<:end")
    void deleteAll(int start, int end);

    @Query("SELECT * FROM element limit :start,:count")
    List<Element> findList(int start, int count);


    @Query("SELECT * FROM element where 1=1 limit :start,:count ")
    Element[] findAll(int start, int count);


    @Query("SELECT geometry FROM element where rowId>:value limit :start,:count")
    String[] find(int value, int start, int count);

    @Query("SELECT * FROM element where uuid =:uuid")
    Element findById(String uuid);

    @Query("SELECT * FROM element where uuid in (select element_uuid from tileElement where tilex>=:minx and tilex<=:maxx and tiley>=:miny and tiley <=:maxy)")
    List<Element> findList(int minx, int maxx, int miny, int maxy);

    @Query("SELECT * FROM element where uuid in (select distinct element_uuid from tileElement where tilex IN (:xList) and tiley IN (:yList))")
    List<Element> findList(Set<Integer> xList, Set<Integer> yList);

    @Query("SELECT * FROM element where display_text like '%'||:keyword||'%' limit :start,:count")
    List<Element> findListByKeyword(String keyword, int start, int count);


    @Query("SELECT * FROM element where display_text like '%'||:keyword||'%' and layer_id in (:layerIds) limit :start,:count")
    List<Element> findListByKeywordLimitLayer(String keyword, Set<String> layerIds, int start, int count);

    @Query("SELECT * FROM element where display_text like '%'||:keyword||'%' and layer_id in (Select uuid from layerManager where project_id in (:projectIds)) limit :start,:count")
    List<Element> findListByKeywordLimitProject(String keyword, Set<String> projectIds, int start, int count);
}
