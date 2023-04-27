package com.navinfo.collect.library.map.layers;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;
import org.oscim.utils.SpatialIndex;

import java.util.HashMap;
import java.util.List;

public class OmdbTaskLinkLayer extends VectorLayer {
    private java.util.Map<String, LineString> lineList = new HashMap<>();
    private Style style;
    public OmdbTaskLinkLayer(Map map) {
        super(map);
    }


}
