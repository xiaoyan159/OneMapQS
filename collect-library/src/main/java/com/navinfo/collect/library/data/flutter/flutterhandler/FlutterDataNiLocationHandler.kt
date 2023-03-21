package com.navinfo.collect.library.data.flutter.flutterhandler

import android.content.Context
import android.util.Log
import com.navinfo.collect.library.data.dao.impl.MapLifeDataBase
import com.navinfo.collect.library.data.entity.NiLocation
import com.navinfo.collect.library.data.handler.DataNiLocationHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 数据操作
 */
class FlutterDataNiLocationHandler(context: Context, dataBase: MapLifeDataBase) :
    DataNiLocationHandler(context, dataBase) {

    /**
     * 保存数据
     */
    fun saveNiLocationData(call: MethodCall, result: MethodChannel.Result) = try {
        if (call.arguments is Map<*, *>) {
            val niLocation = NiLocation()
            niLocation.id = call.argument("uuid")
            niLocation.longitude = call.argument<Double>("longitude")!!
            niLocation.latitude = call.argument<Double>("latitude")!!
            niLocation.altitude = call.argument<Double>("altitude")!!
            niLocation.radius = call.argument<Double>("radius")!!
            niLocation.direction = call.argument<Double>("direction")!!
            niLocation.time = call.argument("time")!!
            if(niLocation.time.length==19){
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val dDate: Date = simpleDateFormat.parse(niLocation.time)
                niLocation.time = dDate.time.toString()
            }
            niLocation.speed = call.argument<Float>("speed")!!
            niLocation.country = call.argument("country")
            niLocation.province = call.argument("province")
            niLocation.city = call.argument("city")
            niLocation.district = call.argument("district")
            niLocation.street = call.argument("street")
            niLocation.town = call.argument("town")
            niLocation.adCode = call.argument("adCode")
            niLocation.cityCode = call.argument("cityCode")
            niLocation.streetNumber = call.argument("streetNumber")
            niLocation.errorCode = call.argument<String?>("errorCode").toString()
            niLocation.errorInfo = call.argument("errorInfo")

            saveDataNiLocation(niLocation) { res, error ->
                if (res) {
                    result.success("$res")
                } else {
                    result.success(error)
                }
            }

        } else {
            result.success("数据格式错误")
        }
    } catch (e: Throwable) {
        e.message?.let { Log.e("jingo", it) }
        Log.e("jingo", e.stackTraceToString())
        Log.e("jingo", e.toString())
        result.success("数据格式错误")
    }

    /**
     * 删除数据
     */
    fun deleteNiLocationData(call: MethodCall, result: MethodChannel.Result) = try {
        if (call.arguments is Map<*, *>) {
            val niLocation = NiLocation()
            niLocation.id = call.argument("uuid")
            deleteData(niLocation) { res, message ->
                if (res) {
                    result.success("success")
                } else {
                    result.success(message)
                }
            };

        } else {
            result.success("数据格式错误")
        }
    } catch (e: Throwable) {
        e.message?.let { Log.e("jingo", it) }
        Log.e("jingo", e.stackTraceToString())
        Log.e("jingo", e.toString())
        result.success("数据格式错误")
    }
}