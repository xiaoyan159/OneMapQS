package com.navinfo.collect.library.map.source;

import com.navinfo.collect.library.system.Constant;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.OverzoomTileDataSource;
import org.oscim.tiling.TileSource;

import java.util.Map;

public class RealmDBTileSource extends TileSource {

    @Override
    public ITileDataSource getDataSource() {
        //return new OverzoomTileDataSource(new RealmDBTileDataSource(), Constant.OVER_ZOOM);
        return new RealmDBTileDataSource();
    }

    @Override
    public OpenResult open() {
        return OpenResult.SUCCESS;
    }

    @Override
    public void close() {

    }
}
