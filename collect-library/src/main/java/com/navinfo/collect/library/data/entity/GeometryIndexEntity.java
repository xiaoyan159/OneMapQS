package com.navinfo.collect.library.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.TypeConverter;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author qj
 * @version V1.0
 * @ClassName: GeometryEntity
 * @Date 2022/4/14
 * @Description: ${TODO}(几何基类)
 */
public class GeometryIndexEntity implements Serializable,Cloneable{
    @ColumnInfo(name = "geometry")
    private String geometry;
    @ColumnInfo(name = "xmax")
    private double xMax;
    @ColumnInfo(name = "xmin")
    private double xMin;
    @ColumnInfo(name = "ymax")
    private double yMax;
    @ColumnInfo(name = "ymin")
    private double yMin;

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public double getxMax() {
        return xMax;
    }

    public void setxMax(double xMax) {
        this.xMax = xMax;
    }

    public double getxMin() {
        return xMin;
    }

    public void setxMin(double xMin) {
        this.xMin = xMin;
    }

    public double getyMax() {
        return yMax;
    }

    public void setyMax(double yMax) {
        this.yMax = yMax;
    }

    public double getyMin() {
        return yMin;
    }

    public void setyMin(double yMin) {
        this.yMin = yMin;
    }

    public GeometryIndexEntity() {
        super();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
