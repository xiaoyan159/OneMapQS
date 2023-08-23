/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 Izumi Kawashima
 * Copyright 2017 Longri
 * Copyright 2017-2018 devemux86
 * Copyright 2017 nebular
 * Copyright 2017 Wolfgang Schramm
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.navinfo.collect.library.map.cluster

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.text.TextPaint
import android.util.Log
import com.navinfo.collect.library.R
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.backend.CanvasAdapter
import org.oscim.backend.canvas.Bitmap
import org.oscim.core.MercatorProjection
import org.oscim.core.PointF
import org.oscim.core.Tile
import org.oscim.layers.marker.*
import org.oscim.renderer.GLViewport
import org.oscim.renderer.bucket.SymbolItem
import org.oscim.utils.FastMath
import org.oscim.utils.geom.GeometryUtils

/**
 * An extension to the MarkerRenderer with item clustering support.
 */
open class ClusterMarkerRenderer : MarkerRenderer {
    private var mContext: Context? = null
    protected var mStyleBackground = CLUSTER_COLORBACK
    protected var mStyleForeground = CLUSTER_COLORTEXT

    /**
     * Discrete scale step, used to trigger reclustering on significant scale change
     */
    private var mScalePow = 0

    /**
     * Map scale to cluster the marker
     */
    private var mClusterScale = 0.0

    /**
     * We use a flat Sparse array to calculate the clusters. The sparse array models a 2D map where every (x,y) denotes
     * a grid slot, ie. 64x64dp. For efficiency I use a linear sparsearray with ARRindex = SLOTypos * max_x + SLOTxpos"
     */
    //    private final SparseIntArray mGridMap = new SparseIntArray(200); // initial space for 200 markers, that's not a lot of memory, and in most cases will avoid resizing the array
    private val mGridMap =
        HashMap<Int, Any?>() // initial space for 200 markers, that's not a lot of memory, and in most cases will avoid resizing the array

    /**
     * Whether to enable clustering or disable the functionality
     */
    private val mClusteringEnabled: Boolean

    /**
     * Constructs a clustered marker renderer
     *
     * @param markerLayer   The owner layer
     * @param defaultSymbol The default symbol
     * @param style         The desired style, or NULL to disable clustering
     */
    constructor(
        context: Context?,
        markerLayer: MarkerLayer?,
        defaultSymbol: MarkerSymbol?,
        style: ClusterStyle?
    ) : super(markerLayer, defaultSymbol) {
        mContext = context
        mClusteringEnabled = style != null
        if (mClusteringEnabled) {
            setClusterStyle(style!!.foreground, style.background)
            for (k in 0..CLUSTER_MAXSIZE) {
                // cache bitmaps so render thread never creates them
                // we create CLUSTER_MAXSIZE bitmaps. Bigger clusters will show like "+"
                getClusterBitmap(k)
            }
        }
    }

    private constructor(
        markerLayer: MarkerLayer, defaultSymbol: MarkerSymbol, style: ClusterStyle?
    ) : super(markerLayer, defaultSymbol) {
        mClusteringEnabled = style != null
        if (mClusteringEnabled) {
            setClusterStyle(style!!.foreground, style.background)
            for (k in 0..CLUSTER_MAXSIZE) {
                // cache bitmaps so render thread never creates them
                // we create CLUSTER_MAXSIZE bitmaps. Bigger clusters will show like "+"
                getClusterBitmap(k)
            }
        }
    }

    /**
     * Configures the cluster icon style. This is called by the constructor and cannot be made public because
     * we pre-cache the icons at construction time so the renderer does not have to create them while rendering
     *
     * @param backgroundColor Background color
     * @param foregroundColor text & border color
     */
    private fun setClusterStyle(foregroundColor: Int, backgroundColor: Int) {
        mStyleBackground = backgroundColor
        mStyleForeground = foregroundColor
    }

    protected override fun populate(size: Int) {
        repopulateCluster(size, mClusterScale)
    }

