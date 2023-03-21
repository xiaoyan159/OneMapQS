package com.navinfo.collect.library.data.entity;

import com.navinfo.collect.library.system.Constant;
import com.navinfo.collect.library.utils.GeometryTools;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.oscim.core.MercatorProjection;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.realm.RealmDictionary;
import io.realm.RealmObject;
import io.realm.RealmSet;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class GeometryFeatureEntity extends RealmObject implements Cloneable {
    @PrimaryKey
    private String id = UUID.randomUUID().toString();
    private String name;
    @Required
    private String geometry;
    private byte[] wkb;
    private double xmax;
    private double xmin;
    private double ymax;
    private double ymin;
    private LayerEntity layerEntity;
    private RealmSet<Integer> tileX = new RealmSet<>();
    private RealmSet<Integer> tileY = new RealmSet<>();
    private RealmDictionary<String> properties = new RealmDictionary<>();
    private RealmDictionary<String> otherProperties = new RealmDictionary<>();

    public GeometryFeatureEntity() {
    }

    public GeometryFeatureEntity(String name, String geometry) {
        this.name = name;
        this.geometry = geometry;
    }

    public GeometryFeatureEntity(String name, String geometry, double xmax, double xmin, double ymax, double ymin) {
        this.name = name;
        this.geometry = geometry;
        this.xmax = xmax;
        this.xmin = xmin;
        this.ymax = ymax;
        this.ymin = ymin;
    }

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

    public double getXmax() {
        return xmax;
    }

    public void setXmax(double xmax) {
        this.xmax = xmax;
    }

    public double getXmin() {
        return xmin;
    }

    public void setXmin(double xmin) {
        this.xmin = xmin;
    }

    public double getYmax() {
        return ymax;
    }

    public void setYmax(double ymax) {
        this.ymax = ymax;
    }

    public double getYmin() {
        return ymin;
    }

    public void setYmin(double ymin) {
        this.ymin = ymin;
    }

    public Map<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new RealmDictionary<>();
        }
        return properties;
    }

    public void addProperties(String k, String v) {
        this.properties.put(k, v);
    }

    public RealmDictionary<String> getOtherProperties() {
        if (this.otherProperties == null) {
            this.otherProperties = new RealmDictionary<>();
        }
        return otherProperties;
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
        // 每一次对geometry的更新，都会影响到数据的瓦片编号，记录数据的20级瓦片编号
        getTileXByGeometry(geometry, tileX);
        getTileYByGeometry(geometry, tileY);

        Envelope envelope = geometry.getBoundary().getEnvelopeInternal();
        setXmin(envelope.getMinX());
        setXmax(envelope.getMaxX());
        setYmin(envelope.getMinY());
        setYmax(envelope.getMaxY());
    }

    public LayerEntity getLayerEntity() {
        return layerEntity;
    }

    public void setLayerEntity(LayerEntity layerEntity) {
        this.layerEntity = layerEntity;
    }

    /**
     * 根据给定的geometry计算其横跨的20级瓦片X值
     * */
    private Set<Integer> getTileXByGeometry(Geometry geometry, Set<Integer> tileXSet) {
//        long startTime = System.currentTimeMillis();
        if (tileXSet == null) {
            tileXSet = new RealmSet<>();
        }
        if (geometry!=null) {
            Geometry envelope = geometry.getEnvelope();
            if (envelope!=null) {
                Coordinate[] coordinates = envelope.getCoordinates();
                // 最小最大x轴坐标，索引0位最小x值，索引1位最大x值
                if (coordinates!=null&&coordinates.length>0) {
                    double[] minMaxX = new double[]{coordinates[0].x, coordinates[0].x};
                    for (Coordinate coordinate: coordinates) {
                        // 获取最大和最小x的值
                        if (coordinate.x<minMaxX[0]) {
                            minMaxX[0] = coordinate.x;
                        }
                        if (coordinate.x>minMaxX[1]) {
                            minMaxX[1] = coordinate.x;
                        }
                    }
                    // 分别计算最大和最小x值对应的tile号
                    int tileX0 = MercatorProjection.longitudeToTileX(minMaxX[0], (byte) Constant.OVER_ZOOM);
                    int tileX1 = MercatorProjection.longitudeToTileX(minMaxX[1], (byte) Constant.OVER_ZOOM);
                    int minTileX = tileX0 <= tileX1? tileX0: tileX1;
                    int maxTileX = tileX0 <= tileX1? tileX1: tileX0;
                    for (int i = minTileX; i <= maxTileX; i++) {
                        tileXSet.add(i);
                    }
                }
            }
        }
//        System.out.println("XGeometry-time:"+(System.currentTimeMillis()-startTime));
        return tileXSet;
    }

    /**
     * 根据给定的geometry计算其横跨的20级瓦片Y值
     * */
    private Set<Integer> getTileYByGeometry(Geometry geometry, Set<Integer> tileYSet) {
//        long startTime = System.currentTimeMillis();
        if (tileYSet == null) {
            tileYSet = new RealmSet<>();
        }
        Geometry envelope = geometry.getEnvelope();
        if (envelope!=null) {
            Coordinate[] coordinates = envelope.getCoordinates();
            // 最小最大x轴坐标，索引0位最小x值，索引1位最大y值
            if (coordinates!=null&&coordinates.length>0) {
                double[] minMaxY = new double[]{coordinates[0].y, coordinates[0].y};
                for (Coordinate coordinate: coordinates) {
                    // 获取最大和最小y的值
                    if (coordinate.y<minMaxY[0]) {
                        minMaxY[0] = coordinate.y;
                    }
                    if (coordinate.y>minMaxY[1]) {
                        minMaxY[1] = coordinate.y;
                    }
                }
                // 分别计算最大和最小x值对应的tile号
                int tileY0 = MercatorProjection.latitudeToTileY(minMaxY[0], (byte) Constant.OVER_ZOOM);
                int tileY1 = MercatorProjection.latitudeToTileY(minMaxY[1], (byte) Constant.OVER_ZOOM);
                int minTileY = tileY0 <= tileY1? tileY0: tileY1;
                int maxTileY = tileY0 <= tileY1? tileY1: tileY0;
                for (int i = minTileY; i <= maxTileY; i++) {
                    tileYSet.add(i);
                }
            }
        }
//        System.out.println("YGeometry-time:"+(System.currentTimeMillis()-startTime));
        return tileYSet;
    }

    public byte[] getWkb() {
        return wkb;
    }

    public void setWkb(byte[] wkb) {
        this.wkb = wkb;
    }
}
