package com.navinfo.collect.library.map.source;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase;
import com.navinfo.collect.library.data.entity.Element;
import com.navinfo.collect.library.data.entity.LayerManager;

import org.oscim.core.BoundingBox;
import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapLifeDBTileDataSource implements ITileDataSource {

    private Context mCon;
    private String dbName;

    public MapLifeDBTileDataSource(Context mCon, String dbName) {
        this.mCon = mCon;
        this.dbName = dbName;
    }

    private MapElement element = new MapElement();

    private final ThreadLocal<MapLifeDataDecoder> mThreadLocalDecoders = new ThreadLocal<MapLifeDataDecoder>() {
        @Override
        protected MapLifeDataDecoder initialValue() {
            return new MapLifeDataDecoder();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void query(MapTile tile, ITileDataSink mapDataSink) {


        // 获取tile对应的坐标范围
        if (tile.zoomLevel >= 10 && tile.zoomLevel <= 20) {

            int m = 20 - tile.zoomLevel;
            int xStart = (int) tile.tileX << m;
            int xEnd = (int) ((tile.tileX + 1) << m);
            int yStart = (int) tile.tileY << m;
            int yEnd = (int) ((tile.tileY + 1) << m);

            List<Element> list = MapLifeDataBase.getDatabase(mCon, dbName).getElementDao().findList(xStart, xEnd, yStart, yEnd);
            mThreadLocalDecoders.get().decode(tile, mapDataSink, "MapLifeDBTile", list);

            mapDataSink.completed(QueryResult.SUCCESS);

        }
    }

    @Override
    public void dispose() {

    }

    @Override
    public void cancel() {

    }

    public void onResume() {

    }

}
