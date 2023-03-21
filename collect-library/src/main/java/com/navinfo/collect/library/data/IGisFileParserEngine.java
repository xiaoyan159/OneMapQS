package com.navinfo.collect.library.data;

import com.navinfo.collect.library.data.entity.LayerEntity;

import org.locationtech.jts.geom.Geometry;

import java.io.File;
import java.util.List;

/**
 * 常用Gis文件的解析写入引擎
 * */
public interface IGisFileParserEngine {
    void initEngine();

    List<LayerEntity> parserGisFile(File gisFile);
}
