package com.navinfo.collect.library.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

import com.google.protobuf.Any;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author qj
 * @version V1.0
 * @ClassName: Element
 * @Date 2022/4/14
 * @Description: ${Element}(要素数据)
 */
//@Entity(tableName = "element",indices = {@Index("visibility"),@Index(value = {"maxx", "minx", "maxy", "miny"})})
@Entity(tableName = "element", indices = {@Index("visibility")})
public class Element extends DataBase {

    @ColumnInfo(name = "layer_id")
    private String LayerId;

    @ColumnInfo(name = "display_style")
    private String DisplayStyle;

    @ColumnInfo(name = "display_text")
    private String DisplayText;

    @ColumnInfo(name = "start_level")
    private int StartLevel;

    @ColumnInfo(name = "end_level")
    private int EndLevel;

    @ColumnInfo(name = "zindex")
    private int ZIndex;

    @ColumnInfo(name = "visibility")
    private int Visibility;

    @ColumnInfo(name = "operation_time")
    private String OperationTime;

    @ColumnInfo(name = "export_time")
    private String ExportTime;

    @ColumnInfo(name = "t_lifecycle")
    private int TLifecycle;

    @ColumnInfo(name = "t_status")
    private int TStatus;

    @Ignore
    private Map<String, String> values;

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }

    public String getLayerId() {
        return LayerId;
    }

    public void setLayerId(String layerId) {
        this.LayerId = layerId;
    }

    public String getDisplayStyle() {
        return DisplayStyle;
    }

    public void setDisplayStyle(String displayStyle) {
        DisplayStyle = displayStyle;
    }

    public String getDisplayText() {
        return DisplayText;
    }

    public void setDisplayText(String displayText) {
        DisplayText = displayText;
    }

    public int getStartLevel() {
        return StartLevel;
    }

    public void setStartLevel(int startLevel) {
        StartLevel = startLevel;
    }

    public int getEndLevel() {
        return EndLevel;
    }

    public void setEndLevel(int endLevel) {
        EndLevel = endLevel;
    }

    public int getZIndex() {
        return ZIndex;
    }

    public void setZIndex(int ZIndex) {
        this.ZIndex = ZIndex;
    }

    public int getVisibility() {
        return Visibility;
    }

    public void setVisibility(int visibility) {
        Visibility = visibility;
    }

    public String getOperationTime() {
        return OperationTime;
    }

    public void setOperationTime(String operationTime) {
        OperationTime = operationTime;
    }

    public String getExportTime() {
        return ExportTime;
    }

    public void setExportTime(String exportTime) {
        ExportTime = exportTime;
    }

    public int getTLifecycle() {
        return TLifecycle;
    }

    public void setTLifecycle(int TLifecycle) {
        this.TLifecycle = TLifecycle;
    }

    public int getTStatus() {
        return TStatus;
    }

    public void setTStatus(int TStatus) {
        this.TStatus = TStatus;
    }


    @NotNull
    public Map toMap() {
        Map map = new HashMap();
        map.put("layerId", LayerId);
        map.put("uuid", getId());
        map.put("geometry", getGeometry());
        map.put("displayText", DisplayText);
        map.put("displayStyle", DisplayStyle);
        map.put("tOperateDate", OperationTime);
        map.put("tLifecycle", TLifecycle);
        map.put("tStatus", TStatus);
        map.put("values", values);
        return map;
    }

    public  static Element fromMap(Map<String, Object> map) {
        try {
            Element element = new Element();
            element.setLayerId((String) map.get("layerId"));
            element.setId((String) map.get("uuid"));
            element.setGeometry((String) map.get("geometry"));
            element.setDisplayText((String) map.get("displayText"));
            element.setDisplayStyle((String) map.get("displayStyle"));
            element.setOperationTime((String) map.get("tOperateDate"));
            element.setTLifecycle((Integer) map.get("tLifecycle"));
            element.setTStatus((Integer) map.get("tStatus"));
            return element;
        } catch (Exception e) {
            return null;
        }

    }

    public static Element fromJson(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            return fromJson(jsonObject);
        } catch (Exception e) {
            return null;
        }
    }

    public static Element fromJson(JSONObject jsonObject) {
        Element element = new Element();
        element.setLayerId(jsonObject.optString("layerId"));
        element.setId(jsonObject.optString("uuid"));
        element.setGeometry(jsonObject.optString("geometry"));
        element.setDisplayText(jsonObject.optString("displayText"));
        element.setDisplayStyle(jsonObject.optString("displayStyle"));
        element.setOperationTime(jsonObject.optString("tOperateDate"));
        element.setTLifecycle(jsonObject.optInt("tLifecycle"));
        element.setTStatus(jsonObject.optInt("tStatus"));
        return element;
    }


}

