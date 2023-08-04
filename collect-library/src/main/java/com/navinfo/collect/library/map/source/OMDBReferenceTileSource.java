package com.navinfo.collect.library.map.source;

import com.navinfo.collect.library.system.Constant;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.OverzoomTileDataSource;
import org.oscim.tiling.TileSource;

public class OMDBReferenceTileSource extends TileSource {

    @Override
    public ITileDataSource getDataSource() {
        //return new OverzoomTileDataSource(new OMDBReferenceDataSource(), Constant.OVER_ZOOM);
        return new OMDBReferenceDataSource();
    }

    @Override
    public OpenResult open() {
        return OpenResult.SUCCESS;
    }

    @Override
    public void close() {

    }
}
