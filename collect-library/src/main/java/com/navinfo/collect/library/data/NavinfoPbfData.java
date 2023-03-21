package com.navinfo.collect.library.data;

import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;
import com.navinfo.collect.library.data.entity.GeometryFeatureEntity;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.Resultdata;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXYM;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.io.ShapeIO;
import org.locationtech.spatial4j.io.ShapeReader;
import org.locationtech.spatial4j.io.ShapeWriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NavinfoPbfData {
    private static GeometryFactory factory;
    private static SpatialContext spatialContext;
    private static ShapeReader shapeReader;
    private static ShapeWriter shapeWriter;
    private static WKBWriter wkbWriter;
    private boolean isInit = false;

    public NavinfoPbfData() {
        if (!isInit) {
            init();
        }
    }

    public static boolean init() {
        SpatialContextFactory spatialContextFactory = new SpatialContextFactory();
        wkbWriter= new WKBWriter(3, true);
        factory = new JtsSpatialContextFactory().getGeometryFactory();

        spatialContext = new SpatialContext(spatialContextFactory);
        shapeReader = spatialContext.getFormats().getWktReader();
        shapeWriter = spatialContext.getFormats().getWktWriter();
        return true;
    }

    public static Resultdata.ResultData readPbfData(InputStream inputStream) throws IOException {
        return Resultdata.ResultData.parseFrom(inputStream);
    }

    public static Geometry createGeometry(GeneratedMessageV3 geometry) {
        Geometry resultGeometry = null;
        if (geometry instanceof com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Point2d) {
            com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Point2d point2d = (com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Point2d) geometry;
            Coordinate coordinate = new Coordinate(point2d.getLongitudeDegrees(), point2d.getLatitudeDegrees());
            resultGeometry = factory.createPoint(coordinate);
        } else if (geometry instanceof com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Point3d) {
            com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Point3d point3d = (com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Point3d) geometry;
            CoordinateXYZM coordinate = new CoordinateXYZM(point3d.getLatLon().getLongitudeDegrees(), point3d.getLatLon().getLatitudeDegrees(), point3d.getElevation().getZ(), 7);
            resultGeometry = factory.createPoint(coordinate);
        } else if (geometry instanceof com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.LineString2d) {
            com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.LineString2d lineString2d = (com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.LineString2d) geometry;
            Coordinate[] coordinates = new Coordinate[lineString2d.getLinestringPointsCount()];
            for (int i = 0; i < lineString2d.getLinestringPointsCount(); i++) {
                coordinates[i] = new Coordinate(lineString2d.getLinestringPoints(i).getLongitudeDegrees(), lineString2d.getLinestringPoints(i).getLatitudeDegrees());
            }
            resultGeometry = factory.createLineString(coordinates);
        } else if (geometry instanceof com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.LineString3d) {
            com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.LineString3d lineString3d = (com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.LineString3d) geometry;
            CoordinateXYZM[] coordinates = new CoordinateXYZM[lineString3d.getLinestringPointsCount()];
            for (int i = 0; i < lineString3d.getLinestringPointsCount(); i++) {
                coordinates[i] = new CoordinateXYZM(lineString3d.getLinestringPoints(i).getLatLon().getLongitudeDegrees(), lineString3d.getLinestringPoints(i).getLatLon().getLatitudeDegrees(), lineString3d.getLinestringPoints(i).getElevation().getZ(), 7);
            }
            resultGeometry = factory.createLineString(coordinates);
        } else if (geometry instanceof com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.MultiLineString2d) {
            com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.MultiLineString2d multiLineString2d = (com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.MultiLineString2d) geometry;
            LineString[] lineStrings = new LineString[multiLineString2d.getLineStringsCount()];
            for (int i = 0; i < multiLineString2d.getLineStringsCount(); i++) {
                lineStrings[i] = (LineString) createGeometry(multiLineString2d.getLineStrings(i));
            }
            resultGeometry = factory.createMultiLineString(lineStrings);
        } else if (geometry instanceof com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.MultiLineString3d) {
            com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.MultiLineString3d multiLineString3d = (com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.MultiLineString3d) geometry;
            LineString[] lineStrings = new LineString[multiLineString3d.getLineStringsCount()];
            for (int i = 0; i < multiLineString3d.getLineStringsCount(); i++) {
                lineStrings[i] = (LineString) createGeometry(multiLineString3d.getLineStrings(i));
            }
            resultGeometry = factory.createMultiLineString(lineStrings);
        } else if (geometry instanceof com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Polygon2d) {
            com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Polygon2d polygon2d = (com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Polygon2d) geometry;
            Coordinate[] coordinates = new Coordinate[polygon2d.getPolygonPointsCount()];
            for (int i = 0; i < polygon2d.getPolygonPointsCount(); i++) {
                coordinates[i] = new Coordinate(polygon2d.getPolygonPoints(i).getLongitudeDegrees(), polygon2d.getPolygonPoints(i).getLatitudeDegrees());
            }
            resultGeometry = factory.createPolygon(coordinates);
        } else if (geometry instanceof com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Polygon3d) {
            com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Polygon3d polygon3d = (com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Polygon3d) geometry;
            CoordinateXYZM[] coordinates = new CoordinateXYZM[polygon3d.getPolygonPointsCount()];
            for (int i = 0; i < polygon3d.getPolygonPointsCount(); i++) {
                coordinates[i] = new CoordinateXYZM(polygon3d.getPolygonPoints(i).getLatLon().getLongitudeDegrees(), polygon3d.getPolygonPoints(i).getLatLon().getLatitudeDegrees(), polygon3d.getPolygonPoints(i).getElevation().getZ(), 7);
            }
            resultGeometry = factory.createPolygon(coordinates);
        } else if (geometry instanceof com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.MultiPolygon2d) {
            com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.MultiPolygon2d multiPolygon2d = (com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.MultiPolygon2d) geometry;
            Polygon[] polygons = new Polygon[multiPolygon2d.getPolygonsCount()];
            for (int i = 0; i < multiPolygon2d.getPolygonsCount(); i++) {
                polygons[i] = (Polygon) createGeometry(multiPolygon2d.getPolygons(i));
            }
            resultGeometry = factory.createMultiPolygon(polygons);
        } else if (geometry instanceof com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.MultiPolygon3d) {
            com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.MultiPolygon3d multiPolygon3d = (com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.MultiPolygon3d) geometry;
            Polygon[] polygons = new Polygon[multiPolygon3d.getPolygonsCount()];
            for (int i = 0; i < multiPolygon3d.getPolygonsCount(); i++) {
                polygons[i] = (Polygon) createGeometry(multiPolygon3d.getPolygons(i));
            }
            resultGeometry = factory.createMultiPolygon(polygons);
        }
        return resultGeometry;
    }

    public static GeometryFeatureEntity createGeometryEntity(Geometry geometry, Map<Descriptors.FieldDescriptor, Object> userData, Map<String, String> extendFields) {
        if (geometry == null) {
            return null;
        }
        StringBuilder prefixStr = new StringBuilder();
        GeometryFeatureEntity entity = new GeometryFeatureEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setGeometry(geometry);
        entity.setWkb(wkbWriter.write(geometry));
        if (userData!=null&&!userData.isEmpty()) {
            for (Map.Entry<Descriptors.FieldDescriptor, Object> e: userData.entrySet()) {
                getFieldEntry(e, prefixStr, entity.getProperties());
            }
        }
        if (extendFields!=null&&!extendFields.isEmpty()) {
            entity.getOtherProperties().putAll(extendFields);
        }
        return entity;
    }

    /**
     * 递归调用，如果存在子属性，则父子属性以{parent.child}格式存入map
     * */
    private static void getFieldEntry(Map.Entry<Descriptors.FieldDescriptor, Object> e, StringBuilder prefix, Map<String, String> properties) {
        prefix.append(".").append(e.getKey().getName());
        if (e.getValue() instanceof GeneratedMessageV3) {
            for (Map.Entry<Descriptors.FieldDescriptor, Object> subE: ((GeneratedMessageV3)e.getValue()).getAllFields().entrySet()) {
                getFieldEntry(subE, prefix, properties);
            }
        } else {
            properties.put(e.getKey().getName(), e.getValue().toString());
        }
    }
}
