package com.navinfo.collect.library.data.entity;

import android.util.Log;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author qj
 * @version V1.0
 * @ClassName: LayerManager
 * @Date 2022/4/14
 * @Description: ${LayerManager}(图层管理)
 */
@Entity(tableName = "layerManager")
public class LayerManager extends Feature {

    ///项目Id
    @ColumnInfo(name = "project_id")
    private String projectId;
    ///图层名称
    @ColumnInfo(name = "layer_name")
    private String layerName;
    ///图层级别
    @ColumnInfo(name = "zindex")
    private int ZIndex;
    ///图层是否显示
    @ColumnInfo(name = "visibility")
    private boolean visibility;
    ///导出时间
    @ColumnInfo(name = "export_time")
    private String exportTime;
    ///导入时间
    @ColumnInfo(name = "import_time")
    private String importTime;
    @ColumnInfo(name = "bundle")
    private String bundle;
    ///图层表述信息
    @ColumnInfo(name = "describe")
    private String describe;

    ///点线面类型
    @ColumnInfo(name = "geometryType")
    private int geometryType;

    ///来源c
    @ColumnInfo(name = "source")
    private String source;

    @ColumnInfo(name = "style")
    private String style;

    ///每个字段的样式和控制条件
    @Ignore
    private List<CustomLayerItem> itemList;

    public List<CustomLayerItem> getItemList() {
        return itemList;
    }

    public void setItemList(List<CustomLayerItem> itemList) {
        this.itemList = itemList;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public String getDescribe() {
        return describe;
    }

    public int getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(int geometryType) {
        this.geometryType = geometryType;
    }


    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public int getZIndex() {
        return ZIndex;
    }

    public void setZIndex(int zIndex) {
        this.ZIndex = zIndex;
    }

    public boolean getVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public String getExportTime() {
        return exportTime;
    }

    public void setExportTime(String exportTime) {
        this.exportTime = exportTime;
    }

    public String getImportTime() {
        return importTime;
    }

    public void setImportTime(String importTime) {
        this.importTime = importTime;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "LayerManager{" +
                "id='" + getId() + '\'' +
                ",projectId='" + projectId + '\'' +
                ", layerName='" + layerName + '\'' +
                ", ZIndex=" + ZIndex +
                ", visibility=" + visibility +
                ", exportTime='" + exportTime + '\'' +
                ", importTime='" + importTime + '\'' +
                ", bundle='" + bundle + '\'' +
                ", describe='" + describe + '\'' +
                ", geometryType='" + geometryType + '\'' +
                ", source='" + source + '\'' +
                ", style='" + style + '\'' +
                '}';
    }

    public Map toMap() {
        Map map = new HashMap();
        map.put("uuid", getId());
        map.put("layerName", getLayerName());
        map.put("ZIndex", ZIndex);
        map.put("visibility", getVisibility());
        map.put("exportTime", exportTime);
        map.put("import_time", getImportTime());
        map.put("describe", getDescribe());
        map.put("geometryType", getGeometryType());
        map.put("source", source);
        map.put("style", style);
        return map;
    }

}
