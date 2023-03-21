package com.navinfo.collect.library.utils;

import org.oscim.core.GeoPoint;
import java.util.List;

/**
 * 测距工具
 */
public class DistanceUtil {
    private static final double EARTH_RADIUS = 6371000.0; // 平均半径,单位：m；不是赤道半径。赤道为6378左右
    static double metersPerDegree = 2.0 * Math.PI * EARTH_RADIUS / 360.0;
    static double radiansPerDegree = Math.PI / 180.0;
    static double degreesPerRadian = 180.0 / Math.PI;

    /**ee
     * 返回两个点之间的距离
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return
     */
    public static double getDistance(Double lat1, Double lng1, Double lat2, Double lng2) {

        double radiansAX = Math.toRadians(lng1); // A经弧度

        double radiansAY = Math.toRadians(lat1); // A纬弧度

        double radiansBX = Math.toRadians(lng2); // B经弧度

        double radiansBY = Math.toRadians(lat2); // B纬弧度


        double cos = Math.cos(radiansAY) * Math.cos(radiansBY) * Math.cos(radiansAX - radiansBX)

                + Math.sin(radiansAY) * Math.sin(radiansBY);

        double acos = Math.acos(cos); // 反余弦值

        return EARTH_RADIUS * acos; // 最终结果

    }

    /**
     * 平面多边形面积
     * @param points
     * @return
     */
    public static double planarPolygonAreaMeters2(List<GeoPoint> points) {
        if (points.size() < 3)
            return 0;
        double a = 0;
        for (int i = 0; i < points.size(); ++i) {
            int j = (i + 1) % points.size();
            double xi = points.get(i).getLongitude() * metersPerDegree * Math.cos(points.get(i).getLatitude() * radiansPerDegree);
            double yi = points.get(i).getLatitude() * metersPerDegree;
            double xj = points.get(j).getLongitude() * metersPerDegree * Math.cos(points.get(j).getLatitude() * radiansPerDegree);
            double yj = points.get(j).getLatitude() * metersPerDegree;
            a += xi * yj - xj * yi;
        }
        return Math.abs(a / 2);
    }

    /**
     * 球面多边形面积计算
     * @param list
     * @return
     */
    public static double sphericalPolygonAreaMeters2(List<GeoPoint> list) {
        double totalAngle = 0;
        for (int i = 0; i < list.size(); i++) {
            int j = (i + 1) % list.size();
            int k = (i + 2) % list.size();
            totalAngle += angle(list.get(i), list.get(j), list.get(k));
        }
        double planarTotalAngle = (list.size() - 2) * 180.0;
        double sphericalExcess = totalAngle - planarTotalAngle;
        if (sphericalExcess > 420.0) {
            totalAngle = list.size() * 360.0 - totalAngle;
            sphericalExcess = totalAngle - planarTotalAngle;
        } else if (sphericalExcess > 300.0 && sphericalExcess < 420.0) {
            sphericalExcess = Math.abs(360.0 - sphericalExcess);
        }
        return sphericalExcess * radiansPerDegree * EARTH_RADIUS * EARTH_RADIUS;
    }

    /**
     * 角度
     * @param p1
     * @param p2
     * @param p3
     * @return
     */
    public static double angle(GeoPoint p1, GeoPoint p2, GeoPoint p3) {
        double bearing21 = bearing(p2, p1);
        double bearing23 = bearing(p2, p3);
        double angle = bearing21 - bearing23;
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }


    /**
     * 方向
     * @param from
     * @param to
     * @return
     */
    public static double bearing(GeoPoint from, GeoPoint to) {
        double lat1 = from.getLatitude() * radiansPerDegree;
        double lon1 = from.getLongitude() * radiansPerDegree;
        double lat2 = to.getLatitude() * radiansPerDegree;
        double lon2 = to.getLongitude() * radiansPerDegree;
        double angle = -Math.atan2(Math.sin(lon1 - lon2) * Math.cos(lat2), Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));
        if (angle < 0) {
            angle += Math.PI * 2.0;
        }
        angle = angle * degreesPerRadian;
        return angle;
    }
}
