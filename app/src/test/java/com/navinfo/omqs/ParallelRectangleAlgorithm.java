package com.navinfo.omqs;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
public class ParallelRectangleAlgorithm {
    public static List<Point> getParallelRectanglePoints(List<Point> polygon) {
        // Step 1: Get the first two points of the polygon to form a line
        List<Point> line = new ArrayList<>();
        line.add(polygon.get(0));
        line.add(polygon.get(1));
        // Step 2: Find the points on the opposite side or farther away from the line
        List<Point> farPoints = new ArrayList<>();
        for (int i = 2; i < polygon.size(); i++) {
            Point point = polygon.get(i);
            int side = getSide(line.get(0), line.get(1), point);
            double distance = getDistance(line.get(0), line.get(1), point);
            if (side != 0 && side != Math.signum(distance)) {
                farPoints.add(point);
            }
        }
        // Step 3: Find the perpendicular lines to the first line passing through the farthest points
        List<Point> perpendicularLines = new ArrayList<>();
        for (Point farPoint : farPoints) {
            double dx = line.get(1).getY() - line.get(0).getY();
            double dy = line.get(0).getX() - line.get(1).getX();
            double magnitude = Math.sqrt(dx * dx + dy * dy);
            dx /= magnitude;
            dy /= magnitude;
            double distance = Math.abs((farPoint.getX() - line.get(0).getX()) * dx + (farPoint.getY() - line.get(0).getY()) * dy);
            Point perpendicularLine = new Point(farPoint.getX() + distance * dx, farPoint.getY() + distance * dy);
            perpendicularLines.add(perpendicularLine);
        }
        // Step 4: Find the intersection points of the perpendicular lines
        List<Point> intersectionPoints = new ArrayList<>();
        for (int i = 0; i < perpendicularLines.size(); i++) {
            for (int j = i + 1; j < perpendicularLines.size(); j++) {
                Point intersectionPoint = getIntersectionPoint(line.get(0), perpendicularLines.get(i), line.get(1), perpendicularLines.get(j));
                intersectionPoints.add(intersectionPoint);
            }
        }
        return intersectionPoints;
    }
    private static int getSide(Point p1, Point p2, Point point) {
        return Integer.signum((int) Math.signum((p2.getX() - p1.getX()) * (point.getY() - p1.getY()) - (p2.getY() - p1.getY()) * (point.getX() - p1.getX())));
    }
    private static double getDistance(Point p1, Point p2, Point point) {
        return ((p2.getX() - p1.getX()) * (point.getY() - p1.getY()) - (p2.getY() - p1.getY()) * (point.getX() - p1.getX())) / Math.sqrt((p2.getX() - p1.getX()) * (p2.getX() - p1.getX()) + (p2.getY() - p1.getY()) * (p2.getY() - p1.getY()));
    }
    private static Point getIntersectionPoint(Point p1, Point p2, Point p3, Point p4) {
        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();
        double x3 = p3.getX();
        double y3 = p3.getY();
        double x4 = p4.getX();
        double y4 = p4.getY();
        double x = ((x2 - x1) * (x3 * y4 - x4 * y3) - (x4 - x3) * (x1 * y2 - x2 * y1)) / ((x2 - x1) * (y3 - y4) - (x4 - x3) * (y1 - y2));
        double y = ((y3 - y4) * (x1 * y2 - x2 * y1) - (y1 - y2) * (x3 * y4 - x4 * y3)) / ((x2 - x1) * (y3 - y4) - (x4 - x3) * (y1 - y2));
        return new Point(x, y);
    }

    @Test
    public void test() {
        List<Point> polygon = new ArrayList<>();
        polygon.add(new Point(0, 0));
        polygon.add(new Point(2, 0));
        polygon.add(new Point(2, 1));
        polygon.add(new Point(1, 1));
        polygon.add(new Point(1, 2));
        polygon.add(new Point(0, 2));
        List<Point> result = getParallelRectanglePoints(polygon);
        for (Point point : result) {
            System.out.println(point);
        }
    }
}
class Point {
    private double x;
    private double y;
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}