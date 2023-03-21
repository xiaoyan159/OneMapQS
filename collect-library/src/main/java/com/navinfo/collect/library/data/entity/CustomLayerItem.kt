package com.navinfo.collect.library.data.entity


enum class DataLayerItemType {
    ///未知类型
    DataLayerItemTypeUnKnow,

    ///输入框
    DataLayerItemTypeInput,

    ///输入框集合
    DataLayerItemTypeInputArray,

    ///纯展示文本
    DataLayerItemTypeText,

    ///单选框
    DataLayerItemTypeSingleSelection,

    ///多选框
    DataLayerItemTypeMultipleSelection,

    ///照片
    DataLayerItemTypePhoto,

    ///多级菜单
    DataLayerItemTypeMultiLevelMenu
}

data class CustomLayerItem(
    ///对应的数据库字段名称
    val key: String,
    ///用来显示控件名称
    val title: String = key,
    ///控件类型
    val type: DataLayerItemType,
    ///非必填项
//    val nullable: Boolean = true,
    ///是不是主键
//    val primaryKey: Boolean = false,
    ///默认值
//    var value: Any,
    ///多选或单选的内容，如果 [isSelect] 为 false 可忽略这字段
//    var selectOptions: String,
    ///是否是主名称属性,设置为true，地图上会已该字段内容渲染
//    val isMainName: Boolean = false,
    ///简介
    val describe: String,
    val itemBean: String,
    ///检查项
//    val checkManagerList: List<CheckManager>,
) {

    fun toMap(): Map<String, *> {
//        val checkManagerMapList = mutableListOf<Map<String, *>>()
//        for (check in checkManagerList) {
//            checkManagerMapList.add(check.toMap())
//        }
        return mapOf(
            "key" to key,
            "title" to title,
            "type" to type.ordinal,
//            "nullable" to nullable,
//            "primaryKey" to primaryKey,
//            "value" to value,
//            "selectOptions" to selectOptions,
//            "isMainName" to isMainName,
            "describe" to describe,
//            "checkManagerList" to checkManagerMapList,
        )
    }

    companion object {
//        fun fromJson(json:JSONObject): CustomLayerItem {
//            return CustomLayerItem()
//        }
    }
}

//class DataLayerItemTypeAdapter {
//    @ToJson
//    fun toJson(enum: DataLayerItemType): Int {
//        return enum.ordinal;
//    }
//
//    @FromJson
//    fun fromJson(type: Int): DataLayerItemType {
//        return DataLayerItemType.values()[type];
//    }
//}