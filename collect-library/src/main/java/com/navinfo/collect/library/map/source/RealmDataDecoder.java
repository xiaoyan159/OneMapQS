package com.navinfo.collect.library.map.source;

import static org.oscim.core.MercatorProjection.latitudeToY;
import static org.oscim.core.MercatorProjection.longitudeToX;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.navinfo.collect.library.data.entity.GeometryFeatureEntity;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.source.mvt.TileDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RealmDataDecoder extends TileDecoder {
    private final String mLocale;

    private static final float REF_TILE_SIZE = 4096.0f;

    private final GeometryFactory mGeomFactory;
    private final MapElement mMapElement;
    private ITileDataSink mTileDataSink;
    private double mTileY, mTileX, mTileScale;
    private WKBReader wkbReader;

    public RealmDataDecoder() {
        super();
        mLocale = "";
        mGeomFactory = new GeometryFactory();
        mMapElement = new MapElement();
        mMapElement.layer = 5;
        wkbReader = new WKBReader();
    }

    @Override
    public boolean decode(Tile tile, ITileDataSink sink, InputStream is) throws IOException {
        mTileDataSink = sink;
        mTileScale = 1 << tile.zoomLevel;
        mTileX = tile.tileX / mTileScale;
        mTileY = tile.tileY / mTileScale;
        mTileScale *= Tile.SIZE;
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean decode(Tile tile, ITileDataSink sink, List<GeometryFeatureEntity> listResult) {
        mTileDataSink = sink;
        mTileScale = 1 << tile.zoomLevel;
        mTileX = tile.tileX / mTileScale;
        mTileY = tile.tileY / mTileScale;
        mTileScale *= Tile.SIZE;

        listResult.stream().iterator().forEachRemaining(new Consumer<GeometryFeatureEntity>() {
            @Override
            public void accept(GeometryFeatureEntity geometryFeatureEntity) {
                Log.d("RealmDBTileDataSource", geometryFeatureEntity.getGeometry());
                Map<String, Object> properties= new HashMap<>(geometryFeatureEntity.getOtherProperties().size());
                properties.putAll(geometryFeatureEntity.getOtherProperties());
                try {
                    parseGeometry(geometryFeatureEntity.getLayerEntity().getLayerName(), wkbReader.read(geometryFeatureEntity.getWkb()), properties);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
        return true;
    }

    public void parseGeometry(String layerName, Geometry geometry, Map<String, Object> tags) {
        mMapElement.clear();
        mMapElement.tags.clear();

        parseTags(tags, layerName);
        if (mMapElement.tags.size() == 0) {
            return;
        }

        boolean err = false;
        if (geometry instanceof Point) {
            mMapElement.startPoints();
            processCoordinateArray(geometry.getCoordinates(), false);
        } else if (geometry instanceof MultiPoint) {
            MultiPoint multiPoint = (MultiPoint) geometry;
            for (int i = 0; i < multiPoint.getNumGeometries(); i++) {
                mMapElement.startPoints();
                processCoordinateArray(multiPoint.getGeometryN(i).getCoordinates(), false);
            }
        } else if (geometry instanceof LineString) {
            processLineString((LineString) geometry);
        } else if (geometry instanceof MultiLineString) {
            MultiLineString multiLineString = (MultiLineString) geometry;
            for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
                processLineString((LineString) multiLineString.getGeometryN(i));
            }
        } else if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry;
            processPolygon(polygon);
        } else if (geometry instanceof MultiPolygon) {
            MultiPolygon multiPolygon = (MultiPolygon) geometry;
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                processPolygon((Polygon) multiPolygon.getGeometryN(i));
            }
        } else {
            err = true;
        }

        if (!err) {
            mTileDataSink.process(mMapElement);
        }
    }

    private void processLineString(LineString lineString) {
        mMapElement.startLine();
        processCoordinateArray(lineString.getCoordinates(), false);
    }

    private void processPolygon(Polygon polygon) {
        mMapElement.startPolygon();
        processCoordinateArray(polygon.getExteriorRing().getCoordinates(), true);
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            mMapElement.startHole();
            processCoordinateArray(polygon.getInteriorRingN(i).getCoordinates(), true);
        }
    }

    private void processCoordinateArray(Coordinate[] coordinates, boolean removeLast) {
        int length = removeLast ? coordinates.length - 1 : coordinates.length;
        for (int i = 0; i < length; i++) {
            mMapElement.addPoint((float) ((longitudeToX(coordinates[i].x) - mTileX) * mTileScale),
                    (float) ((latitudeToY(coordinates[i].y) - mTileY) * mTileScale));
        }

//        int length = removeLast ? coordinates.length - 1 : coordinates.length;
//        // 初始化3D数据类型
//        float[] point3D = new float[coordinates.length*3];
//        for (int i = 0; i < length; i++) {
//            point3D[i*3] = (float) coordinates[i].x;
//            point3D[(i*3)+1] = (float) coordinates[i].y;
//            point3D[(i*3)+2] = (float) coordinates[i].z;
//        }
//        mMapElement.points = point3D;
//        mMapElement.pointNextPos = mMapElement.points.length;
//        mMapElement.type = GeometryBuffer.GeometryType.TRIS;
    }

    private void parseTags(Map<String, Object> map, String layerName) {
        mMapElement.tags.add(new Tag("layer", layerName));
        boolean hasName = false;
        String fallbackName = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String val = (value instanceof String) ? (String) value : String.valueOf(value);
            if (key.startsWith(Tag.KEY_NAME)) {
                int len = key.length();
                if (len == 4) {
                    fallbackName = val;
                    continue;
                }
                if (len < 7)
                    continue;
                if (mLocale.equals(key.substring(5))) {
                    hasName = true;
                    mMapElement.tags.add(new Tag(Tag.KEY_NAME, val, false));
                }
            } else {
                mMapElement.tags.add(new Tag(key, val));
            }
        }
        if (!hasName && fallbackName != null)
            mMapElement.tags.add(new Tag(Tag.KEY_NAME, fallbackName, false));
    }
}
