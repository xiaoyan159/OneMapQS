package com.navinfo.omqs.ui.activity.map

import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import com.navinfo.collect.library.data.dao.impl.TraceDataBase
import com.navinfo.collect.library.data.entity.NiLocation
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.handler.NiLocationListener
import com.navinfo.collect.library.map.handler.OnQsRecordItemClickListener
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.ui.dialog.CommonDialog
import com.navinfo.omqs.ui.manager.TakePhotoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import io.realm.RealmSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.oscim.core.GeoPoint
import org.videolan.libvlc.LibVlcUtil
import javax.inject.Inject

/**
 * 创建Activity全局viewmode
 */

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mapController: NIMapController,
    private val traceDataBase: TraceDataBase,
    private val realmOperateHelper: RealmOperateHelper
) : ViewModel() {

    private var mCameraDialog: CommonDialog? = null

    //地图点击捕捉到的质检数据ID列表
    val liveDataQsRecordIdList = MutableLiveData<List<String>>()

    //看板数据
    val liveDataSignList = MutableLiveData<List<SignBean>>()


    //    private var niLocationList: MutableList<NiLocation> = ArrayList<NiLocation>()
    var testPoint = GeoPoint(0, 0)

    init {
        mapController.markerHandle.setOnQsRecordItemClickListener(object :
            OnQsRecordItemClickListener {
            override fun onQsRecordList(list: MutableList<String>) {
                liveDataQsRecordIdList.value = list
            }
        })
        initLocation()
        viewModelScope.launch {
            mapController.onMapClickFlow.collect {
                testPoint = it
            }
        }

    }

    private fun initLocation() {
        //        mapController.locationLayerHandler.setNiLocationListener(NiLocationListener {
//            addSaveTrace(it)
//
//        })
        //用于定位点存储到数据库
        viewModelScope.launch(Dispatchers.Default) {
            mapController.locationLayerHandler.niLocationFlow.collect { location ->
                location.longitude = testPoint.longitude
                location.latitude = testPoint.latitude
                val geometry = GeometryTools.createGeometry(
                    GeoPoint(
                        location.latitude,
                        location.longitude
                    )
                )
                val tileX = RealmSet<Int>()
                GeometryToolsKt.getTileXByGeometry(geometry.toString(), tileX)
                val tileY = RealmSet<Int>()
                GeometryToolsKt.getTileYByGeometry(geometry.toString(), tileY)

                //遍历存储tile对应的x与y的值
                tileX.forEach { x ->
                    tileY.forEach { y ->
                        location.tilex = x
                        location.tiley = y
                    }
                }
                Log.e("jingo", "定位点插入 ${Thread.currentThread().name}")
                traceDataBase.niLocationDao.insert(location)
            }
        }
        //用于定位点捕捉道路
        viewModelScope.launch(Dispatchers.Default) {
            mapController.locationLayerHandler.niLocationFlow.collect { location ->
                Log.e("jingo", "定位点绑定道路 ${Thread.currentThread().name}")
                location.longitude = testPoint.longitude
                location.latitude = testPoint.latitude
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val linkList = realmOperateHelper.queryLink(
                        point = GeometryTools.createPoint(
                            location.longitude,
                            location.latitude
                        ),
                    )
                    //看板数据
                    val signList = mutableListOf<SignBean>()
                    if (linkList.isNotEmpty()) {
                        val link = linkList[0]
                        val linkId = link.properties[RenderEntity.Companion.LinkTable.linkPid]
                        mapController.lineHandler.showLine(link.geometry)
                        linkId?.let {
                            var elementList = realmOperateHelper.queryLinkByLinkPid(it)
                            for (element in elementList) {
                                val distance = GeometryTools.distanceToDouble(
                                    GeoPoint(
                                        location.latitude, location.longitude,
                                    ),
                                    GeometryTools.createGeoPoint(element.geometry)
                                )
                                signList.add(
                                    SignBean(
                                        iconId = R.drawable.icon_speed_limit,
                                        iconText = element.name,
                                        distance = distance.toInt(),
                                    )
                                )
                            }
                            liveDataSignList.postValue(signList)
                            Log.e("jingo", "自动捕捉数据 共${elementList.size}条")
                        }
                    }
                }
            }
        }

        //显示轨迹图层
        mapController.layerManagerHandler.showNiLocationLayer()
    }

    /**
     * 点击我的位置，回到我的位置
     */
    fun onClickLocationButton() {
        mapController.locationLayerHandler.animateToCurrentPosition()
    }

    override fun onCleared() {
        super.onCleared()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mapController.lineHandler.removeLine()
        }
    }

    //点击相机按钮
    fun onClickCameraButton(context: Context) {

        Log.e("qj", LibVlcUtil.hasCompatibleCPU(context).toString())

        //ToastUtils.showShort("点击了相机")

        if (mCameraDialog == null) {
            mCameraDialog = CommonDialog(
                context,
                context.resources.getDimension(R.dimen.head_img_width)
                    .toInt() * 3 + context.resources.getDimension(R.dimen.ten)
                    .toInt() + context.resources.getDimension(R.dimen.twenty_four).toInt(),
                context.resources.getDimension(R.dimen.head_img_width).toInt() + 10,
                1
            )
            mCameraDialog!!.setCancelable(true)
        }
        mCameraDialog!!.openCamear(mCameraDialog!!.getmShareUtil().continusTakePhotoState)
        mCameraDialog!!.show()
        mCameraDialog!!.setOnDismissListener(DialogInterface.OnDismissListener {
            mCameraDialog!!.hideLoading()
            mCameraDialog!!.stopVideo()
            try {
                if (!mCameraDialog!!.getmShareUtil().connectstate) {
                    mCameraDialog!!.updateCameraResources(1, mCameraDialog!!.getmDeviceNum())
                }
                TakePhotoManager.getInstance().getCameraVedioClent(mCameraDialog!!.getmDeviceNum())
                    .StopSearch()
            } catch (e: Exception) {
            }
        })
        mCameraDialog!!.setOnShowListener(DialogInterface.OnShowListener {
            mCameraDialog!!.initmTakePhotoOrRecord(mCameraDialog!!.getmShareUtil().selectTakePhotoOrRecord)
            if (!mCameraDialog!!.isShowVideo && mCameraDialog!!.getmShareUtil().connectstate && mCameraDialog!!.getmShareUtil().continusTakePhotoState) {
                mCameraDialog!!.playVideo()
            }
        })
    }


