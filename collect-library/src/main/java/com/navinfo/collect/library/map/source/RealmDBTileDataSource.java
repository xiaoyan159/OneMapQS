package com.navinfo.collect.library.map.source;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.navinfo.collect.library.data.RealmUtils;
import com.navinfo.collect.library.data.entity.GeometryFeatureEntity;
import com.navinfo.collect.library.system.Constant;

import org.oscim.core.MapElement;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmQuery;

public class RealmDBTileDataSource implements ITileDataSource {
    private final ThreadLocal<RealmDataDecoder> mThreadLocalDecoders = new ThreadLocal<RealmDataDecoder>() {
        @Override
        protected RealmDataDecoder initialValue() {
            return new RealmDataDecoder();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void query(MapTile tile, ITileDataSink mapDataSink) {
        // 获取tile对应的坐标范围
        if (tile.zoomLevel>=15&&tile.zoomLevel<=Constant.OVER_ZOOM) {
            int m = Constant.OVER_ZOOM-tile.zoomLevel;
            int xStart = (int)tile.tileX<<m;
            int xEnd = (int)((tile.tileX+1)<<m);
            int yStart = (int)tile.tileY<<m;
            int yEnd = (int)((tile.tileY+1)<<m);

            RealmQuery<GeometryFeatureEntity> realmQuery = Realm.getInstance(RealmUtils.getInstance().getRealmConfiguration()).where(GeometryFeatureEntity.class)
                    .rawPredicate("tileX>="+xStart+" and tileX<="+xEnd+" and tileY>="+yStart+" and tileY<="+yEnd);
            // 筛选不显示的数据
            if (Constant.HAD_LAYER_INVISIABLE_ARRAY!=null&&Constant.HAD_LAYER_INVISIABLE_ARRAY.length>0) {
                realmQuery.beginGroup();
                for (String type: Constant.HAD_LAYER_INVISIABLE_ARRAY) {
                    realmQuery.notEqualTo("name", type);
                }
                realmQuery.endGroup();
            }
            List<GeometryFeatureEntity> listResult = realmQuery.distinct("id").findAll();
            mThreadLocalDecoders.get().decode(tile, mapDataSink, listResult);
            mapDataSink.completed(QueryResult.SUCCESS);
//            Log.d("RealmDBTileDataSource", "tile:"+tile.getBoundingBox().toString());
        } else {
            mapDataSink.completed(QueryResult.SUCCESS);
        }
    }

    @Override
    public void dispose() {

    }

    @Override
    public void cancel() {
        if (Realm.getInstance(RealmUtils.getInstance().getRealmConfiguration()).isInTransaction()) {
            Realm.getInstance(RealmUtils.getInstance().getRealmConfiguration()).cancelTransaction();
        }
    }
}