    /**
     * Repopulates item list clustering close markers. This is triggered from update() when
     * a significant change in scale has happened.
     *
     * @param size  Item list size
     * @param scale current map scale
     */
    private fun repopulateCluster(size: Int, scale: Double) {
        /* the grid slot size in px. increase to group more aggressively. currently set to marker size */
        if (mMapPosition.zoomLevel == 15) {
            MAP_GRID_SIZE_DP = 128
        } else {
            MAP_GRID_SIZE_DP = 64
        }
        val GRIDSIZE = ClusterUtils.getPixels(MAP_GRID_SIZE_DP.toFloat())

        /* the factor to map into Grid Coordinates (discrete squares of GRIDSIZE x GRIDSIZE) */
        val factor = scale / GRIDSIZE
        val tmp = arrayOfNulls<Clustered>(size)

        // clear grid map to count items that share the same "grid slot"
        mGridMap.clear()
        for (i in 0 until size) {
            tmp[i] = Clustered()
            val it = tmp[i]
            it!!.item = mMarkerLayer.createItem(i)

            /* pre-project points */MercatorProjection.project(it.item.point, mMapPoint)
            it.px = mMapPoint.x
            it.py = mMapPoint.y

            // items can be declared non-clusterable
            if (it.item !is NonClusterable) {
                val absposx = (it.px * factor).toInt()
                // absolute item X position in the grid
                val absposy = (it.py * factor).toInt()
                // absolute item Y position
                val maxcols = factor.toInt()
                // Grid number of columns
                val itemGridIndex = absposx + absposy * maxcols // Index in the sparsearray map

                // we store in the linear sparsearray the index of the marker,
                // ie, index = y * maxcols + x; array[index} = markerIndex

                // Lets check if there's already an item in the grid slot
//                final int storedIndexInGridSlot = mGridMap.get(itemGridIndex, -1);
                var list: ArrayList<Int>? = null
                if (mGridMap.containsKey(itemGridIndex)) {
                    list = mGridMap[itemGridIndex] as ArrayList<Int>?
                }
                if (list == null) {
                    list = ArrayList()
                    list.add(i)
                    if (it.item is ClusterMarkerItem) {
                        (it.item as ClusterMarkerItem).clusterList = list
                    }
                    mGridMap[itemGridIndex] = list
                    // no item at that grid position. The grid slot is free so let's
                    // store this item "i" (we identify every item by its InternalItem index)
//                    mGridMap.put(itemGridIndex, i);
                    //Log.v(TAG, "UNclustered item at " + itemGridIndex);
                } else {
                    // at that grid position there's already a marker index
                    // mark this item as clustered out, so it will be skipped in the update() call
                    it.clusteredOut = true
                    list.add(i)
                    for (n in list) {
                        val item: MarkerInterface = mMarkerLayer.createItem(n)
                        if (item is ClusterMarkerItem) {
                            (item as ClusterMarkerItem).clusterList = list
                        }
                    }
                    // and increment the count on its "parent" that will from now on act as a cluster
                    tmp[list[0]]!!.clusterSize++

                    //Log.v(TAG, "Clustered item at " + itemGridIndex + ", \'parent\' size " + (tmp[storedIndexInGridSlot].clusterSize));
                }
            }
        }

        /* All ready for update. */synchronized(this) {
            mUpdate = true
            mItems = tmp
        }
    }

