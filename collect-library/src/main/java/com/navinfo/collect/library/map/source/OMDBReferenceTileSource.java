package com.navinfo.collect.library.map.source;

import android.util.Log;

import com.navinfo.collect.library.data.entity.RenderEntity;
import com.navinfo.collect.library.system.Constant;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.OverzoomTileDataSource;
import org.oscim.tiling.TileSource;

import io.realm.Realm;

public class OMDBReferenceTileSource extends RealmDBTileSource {
    private OMDBReferenceDataSource omdbReferenceTileSource = new OMDBReferenceDataSource();

    @Override
    public ITileDataSource getDataSource() {
        //return new OverzoomTileDataSource(new OMDBReferenceDataSource(), Constant.OVER_ZOOM);
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
