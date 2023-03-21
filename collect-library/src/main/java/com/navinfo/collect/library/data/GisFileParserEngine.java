package com.navinfo.collect.library.data;

import org.gdal.gdal.gdal;
import org.gdal.ogr.ogr;

/**
 * 使用gdal和ogr解析Gis数据的引擎
 */
public abstract class GisFileParserEngine implements IGisFileParserEngine {
    public GisFileParserEngine() {
        initEngine();
    }

    @Override
    public void initEngine() {
        ogr.RegisterAll();
        gdal.AllRegister();
        gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
        gdal.SetConfigOption("SHAPE_ENCODING", "CP936");
    }
}