    @Synchronized
    override fun update(v: GLViewport) {
        val scale: Double = Tile.SIZE * v.pos.scale
        if (mClusteringEnabled) {
            /*
              Clustering check: If clustering is enabled and there's been a significant scale change
              trigger repopulation and return. After repopulation, this will be called again
             */

            // (int) log of scale gives us adequate steps to trigger clustering
            val scalePow = FastMath.log2(scale.toInt())
            if (scalePow != mScalePow) {
                mScalePow = scalePow
                mClusterScale = scale

                // post repopulation to the main thread
                mMarkerLayer.map().post(Runnable { repopulateCluster(mItems.size, scale) })

                // and get out of here
                return
            }
        }
        if (!v.changed() && !mUpdate) return
        mUpdate = false
        val mx: Double = v.pos.x
        val my: Double = v.pos.y

        //int changesInvisible = 0;
        //int changedVisible = 0;
        var numVisible = 0

        // Increase view to show items that are partially visible
        mMarkerLayer.map().viewport().getMapExtents(mBox, (Tile.SIZE shr 1).toFloat())
        val flip: Long = (Tile.SIZE * v.pos.scale).toLong() shr 1
        if (mItems == null) {
            if (buckets.get() != null) {
                buckets.clear()
                compile()
            }
            return
        }
        val angle = Math.toRadians(v.pos.bearing.toDouble())
        val cos = Math.cos(angle).toFloat()
        val sin = Math.sin(angle).toFloat()

        /* check visibility */for (itm in mItems) {
            val it = itm as Clustered
            it.changes = false
            it.x = ((it.px - mx) * scale).toFloat()
            it.y = ((it.py - my) * scale).toFloat()
            if (it.x > flip) it.x -= (flip shl 1).toFloat() else if (it.x < -flip) it.x += (flip shl 1).toFloat()
            if (it.clusteredOut || !GeometryUtils.pointInPoly(it.x, it.y, mBox, 8, 0)) {
                // either properly invisible, or clustered out. Items marked as clustered out mean there's another item
                // on the same-ish position that will be promoted to cluster marker, so this particular item is considered
                // invisible
                if (it.visible && !it.clusteredOut) {
                    // it was previously visible, but now it won't
                    it.changes = true
                    // changes to invible
                    //changesInvisible++;
                }
                continue
            }

            // item IS definitely visible
            it.dy = sin * it.x + cos * it.y
            if (!it.visible) {
                it.visible = true
                //changedVisible++;
            }
            numVisible++
        }

        //log.debug(numVisible + " " + changedVisible + " " + changesInvisible);

        /* only update when zoomlevel changed, new items are visible
         * or more than 10 of the current items became invisible */
        //if ((numVisible == 0) && (changedVisible == 0 && changesInvisible < 10))
        //	return;
        buckets.clear()
        if (numVisible == 0) {
            compile()
            return
        }
        /* keep position for current state */mMapPosition.copy(v.pos)
        mMapPosition.bearing = -mMapPosition.bearing

        // why do we sort ? z-index?
        sort(mItems, 0, mItems.size)
        //log.debug(Arrays.toString(mItems));
        for (itm in mItems) {
            val it = itm as Clustered

            // skip invisible AND clustered-out
            if (!it.visible || it.clusteredOut) continue
            if (it.changes) {
                it.visible = false
                continue
            }
            val s: SymbolItem = SymbolItem.pool.get()
            if (it.clusterSize > 0) {
                // this item will act as a cluster, just use a proper bitmap
                // depending on cluster size, instead of its marker
                val bitmap = getClusterBitmap(it.clusterSize + 1)
                s.set(it.x, it.y, bitmap, true, false)
                s.offset = PointF(0.5f, 0.5f)
                s.billboard = true // could be a parameter
            } else {
                // normal item, use its marker
                var symbol: MarkerSymbol? = it.item.marker
                if (symbol == null) symbol = mDefaultMarker
                symbol?.let { symbol ->
                    s.set(it.x, it.y, symbol.bitmap, true, false)
                    s.offset = symbol.hotspot
                    s.billboard = symbol.isBillboard
                }

            }
            mSymbolLayer.pushSymbol(s)
        }
        buckets.set(mSymbolLayer)
        buckets.prepare()
        compile()
    }

