package com.navinfo.collect.library.map.source;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.navinfo.collect.library.data.entity.RenderEntity;
import com.navinfo.collect.library.system.Constant;
import com.navinfo.collect.library.utils.GeometryTools;
import com.navinfo.collect.library.utils.MapParamUtils;

import org.locationtech.jts.geom.Polygon;
import org.oscim.core.MapPosition;
import org.oscim.layers.tile.MapTile;
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmQuery;

public class OMDBTileDataSource implements ITileDataSource {

    class RealmObject {
        int threadCode;
        int realmConfigCode;
        Realm realm;
    }

//    class DataObject {
//        int threadCode = 0;
//        byte zoom = 0;
//        String lonLat = "";
//        List<String> listIds = new ArrayList<>();
//    }

    private boolean isUpdate;
    private Viewport viewport;

    private List<RealmObject> realmObjectList = new ArrayList<>();

//    private List<DataObject> dataObjectList = new ArrayList<>();

    private final ThreadLocal<OMDBDataDecoder> mThreadLocalDecoders = new ThreadLocal<OMDBDataDecoder>() {
        @Override
        protected OMDBDataDecoder initialValue() {
            return new OMDBDataDecoder();
        }
    };

    public OMDBTileDataSource(Viewport viewport) {
        this.viewport = viewport;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void query(MapTile tile, ITileDataSink mapDataSink) {
        // 获取tile对应的坐标范围
        if (tile.zoomLevel >= Constant.OMDB_MIN_ZOOM && tile.zoomLevel <= Constant.DATA_ZOOM) {
            Realm realm = null;
            int threadCode = Thread.currentThread().hashCode();
//            MapPosition pos = new MapPosition();
//            viewport.getMapPosition(pos);
//            DataObject newDataObject = new DataObject();
//            newDataObject.zoom = tile.zoomLevel;
//            newDataObject.threadCode = threadCode;
//            newDataObject.lonLat = pos.getX() + "," + pos.getY();
            synchronized (realmObjectList) {
                int configCode = MapParamUtils.getTaskConfig().hashCode();
                for (RealmObject object : realmObjectList) {
                    if (object.threadCode == threadCode) {
                        if (object.realmConfigCode == configCode) {
                            realm = object.realm;
                        } else {
                            object.realm.close();
                            realmObjectList.remove(object);
                        }
                        break;
                    }
                }
                if (realm == null) {
                    realm = Realm.getInstance(MapParamUtils.getTaskConfig());
                    RealmObject o = new RealmObject();
                    o.threadCode = threadCode;
                    o.realmConfigCode = configCode;
                    o.realm = realm;
                    realmObjectList.add(o);
                }
            }
//            Log.e("jingo", " " + Realm.getDefaultInstance().hashCode() + " " + Realm.getInstance(MapParamUtils.getTaskConfig()).hashCode());

//            Realm realm = Realm.getInstance(MapParamUtils.getTaskConfig());
            RealmQuery<RenderEntity> realmQuery = realm.where(RenderEntity.class);
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
            if (MapParamUtils.getDataLayerEnum() != null) {
                sql += " and enable" + MapParamUtils.getDataLayerEnum().getSql();
            } else {
                sql += " and enable>=0";
            }

            realmQuery.rawPredicate(sql);
            if (MapParamUtils.getDataLayerEnum() != null) {
                MapParamUtils.getDataLayerEnum().getSql();
            }

//            realmQuery.greaterThanOrEqualTo("tileXMin", xStart);
//            realmQuery.lessThanOrEqualTo("tileXMax", xEnd);
//            realmQuery.greaterThanOrEqualTo("tileYMin", yStart);
//            realmQuery.lessThanOrEqualTo("tileYMax", yEnd);
//            realmQuery.like("geometry","116.31509664888955 39.83318797612014 0");
            // 筛选不显示的数据
            if (Constant.HAD_LAYER_INVISIABLE_ARRAY != null && Constant.HAD_LAYER_INVISIABLE_ARRAY.length > 0) {
                realmQuery.beginGroup();
                for (String type : Constant.HAD_LAYER_INVISIABLE_ARRAY) {
                    realmQuery.notEqualTo("table", type);
                }
                realmQuery.endGroup();
            }
            long time = System.currentTimeMillis();
            List<RenderEntity> listResult = realmQuery/*.distinct("id")*/.findAll();
            long newTime = System.currentTimeMillis() - time;

            Log.e("jingo", "当前OMDBTileDataSource " + Thread.currentThread().hashCode() + " 当前realm " + realm.hashCode() + " 查询耗时" + newTime );
            // 数据记录的tile号是以正外接tile号列表，此处过滤并未与当前tile相交的数据
            if (!listResult.isEmpty()) {
                Polygon tilePolygon = GeometryTools.getTilePolygon(tile);
//                System.out.println("第一条数据的最小x值:" + listResult.get(0).getTileX().stream().min(Integer::compare).get());
//                System.out.println("当前tile的:" + listResult.get(0).getTileX().stream().min(Integer::compare).get());
//                synchronized (dataObjectList) {
//                    int index = -1;
//                    for (int i = 0; i < dataObjectList.size(); i++) {
//                        DataObject dataObject = dataObjectList.get(i);
//                        if (dataObject.threadCode == newDataObject.threadCode) {
//                            index = i;
//                        } else if (dataObject.zoom == tile.zoomLevel && dataObject.lonLat.equals(newDataObject.lonLat)) {
//                            listResult = listResult.stream().filter((RenderEntity renderEntity) -> {
//                                        for (String id : dataObject.listIds) {
//                                            if (id.equals(renderEntity.getId())) {
//                                                return false;
//                                            }
//                                        }
//                                        return renderEntity.getWkt().intersects(tilePolygon);
//                                    })
//                                    /*过滤数据，只有最小x（屏幕的最小x或数据的最小x会被渲染，跨Tile的其他数据不再重复渲染）*/
////                        .filter((RenderEntity renderEntity) -> MercatorProjection.longitudeToTileX(viewport.fromScreenPoint(0,0).getLongitude(), (byte) Constant.DATA_ZOOM) == currentTileX || renderEntity.getTileX().stream().min(Integer::compare).get() == currentTileX)
//                                    .collect(Collectors.toList());
//                        }
//                    }
//                    if (index > -1) {
//                        dataObjectList.remove(index);
//                    }
//                    for (RenderEntity renderEntity : listResult) {
//                        newDataObject.listIds.add(renderEntity.getId());
//                    }
//                    dataObjectList.add(newDataObject);
//                }
                listResult = listResult.stream().filter((RenderEntity renderEntity) ->
                                renderEntity.getWkt().intersects(tilePolygon)
                        )
                        /*过滤数据，只有最小x（屏幕的最小x或数据的最小x会被渲染，跨Tile的其他数据不再重复渲染）*/
//                        .filter((RenderEntity renderEntity) -> MercatorProjection.longitudeToTileX(viewport.fromScreenPoint(0,0).getLongitude(), (byte) Constant.DATA_ZOOM) == currentTileX || renderEntity.getTileX().stream().min(Integer::compare).get() == currentTileX)
                        .collect(Collectors.toList());
                mThreadLocalDecoders.get().decode(tile.zoomLevel, tile, mapDataSink, listResult);
                mapDataSink.completed(QueryResult.SUCCESS);
            } else {
                mapDataSink.completed(QueryResult.SUCCESS);
            }

//            realm.close();
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
