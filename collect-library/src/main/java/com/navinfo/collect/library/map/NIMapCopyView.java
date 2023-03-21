package com.navinfo.collect.library.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.navinfo.collect.library.R;
import com.navinfo.collect.library.data.entity.Element;
import com.navinfo.collect.library.utils.GeometryTools;
import org.oscim.core.GeoPoint;
import org.oscim.core.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NIMapCopyView extends View {

    Paint paint;

    private List<Element> pointList = new ArrayList<>();
    private Map<String, Bitmap> bitmapMap = new HashMap<>();
    private Map<String, List<Point>> linesMap = new HashMap<>();
    private NIMapView mNiMapView;

    public NIMapCopyView(Context context, NIMapView mapView) {
        this(context, null, 0);
        this.mNiMapView = mapView;
    }

    public NIMapCopyView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public NIMapCopyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, -1);
    }

    public NIMapCopyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        paint = new Paint();
        paint.setColor(context.getColor(R.color.default_red));
        paint.setStrokeWidth(5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Element element : pointList) {
            List<Point> lines = linesMap.get(element.getId());
            if (lines != null && lines.size() > 0) {
                for (int i = 0; i < lines.size() - 1; i++) {
                    canvas.drawLine((float) lines.get(i).x, (float) lines.get(i).y,
                            (float) lines.get(i + 1).x, (float) lines.get(i + 1).y, paint);

                }
                Bitmap bitmap = bitmapMap.get(element.getId());
                if (bitmap != null) {
                    canvas.drawBitmap(bitmap, (float) lines.get(0).x - bitmap.getWidth() / 2, (float) lines.get(0).y - bitmap.getHeight(), paint);
                }
                bitmapMap.remove(element.getId());
                bitmap.recycle();
                bitmap = null;
            }
        }
    }

    public void setPointList(List<Element> pointList) {
        if (pointList == null || pointList.size() == 0) {
            this.pointList.clear();
            this.bitmapMap.clear();
            this.linesMap.clear();
            invalidate();
            return;
        }

        this.pointList = pointList;
        initData(pointList);
        invalidate();
    }

    private void initData(List<Element> pointList) {
        for (Element element : pointList) {
            List<GeoPoint> list = GeometryTools.getGeoPoints(element.getGeometry());
            List<Point> lines = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                GeoPoint geoPoint = list.get(i);
                Point point1 = new Point();
                this.mNiMapView.getVtmMap().viewport().toScreenPoint(geoPoint, false, point1);
                lines.add(point1);
            }
            linesMap.put(element.getId(), lines);
            Bitmap bitmap = BitmapFactory.decodeResource(getContext().getResources(), getBitmapId(element));
            bitmapMap.put(element.getId(), bitmap);
        }
    }

    private int getBitmapId(Element element) {
        int resId = -1;
        if (element.getTStatus() == 2) {
            if (element.getTLifecycle() == 0) {
                if (resId == -1) {
                    resId = R.mipmap.map_icon_line_pr;
                }
            } else {
                if (resId == -1) {
                    resId = R.mipmap.map_icon_line_add;
                }
            }
        } else if (element.getTStatus() == 3) {
            if (element.getTLifecycle() == 0) {
                if (resId == -1) {
                    resId = R.mipmap.map_icon_polygon_pr;
                }
            } else {
                if (resId == -1) {
                    resId = R.mipmap.map_icon_polygon_add;
                }
            }
        } else {
            if (element.getTLifecycle() == 0) {
                if (resId == -1) {
                    resId = R.mipmap.map_icon_point_pr;
                }
            } else {
                if (resId == -1) {
                    resId = R.mipmap.map_icon_point_add;
                }
            }
        }
        return resId;
    }

    public Map<String, List<GeoPoint>> getGeoPointsMap() {
        Map<String, List<GeoPoint>> map = new HashMap<>();

        for (Element element : pointList) {
            List<GeoPoint> list = new ArrayList<>();
            List<Point> lines = linesMap.get(element.getId());
            if (lines.size() > 0) {
                for (Point point : lines) {
                    GeoPoint geoPoint = this.mNiMapView.getVtmMap().viewport().fromScreenPoint((float) point.x, (float) point.y);
                    list.add(geoPoint);
                }
            }
            map.put(element.getId(), list);
        }
        return map;
    }
}
