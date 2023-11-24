package com.navinfo.collect.library.map.source;

import static org.oscim.core.MercatorProjection.latitudeToY;
import static org.oscim.core.MercatorProjection.longitudeToX;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.navinfo.collect.library.data.entity.RenderEntity;
import com.navinfo.collect.library.enums.DataCodeEnum;
import com.navinfo.collect.library.utils.GeometryTools;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.source.mvt.TileDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class OMDBDataDecoder extends TileDecoder {
    private final String mLocale;

    private static final float REF_TILE_SIZE = 4096.0f;

    private final GeometryFactory mGeomFactory;
    private final MapElement mMapElement;
    private ITileDataSink mTileDataSink;
    private double mTileY, mTileX, mTileScale;

    public OMDBDataDecoder() {
        super();
        mLocale = "";
        mGeomFactory = new GeometryFactory();
        mMapElement = new MapElement();
        mMapElement.layer = 5;
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
    public boolean decode(int mapLevel, Tile tile, ITileDataSink sink, List<RenderEntity> listResult) {
        mTileDataSink = sink;
        mTileScale = 1 << tile.zoomLevel;
        mTileX = tile.tileX / mTileScale;
        mTileY = tile.tileY / mTileScale;
        mTileScale *= Tile.SIZE;
        List<RenderEntity> list = new ArrayList<>();
        List<RenderEntity> traffList = new ArrayList<>();
        listResult.stream().iterator().forEachRemaining(new Consumer<RenderEntity>() {
            @Override
            public void accept(RenderEntity renderEntity) {
                if (!(mapLevel < renderEntity.getZoomMin() || mapLevel > renderEntity.getZoomMax())) {
                    if (renderEntity.getCode().equals(DataCodeEnum.OMDB_TRAFFIC_SIGN.getCode())) {
                        list.add(renderEntity);
                    } else if (renderEntity.getCode().equals(DataCodeEnum.OMDB_TRAFFICLIGHT.getCode())) {
                        traffList.add(renderEntity);
                    } else {
                        Map<String, Object> properties = new HashMap<>(renderEntity.getProperties().size());
                        properties.putAll(renderEntity.getProperties());
                        parseGeometry(renderEntity.getTable(), renderEntity.getWkt(), properties);
                    }
                } else {
//                    Log.e("qj","render"+renderEntity.name+"=="+renderEntity.getZoomMin()+"==="+renderEntity.getZoomMax()+"==="+renderEntity.getEnable());
                }
            }
        });
        //增加交通标牌聚合显示
        List<RenderEntity> list1 = GeometryTools.groupByDistance(DataCodeEnum.OMDB_TRAFFIC_SIGN.getCode(), list, 5.0);
        if (list1 != null && list1.size() > 0) {
            Log.e("qj", "聚合交通标牌转换开始" + list.size());
            list1.stream().iterator().forEachRemaining(new Consumer<RenderEntity>() {
                @Override
                public void accept(RenderEntity renderEntity) {
                    Map<String, Object> properties = new HashMap<>(renderEntity.getProperties().size());
                    properties.putAll(renderEntity.getProperties());
                    parseGeometry(renderEntity.getTable(), renderEntity.getWkt(), properties);
                }
            });
            Log.e("qj", "聚合交通标牌转换结束" + list1.size());
        }
        //增加交通标牌聚合显示
        List<RenderEntity> list2 = GeometryTools.groupByDistance(DataCodeEnum.OMDB_TRAFFICLIGHT.getCode(), traffList, 5.0);
        if (list2 != null && list2.size() > 0) {
            Log.e("qj", "聚合红绿灯转换开始" + traffList.size());
            list2.stream().iterator().forEachRemaining(new Consumer<RenderEntity>() {
                @Override
                public void accept(RenderEntity renderEntity) {
                    Map<String, Object> properties = new HashMap<>(renderEntity.getProperties().size());
                    properties.putAll(renderEntity.getProperties());
                    parseGeometry(renderEntity.getTable(), renderEntity.getWkt(), properties);
                }
            });
            Log.e("qj", "聚合红绿灯转换结束" + list2.size());
        }
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
//            double z = longitudeToX(MercatorProjection.pixelXToLongitudeWithScale(MercatorProjection.metersToPixelsWithScale((float) coordinates[i].z, coordinates[i].y, mTileScale), mTileScale))* mTileScale/8;
            mMapElement.addPoint((float) ((longitudeToX(coordinates[i].x) - mTileX) * mTileScale),
                    (float) ((latitudeToY(coordinates[i].y) - mTileY) * mTileScale), /*(float) coordinates[i].z*/0);
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

    public void clean() {
        if (mTileDataSink != null) {
            mTileDataSink.notifyAll();
        }
    }
}
