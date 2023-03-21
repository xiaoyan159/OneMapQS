package com.navinfo.collect.library.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

/**
 * 项目
 */
@Entity(tableName = "project")
public class Project extends Feature {
    //项目名称
    @ColumnInfo(name = "project_name")
    private String name;
    ///创建时间
    @ColumnInfo(name = "project_time")
    private String createTime;
    ///项目描述信息
    @ColumnInfo(name = "project_describe")
    private String describe;
    ///项目是否显示
    @ColumnInfo(name = "project_visibility")
    private boolean visibility;
    ///任务圈
    @ColumnInfo(name = "project_geometry")
    private String geometry;
    ///任务圈显隐
    @ColumnInfo(name = "project_geometry_visibility")
    private boolean geometryVisibility;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public boolean getVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public boolean getGeometryVisibility() {
        return geometryVisibility;
    }

    public void setGeometryVisibility(boolean geometryVisibility) {
        this.geometryVisibility = geometryVisibility;
    }
}
