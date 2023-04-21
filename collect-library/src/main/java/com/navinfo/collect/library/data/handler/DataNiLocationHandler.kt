package com.navinfo.collect.library.data.handler

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CONFLICT_NONE
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.dao.impl.TraceDataBase
import com.navinfo.collect.library.data.entity.*
import com.navinfo.collect.library.data.entity.DataLayerItemType.*
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import io.realm.RealmSet
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader
import org.oscim.core.GeoPoint
import org.oscim.core.MercatorProjection
import kotlin.concurrent.thread

/**
 * 数据库操作
 */


open class
DataNiLocationHandler(context: Context, dataBase: TraceDataBase) :
    BaseDataHandler(context, dataBase) {

    /**
     * 保存数据
     */
    fun saveDataNiLocation(
        niLocation: NiLocation,
        callback: (res: Boolean, errorString: String) -> Unit
    ) {
        thread(start = true) {
            try {

                val geometry = GeometryTools.createGeometry(
                    GeoPoint(
                        niLocation.latitude,
                        niLocation.longitude
                    )
                )
                val tileX = RealmSet<Int>()
                GeometryToolsKt.getTileXByGeometry(geometry.toString(), tileX)
                val tileY = RealmSet<Int>()
                GeometryToolsKt.getTileYByGeometry(geometry.toString(), tileY)

                //遍历存储tile对应的x与y的值
                tileX.forEach { x ->

                    tileY.forEach { y ->
                        niLocation.tilex = x
                        niLocation.tiley = y
                    }

                }
                val niLocationLoad = (mDataBase as TraceDataBase).niLocationDao.find(niLocation.id);
                if(niLocationLoad!=null){
                    (mDataBase as TraceDataBase).niLocationDao.update(niLocation)
                }else{
                    (mDataBase as TraceDataBase).niLocationDao.insert(niLocation)
                }
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(true, "")
                }
            } catch (e: Throwable) {
                e.message?.let { Log.e("qj", it) }
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(false, "${e.message}")
                }
            }

        }
    }

    /**
     * 删除数据
     */

    fun deleteData(niLocation: NiLocation, callback: (res: Boolean, errorString: String) -> Unit) {
        thread(start = true) {
            try {
                mDataBase.openHelper.writableDatabase.delete(
                    "'niLocation'",
                    "uuid=?",
                    arrayOf("'${niLocation.id}'")
                )
                (mDataBase as TraceDataBase).niLocationDao.delete(niLocation);
            } catch (e: Throwable) {
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(false, "${e.message}")
                }
            }
            Handler(Looper.getMainLooper()).post {
                callback.invoke(true, "")
            }
        }
    }

}

