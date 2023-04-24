package com.navinfo.collect.library.map.source;

import com.navinfo.collect.library.system.Constant;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.OverzoomTileDataSource;
import org.oscim.tiling.TileSource;

public class OMDBTileSource extends TileSource {

    @Override
    public ITileDataSource getDataSource() {
        return new OverzoomTileDataSource(new OMDBTileDataSource(), Constant.OVER_ZOOM);
    }

    @Override
    public OpenResult open() {
        return OpenResult.SUCCESS;
    }

    @Override
    public void close() {

    }
}
