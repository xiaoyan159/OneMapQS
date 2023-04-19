package com.navinfo.collect.library.map.layers;

import android.util.Log;

import org.oscim.backend.CanvasAdapter;
import org.oscim.core.Box;
import org.oscim.core.Tile;
import org.oscim.event.Gesture;
import org.oscim.event.MotionEvent;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerRendererFactory;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.map.Map;
import org.oscim.map.Viewport;

import java.util.ArrayList;
import java.util.List;

/*
 *com.nmp.map.layer
 *zhjch
 *2021/12/9
 *15:32
 *说明（）
 */
public class MyItemizedLayer extends ItemizedLayer {
    private OnItemGestureListener mOnItemGestureListener;
    private final ActiveItem mActiveItemSingleTap;
    private final ActiveItem mActiveItemLongPress;

    public MyItemizedLayer(Map map, MarkerSymbol defaultMarker) {
        this(map, new ArrayList<>(), defaultMarker, null);
    }

    public MyItemizedLayer(Map map, List<MarkerInterface> list, MarkerSymbol defaultMarker, OnItemGestureListener listener) {
        super(map, list, defaultMarker, null);
        mOnItemGestureListener = listener;
        this.mActiveItemSingleTap = new NamelessClass_2();
        this.mActiveItemLongPress = new NamelessClass_1();
    }

    public MyItemizedLayer(Map map, MarkerRendererFactory markerRendererFactory) {
        this(map, new ArrayList<>(), markerRendererFactory, null);
    }

    class NamelessClass_2 implements ActiveItem {
        NamelessClass_2() {
        }

        public boolean run(List list1, int nearest) {
            if (mOnItemGestureListener != null) {
                return mOnItemGestureListener.onItemSingleTapUp(MyItemizedLayer.this, list1, nearest);
            }
            return false;
        }
    }

    class NamelessClass_1 implements ActiveItem {
        NamelessClass_1() {
        }

        public boolean run(List list1, int nearest) {
            if (mOnItemGestureListener != null) {
                return mOnItemGestureListener.onItemLongPress(MyItemizedLayer.this, list1, nearest);
            }
            return false;
        }
    }

    public MyItemizedLayer(Map map, List<MarkerInterface> list, MarkerRendererFactory markerRendererFactory, OnItemGestureListener listener) {
        super(map, list, markerRendererFactory, null);
        mOnItemGestureListener = listener;
        this.mActiveItemSingleTap = new NamelessClass_2();
        this.mActiveItemLongPress = new NamelessClass_1();
    }

    @Override
    public boolean onGesture(Gesture g, MotionEvent e) {
        if (!this.isEnabled()) {
            return false;
        } else if (g instanceof Gesture.Tap) {
            return this.activateSelectedItems(e, this.mActiveItemSingleTap);
        } else {
            return g instanceof Gesture.LongPress ? this.activateSelectedItems(e, this.mActiveItemLongPress) : false;
        }
    }

    private boolean activateSelectedItems(MotionEvent event, ActiveItem task) {
        int size = this.mItemList.size();

        Log.e("jingo", "地图点击 size =" + size);
        if (size == 0) {
            return false;
        } else {
            int eventX = (int) event.getX() - this.mMap.getWidth() / 2;
            int eventY = (int) event.getY() - this.mMap.getHeight() / 2;
            Viewport mapPosition = this.mMap.viewport();
            Box box = mapPosition.getBBox((Box) null, Tile.SIZE / 2);
            box.map2mercator();
            box.scale(1000000.0D);
            int nearest = -1;
            int inside = -1;
//            double insideY = -1.7976931348623157E308D;
            double dist = (double) (20.0F * CanvasAdapter.getScale() * 20.0F * CanvasAdapter.getScale());
            List list = new ArrayList();
            for (int i = 0; i < size; ++i) {
                MarkerInterface item = (MarkerInterface) this.mItemList.get(i);
                if (box.contains((double) item.getPoint().longitudeE6, (double) item.getPoint().latitudeE6)) {
                    mapPosition.toScreenPoint(item.getPoint(), this.mTmpPoint);
                    float dx = (float) ((double) eventX - this.mTmpPoint.x);
                    float dy = (float) ((double) eventY - this.mTmpPoint.y);
                    MarkerSymbol it = item.getMarker();
                    if (it == null) {
                        continue;
//                        it = this.mMarkerRenderer.mDefaultMarker;
                    }

                    if (it.isInside(dx, dy)) {// && this.mTmpPoint.y > insideY) {
//                        insideY = this.mTmpPoint.y;
                        inside = i;
                        list.add(i);
                    }

                    if (inside < 0) {
                        double d = (double) (dx * dx + dy * dy);
                        if (d <= dist) {
                            dist = d;
                            nearest = i;
                        }
                    }
                }
            }

            if (inside >= 0) {
                nearest = inside;
            }

            if (nearest >= 0 && task.run(list, nearest)) {
                this.mMarkerRenderer.update();
                this.mMap.render();
                return true;
            } else {
                return false;
            }
        }
    }

    public interface ActiveItem {
        boolean run(List list, int nearest);
    }

    public void setOnItemGestureListener(OnItemGestureListener listener) {
        this.mOnItemGestureListener = listener;
    }

    public interface OnItemGestureListener {
        boolean onItemSingleTapUp(MyItemizedLayer layer, List<Integer> list, int nearest);

        boolean onItemLongPress(MyItemizedLayer layer, List<Integer> list, int nearest);
    }
}
