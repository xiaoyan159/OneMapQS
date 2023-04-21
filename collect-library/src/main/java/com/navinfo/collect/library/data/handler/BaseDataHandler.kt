package com.navinfo.collect.library.data.handler

import android.content.Context
import androidx.room.RoomDatabase

open class BaseDataHandler(context: Context, dataBase: RoomDatabase) {
    protected val mContext: Context = context;
    protected val mDataBase: RoomDatabase = dataBase;
}