package com.navinfo.collect.library.data

import android.content.Context
import com.navinfo.collect.FlutterBaseActivity
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.handler.DataLayerHandler
import com.navinfo.collect.library.system.Constant

/**
 *  地图控制器
 */

open class NIDataController(
    context: Context,
    activity: FlutterBaseActivity,
) {
    protected val mContext = context
    protected val mActivity = activity
    internal val mDateBase: MapLifeDataBase = MapLifeDataBase.getDatabase(
        context, "${Constant.ROOT_PATH}/coremap.db"
    )

    init {
        RealmUtils.getInstance().init(mContext, Constant.ROOT_PATH, "hd-data")
    }

    protected val dataHandler: DataLayerHandler = DataLayerHandler(mContext, mDateBase);

    open fun release() {
        mDateBase.close()
    }
}

