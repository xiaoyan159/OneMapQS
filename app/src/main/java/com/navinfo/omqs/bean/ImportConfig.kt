package com.navinfo.omqs.bean


class ImportConfig {
    var tables: MutableList<TableInfo> = mutableListOf()
    val tableGroupName: String = "OMDB数据"
    var checked : Boolean = true
}

class TableInfo {
    val table: String = ""
    val code: Int = 0
    val name: String = ""
    var checked : Boolean = true
}