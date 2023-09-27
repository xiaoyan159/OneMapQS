package com.navinfo.collect.library.map.source;

import android.util.Log;

import com.navinfo.collect.library.data.entity.RenderEntity;
import com.navinfo.collect.library.system.Constant;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.OverzoomTileDataSource;

import io.realm.Realm;

public class OMDBTileSource extends RealmDBTileSource {
    private OMDBTileDataSource omdbTileSource = new OMDBTileDataSource();
    @Override
    public ITileDataSource getDataSource() {
       // return new OverzoomTileDataSource(new OMDBTileDataSource(), Constant.OVER_ZOOM);
        return new OverzoomTileDataSource(omdbTileSource, Constant.OVER_ZOOM);
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
        omdbTileSource.update();
    }
}
