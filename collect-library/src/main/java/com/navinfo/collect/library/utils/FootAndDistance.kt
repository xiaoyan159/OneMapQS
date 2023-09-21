package com.navinfo.collect.library.utils

import org.locationtech.jts.geom.*
import org.locationtech.jts.io.WKTWriter
import org.oscim.core.GeoPoint

class FootAndDistance(private val point: GeoPoint) {

    private val pt = arrayOf(Coordinate(), Coordinate())
    private var distance = Double.NaN
    private var isNull = true
    var footIndex = 0

    /**
     * Initializes this instance.
     */
    fun initialize() {
        isNull = true
    }

    /**
     * Initializes the points, computing the distance between them.
     * @param p0 the 1st point
     * @param p1 the 2nd point
     */
    fun initialize(p0: Coordinate, p1: Coordinate) {
        initialize(p0, p1, p0.distance(p1))
    }

    /**
     * Initializes the points, avoiding recomputing the distance.
     * @param p0 the 1st point
     * @param p1 the 2nd point
     * @param distance the distance between p0 and p1
     */
    fun initialize(p0: Coordinate, p1: Coordinate, distance: Double) {
        pt[0].setCoordinate(p0)
        pt[1].setCoordinate(p1)
        this.distance = distance
        isNull = false
    }

    /**
     * Gets the distance between the paired points
     * @return the distance between the paired points
     */
    fun getDistance(): Double {
        return distance
    }

    /**
     * Gets the paired points
     * @return the paired points
     */
    fun getCoordinates(): Array<Coordinate> {
        return pt
    }

    /**
     * Gets one of the paired points
     * @param i the index of the paired point (0 or 1)
     * @return A point
     */
    fun getCoordinate(i: Int): Coordinate {
        return pt[i]
    }


    fun setMinimum(p0: Coordinate, p1: Coordinate): Boolean {
        if (isNull) {
            initialize(p0, p1)
            return true
        }
        val dist = p0.distance(p1)
        if (dist < distance) {
            initialize(p0, p1, dist)
            return true
        }
        return false
    }

    override fun toString(): String {
        return WKTWriter.toLineString(pt[0], pt[1])
    }


    fun getMeterDistance(): Double {
        return if (!distance.isNaN() && pt.isNotEmpty())
            GeometryTools.getDistance(
                point.latitude,
                point.longitude,
                pt[0].y,
                pt[0].x
            )
        else {
            Double.NaN
        }
    }

    fun computeDistance(geom: Geometry, pt: Coordinate) {
        when (geom) {
            is LineString -> {
                computeDistance(geom, pt)
            }
            is Polygon -> {
                computeDistance(geom, pt)
            }
            is GeometryCollection -> {
                for (i in 0 until geom.numGeometries) {
                    val g = geom.getGeometryN(i)
                    computeDistance(g, pt)
                }
            }
            else -> { // assume geom is Point
                setMinimum(geom.coordinate, pt)
            }
        }
    }

    fun computeDistance(line: LineString, pt: Coordinate) {
        val tempSegment = LineSegment()
        val coords = line.coordinates
        for (i in 0 until (coords.size - 1)) {
            tempSegment.setCoordinates(coords[i], coords[i + 1])
            // this is somewhat inefficient - could do better
            val closestPt = tempSegment.closestPoint(pt)
            if (setMinimum(closestPt, pt)) {
                footIndex = i
            }
        }
    }

    fun computeDistance(segment: LineSegment, pt: Coordinate) {
        val closestPt = segment.closestPoint(pt)
        setMinimum(closestPt, pt)
    }

    fun computeDistance(poly: Polygon, pt: Coordinate) {
        computeDistance(poly.exteriorRing, pt)
        for (i in 0 until poly.numInteriorRing) {
            computeDistance(poly.getInteriorRingN(i), pt)
        }
    }

}