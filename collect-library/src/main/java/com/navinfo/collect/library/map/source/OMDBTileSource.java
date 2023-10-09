package com.navinfo.collect.library.map.source;

import android.util.Log;

import com.navinfo.collect.library.data.entity.RenderEntity;
import com.navinfo.collect.library.system.Constant;

import org.oscim.map.Viewport;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.OverzoomTileDataSource;

import io.realm.Realm;

public class OMDBTileSource extends RealmDBTileSource {
    private Viewport viewport;
    private OMDBTileDataSource omdbTileDataSource;

    public OMDBTileSource(Viewport viewport) {
        this.viewport = viewport;
        this.omdbTileDataSource = new OMDBTileDataSource(this.viewport);
    }

    @Override
    public ITileDataSource getDataSource() {
       // return new OverzoomTileDataSource(new OMDBTileDataSource(), Constant.OVER_ZOOM);
        return new OverzoomTileDataSource(omdbTileDataSource, Constant.OVER_ZOOM);
    }

    @Override
    public OpenResult open() {
        Log.d("qj", Realm.getDefaultInstance().where(RenderEntity.class).findAll().size()+"open安装数量");
        return OpenResult.SUCCESS;
    }

    @Override
    public void close() {

    }

    @Override
    public void update() {
        super.update();
        omdbTileDataSource.update();
    }
}
