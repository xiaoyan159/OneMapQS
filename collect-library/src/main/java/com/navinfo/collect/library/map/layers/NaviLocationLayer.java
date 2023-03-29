package com.navinfo.collect.library.map.layers;

import android.annotation.SuppressLint;
import android.content.Context;

import com.navinfo.collect.library.R;
import com.navinfo.collect.library.map.NIMapView;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.layers.Layer;
import org.oscim.layers.LocationTextureLayer;
import org.oscim.map.Map;
import org.oscim.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class NaviLocationLayer extends LocationTextureLayer {
    public NaviLocationLayer(Context mContext, Map map) {
        this(mContext, map, CanvasAdapter.getScale());
    }

    public NaviLocationLayer(Context mContext, Map map, float scale) {
        super(map, scale);
        init(mContext);
    }

    private void init(Context mContext) {
        createLocationLayers(mContext, mMap);
    }

    @SuppressLint("ResourceType")
    private Layer createLocationLayers(Context mContext, Map mMap) {

        InputStream is = null;

        Bitmap bitmapArrow = null;
        try {
            is = mContext.getResources().openRawResource(R.mipmap.icon_location_arrow);
            bitmapArrow = CanvasAdapter.decodeBitmap(is, (int) (48 * CanvasAdapter.getScale()), (int) (48 * CanvasAdapter.getScale()), 100);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(is);
        }

        Bitmap bitmapMarker = null;
        try {
            is = mContext.getResources().openRawResource(R.mipmap.icon_location_arrow);
            bitmapMarker = CanvasAdapter.decodeBitmap(is, (int) (48 * CanvasAdapter.getScale()), (int) (48 * CanvasAdapter.getScale()), 100);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(is);
        }

        // 显示当前位置图层
        this.locationRenderer.setBitmapArrow(bitmapArrow);
        this.locationRenderer.setBitmapMarker(bitmapMarker);
        this.locationRenderer.setColor(0xffa1dbf5);
        this.locationRenderer.setShowAccuracyZoom(4);
        this.setEnabled(true); // 默认开启当前位置显示
        mMap.layers().add(this, NIMapView.LAYER_GROUPS.NAVIGATION.getGroupIndex());
        return this;
    }
}
