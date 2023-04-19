package com.navinfo.omqs.db

import android.content.Context
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.spatialite.database.SQLiteDatabase
import org.spatialite.database.SQLiteOpenHelper

class OmdbDataBaseHelper @AssistedInject constructor(@Assisted("context")context: Context, @Assisted("dbName") dbName: String, @Assisted("dbVersion") dbVersion: Int) :
    SQLiteOpenHelper(context, dbName, null, dbVersion) {
    override fun onCreate(db: SQLiteDatabase?) {
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
    }
}