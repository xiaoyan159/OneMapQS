package com.navinfo.collect.library.data

import com.navinfo.collect.library.data.entity.GeometryFeatureEntity
import java.io.File

interface GisFileUtils {
    fun parserGisFile(file: File): List<GeometryFeatureEntity>
}