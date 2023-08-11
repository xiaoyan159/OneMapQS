package com.navinfo.collect.library.map.source;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.navinfo.collect.library.data.RealmUtils;
import com.navinfo.collect.library.data.entity.GeometryFeatureEntity;
import com.navinfo.collect.library.data.entity.RenderEntity;
import com.navinfo.collect.library.system.Constant;
import com.navinfo.collect.library.utils.RealmDBParamUtils;

import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;

public class OMDBTileDataSource implements ITileDataSource {
    private boolean isUpdate;
    private final ThreadLocal<OMDBDataDecoder> mThreadLocalDecoders = new ThreadLocal<OMDBDataDecoder>() {
        @Override
        protected OMDBDataDecoder initialValue() {
            return new OMDBDataDecoder();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void query(MapTile tile, ITileDataSink mapDataSink) {
        // 获取tile对应的坐标范围
        if (tile.zoomLevel >= Constant.OMDB_MIN_ZOOM && tile.zoomLevel <= Constant.OVER_ZOOM) {
            int m = Constant.OVER_ZOOM - tile.zoomLevel;
            int xStart = (int) tile.tileX << m;
            int xEnd = (int) ((tile.tileX + 1) << m);
            int yStart = (int) tile.tileY << m;
            int yEnd = (int) ((tile.tileY + 1) << m);
            if(isUpdate){
                Realm.getDefaultInstance().refresh();
                isUpdate = false;
            }
            RealmQuery<RenderEntity> realmQuery = Realm.getDefaultInstance().where(RenderEntity.class).rawPredicate("taskId ="+RealmDBParamUtils.getTaskId() +" and tileX>=" + xStart + " and tileX<=" + xEnd + " and tileY>=" + yStart + " and tileY<=" + yEnd + " and enable>=1");
            // 筛选不显示的数据
            if (Constant.HAD_LAYER_INVISIABLE_ARRAY != null && Constant.HAD_LAYER_INVISIABLE_ARRAY.length > 0) {
                realmQuery.beginGroup();
                for (String type : Constant.HAD_LAYER_INVISIABLE_ARRAY) {
                    realmQuery.notEqualTo("table", type);
                }
                realmQuery.endGroup();
            }
            List<RenderEntity> listResult = realmQuery/*.distinct("id")*/.findAll();
            if (!listResult.isEmpty()) {
                mThreadLocalDecoders.get().decode(tile.zoomLevel, tile, mapDataSink, listResult);
            }
            mapDataSink.completed(QueryResult.SUCCESS);
        } else {
            mapDataSink.completed(QueryResult.SUCCESS);
        }
    }

    @Override
    public void dispose() {

    }

    @Override
    public void cancel() {
        if (Realm.getDefaultInstance().isInTransaction()) {
            Realm.getDefaultInstance().cancelTransaction();
        }
    }

    public void update(){
        isUpdate = true;
        Log.e("qj",Thread.currentThread().getName());
    }
}
