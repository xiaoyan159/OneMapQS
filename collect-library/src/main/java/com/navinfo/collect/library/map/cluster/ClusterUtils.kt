/*
 * Copyright 2017 nebular
 * Copyright 2017 devemux86
 * Copyright 2017 Wolfgang Schramm
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

import org.oscim.backend.CanvasAdapter
import org.oscim.backend.canvas.Bitmap
import org.oscim.backend.canvas.Canvas
import org.oscim.backend.canvas.Paint

/**
 * A simple utility class to make clustered markers functionality self-contained.
 * Includes a method to translate between DPs and PXs and a circular icon generator.
 */
object ClusterUtils {
    /**
     * Get pixels from DPs
     *
     * @param dp Value in DPs
     * @return Value in PX according to screen density
     */
    fun getPixels(dp: Float): Int {
        return (CanvasAdapter.getScale() * dp).toInt()
    }

    class ClusterDrawable(sizedp: Int, foregroundColor: Int, backgroundColor: Int, text: String) {
        private val mPaintText = CanvasAdapter.newPaint()
        private val mPaintCircle = CanvasAdapter.newPaint()
        private val mPaintBorder = CanvasAdapter.newPaint()
        private var mSize = 0
        private var mText: String? = null

        /**
         * Generates a circle with a number inside
         *
         * @param sizedp          Size in DPs
         * @param foregroundColor Foreground
         * @param backgroundColor Background
         * @param text            Text inside. Will only work for a single character!
         */
        init {
            setup(sizedp, foregroundColor, backgroundColor)
            setText(text)
        }

        private fun setup(sizedp: Int, foregroundColor: Int, backgroundColor: Int) {
            mSize = getPixels(sizedp.toFloat())
            mPaintText.setTextSize(getPixels((sizedp * 0.6666666).toInt().toFloat()).toFloat())
            mPaintText.color = foregroundColor
            mPaintCircle.color = backgroundColor
            mPaintCircle.style = Paint.Style.FILL
            mPaintBorder.color = foregroundColor
            mPaintBorder.style = Paint.Style.STROKE
            mPaintBorder.strokeWidth = 2.0f * CanvasAdapter.getScale()
        }

        private fun setText(text: String) {
            mText = text
        }

        private fun draw(canvas: Canvas) {
            val halfsize = mSize shr 1
            val noneClippingRadius = halfsize - getPixels(2f)

            // fill
            canvas.drawCircle(
                halfsize.toFloat(),
                halfsize.toFloat(),
                noneClippingRadius.toFloat(),
                mPaintCircle
            )
            // outline
            canvas.drawCircle(
                halfsize.toFloat(),
                halfsize.toFloat(),
                noneClippingRadius.toFloat(),
                mPaintBorder
            )
            // draw the number at the center
            canvas.drawText(
                mText,
                (canvas.width - mPaintText.getTextWidth(mText)) * 0.5f,
                (canvas.height + mPaintText.getTextHeight(mText)) * 0.5f,
                mPaintText
            )
        }

        val bitmap: Bitmap
            get() {
                var width = mSize
                var height = mSize
                width = if (width > 0) width else 1
                height = if (height > 0) height else 1
                val bitmap = CanvasAdapter.newBitmap(width, height, 0)
                val canvas = CanvasAdapter.newCanvas()
                canvas.setBitmap(bitmap)
                draw(canvas)
                return bitmap
            }
    }
}