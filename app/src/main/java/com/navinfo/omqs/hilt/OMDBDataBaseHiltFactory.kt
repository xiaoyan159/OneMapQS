package com.navinfo.omqs.hilt

import android.content.Context
import com.navinfo.omqs.db.OmdbDataBaseHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@AssistedFactory
interface OMDBDataBaseHiltFactory {
    fun obtainOmdbDataBaseHelper(@Assisted("context")context: Context, @Assisted("dbName") dbName: String, @Assisted("dbVersion") dbVersion: Int): OmdbDataBaseHelper
}