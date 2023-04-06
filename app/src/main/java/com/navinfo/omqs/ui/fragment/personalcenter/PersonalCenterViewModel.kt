package com.navinfo.omqs.ui.fragment.personalcenter

import androidx.lifecycle.ViewModel
import java.io.File

class PersonalCenterViewModel: ViewModel() {
    fun importOmdbData(omdbFile: File) {
        // 检查File是否为sqlite数据库
        if (omdbFile == null || omdbFile.exists()) {
            throw Exception("文件不存在")
        }
        if (!omdbFile.name.endsWith(".sqlite") and !omdbFile.name.endsWith("db")) {
            throw Exception("文件不存在")
        }
    }
}