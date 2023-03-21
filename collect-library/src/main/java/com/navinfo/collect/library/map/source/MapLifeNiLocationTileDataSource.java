package com.navinfo.collect.library.map.source;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase;
import com.navinfo.collect.library.data.entity.Element;
import com.navinfo.collect.library.data.entity.NiLocation;
import org.oscim.core.MapElement;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;
import java.util.List;

public class MapLifeNiLocationTileDataSource implements ITileDataSource {

    private Context mCon;
    private String dbName;

    private long mStartTime;
    private long mEndTime;

    public MapLifeNiLocationTileDataSource(Context mCon, String dbName) {
        this.mCon = mCon;
        this.dbName = dbName;
    }

    private MapElement element = new MapElement();

    private final ThreadLocal<MapLifeNiLocationDecoder> mThreadLocalDecoders = new ThreadLocal<MapLifeNiLocationDecoder>() {
        @Override
        protected MapLifeNiLocationDecoder initialValue() {
            return new MapLifeNiLocationDecoder();
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
            List<NiLocation> list = null;
            if(mEndTime!=0){
                list = MapLifeDataBase.getDatabase(mCon, dbName).getNiLocationDao().timeTofindList(xStart, xEnd, yStart, yEnd,mStartTime,mEndTime);
            }else{
                list = MapLifeDataBase.getDatabase(mCon, dbName).getNiLocationDao().findList(xStart, xEnd, yStart, yEnd);
            }

            Log.e("qj","query"+(list==null?0:list.size())+"==="+xStart+"==="+xEnd+"==="+yStart+"==="+yEnd);
            mThreadLocalDecoders.get().decode(tile, mapDataSink, "MapLifeNiLocationTile", list);

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

    public void setStartTime(long startTime){
        this.mStartTime = startTime;
    }

    public void setEndTime(long endTime){
        this.mEndTime = endTime;
    }

}
