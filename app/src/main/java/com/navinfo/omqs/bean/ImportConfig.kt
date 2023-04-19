package com.navinfo.omqs.bean


class ImportConfig {
    var tables: MutableList<TableInfo> = mutableListOf()
}

class TableInfo {
    val table: String = ""
    val code: Int = 0
    val name: String = ""
}