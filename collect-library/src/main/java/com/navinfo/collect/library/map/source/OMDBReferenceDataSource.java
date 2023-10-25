package com.navinfo.collect.library.map.source;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.navinfo.collect.library.data.entity.ReferenceEntity;
import com.navinfo.collect.library.data.entity.RenderEntity;
import com.navinfo.collect.library.system.Constant;
import com.navinfo.collect.library.utils.GeometryTools;
import com.navinfo.collect.library.utils.MapParamUtils;

import org.locationtech.jts.geom.Polygon;
import org.oscim.layers.tile.MapTile;
import org.oscim.map.Viewport;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.realm.Realm;
import io.realm.RealmQuery;

public class OMDBReferenceDataSource implements ITileDataSource {
    private boolean isUpdate;
    private Viewport viewport;

    public OMDBReferenceDataSource(Viewport viewport) {
        this.viewport = viewport;
    }

    private final ThreadLocal<OMDBReferenceDecoder> mThreadLocalDecoders = new ThreadLocal<OMDBReferenceDecoder>() {
        @Override
        protected OMDBReferenceDecoder initialValue() {
            return new OMDBReferenceDecoder();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void query(MapTile tile, ITileDataSink mapDataSink) {
        // 获取tile对应的坐标范围
        if (tile.zoomLevel >= Constant.OMDB_MIN_ZOOM && tile.zoomLevel <= Constant.DATA_ZOOM) {
            Realm realm = Realm.getInstance(MapParamUtils.getTaskConfig());
            RealmQuery<ReferenceEntity> realmQuery = realm.where(ReferenceEntity.class);
            int m = Constant.DATA_ZOOM - tile.zoomLevel;
            int xStart = tile.tileX;
            int xEnd = tile.tileX + 1;
            int yStart = tile.tileY;
            int yEnd = tile.tileY + 1;
            if (m > 0) {
                xStart = (int) (xStart << m);
                xEnd = (int) (xEnd << m);
                yStart = (int) (yStart << m);
                yEnd = (int) (yEnd << m);
            }
            final int currentTileX = xStart;

            if (isUpdate) {
                realm.refresh();
                isUpdate = false;
            }

            String sql = " ((tileXMin <= " + xStart + " and tileXMax >= " + xStart + ") or (tileXMin <=" + xEnd + " and tileXMax >=" + xStart + ")) and ((tileYMin <= " + yStart + " and tileYMax >= " + yStart + ") or (tileYMin <=" + yEnd + " and tileYMin >=" + yStart + "))";

//            String sql = " tileX>=" + xStart + " and tileX<=" + xEnd + " and tileY>=" + yStart + " and tileY<=" + yEnd + "";

            if (MapParamUtils.getDataLayerEnum() != null) {
                sql += " and enable" + MapParamUtils.getDataLayerEnum().getSql();
            } else {
                sql += " and enable>=0";
            }

            realmQuery.rawPredicate(sql);
            // 筛选不显示的数据
            if (Constant.HAD_LAYER_INVISIABLE_ARRAY != null && Constant.HAD_LAYER_INVISIABLE_ARRAY.length > 0) {
                realmQuery.beginGroup();
                for (String type : Constant.HAD_LAYER_INVISIABLE_ARRAY) {
                    realmQuery.notEqualTo("table", type);
                }
                realmQuery.endGroup();
            }
            List<ReferenceEntity> listResult = realmQuery/*.distinct("id")*/.findAll();
            if (!listResult.isEmpty()) {
                Polygon tilePolygon = GeometryTools.getTilePolygon(tile);
                listResult = listResult.stream().filter((ReferenceEntity referenceEntity) -> referenceEntity.getWkt().intersects(tilePolygon))
                        /*过滤数据，只有最小x（屏幕的最小x或数据的最小x会被渲染，跨Tile的其他数据不再重复渲染）*/
//                        .filter((ReferenceEntity referenceEntity) -> MercatorProjection.longitudeToTileX(viewport.fromScreenPoint(0,0).getLongitude(), (byte) Constant.DATA_ZOOM) == currentTileX || referenceEntity.getTileX().stream().min(Integer::compare).get() == currentTileX)
                        .collect(Collectors.toList());
                mThreadLocalDecoders.get().decode(tile.zoomLevel, tile, mapDataSink, listResult);
                mapDataSink.completed(QueryResult.SUCCESS);
            } else {
                mapDataSink.completed(QueryResult.SUCCESS);
            }
            realm.close();
        } else {
            mapDataSink.completed(QueryResult.SUCCESS);
        }

    }

    @Override
    public void dispose() {

    }

    @Override
    public void cancel() {
//        if (Realm.getDefaultInstance().isInTransaction()) {
//            Realm.getDefaultInstance().cancelTransaction();
//        }
    }

    public void update() {
        isUpdate = true;
    }
}
