package com.navinfo.collect.library.importExport

import android.content.Context
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase

open class ImportExportController(context: Context, dataBase: MapLifeDataBase) {
    protected val mContext: Context = context;
    protected val mDataBase: MapLifeDataBase = dataBase;

}