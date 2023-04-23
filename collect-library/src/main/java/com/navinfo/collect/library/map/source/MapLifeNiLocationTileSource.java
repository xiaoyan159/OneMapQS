package com.navinfo.collect.library.map.source;

import android.content.Context;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource;

import java.util.Date;

public class MapLifeNiLocationTileSource extends TileSource {
    private Context mCon;
    private String dbName;
    private MapLifeNiLocationTileDataSource mapLifeNiLocationTileDataSource;
    public MapLifeNiLocationTileSource(Context mCon, String dbName) {
        this.mCon = mCon;
        this.dbName = dbName;
        mapLifeNiLocationTileDataSource = new MapLifeNiLocationTileDataSource(mCon, dbName);
    }

    @Override
    public ITileDataSource getDataSource() {
        return mapLifeNiLocationTileDataSource;
    }

    @Override
    public OpenResult open() {
        return OpenResult.SUCCESS;
    }

    @Override
    public void close() {
    }
    public void onResum(){
        mapLifeNiLocationTileDataSource.onResume();
    }

    public void setTime(long startTime,long endTime){
        mapLifeNiLocationTileDataSource.setStartTime(startTime);
        mapLifeNiLocationTileDataSource.setEndTime(endTime);
    }
}
