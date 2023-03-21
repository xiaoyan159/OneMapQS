package com.navinfo.collect.library.map.layers;


import android.util.Log;

import com.navinfo.collect.library.utils.GeometryTools;

import org.locationtech.jts.geom.Polygon;
import org.oscim.backend.CanvasAdapter;
import org.oscim.core.GeoPoint;
import org.oscim.core.Point;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.JtsDrawable;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;
import org.oscim.utils.GeoPointUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*
 *com.nmp.map.layer
 *zhjch
 *2021/9/28
 *9:28
 *说明（）
 */
public class NIPolygonLayer extends VectorLayer {
    private JtsDrawable mDrawable;
    protected Style mStyle;
    protected final ArrayList<GeoPoint> mPoints = new ArrayList<>();
    private boolean isClose = true;

    public NIPolygonLayer(Map map, Style style) {
        super(map);
        mStyle = style;
    }

    public NIPolygonLayer(Map map, int lineColor, float lineWidth) {
        this(map, Style.builder()
                .fixed(true)
                .strokeColor(lineColor)
                .strokeWidth(lineWidth)
                .build());
    }

    public NIPolygonLayer(Map map, int lineColor) {
        this(map, lineColor, 2);
    }

    /**
     * 设置polygon的点位
     */
    public void setPolygonString(List<GeoPoint> pointList, boolean isClose) {

    }

    /**
     * 移除正在绘制的的图形
     */
    public void removeCurrentDrawable() {
        if (mDrawable != null) {
            remove(mDrawable);
            mDrawable = null;
        }
    }


    /**
     * @param :
     * @return :
     * @method : getPolygon
     * @Author : xiaoxiao
     * @Describe : 获取当前polygon的wkt
     * @Date : 2018/12/18
     */
    public Polygon getPolygon() {
        if (mPoints == null || mPoints.size() < 3) {
            return null;
        }
        if (mPoints.get(0).distance(mPoints.get(mPoints.size() - 1)) > 0) {
            mPoints.add(mPoints.get(0));
        }
        return GeometryTools.createPolygon(mPoints);
    }


    private final Point mPoint1 = new Point();
    private final Point mPoint2 = new Point();


    public void setStyle(Style style) {
        mStyle = style;
    }

    public void clearPath() {
        if (!mPoints.isEmpty())
            mPoints.clear();

        updatePoints();
    }

    public void setPoints(Collection<? extends GeoPoint> pts) {
        mPoints.clear();
        mPoints.addAll(pts);
        updatePoints();
    }

    public void addPoint(GeoPoint pt) {
        mPoints.add(pt);
        updatePoints();
    }

    public void addPoint(int latitudeE6, int longitudeE6) {
        mPoints.add(new GeoPoint(latitudeE6, longitudeE6));
        updatePoints();
    }

    public void addPoints(Collection<? extends GeoPoint> pts) {
        mPoints.addAll(pts);
        updatePoints();
    }

    private void updatePoints() {

        if (mPoints.size() > 2 && !GeometryTools.createGeometry(mPoints.get(0)).equals(GeometryTools.createGeometry(mPoints.get(mPoints.size() - 1)))) {
            mPoints.add(mPoints.get(0));
        }
        synchronized (this) {
            removeCurrentDrawable();
            if (mPoints.size() == 2) {
                mDrawable = new LineDrawable(mPoints, mStyle);
                add(mDrawable);
            } else if (mPoints.size() > 3) {
                //绘制报错，现用线代替，后续解决
                // mDrawable = new PolygonDrawable(mPoints, mStyle);
                mDrawable = new LineDrawable(mPoints, mStyle);
                add(mDrawable);
            }else {
                if(mPoints.size()>0){
                    mDrawable = new PointDrawable(mPoints.get(0),mStyle);
                    add(mDrawable);
                }
            }

            if (mPoints.size() > 3 && mPoints.get(0) == mPoints.get(mPoints.size() - 1)) {
                mPoints.remove(mPoints.size() - 1);
            }

        }
        mWorker.submit(0);
    }

    public List<GeoPoint> getPoints() {
        return mPoints;
    }


    @Override
    public synchronized boolean contains(float x, float y) {
        // Touch min 20 px at baseline mdpi (160dpi)
        double distance = Math.max(20 / 2 * CanvasAdapter.getScale(), mStyle.strokeWidth);
        for (int i = 0; i < mPoints.size() - 1; i++) {
            if (i == 0)
                mMap.viewport().toScreenPoint(mPoints.get(i), false, mPoint1);
            else {
                mPoint1.x = mPoint2.x;
                mPoint1.y = mPoint2.y;
            }
            mMap.viewport().toScreenPoint(mPoints.get(i + 1), false, mPoint2);
            if (GeoPointUtils.distanceSegmentPoint(mPoint1.x, mPoint1.y, mPoint2.x, mPoint2.y, x, y) <= distance)
                return true;
        }
        return false;
    }
}