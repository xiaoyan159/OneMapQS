package com.navinfo.collect.library.enums

/*
* 图层枚举定义
* */
enum class DataLayerEnum(var tableName: String, var sql: String) {
    SHOW_ALL_LAYERS("显示所有图层", ">=0"), ONLY_ENABLE_LAYERS(
        "仅显示可用图层",
        ">=1"
    )
}

