package com.navinfo.collect.library.map.source;

import android.content.Context;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.OverzoomTileDataSource;
import org.oscim.tiling.TileSource;

public class MapLifeDBTileSource extends TileSource {
    private Context mCon;
    private String dbName;
    private MapLifeDBTileDataSource mapLifeDBTileDataSource;
    public MapLifeDBTileSource(Context mCon, String dbName) {
        this.mCon = mCon;
        this.dbName = dbName;
        mapLifeDBTileDataSource = new MapLifeDBTileDataSource(mCon, dbName);
    }

    @Override
    public ITileDataSource getDataSource() {
//        return new OverzoomTileDataSource(new MapLifeDBTileDataSource(mCon,dbName), 18);
        return mapLifeDBTileDataSource;
    }

    @Override
    public OpenResult open() {
        return OpenResult.SUCCESS;
    }

    @Override
    public void close() {
    }
    public void onResum(){
        mapLifeDBTileDataSource.onResume();
    }
}
