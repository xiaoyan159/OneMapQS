package com.navinfo.collect.library.map.source;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.navinfo.collect.library.data.entity.Element;
import com.navinfo.collect.library.data.entity.NiLocation;
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
import org.oscim.core.GeoPoint;
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
import java.util.Random;

import static org.oscim.core.MercatorProjection.latitudeToY;
import static org.oscim.core.MercatorProjection.longitudeToX;

public class MapLifeNiLocationDecoder extends TileDecoder {
    private final String mLocale;

    private static final float REF_TILE_SIZE = 4096.0f;

    private final GeometryFactory mGeomFactory;
    private final MapElement mMapElement;
    private ITileDataSink mTileDataSink;
    private double mTileY, mTileX, mTileScale;

    public MapLifeNiLocationDecoder() {
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
    public boolean decode(Tile tile, ITileDataSink sink, String layerName,List<NiLocation> niLocationList) {
        mTileDataSink = sink;
        mTileScale = 1 << tile.zoomLevel;
        mTileX = tile.tileX / mTileScale;
        mTileY = tile.tileY / mTileScale;
        mTileScale *= Tile.SIZE;

        if(niLocationList!=null){
//            Log.e("qj","decode==geometry=="+niLocationList.size());
            Random random = new Random();
            int count = 0;
            for (NiLocation niLocation:niLocationList) {
                Map<String, Object> properties= new HashMap<>();
                double anyNum = random.nextDouble();
                if(anyNum-0.0005d>0){
                    anyNum = 0.005d/30d;
                }
                Geometry geometry = GeometryTools.createGeometry(new GeoPoint(niLocation.getLatitude()+anyNum,niLocation.getLongitude()+anyNum));//116.245567 40.073525
                if(geometry!=null){
                    if(geometry.getGeometryType().equalsIgnoreCase("Point")){
                        properties.put("nav_style","symbol_track_point");//symbol_track_point
                    }
                    if(count==0){
                        properties.put("nav_style","symbol_object_line");
                        parseGeometry(layerName, GeometryTools.createGeometry("LINESTRING (116.245567 40.073475, 116.245855 40.072811, 116.24706 40.073034)"), properties);
                    }
//                    if(count%55==0){
//                        Log.e("qj","decode==geometry==symbol_track_point"+geometry.toString()+String.valueOf(anyNum));
//                    }
                    parseGeometry(layerName, geometry, properties);
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
        } else if (geometry instanceof LineString) {
            processLineString((LineString) geometry);
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
            mMapElement.tags.add(new Tag(key, val));
        }
    }
}
