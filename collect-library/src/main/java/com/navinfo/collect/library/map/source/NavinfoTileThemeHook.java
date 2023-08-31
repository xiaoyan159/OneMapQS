package com.navinfo.collect.library.map.source;

import org.oscim.core.MapElement;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.theme.styles.RenderStyle;

import java.util.HashMap;
import java.util.Map;

/**
 *  数据在加载前对MapElement做处理
 *
 * */
public class NavinfoTileThemeHook implements VectorTileLayer.TileLoaderThemeHook {
    // 缓存样式，如果存在相同的文字图片，从缓存直接获取
    private Map<String, RenderStyle> styleMap = new HashMap<>();

    @Override
    public boolean process(MapTile tile, RenderBuckets buckets, MapElement element, RenderStyle style, int level) {
//        if (style instanceof AreaStyle) {
//            AreaStyle area = (AreaStyle) style;
//            if (area.src!=null&&area.texture==null) {
//                String textValue = null;
//                if (area.src.startsWith("@text-src:")) {
//                    String tagKey = area.src.replace("@text-src:", "");
//                    // 根据配置的tagKey获取对应的文字值
//                    textValue = element.tags.getValue(tagKey);
//                } else if (area.src.startsWith("@text:")) {
//                    // 构建一个文字图片
//                    textValue = area.src.substring(5);
//                }
//
//                // 使用文本值生成纹理图片
//                if (textValue==null) {
//                    return false;
//                }
//
//                // 根据引用字段，生成新的style样式
//                RenderStyle cacheStyle = styleMap.get(textValue);
//                if (cacheStyle!=null) {
//                    style.set(cacheStyle);
//                } else { // 不存在缓存style，需要按照文字生成对应的图片纹理
//                    AreaStyle.AreaBuilder<?> builder = new AreaStyle.AreaBuilder();
//                    builder.set((AreaStyle) style);
//                    // 构建一个文字图片
//                    Canvas canvas = CanvasAdapter.newCanvas();
//                    Paint paint = CanvasAdapter.newPaint();
//                    paint.setColor(Color.WHITE);
//                    paint.setTextSize(15);
//
//                    Bitmap bitmap = CanvasAdapter.newBitmap(Math.round(paint.getTextWidth(textValue)+10), Math.round(paint.getTextHeight(textValue)+10), 0);
//                    canvas.setBitmap(bitmap);
//                    canvas.drawText(textValue, 5
//                            , 5+paint.getTextHeight(textValue), paint, paint);
//                    builder.texture = new TextureItem(Utils.potBitmap(bitmap), area.repeat);
//                    builder.src = null; // 清空原有的带@的src配置
//                    area = builder.build();
//                    styleMap.put(textValue, area);
//                    style.set(area);
//                }
//            }
//        }
//        style.update();
        return false; // 如果返回true，后续的hook将不会执行
    }

    @Override
    public void complete(MapTile tile, boolean success) {

    }
}
