package com.navinfo.omqs.hilt

import android.content.Context
import com.navinfo.omqs.db.ImportOMDBHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import java.io.File

@AssistedFactory
interface ImportOMDBHiltFactory {
    fun obtainImportOMDBHelper(@Assisted("context")context: Context, @Assisted("omdbFile") omdbFile: File): ImportOMDBHelper
}