//    fun startSaveTraceThread(context: Context) {
//        Thread(Runnable {
//            try {
//                while (true) {
//
//                    if (niLocationList != null && niLocationList.size > 0) {
//
//                        var niLocation = niLocationList[0]
//                        val geometry = GeometryTools.createGeometry(
//                            GeoPoint(
//                                niLocation.latitude,
//                                niLocation.longitude
//                            )
//                        )
//                        val tileX = RealmSet<Int>()
//                        GeometryToolsKt.getTileXByGeometry(geometry.toString(), tileX)
//                        val tileY = RealmSet<Int>()
//                        GeometryToolsKt.getTileYByGeometry(geometry.toString(), tileY)
//
//                        //遍历存储tile对应的x与y的值
//                        tileX.forEach { x ->
//                            tileY.forEach { y ->
//                                niLocation.tilex = x
//                                niLocation.tiley = y
//                            }
//                        }
//
//                        TraceDataBase.getDatabase(
//                            context,
//                            Constant.USER_DATA_PATH + "/trace.sqlite"
//                        ).niLocationDao.insert(niLocation)
//                        niLocationList.remove(niLocation)
//
//                        Log.e("qj", "saveTrace==${niLocationList.size}")
//                    }
//                    Thread.sleep(30)
//                }
//            } catch (e: InterruptedException) {
//                e.printStackTrace()
//                Log.e("qj", "异常==${e.message}")
//            }
//        }).start()
//    }

//    //增加轨迹存储
//    fun addSaveTrace(niLocation: NiLocation) {
//        if (niLocation != null && niLocationList != null) {
//            niLocationList.add(niLocation)
//        }
//    }

    /**
     * 处理页面调转
     */
    fun navigation(activity: MainActivity, list: List<String>) {
        //获取右侧fragment容器
        val naviController = activity.findNavController(R.id.main_activity_right_fragment)

        naviController.currentDestination?.let { navDestination ->
//            when (val fragment =
//                activity.supportFragmentManager.findFragmentById(navDestination.id)) {
//                //判断右侧的fragment是不是质检数据
////                is EvaluationResultFragment -> {
////                    val viewModelFragment =
////                        ViewModelProvider(fragment)[EvaluationResultViewModel::class.java]
////                    viewModelFragment.notifyData(list)
////                }
//                is EmptyFragment -> {
//                    if (list.size == 1) {
//                        val bundle = Bundle()
//                        bundle.putString("QsId", list[0])
//                        naviController.navigate(R.id.EvaluationResultFragment, bundle)
//                    }
//                }
//            }
            when (navDestination.id) {
                R.id.EmptyFragment -> {
                    if (list.size == 1) {
                        val bundle = Bundle()
                        bundle.putString("QsId", list[0])
                        naviController.navigate(R.id.EvaluationResultFragment, bundle)
                    }
                }
            }
        }

    }
}