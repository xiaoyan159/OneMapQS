package com.navinfo.collect.library.data.handler

import android.content.Context
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase

open class BaseDataHandler(context: Context, dataBase: MapLifeDataBase) {
    protected val mContext: Context = context;
    protected val mDataBase: MapLifeDataBase = dataBase;
}