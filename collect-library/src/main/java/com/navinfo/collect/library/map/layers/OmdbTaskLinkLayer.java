package com.navinfo.collect.library.map.layers;

import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.map.Map;
import org.oscim.utils.SpatialIndex;

public class OmdbTaskLinkLayer extends VectorLayer {
    public OmdbTaskLinkLayer(Map map, SpatialIndex<Drawable> index) {
        super(map, index);
    }

    public OmdbTaskLinkLayer(Map map) {
        super(map);
    }

}