    /**
     * Gets a bitmap for a given cluster size
     *
     * @param size The cluster size. Can be greater than CLUSTER_MAXSIZE.
     * @return A somewhat cool bitmap to be used as the cluster marker
     */
    open fun getClusterBitmap(size: Int): Bitmap? {
        var size = size
        val strValue: String
        if (size >= CLUSTER_MAXSIZE) {
            // restrict cluster indicator size. Bigger clusters will show as "+" instead of ie. "45"
            size = CLUSTER_MAXSIZE
            strValue = "+"
        } else {
            strValue = size.toString()
        }

        // return cached bitmap if exists. cache hit !
        if (mClusterBitmaps[size] != null) return mClusterBitmaps[size]

        // create and cache bitmap. This is unacceptable inside the GL thread,
        // so we'll call this routine at the beginning to pre-cache all bitmaps
        // Can customize cluster bitmap here
//
//        Canvas canvas = new Canvas(bitmap);
//        TextPaint textPaint = new TextPaint();
//        textPaint.setColor(Color.WHITE);
//        int width = (int) Math.ceil(textPaint.measureText(strValue));
//        android.graphics.Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
//        int height = (int) Math.ceil(Math.abs(fontMetrics.bottom) + Math.abs(fontMetrics.top));
//        textPaint.setTextSize(13 * CanvasAdapter.getScale());
//        canvas.drawText(strValue, 0, Math.abs(fontMetrics.ascent), textPaint);
        val textPaint = TextPaint()
        val h: Float = 13 * CanvasAdapter.getScale()
        textPaint.textSize = h
        textPaint.strokeWidth = 2f
        textPaint.color = Color.WHITE
        val bitmap = BitmapFactory.decodeResource(
            mContext!!.resources, R.mipmap.map_icon_cluster
        ).copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val w = textPaint.getTextWidths(strValue, FloatArray(strValue.length))
        val textRect = Rect()
        textPaint.getTextBounds(strValue, 0, strValue.length, textRect)
        canvas.drawText(
            strValue,
            ((bitmap.width - textRect.width()) / 2 - 4).toFloat(),
            ((bitmap.height + textRect.height()) / 2).toFloat(),
            textPaint
        )
        val bitmapNew: Bitmap = AndroidBitmap(bitmap)
        //        Canvas canvas = CanvasAdapter.newCanvas();
//        canvas.setBitmap(bitmapNew);
//        mSize = ClusterUtils.getPixels(sizedp);
//        int halfsize = mSize >> 1;
//        final int noneClippingRadius = halfsize - getPixels(2);
//        Paint mPaintText = CanvasAdapter.newPaint();
        // draw the number at the center
//        canvas.drawText(strValue,
//                (canvas.getWidth() - mPaintText.getTextWidth(strValue)) * 0.5f,
//                (canvas.getHeight() + mPaintText.getTextHeight(strValue)) * 0.5f,
//                mPaintText);
//        ClusterUtils.ClusterDrawable drawable = new ClusterUtils.ClusterDrawable(
//                MAP_MARKER_CLUSTER_SIZE_DP - CLUSTER_MAXSIZE + size, // make size dependent on cluster size
//                mStyleForeground,
//                mStyleBackground,
//                strValue
//        );
        mClusterBitmaps[size] = bitmapNew
        return mClusterBitmaps[size]
    }

    /**
     * Class to wrap the cluster icon style properties
     */
    class ClusterStyle
    /**
     * Creates the Cluster style
     *
     * @param fore Foreground (border and text) color
     * @param back Background (circle) color
     */(val foreground: Int, val background: Int)

    companion object {
        /**
         * Max number to display inside a cluster icon
         */
        protected const val CLUSTER_MAXSIZE = 999

        /**
         * default color of number inside the icon. Would be super-cool to cook this into the map theme
         */
        private const val CLUSTER_COLORTEXT = -0x7fff40

        /**
         * default color of circle background
         */
        private const val CLUSTER_COLORBACK = -0x1

        /**
         * Map Cluster Icon Size. This is the biggest size for clusters of CLUSTER_MAXSIZE elements. Smaller clusters will be slightly smaller
         */
        protected const val MAP_MARKER_CLUSTER_SIZE_DP = 48

        /**
         * Clustering grid square size, decrease to cluster more aggresively. Ideally this value is the typical marker size
         */
        private var MAP_GRID_SIZE_DP = 64

        /**
         * cached bitmaps database, we will cache cluster bitmaps from 1 to MAX_SIZE
         * and always use same bitmap for efficiency
         */
        protected var mClusterBitmaps = arrayOfNulls<Bitmap>(CLUSTER_MAXSIZE + 1)

        /**
         * Convenience method for instantiating this renderer via a factory, so the layer construction semantic is more pleasing to the eye
         *
         * @param defaultSymbol Default symbol to use if the Marker is not assigned a symbol
         * @param style         Cluster icon style, or NULL to disable clustering functionality
         * @return A factory to be passed to the ItemizedLayer constructor in order to enable the cluster functionality
         */
        fun factory(defaultSymbol: MarkerSymbol, style: ClusterStyle?): MarkerRendererFactory {
            return MarkerRendererFactory { markerLayer ->
                ClusterMarkerRenderer(
                    markerLayer, defaultSymbol, style
                )
            }
        }
    }
}