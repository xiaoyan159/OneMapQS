package com.navinfo.collect.library.data.entity;

import org.locationtech.jts.geom.Geometry;

import java.util.List;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class LayerEntity extends RealmObject {
    @Required
    @PrimaryKey
    private String layerName;
    private String fromDataName;
    private int GeomType;
    private String layerTableName;
    @Ignore
    private List<Geometry> featureList;

    public LayerEntity() {
    }


    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getFromDataName() {
        return fromDataName;
    }

    public void setFromDataName(String fromDataName) {
        this.fromDataName = fromDataName;
    }

    public int getGeomType() {
        return GeomType;
    }

    public void setGeomType(int geomType) {
        GeomType = geomType;
    }

    public List<Geometry> getFeatureList() {
        return featureList;
    }

    public void setFeatureList(List<Geometry> featureList) {
        this.featureList = featureList;
    }

    public String getLayerTableName() {
        return layerTableName;
    }

    public void setLayerTableName(String layerTableName) {
        this.layerTableName = layerTableName;
    }
}
