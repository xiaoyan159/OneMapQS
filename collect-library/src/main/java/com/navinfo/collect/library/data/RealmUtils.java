package com.navinfo.collect.library.data;

import android.content.Context;
import android.util.Log;

import com.google.protobuf.GeneratedMessageV3;
import com.navinfo.collect.library.data.entity.GeometryFeatureEntity;
import com.navinfo.collect.library.data.entity.LayerEntity;
import com.navinfo.collect.library.utils.GeometryTools;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.Hadlanelink;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.Hadlanemarklink;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.Objectarrow;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.Objectcrosswalk;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.Objectpole;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.Objectsymbol;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.Objecttrafficlights;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.Resultdata;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.object.BoundaryOuterClass;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.object.Boundarytype;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.object.MarkingOuterClass;
import com.navinfo.onemap.det.sdkpbf.proto.crowdsource.object.Paposition;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import kotlin.jvm.internal.Intrinsics;


public class RealmUtils {
    private Context mContext;
    private static RealmUtils instance;
    private Realm realm;
    private RealmConfiguration realmConfiguration;
    private String defaultDir = "";//NILayerManager.defaultDir;
    private String realmName;

    private final String NAME = "name";
    private final String TYPE = "navi_type";

    public void init(Context mContext, String dir, String realmName) {
        this.mContext = mContext;
        Realm.init(this.mContext);
        this.realmName = realmName;
        realmConfiguration = new RealmConfiguration.Builder()
                .directory(dir == null?new File(defaultDir): new File(dir))
                .name(realmName)
                .allowWritesOnUiThread(true)
                .allowQueriesOnUiThread(true)
                .schemaVersion(1)
                .deleteRealmIfMigrationNeeded()
                .encryptionKey(Arrays.copyOf(new String("encryp").getBytes(StandardCharsets.UTF_8), 64))
                .build();
        System.out.println("encryp:"+toHexString(Arrays.copyOf(new String("encryp").getBytes(StandardCharsets.UTF_8), 64)));
        Realm.setDefaultConfiguration(realmConfiguration);
    }

    public static RealmUtils getInstance() {
        if (instance == null) {
            instance = new RealmUtils();
        }
        return instance;
    }

    public Realm getRealm() throws Exception {
        if (realmConfiguration == null) {
            throw new Exception("请先调用Realm.init方法初始化！");
        }
        return Realm.getDefaultInstance();
    }

    public RealmConfiguration getRealmConfiguration() {
        return realmConfiguration;
    }


