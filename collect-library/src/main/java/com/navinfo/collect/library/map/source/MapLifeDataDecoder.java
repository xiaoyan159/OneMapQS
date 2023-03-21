package com.navinfo.collect.library.map.source;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.navinfo.collect.library.data.entity.Element;
import com.navinfo.collect.library.utils.GeometryTools;
import org.json.JSONObject;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.oscim.core.MercatorProjection.latitudeToY;
import static org.oscim.core.MercatorProjection.longitudeToX;

public class MapLifeDataDecoder extends TileDecoder {
    private final String mLocale;

    private static final float REF_TILE_SIZE = 4096.0f;

    private final GeometryFactory mGeomFactory;
    private final MapElement mMapElement;
    private ITileDataSink mTileDataSink;
    private double mTileY, mTileX, mTileScale;

    public MapLifeDataDecoder() {
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
    public boolean decode(Tile tile, ITileDataSink sink, String layerName,List<Element> elementList) {
        mTileDataSink = sink;
        mTileScale = 1 << tile.zoomLevel;
        mTileX = tile.tileX / mTileScale;
        mTileY = tile.tileY / mTileScale;
        mTileScale *= Tile.SIZE;

        if(elementList!=null){
            int count = 0;
            JSONObject jsonObject = new JSONObject();
            //默认最多显示为15个字，超出部分用...代替
            int maxLength = 15;
            String title = "无名称";
            for (Element element:elementList) {
//                Log.e("qj","decode==geometry=="+elementList.size());
                maxLength = 15;
                Map<String, Object> properties= new HashMap<>();
                properties.put("t_lifecycle",element.getTLifecycle());
                properties.put("start_level",element.getStartLevel());
                try{
                    if(!TextUtils.isEmpty(element.getDisplayStyle())){
                        jsonObject = new JSONObject(element.getDisplayStyle());
                        if(jsonObject!=null&&jsonObject.has("maxLength")){
                            maxLength = jsonObject.optInt("maxLength");
                        }
                    }
                }catch (Exception e){

                }
                //增加渲染文字最多值域设置
                title = TextUtils.isEmpty(element.getDisplayText())?"无名称":element.getDisplayText();
                if(title.length()>15){
                    title = title.substring(0,15)+"...";
                }
                properties.put("name", title);
                //Log.d("qj", "绘制"+element.getGeometry());
                Geometry geometry = GeometryTools.createGeometry(element.getGeometry());
                if(geometry!=null){
                    if(geometry.getGeometryType().equalsIgnoreCase("Point")){
                        properties.put("nav_style","symbol_object_point");
                    }else if(geometry.getGeometryType().equalsIgnoreCase("LineString")){
                        properties.put("nav_style","symbol_object_line");
                    }else if(geometry.getGeometryType().equalsIgnoreCase("Polygon")){
                        properties.put("nav_style","symbol_object_polygon");
                    }
//                    Log.e("qj","decode==geometry=="+geometry.toString());
                }

                parseGeometry(layerName, GeometryTools.createGeometry(element.getGeometry()), properties);
                if(count==0){
                    properties.put("nav_style","symbol_object_line");
                    //parseGeometry(layerName, GeometryTools.createGeometry("LINESTRING(116.26732172082546 40.091692296269144,116.26342553825292 40.09413363608908)"), properties);
                }
                //增加主点、其他点显示
                if(jsonObject!=null){
                    //解析主点
                    if(jsonObject.has("masterPoint")){
                        properties.put("nav_style","symbol_object_point");
                        String masterPoint = jsonObject.optString("masterPoint");
                        if(!TextUtils.isEmpty(masterPoint)){
                            Geometry masterPointGeometry = GeometryTools.createGeometry(masterPoint);
                            parseGeometry(layerName, masterPointGeometry, properties);
                        }
                    }
                    //解析其他显示点位
                    if(jsonObject.has("otherPoint")){
                        properties.put("nav_style","symbol_object_point");
                        String otherPoint = jsonObject.optString("otherPoint");
                        if(!TextUtils.isEmpty(otherPoint)){
                            Geometry otherPointGeometry = GeometryTools.createGeometry(otherPoint);
                            parseGeometry(layerName, otherPointGeometry, properties);
                        }
                    }
                }
                count ++;
             }
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
            mMapElement.addPoint((float) ((longitudeToX(coordinates[i].x) - mTileX) * mTileScale),
                    (float) ((latitudeToY(coordinates[i].y) - mTileY) * mTileScale));
        }
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
