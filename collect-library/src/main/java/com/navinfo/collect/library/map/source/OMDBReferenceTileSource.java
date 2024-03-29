package com.navinfo.collect.library.map.source;

import org.oscim.map.Viewport;
import org.oscim.tiling.ITileDataSource;

public class OMDBReferenceTileSource extends RealmDBTileSource {
    private OMDBReferenceDataSource omdbReferenceTileSource;
    private Viewport viewport;

    public OMDBReferenceTileSource(Viewport viewport) {
        this.viewport = viewport;
        this.omdbReferenceTileSource = new OMDBReferenceDataSource(this.viewport);
    }

    @Override
    public ITileDataSource getDataSource() {
        //return new OverzoomTileDataSource(new OMDBReferenceDataSource(), Constant.OVER_ZOOM);
//        return new OverzoomTileDataSource(omdbReferenceTileSource, Constant.OVER_ZOOM);
        return omdbReferenceTileSource;
    }

    @Override
    public OpenResult open() {
        return OpenResult.SUCCESS;
    }

    @Override
    public void close() {

    }

    @Override
    public void update() {
        super.update();
        omdbReferenceTileSource.update();
    }
}
