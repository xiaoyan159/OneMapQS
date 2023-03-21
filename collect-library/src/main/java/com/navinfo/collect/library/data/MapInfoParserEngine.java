package com.navinfo.collect.library.data;

import com.navinfo.collect.library.data.entity.GeometryFeatureEntity;
import com.navinfo.collect.library.data.entity.LayerEntity;

import org.gdal.gdal.gdal;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.ogr;
import org.locationtech.jts.geom.Geometry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MapInfoParserEngine extends GisFileParserEngine {

    /**
     * 读取MapInfo文件
     * */
    @Override
    public List<LayerEntity> parserGisFile(File mifFile) {
        DataSource ds = ogr.Open(mifFile.getAbsolutePath(), 0);
        if (ds == null) {
            System.out.println("打开文件失败");
            return null;
        }
        System.out.println("打开文件成功");
        List<LayerEntity> layerEntityList = new ArrayList<>();
        if (ds.GetLayerCount()>0) {
            for (int i = 0; i < ds.GetLayerCount(); i++) {
                org.gdal.ogr.Layer layer = ds.GetLayer(i);
                if (layer == null) {
                    System.out.println("获取第"+i+"个图层失败");
                    continue;
                }

                LayerEntity layerEntity = new LayerEntity();
                layerEntity.setLayerName(layer.GetName());
                layerEntity.setGeomType(layer.GetGeomType());
                layerEntity.setFromDataName(mifFile.getName());


                System.out.println("读取到Layer" + layer.GetName());
                layer.ResetReading();
                for (int j = 0; j < layer.GetFeatureCount(); j++) {
                    GeometryFeatureEntity entity = new GeometryFeatureEntity();
                    Feature feature = layer.GetFeature(j);
                    entity.setGeometry(feature.GetGeometryRef().toString());
                }
            }
        }
        return layerEntityList;
    }
}
