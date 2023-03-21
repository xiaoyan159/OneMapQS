package com.navinfo.collect.library.data.handler

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.navinfo.collect.library.data.DataConversion
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.entity.CheckManager
import com.navinfo.collect.library.data.entity.CustomLayerItem
import com.navinfo.collect.library.data.entity.DataLayerItemType
import com.navinfo.collect.library.data.entity.LayerManager
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

/**
 * 数据库操作
 */


open class DataLayerHandler(context: Context, dataBase: MapLifeDataBase) :
    BaseDataHandler(context, dataBase) {

    /**
     * 根据json创建数据库
     * [
     * {"key":"name","type":"TEXT","nullable":true},
     * {"key":"address","type":"TEXT","nullable":true},
     * {"key":"type","type":"INTEGER","nullable":false},
     * {"key":"longitude","type":"REAL","nullable":false},
     * {"key":"latitude","type":"REAL","nullable":false},
     * {"key":"id","type":"INTEGER","nullable":false,"primaryKey":true}
     * ]
     *
     *
     *  _db.execSQL("CREATE TABLE IF NOT EXISTS `edit_pois` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `postCode` TEXT, `address` TEXT, `kindCode` TEXT, `uuid` TEXT, `geometry` TEXT, `maxx` REAL NOT NULL, `minx` REAL NOT NULL, `maxy` REAL NOT NULL, `miny` REAL NOT NULL)");
    _db.execSQL("CREATE TABLE IF NOT EXISTS `element` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `display_style` TEXT, `display_text` TEXT, `start_level` INTEGER NOT NULL, `end_level` INTEGER NOT NULL, `zindex` INTEGER NOT NULL, `visibility` INTEGER NOT NULL, `operation_time` TEXT, `export_time` TEXT, `t_lifecycle` INTEGER NOT NULL, `t_status` INTEGER NOT NULL, `bundle` TEXT, `uuid` TEXT, `geometry` TEXT, `maxx` REAL NOT NULL, `minx` REAL NOT NULL, `maxy` REAL NOT NULL, `miny` REAL NOT NULL)");
    _db.execSQL("CREATE TABLE IF NOT EXISTS `layerElement` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT, `layer_uuid` TEXT, `element_uuid` TEXT)");
    _db.execSQL("CREATE TABLE IF NOT EXISTS `layerManager` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT, `layer_name` TEXT, `zindex` INTEGER NOT NULL, `visibility` INTEGER NOT NULL, `export_time` TEXT, `import_time` TEXT, `bundle` TEXT)");
    _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
    _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7a041262c922535fc32b56d4dd16ea92')");
     *
     */

    fun createTable(
        layer: LayerManager,
        list: List<CustomLayerItem>,
        callback: (res: Boolean, errorString: String) -> Unit
    ) {
        thread(start = true) {
            try {
                val sql = StringBuffer();
                val database: SupportSQLiteDatabase = mDataBase.openHelper.writableDatabase
                val locLayer = mDataBase.layerManagerDao.findLayerManager(layer.id);
                if (locLayer != null) {
                    sql.append("CREATE TABLE IF NOT EXISTS '${layer.id}temp' ( uuid TEXT NOT NULL, ")
                    val columnBuffer = StringBuffer()
                    columnBuffer.append("uuid")
                    var count = 0;
                    //比对旧的数据库表，看看哪些字段需要更改
                    val items = DataConversion.jsonToLayerItemsList(locLayer.bundle)
                    for (i in list.indices) {
                        if (i > 0) {
                            sql.append(", ")
                        }
                        sql.append("'${list[i].key}' ")
                        sql.append(layerItemTypeToDBType(list[i].type))
                        for (j in items.indices) {
                            //这里只根据id是否相同进行了判断，如果类型变换了话会有风险，需要应用层做限制
                            if (items[j].key == list[i].key) {
                                columnBuffer.append(",")
                                columnBuffer.append(items[j].key)
                                count++
                                break
                            }
                        }
                    }
                    sql.append(")")

                    if (count == items.size && count == list.size) {
                        mDataBase.layerManagerDao.update(layer)
                        Handler(Looper.getMainLooper()).post {
                            callback.invoke(true, "")
                        }
                    } else {
                        var b = true
                        val sql2 =
                            "insert into \"${layer.id}temp\"($columnBuffer) select $columnBuffer from \"${layer.id}\""
                        try {
                            database.beginTransaction()
                            database.execSQL(sql.toString())
                            database.execSQL(sql2)
                            database.execSQL("drop TABLE \"${layer.id}\"")
                            database.execSQL("ALTER TABLE \"${layer.id}temp\" RENAME TO \"${layer.id}\"")
                            database.setTransactionSuccessful()
                        } catch (e: Throwable) {
                            Handler(Looper.getMainLooper()).post {
                                callback.invoke(false, "${e.message}")
                            }
                            b = false
                        } finally {
                            database.endTransaction()
                        }
                        if (b) {
                            mDataBase.layerManagerDao.update(layer)
                            Handler(Looper.getMainLooper()).post {
                                callback.invoke(true, "")
                            }
                        }
                    }
                } else {
                    sql.append("CREATE TABLE IF NOT EXISTS '${layer.id}' ( uuid TEXT NOT NULL, ")
                    for (i in list.indices) {
                        if (i > 0) {
                            sql.append(", ")
                        }
                        sql.append("'${list[i].key}' ")
                        sql.append(layerItemTypeToDBType(list[i].type))
                    }
                    sql.append(")")
                    mDataBase.layerManagerDao.insert(layer)
                    mDataBase.openHelper.writableDatabase.execSQL(sql.toString())
                    Handler(Looper.getMainLooper()).post {
                        callback.invoke(true, "")
                    }
                }
            } catch (e: Throwable) {
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(false, "${e.message}")
                }
            }

        }
    }

    /**
     * 将数据图层自定义子表所有配置转成JSON形式
     * （现在用在向数据库插入数据图层表时，主要用于原生层）
     */
    private fun customLayerItemListToBundle(list: List<CustomLayerItem>): String {
        val jsonArray = JSONArray()
        for (item in list) {
            val jsonObject = JSONObject()
            jsonObject.put("key", item.key)
            jsonObject.put("title", item.title)
            jsonObject.put("type", item.type.ordinal)
//            jsonObject.put("nullable", item.nullable)
//            jsonObject.put("primaryKey", item.primaryKey)
//            jsonObject.put("value", item.value)
//            jsonObject.put("selectOptions", item.selectOptions)
//            jsonObject.put("isMainName", item.isMainName)
            jsonObject.put("describe", item.describe)
            val checkIdsArray = JSONArray()
//            for (c in item.checkManagerList) {
//                checkIdsArray.put(c.id)
//            }
            jsonObject.put(
                "checkManagerIds", checkIdsArray
            )
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    /**
     * 将字段的控件类型，转换成数据库字段类型
     *
     * enum LayerItemType {
    ///输入框
    layerItemTypeInput,

    ///输入框集合
    layerItemTypeInputArray,

    ///纯展示文本
    layerItemTypeText,

    ///单选框
    layerItemTypeSingleSelection,

    ///多选框
    layerItemTypeMultipleSelection，

    ///菜单
    layerItemTypeMultiLevelMenu
    }
     */
    private fun layerItemTypeToDBType(type: DataLayerItemType): String {
        when (type) {
            DataLayerItemType.DataLayerItemTypeInput -> return "TEXT"
        }
        return "TEXT";
    }


    /**
     * 查询获取图层列表
     */
    fun getDataLayerList(callback: (list: List<LayerManager>) -> Unit) {
        thread(start = true) {
            val list = mDataBase.layerManagerDao.findList();
//            ///获取拓展部分
//            for (l in list) {
//                l.itemList = getItemList(l.bundle);
//            }
            Handler(Looper.getMainLooper()).post {
                callback.invoke(list)
            }
        }
    }

    /**
     * 解析dataLayer的bundle部分
     */
    private fun getItemList(bundle: String): List<CustomLayerItem>? {
        val itemList: MutableList<CustomLayerItem> = ArrayList()
        if (bundle != null) {
            try {
                val jsonArray = JSONArray(bundle)
                for (i in 0 until jsonArray.length()) {
                    val itemObject = jsonArray.getJSONObject(i)
                    val checkManagerList = mutableListOf<CheckManager>()
                    val idArray = itemObject.optJSONArray("checkManagerIds")
                    if (idArray != null) {
                        for (j in 0 until idArray.length()) {
                            val id = idArray[j] as Int
                            val check = mDataBase.checkManagerDao.findCheckManagerById(id.toLong())
                            if (check != null)
                                checkManagerList.add(check)
                        }
                    }
                    val item = CustomLayerItem(
                        key = itemObject.optString("key"),
                        title = itemObject.optString("title"),
                        type = DataLayerItemType.values()[itemObject.optInt("type")],
//                        nullable = itemObject.optBoolean("nullable", true),
//                        primaryKey = itemObject.optBoolean("primaryKey", false),
//                        value = itemObject.opt("value"),
//                        selectOptions = itemObject.optString("selectOptions"),
//                        isMainName = itemObject.optBoolean("isMainName"),
                        describe = itemObject.optString("describe"),
//                        checkManagerList =
                        itemBean = ""
                    )
                    itemList.add(item)
                }
            } catch (e: Exception) {
                Log.e("jingo", "CustomLayerItem 列表创建失败 " + e.message)
            }
        }
        return itemList
    }

    /**
     * 查询某个获取图层
     */
    fun getDataLayer(layerId: String, callback: (layer: LayerManager) -> Unit) {
        thread(start = true) {
            val layer = mDataBase.layerManagerDao.findLayerManager(layerId);
            layer.itemList = getItemList(layer.bundle);
            Handler(Looper.getMainLooper()).post {
                callback.invoke(layer)
            }
        }
    }
}