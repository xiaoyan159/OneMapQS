/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.8
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.gdal.ogr;

public interface ogrConstants {
    public final static int wkb25DBit = 0x80000000;
    public final static int wkb25Bit = 0x80000000;
    public final static int wkbUnknown = 0;
    public final static int wkbPoint = 1;
    public final static int wkbLineString = 2;
    public final static int wkbPolygon = 3;
    public final static int wkbMultiPoint = 4;
    public final static int wkbMultiLineString = 5;
    public final static int wkbMultiPolygon = 6;
    public final static int wkbGeometryCollection = 7;
    public final static int wkbCircularString = 8;
    public final static int wkbCompoundCurve = 9;
    public final static int wkbCurvePolygon = 10;
    public final static int wkbMultiCurve = 11;
    public final static int wkbMultiSurface = 12;
    public final static int wkbCurve = 13;
    public final static int wkbSurface = 14;
    public final static int wkbPolyhedralSurface = 15;
    public final static int wkbTIN = 16;
    public final static int wkbNone = 100;
    public final static int wkbLinearRing = 101;
    public final static int wkbCircularStringZ = 1008;
    public final static int wkbCompoundCurveZ = 1009;
    public final static int wkbCurvePolygonZ = 1010;
    public final static int wkbMultiCurveZ = 1011;
    public final static int wkbMultiSurfaceZ = 1012;
    public final static int wkbCurveZ = 1013;
    public final static int wkbSurfaceZ = 1014;
    public final static int wkbPolyhedralSurfaceZ = 1015;
    public final static int wkbTINZ = 1016;
    public final static int wkbPointM = 2001;
    public final static int wkbLineStringM = 2002;
    public final static int wkbPolygonM = 2003;
    public final static int wkbMultiPointM = 2004;
    public final static int wkbMultiLineStringM = 2005;
    public final static int wkbMultiPolygonM = 2006;
    public final static int wkbGeometryCollectionM = 2007;
    public final static int wkbCircularStringM = 2008;
    public final static int wkbCompoundCurveM = 2009;
    public final static int wkbCurvePolygonM = 2010;
    public final static int wkbMultiCurveM = 2011;
    public final static int wkbMultiSurfaceM = 2012;
    public final static int wkbCurveM = 2013;
    public final static int wkbSurfaceM = 2014;
    public final static int wkbPolyhedralSurfaceM = 2015;
    public final static int wkbTINM = 2016;
    public final static int wkbPointZM = 3001;
    public final static int wkbLineStringZM = 3002;
    public final static int wkbPolygonZM = 3003;
    public final static int wkbMultiPointZM = 3004;
    public final static int wkbMultiLineStringZM = 3005;
    public final static int wkbMultiPolygonZM = 3006;
    public final static int wkbGeometryCollectionZM = 3007;
    public final static int wkbCircularStringZM = 3008;
    public final static int wkbCompoundCurveZM = 3009;
    public final static int wkbCurvePolygonZM = 3010;
    public final static int wkbMultiCurveZM = 3011;
    public final static int wkbMultiSurfaceZM = 3012;
    public final static int wkbCurveZM = 3013;
    public final static int wkbSurfaceZM = 3014;
    public final static int wkbPolyhedralSurfaceZM = 3015;
    public final static int wkbTINZM = 3016;
    public final static int wkbPoint25D = 0x80000001;
    public final static int wkbLineString25D = 0x80000002;
    public final static int wkbPolygon25D = 0x80000003;
    public final static int wkbMultiPoint25D = 0x80000004;
    public final static int wkbMultiLineString25D = 0x80000005;
    public final static int wkbMultiPolygon25D = 0x80000006;
    public final static int wkbGeometryCollection25D = 0x80000007;
    public final static int OFTInteger = 0;
    public final static int OFTIntegerList = 1;
    public final static int OFTReal = 2;
    public final static int OFTRealList = 3;
    public final static int OFTString = 4;
    public final static int OFTStringList = 5;
    public final static int OFTWideString = 6;
    public final static int OFTWideStringList = 7;
    public final static int OFTBinary = 8;
    public final static int OFTDate = 9;
    public final static int OFTTime = 10;
    public final static int OFTDateTime = 11;
    public final static int OFTInteger64 = 12;
    public final static int OFTInteger64List = 13;
    public final static int OFSTNone = 0;
    public final static int OFSTBoolean = 1;
    public final static int OFSTInt16 = 2;
    public final static int OFSTFloat32 = 3;
    public final static int OJUndefined = 0;
    public final static int OJLeft = 1;
    public final static int OJRight = 2;
    public final static int wkbXDR = 0;
    public final static int wkbNDR = 1;
    public final static int NullFID = -1;
    public final static int ALTER_NAME_FLAG = 1;
    public final static int ALTER_TYPE_FLAG = 2;
    public final static int ALTER_WIDTH_PRECISION_FLAG = 4;
    public final static int ALTER_NULLABLE_FLAG = 8;
    public final static int ALTER_DEFAULT_FLAG = 16;
    public final static int ALTER_ALL_FLAG = 1 + 2 + 4 + 8 + 16;
    public final static int F_VAL_NULL = 0x00000001;
    public final static int F_VAL_GEOM_TYPE = 0x00000002;
    public final static int F_VAL_WIDTH = 0x00000004;
    public final static int F_VAL_ALLOW_NULL_WHEN_DEFAULT = 0x00000008;
    public final static int F_VAL_ALL = 0xFFFFFFFF;
    public final static String OLCRandomRead = "RandomRead";
    public final static String OLCSequentialWrite = "SequentialWrite";
    public final static String OLCRandomWrite = "RandomWrite";
    public final static String OLCFastSpatialFilter = "FastSpatialFilter";
    public final static String OLCFastFeatureCount = "FastFeatureCount";
    public final static String OLCFastGetExtent = "FastGetExtent";
    public final static String OLCCreateField = "CreateField";
    public final static String OLCDeleteField = "DeleteField";
    public final static String OLCReorderFields = "ReorderFields";
    public final static String OLCAlterFieldDefn = "AlterFieldDefn";
    public final static String OLCTransactions = "Transactions";
    public final static String OLCDeleteFeature = "DeleteFeature";
    public final static String OLCFastSetNextByIndex = "FastSetNextByIndex";
    public final static String OLCStringsAsUTF8 = "StringsAsUTF8";
    public final static String OLCIgnoreFields = "IgnoreFields";
    public final static String OLCCreateGeomField = "CreateGeomField";
    public final static String OLCCurveGeometries = "CurveGeometries";
    public final static String OLCMeasuredGeometries = "MeasuredGeometries";
    public final static String ODsCCreateLayer = "CreateLayer";
    public final static String ODsCDeleteLayer = "DeleteLayer";
    public final static String ODsCCreateGeomFieldAfterCreateLayer = "CreateGeomFieldAfterCreateLayer";
    public final static String ODsCCurveGeometries = "CurveGeometries";
    public final static String ODsCTransactions = "Transactions";
    public final static String ODsCEmulatedTransactions = "EmulatedTransactions";
    public final static String ODsCMeasuredGeometries = "MeasuredGeometries";
    public final static String ODrCCreateDataSource = "CreateDataSource";
    public final static String ODrCDeleteDataSource = "DeleteDataSource";
    public final static String OLMD_FID64 = "OLMD_FID64";
    public final static int OGRERR_NONE = 0;
    public final static int OGRERR_NOT_ENOUGH_DATA = 1;
    public final static int OGRERR_NOT_ENOUGH_MEMORY = 2;
    public final static int OGRERR_UNSUPPORTED_GEOMETRY_TYPE = 3;
    public final static int OGRERR_UNSUPPORTED_OPERATION = 4;
    public final static int OGRERR_CORRUPT_DATA = 5;
    public final static int OGRERR_FAILURE = 6;
    public final static int OGRERR_UNSUPPORTED_SRS = 7;
    public final static int OGRERR_INVALID_HANDLE = 8;
    public final static int OGRERR_NON_EXISTING_FEATURE = 9;
}