    private static final char[] HEX_CHAR = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    /**
     * 方法一：将byte类型数组转化成16进制字符串
     * @explain 字符串拼接
     * @param bytes
     * @return
     */
    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int num;
        for (byte b : bytes) {
            num = b < 0 ? 256 + b : b;
            sb.append(HEX_CHAR[num / 16]).append(HEX_CHAR[num % 16]);
        }
        return sb.toString();
    }

    public float getFileSize(){

        File file = new File(defaultDir+"/"+realmName);

        if(file.exists()&&file.isFile()){
            return file.length();
        }

        return 0;
    }

    public Map<String, Object> importPbfData(List<String> files) throws IOException {
        NavinfoPbfData pbfData = new NavinfoPbfData();
        long startTime = System.currentTimeMillis();
        RealmUtils var10000 = RealmUtils.getInstance();
        Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
        Realm.getInstance(var10000.getRealmConfiguration()).beginTransaction();
        Iterator fileIterator = files.iterator();

        while(fileIterator.hasNext()) {
            String file = (String)fileIterator.next();
            Log.d("Realm-file:", file);
            System.out.print("file:" + file);
            Resultdata.ResultData resultData = pbfData.readPbfData((InputStream)(new FileInputStream(new File(file))));
            long sqlStartTime = System.currentTimeMillis();
            Intrinsics.checkNotNullExpressionValue(resultData, "resultData");
            List var40 = resultData.getHadlanelinkList();
            Intrinsics.checkNotNullExpressionValue(var40, "resultData.hadlanelinkList");
            Collection var9 = (Collection)var40;
            HashMap extentMapx;
            Iterator var12;
            Geometry geometry;
            GeometryFeatureEntity geometryEntityx;
            LayerEntity layerEntity;
            if (!var9.isEmpty()) {
                layerEntity = new LayerEntity();
                layerEntity.setLayerName("test");
                extentMapx = new HashMap();
                ((Map)extentMapx).put(NAME, "车道中心线");
                ((Map)extentMapx).put(TYPE, "had_lane_link");
                var12 = resultData.getHadlanelinkList().iterator();

                while(var12.hasNext()) {
                    Hadlanelink.HadLaneLink hadLaneLink = (Hadlanelink.HadLaneLink)var12.next();
                    Intrinsics.checkNotNullExpressionValue(hadLaneLink, "hadLaneLink");
                    geometry = pbfData.createGeometry((GeneratedMessageV3)hadLaneLink.getGeometry());
                    geometryEntityx = pbfData.createGeometryEntity(geometry, hadLaneLink.getAllFields(), (Map)extentMapx);
                    Intrinsics.checkNotNullExpressionValue(geometryEntityx, "geometryEntity");
                    geometryEntityx.setLayerEntity(layerEntity);
                    geometryEntityx.setName((String)extentMapx.get(NAME));
                    var10000 = RealmUtils.getInstance();
                    Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
                    Realm.getInstance(var10000.getRealmConfiguration()).insertOrUpdate((RealmModel)geometryEntityx);
                }
            }

            var40 = resultData.getHadlanemarklinkList();
            Intrinsics.checkNotNullExpressionValue(var40, "resultData.hadlanemarklinkList");
            var9 = (Collection)var40;
            Geometry lanLinkGeometry;
            Geometry intersection;
            if (!var9.isEmpty()) {
                layerEntity = new LayerEntity();
                layerEntity.setLayerName("test");
                extentMapx = new HashMap();
                ((Map)extentMapx).put(NAME, "车道边线");
                ((Map)extentMapx).put(TYPE, "had_lane_mark_link");
                var12 = resultData.getHadlanemarklinkList().iterator();

                while(var12.hasNext()) {
                    Hadlanemarklink.HadLaneMarkLink hadLaneMarkLink = (Hadlanemarklink.HadLaneMarkLink)var12.next();
                    Intrinsics.checkNotNullExpressionValue(hadLaneMarkLink, "hadLaneMarkLink");
                    geometry = pbfData.createGeometry((GeneratedMessageV3)hadLaneMarkLink.getGeometry());
                    Iterator var15 = hadLaneMarkLink.getBoundaryList().iterator();

                    while(var15.hasNext()) {
                        BoundaryOuterClass.Boundary boundary = (BoundaryOuterClass.Boundary)var15.next();
                        Intrinsics.checkNotNullExpressionValue(boundary, "boundary");
                        Paposition.PaPosition var43 = boundary.getPaPosition();
                        Intrinsics.checkNotNullExpressionValue(var43, "boundary.paPosition");
                        com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Point3d sPoint = var43.getSIndex();
                        var43 = boundary.getPaPosition();
                        Intrinsics.checkNotNullExpressionValue(var43, "boundary.paPosition");
                        com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Point3d ePoint = var43.getEIndex();
                        Coordinate[] var45 = new Coordinate[2];
                        Intrinsics.checkNotNullExpressionValue(sPoint, "sPoint");
                        com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Point2d var10005 = sPoint.getLatLon();
                        Intrinsics.checkNotNullExpressionValue(var10005, "sPoint.latLon");
                        double fileIterator0 = var10005.getLongitudeDegrees();
                        com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.Point2d var10006 = sPoint.getLatLon();
                        Intrinsics.checkNotNullExpressionValue(var10006, "sPoint.latLon");
                        double fileIterator3 = var10006.getLatitudeDegrees();
                        com.navinfo.onemap.det.sdkpbf.proto.geometry.Geometry.ElevationMeasure var10007 = sPoint.getElevation();
                        Intrinsics.checkNotNullExpressionValue(var10007, "sPoint.elevation");
                        var45[0] = new Coordinate(fileIterator0, fileIterator3, var10007.getZ());
                        Intrinsics.checkNotNullExpressionValue(ePoint, "ePoint");
                        var10005 = ePoint.getLatLon();
                        Intrinsics.checkNotNullExpressionValue(var10005, "ePoint.latLon");
                        fileIterator0 = var10005.getLongitudeDegrees();
                        var10006 = ePoint.getLatLon();
                        Intrinsics.checkNotNullExpressionValue(var10006, "ePoint.latLon");
                        fileIterator3 = var10006.getLatitudeDegrees();
                        var10007 = ePoint.getElevation();
                        Intrinsics.checkNotNullExpressionValue(var10007, "ePoint.elevation");
                        var45[1] = new Coordinate(fileIterator0, fileIterator3, var10007.getZ());
                        LineString line = GeometryTools.getLineStrinGeo(var45);
                        lanLinkGeometry = line.buffer(1.0E-7D, 1, 2);
                        intersection = geometry.intersection(lanLinkGeometry);
                        extentMapx.remove("boundary_type");
                        extentMapx.remove("mark_type");
                        extentMapx.remove("mark_color");
                        extentMapx.remove("mark_material");
                        extentMapx.remove("mark_width");
                        if (boundary.getBoundaryType() != null) {
                            Map var46 = (Map)extentMapx;
                            Boundarytype.BoundaryType2 var10002 = boundary.getBoundaryType();
                            Intrinsics.checkNotNullExpressionValue(var10002, "boundary.boundaryType");
                            var46.put("boundary_type", String.valueOf(var10002.getType()));
                            Boundarytype.BoundaryType2 var49 = boundary.getBoundaryType();
                            Intrinsics.checkNotNullExpressionValue(var49, "boundary.boundaryType");
                            if (var49.getType() == 2 && boundary.getMarkingList() != null) {
                                var40 = boundary.getMarkingList();
                                Intrinsics.checkNotNullExpressionValue(var40, "boundary.markingList");
                                Collection var21 = (Collection)var40;
                                if (!var21.isEmpty()) {
                                    var46 = (Map)extentMapx;
                                    Object var48 = boundary.getMarkingList().get(0);
                                    Intrinsics.checkNotNullExpressionValue(var48, "boundary.markingList[0]");
                                    var46.put("mark_type", String.valueOf(((MarkingOuterClass.Marking)var48).getMarkType()));
                                    var46 = (Map)extentMapx;
                                    var48 = boundary.getMarkingList().get(0);
                                    Intrinsics.checkNotNullExpressionValue(var48, "boundary.markingList[0]");
                                    var46.put("mark_color", String.valueOf(((MarkingOuterClass.Marking)var48).getMarkColor()));
                                    var46 = (Map)extentMapx;
                                    var48 = boundary.getMarkingList().get(0);
                                    Intrinsics.checkNotNullExpressionValue(var48, "boundary.markingList[0]");
                                    var46.put("mark_material", String.valueOf(((MarkingOuterClass.Marking)var48).getMarkMaterial()));
                                    var46 = (Map)extentMapx;
                                    var48 = boundary.getMarkingList().get(0);
                                    Intrinsics.checkNotNullExpressionValue(var48, "boundary.markingList[0]");
                                    var46.put("mark_width", String.valueOf(((MarkingOuterClass.Marking)var48).getMarkWidth()));
                                }
                            }
                        }

                        GeometryFeatureEntity geometryEntity = pbfData.createGeometryEntity(intersection, boundary.getAllFields(), (Map)extentMapx);
                        Intrinsics.checkNotNullExpressionValue(geometryEntity, "geometryEntity");
                        geometryEntity.setLayerEntity(layerEntity);
                        geometryEntity.setName((String)extentMapx.get(NAME));
                        var10000 = RealmUtils.getInstance();
                        Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
                        Realm.getInstance(var10000.getRealmConfiguration()).insertOrUpdate((RealmModel)geometryEntity);
                    }
                }
            }

            var40 = resultData.getObjectarrowList();
            Intrinsics.checkNotNullExpressionValue(var40, "resultData.objectarrowList");
            var9 = (Collection)var40;
            if (!var9.isEmpty()) {
                layerEntity = new LayerEntity();
                layerEntity.setLayerName("test");
                extentMapx = new HashMap();
                ((Map)extentMapx).put(NAME, "道路箭头");
                ((Map)extentMapx).put(TYPE, "object_arrow");
                var12 = resultData.getObjectarrowList().iterator();

                label143:
                while(true) {
                    label141:
                    while(true) {
                        if (!var12.hasNext()) {
                            break label143;
                        }

                        Objectarrow.ObjectArrow objectArrow = (Objectarrow.ObjectArrow)var12.next();
                        Intrinsics.checkNotNullExpressionValue(objectArrow, "objectArrow");
                        geometry = pbfData.createGeometry((GeneratedMessageV3)objectArrow.getGeometry());
                        geometryEntityx = pbfData.createGeometryEntity(geometry, objectArrow.getAllFields(), (Map)extentMapx);
                        Intrinsics.checkNotNullExpressionValue(geometryEntityx, "geometryEntity");
                        geometryEntityx.setLayerEntity(layerEntity);
                        geometryEntityx.setName((String)extentMapx.get(NAME));
                        var10000 = RealmUtils.getInstance();
                        Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
                        Realm.getInstance(var10000.getRealmConfiguration()).insertOrUpdate((RealmModel)geometryEntityx);
                        Iterator var41 = objectArrow.getLanePidList().iterator();

                        while(var41.hasNext()) {
                            String lanLinkPid = (String)var41.next();
                            Iterator var47 = resultData.getHadlanelinkList().iterator();

                            while(var47.hasNext()) {
                                Hadlanelink.HadLaneLink lanLink = (Hadlanelink.HadLaneLink)var47.next();
                                Intrinsics.checkNotNullExpressionValue(lanLink, "lanLink");
                                if (lanLinkPid.equals(lanLink.getLaneLinkPid())) {
                                    lanLinkGeometry = pbfData.createGeometry((GeneratedMessageV3)lanLink.getGeometry());
                                    if (lanLinkGeometry.intersects(geometry)) {
                                        intersection = geometry.intersection(lanLinkGeometry);
                                        Intrinsics.checkNotNullExpressionValue(intersection, "intersection");
                                        if (intersection.isValid() && !intersection.isEmpty() && intersection instanceof LineString) {
                                            HashMap extentMap = new HashMap();
                                            ((Map)extentMap).put(TYPE, "symbol_object_arrow");
                                            ((Map)extentMap).put(NAME, "symbol_object_arrow");
                                            ((Map)extentMap).put("arrow_class", String.valueOf(objectArrow.getArrowClass()));
                                            GeometryFeatureEntity geometryEntityxx = pbfData.createGeometryEntity(intersection, (Map)null, (Map)extentMap);
                                            Intrinsics.checkNotNullExpressionValue(geometryEntityxx, "geometryEntity");
                                            geometryEntityxx.setLayerEntity(layerEntity);
                                            geometryEntityxx.setName((String)extentMap.get(NAME));
                                            var10000 = RealmUtils.getInstance();
                                            Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
                                            Realm.getInstance(var10000.getRealmConfiguration()).insertOrUpdate((RealmModel)geometryEntityxx);
                                        }
                                        continue label141;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            var40 = resultData.getObjectcrosswalkList();
            Intrinsics.checkNotNullExpressionValue(var40, "resultData.objectcrosswalkList");
            var9 = (Collection)var40;
            if (!var9.isEmpty()) {
                layerEntity = new LayerEntity();
                layerEntity.setLayerName("test");
                extentMapx = new HashMap();
                ((Map)extentMapx).put(NAME, "人行横道");
                ((Map)extentMapx).put(TYPE, "object_crosswalk");
                var12 = resultData.getObjectcrosswalkList().iterator();

                while(var12.hasNext()) {
                    Objectcrosswalk.ObjectCrossWalk objectCross = (Objectcrosswalk.ObjectCrossWalk)var12.next();
                    Intrinsics.checkNotNullExpressionValue(objectCross, "objectCross");
                    geometry = pbfData.createGeometry((GeneratedMessageV3)objectCross.getGeometry());
                    geometryEntityx = pbfData.createGeometryEntity(geometry, objectCross.getAllFields(), (Map)extentMapx);
                    Intrinsics.checkNotNullExpressionValue(geometryEntityx, "geometryEntity");
                    geometryEntityx.setLayerEntity(layerEntity);
                    geometryEntityx.setName((String)extentMapx.get(NAME));
                    var10000 = RealmUtils.getInstance();
                    Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
                    Realm.getInstance(var10000.getRealmConfiguration()).insertOrUpdate((RealmModel)geometryEntityx);
                }
            }

            var40 = resultData.getObjectpoleList();
            Intrinsics.checkNotNullExpressionValue(var40, "resultData.objectpoleList");
            var9 = (Collection)var40;
            HashMap extentMap3D;
            Iterator var36;
            Geometry geometryx;
            GeometryFeatureEntity geometryEntityxxx;
            GeometryFeatureEntity symbolEntity;
            if (!var9.isEmpty()) {
                layerEntity = new LayerEntity();
                layerEntity.setLayerName("test");
                extentMapx = new HashMap();
                ((Map)extentMapx).put(NAME, "杆状物");
                ((Map)extentMapx).put(TYPE, "object_pole");
                extentMap3D = new HashMap();
                ((Map)extentMap3D).put(TYPE, "symbol_object_pole");
                ((Map)extentMap3D).put(NAME, "symbol_object_pole");
                var36 = resultData.getObjectpoleList().iterator();

                while(var36.hasNext()) {
                    Objectpole.ObjectPole objectPole = (Objectpole.ObjectPole)var36.next();
                    Intrinsics.checkNotNullExpressionValue(objectPole, "objectPole");
                    geometryx = pbfData.createGeometry((GeneratedMessageV3)objectPole.getGeometry());
                    geometryEntityxxx = pbfData.createGeometryEntity(geometryx, objectPole.getAllFields(), (Map)extentMapx);
                    Intrinsics.checkNotNullExpressionValue(geometryEntityxxx, "geometryEntity");
                    geometryEntityxxx.setLayerEntity(layerEntity);
                    geometryEntityxxx.setName((String)extentMapx.get(TYPE));
                    var10000 = RealmUtils.getInstance();
                    Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
                    Realm.getInstance(var10000.getRealmConfiguration()).insertOrUpdate((RealmModel)geometryEntityxxx);
                    Intrinsics.checkNotNullExpressionValue(geometryx, "geometry");
                    symbolEntity = pbfData.createGeometryEntity((Geometry)geometryx.getCentroid(), (Map)null, (Map)extentMap3D);
                    Intrinsics.checkNotNullExpressionValue(symbolEntity, "symbolEntity");
                    symbolEntity.setLayerEntity(layerEntity);
                    symbolEntity.setName((String)extentMap3D.get(NAME));
                    var10000 = RealmUtils.getInstance();
                    Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
                    Realm.getInstance(var10000.getRealmConfiguration()).insertOrUpdate((RealmModel)symbolEntity);
                }
            }

            var40 = resultData.getObjectsymbolList();
            Intrinsics.checkNotNullExpressionValue(var40, "resultData.objectsymbolList");
            var9 = (Collection)var40;
            if (!var9.isEmpty()) {
                layerEntity = new LayerEntity();
                layerEntity.setLayerName("test");
                extentMapx = new HashMap();
                ((Map)extentMapx).put(NAME, "对象标志");
                ((Map)extentMapx).put(TYPE, "object_symbol");
                extentMap3D = new HashMap();
                ((Map)extentMap3D).put(NAME, "symbol_object_symbol");
                ((Map)extentMap3D).put(TYPE, "symbol_object_symbol");
                var36 = resultData.getObjectsymbolList().iterator();

                while(var36.hasNext()) {
                    Objectsymbol.ObjectSymbol objectSymbol = (Objectsymbol.ObjectSymbol)var36.next();
                    Intrinsics.checkNotNullExpressionValue(objectSymbol, "objectSymbol");
                    geometryx = pbfData.createGeometry((GeneratedMessageV3)objectSymbol.getGeometry());
                    geometryEntityxxx = pbfData.createGeometryEntity(geometryx, objectSymbol.getAllFields(), (Map)extentMapx);
                    Intrinsics.checkNotNullExpressionValue(geometryEntityxxx, "geometryEntity");
                    geometryEntityxxx.setLayerEntity(layerEntity);
                    geometryEntityxxx.setName((String)extentMapx.get(NAME));
                    var10000 = RealmUtils.getInstance();
                    Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
                    Realm.getInstance(var10000.getRealmConfiguration()).insertOrUpdate((RealmModel)geometryEntityxxx);
                    Intrinsics.checkNotNullExpressionValue(geometryx, "geometry");
                    symbolEntity = pbfData.createGeometryEntity((Geometry)geometryx.getCentroid(), (Map)null, (Map)extentMap3D);
                    Intrinsics.checkNotNullExpressionValue(symbolEntity, "symbolEntity");
                    symbolEntity.setLayerEntity(layerEntity);
                    symbolEntity.setName((String)extentMap3D.get(NAME));
                    var10000 = RealmUtils.getInstance();
                    Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
                    Realm.getInstance(var10000.getRealmConfiguration()).insertOrUpdate((RealmModel)symbolEntity);
                }
            }

            var40 = resultData.getObjecttrafficlightsList();
            Intrinsics.checkNotNullExpressionValue(var40, "resultData.objecttrafficlightsList");
            var9 = (Collection)var40;
            if (!var9.isEmpty()) {
                layerEntity = new LayerEntity();
                layerEntity.setLayerName("test");
                extentMapx = new HashMap();
                ((Map)extentMapx).put(NAME, "交通灯");
                ((Map)extentMapx).put(TYPE, "object_traffic");
                extentMap3D = new HashMap();
                ((Map)extentMap3D).put(TYPE, "symbol_object_traffic");
                ((Map)extentMap3D).put(NAME, "symbol_object_traffic");
                var36 = resultData.getObjecttrafficlightsList().iterator();

                while(var36.hasNext()) {
                    Objecttrafficlights.ObjectTrafficLights objectTrrafic = (Objecttrafficlights.ObjectTrafficLights)var36.next();
                    Intrinsics.checkNotNullExpressionValue(objectTrrafic, "objectTrrafic");
                    geometryx = pbfData.createGeometry((GeneratedMessageV3)objectTrrafic.getGeometry());
                    geometryEntityxxx = pbfData.createGeometryEntity(geometryx, objectTrrafic.getAllFields(), (Map)extentMapx);
                    Intrinsics.checkNotNullExpressionValue(geometryEntityxxx, "geometryEntity");
                    geometryEntityxxx.setLayerEntity(layerEntity);
                    geometryEntityxxx.setName((String)extentMapx.get(NAME));
                    var10000 = RealmUtils.getInstance();
                    Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
                    Realm.getInstance(var10000.getRealmConfiguration()).insertOrUpdate((RealmModel)geometryEntityxxx);
                    Intrinsics.checkNotNullExpressionValue(geometryx, "geometry");
                    symbolEntity = pbfData.createGeometryEntity((Geometry)geometryx.getCentroid(), (Map)null, (Map)extentMap3D);
                    Intrinsics.checkNotNullExpressionValue(symbolEntity, "symbolEntity");
                    symbolEntity.setLayerEntity(layerEntity);
                    symbolEntity.setName((String)extentMap3D.get(NAME));
                    var10000 = RealmUtils.getInstance();
                    Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
                    Realm.getInstance(var10000.getRealmConfiguration()).insertOrUpdate((RealmModel)symbolEntity);
                }
            }

            long sqlEndTime = System.currentTimeMillis();
            System.out.println(file + "All-Time:" + (sqlEndTime - sqlStartTime));
        }

        var10000 = RealmUtils.getInstance();
        Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
        Realm.getInstance(var10000.getRealmConfiguration()).commitTransaction();
        long endTime = System.currentTimeMillis();
        System.out.println("All-Time:" + (endTime - startTime));
        var10000 = RealmUtils.getInstance();
        Intrinsics.checkNotNullExpressionValue(var10000, "RealmUtils.getInstance()");
        Realm.compactRealm(var10000.getRealmConfiguration());
        System.out.println("Arrow-All-Time: 数据处理结束");
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("success", true);
        resultMap.put("msg", "导入成功");
        resultMap.put("data", (endTime - startTime)+"");
        return resultMap;
    }
}
