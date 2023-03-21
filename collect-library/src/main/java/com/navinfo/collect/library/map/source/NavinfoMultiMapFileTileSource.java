package com.navinfo.collect.library.map.source;

import org.oscim.core.BoundingBox;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.OverzoomTileDataSource;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.geojson.GeojsonTileSource;
import org.oscim.tiling.source.mapfile.IMapFileTileSource;
import org.oscim.tiling.source.mapfile.MapDatabase;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.utils.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 *com.navinfo.map.source
 *zhjch
 *2021/9/17
 *9:46
 *说明（）
 */
public class NavinfoMultiMapFileTileSource extends GeojsonTileSource implements IMapFileTileSource {

//    private static final String DEFAULT_URL = "http://cmp-gateway-sp9-port.ayiqdpfs.cloud.app.ncloud.navinfo.com/maponline/map/online";
    private static final String DEFAULT_URL = "http://epohvqjxts85k6u-port.ayiqdpfs.cloud.app.ncloud.navinfo.com/map/online/geoJson";
    private static final String DEFAULT_PATH = "x={X}&y={Y}&z={Z}";
    private static final TileUrlFormatter mTileUrlFormatter = URL_FORMATTER;


    private static final Logger log = LoggerFactory.getLogger(NavinfoMultiMapFileTileSource.class);

    private final List<MapFileTileSource> mapFileTileSources = new ArrayList<>();
    private final Map<MapFileTileSource, int[]> zoomsByTileSource = new HashMap<>();


    public static class Builder<T extends Builder<T>> extends UrlTileSource.Builder<T> {
        private String locale = "";

        public Builder() {
            super(DEFAULT_URL, DEFAULT_PATH);
            keyName("api_key");
            overZoom(17);
        }

        public T locale(String locale) {
            this.locale = locale;
            return self();
        }

        @Override
        public NavinfoMultiMapFileTileSource build() {
            return new NavinfoMultiMapFileTileSource(this);
        }
    }

    @Override
    public String getTileUrl(Tile tile) {
        StringBuilder sb = new StringBuilder();
        sb.append(DEFAULT_URL).append("?").append(mTileUrlFormatter.formatTilePath(this, tile));
        System.out.println(sb.toString());
        return sb.toString();
    }

    @SuppressWarnings("rawtypes")
    public static Builder<?> builder() {
        return new Builder();
    }

    private final String locale;

    public NavinfoMultiMapFileTileSource(Builder<?> builder) {
        super(builder);
        this.locale = builder.locale;
    }


    public NavinfoMultiMapFileTileSource() {
        this(builder());
    }

    public NavinfoMultiMapFileTileSource(String urlString) {
        this(builder().url(urlString));
    }

    public boolean add(MapFileTileSource mapFileTileSource) {
        if (mapFileTileSources.contains(mapFileTileSource)) {
            throw new IllegalArgumentException("Duplicate map file tile source");
        }
        return mapFileTileSources.add(mapFileTileSource);
    }

    public boolean add(MapFileTileSource mapFileTileSource, int zoomMin, int zoomMax) {
        boolean result = add(mapFileTileSource);
        if (result)
            zoomsByTileSource.put(mapFileTileSource, new int[]{zoomMin, zoomMax});
        return result;
    }

    public BoundingBox getBoundingBox() {
        BoundingBox boundingBox = null;
        for (MapFileTileSource mapFileTileSource : mapFileTileSources) {
            boundingBox = (boundingBox == null) ? mapFileTileSource.getMapInfo().boundingBox : boundingBox.extendBoundingBox(mapFileTileSource.getMapInfo().boundingBox);
        }
        return boundingBox;
    }

    @Override
    public ITileDataSource getDataSource() {
        NavinfoMultiMapDatabase multiMapDatabase = new NavinfoMultiMapDatabase(this, new NavinfoVectorTileDecoder(locale), getHttpEngine());
        for (MapFileTileSource mapFileTileSource : mapFileTileSources) {
            try {
                MapDatabase mapDatabase = new MapDatabase(mapFileTileSource);
                int[] zoomLevels = zoomsByTileSource.get(mapFileTileSource);
                if (zoomLevels != null)
                    mapDatabase.restrictToZoomRange(zoomLevels[0], zoomLevels[1]);
                multiMapDatabase.add(mapDatabase);
            } catch (IOException e) {
                log.debug(e.getMessage());
            }
        }
//        return new NavinfoMultiMapDatabase(this, new NavinfoVectorTileDecoder(locale), getHttpEngine());
        return new OverzoomTileDataSource(multiMapDatabase, mOverZoom);
    }
    private static Map<String, Tag> mappings = new LinkedHashMap<>();

    private static Tag addMapping(String key, String val) {
        Tag tag = new Tag(key, val);
        mappings.put(key + "=" + val, tag);
        return tag;
    }
    @Override
    public void decodeTags(MapElement mapElement, Map<String, Object> properties) {
        boolean hasName = false;
        String fallbackName = null;

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
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
                if (locale.equals(key.substring(5))) {
                    hasName = true;
                    mapElement.tags.add(new Tag(Tag.KEY_NAME, val, false));
                }
                continue;
            }

            Tag tag = mappings.get(key + "=" + val);
            if (tag == null)
                tag = addMapping(key, val);
            mapElement.tags.add(tag);
        }

        if (!hasName && fallbackName != null)
            mapElement.tags.add(new Tag(Tag.KEY_NAME, fallbackName, false));

        // Calculate height of building parts
        if (!properties.containsKey(Tag.KEY_HEIGHT)) {
            if (properties.containsKey(Tag.KEY_VOLUME) && properties.containsKey(Tag.KEY_AREA)) {
                Object volume = properties.get(Tag.KEY_VOLUME);
                String volumeStr = (volume instanceof String) ? (String) volume : String.valueOf(volume);
                Object area = properties.get(Tag.KEY_AREA);
                String areaStr = (area instanceof String) ? (String) area : String.valueOf(area);
                float height = Float.parseFloat(volumeStr) / Float.parseFloat(areaStr);
                String heightStr = String.valueOf(FastMath.round2(height));
                mapElement.tags.add(new Tag(Tag.KEY_HEIGHT, heightStr, false));
            }
        }
    }

    @Override
    public OpenResult open() {
        OpenResult openResult = OpenResult.SUCCESS;
        for (MapFileTileSource mapFileTileSource : mapFileTileSources) {
            OpenResult result = mapFileTileSource.open();
            if (result != OpenResult.SUCCESS)
                openResult = result;
        }
        if(openResult != OpenResult.SUCCESS)
            return  super.open();
        return openResult;
    }

    @Override
    public void close() {
        for (MapFileTileSource mapFileTileSource : mapFileTileSources) {
            mapFileTileSource.close();
        }
        super.close();
    }

    @Override
    public void setCallback(MapFileTileSource.Callback callback) {
        for (MapFileTileSource mapFileTileSource : mapFileTileSources) {
            mapFileTileSource.setCallback(callback);
        }
    }

    @Override
    public void setPreferredLanguage(String preferredLanguage) {
        for (MapFileTileSource mapFileTileSource : mapFileTileSources) {
            mapFileTileSource.setPreferredLanguage(preferredLanguage);
        }
    }
}
