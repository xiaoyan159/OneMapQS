package com.navinfo.collect.library.data.dao.impl;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.navinfo.collect.library.data.entity.Project;

import java.util.List;

@Dao
public interface IProjectManagerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Project... projects);

    @Update
    int update(Project[] projects);

    @Delete
    int delete(Project project);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserts(Project[] projects);

    @Query("DELETE FROM project")
    void deleteAll();

    @Query("SELECT * FROM project where project_visibility =:visable")
    List<Project> findList(int visable);

    @Query("SELECT * FROM project")
    List<Project> findList();

    @Query("SELECT * FROM project where uuid =:id")
    Project findProject(String id);
}
