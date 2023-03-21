package com.navinfo.collect.library.data.db;

import com.navinfo.collect.library.utils.GeometryTools;

import org.locationtech.jts.geom.Geometry;
import java.util.UUID;

public class GeometryEntity {
    private String id = UUID.randomUUID().toString();
    private String name;
    private String geometry;
    private String layerName;
    private String style;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String wkt) {
        if (wkt == null||wkt.isEmpty()) {
            return;
        }
        Geometry geometry = GeometryTools.createGeometry(wkt);
        setGeometry(geometry);
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry.toString();
    }

}
