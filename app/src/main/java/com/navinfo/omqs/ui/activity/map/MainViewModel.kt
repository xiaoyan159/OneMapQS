package com.navinfo.omqs.ui.activity.map

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import com.blankj.utilcode.util.ToastUtils
import com.navinfo.collect.library.data.dao.impl.TraceDataBase
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.handler.OnQsRecordItemClickListener
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.bean.TaskBean
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.ui.dialog.CommonDialog
import com.navinfo.omqs.ui.manager.TakePhotoManager
import com.navinfo.omqs.ui.widget.SignUtil
import com.navinfo.omqs.util.DateTimeUtil
import com.navinfo.omqs.util.SoundMeter
import com.navinfo.omqs.util.SpeakMode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import io.realm.RealmSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.oscim.core.GeoPoint
import org.videolan.libvlc.LibVlcUtil
import java.io.File
import java.util.*
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

//    var testPoint = GeoPoint(0, 0)

    //语音窗体
    private var pop: PopupWindow? = null

    private var mSpeakMode: SpeakMode? = null

    //录音图标
    var volume: ImageView? = null
    var mSoundMeter: SoundMeter? = null

    var menuState: Boolean = false

    val liveDataMenuState = MutableLiveData<Boolean>()

    /**
     * 是不是线选择模式
     */
    private var bSelectRoad = false

    init {
        mapController.markerHandle.setOnQsRecordItemClickListener(object :
            OnQsRecordItemClickListener {
            override fun onQsRecordList(list: MutableList<String>) {
                liveDataQsRecordIdList.value = list
            }
        })
        initLocation()
        viewModelScope.launch {
            mapController.onMapClickFlow.collectLatest {
//                testPoint = it
                if (bSelectRoad) {
                    captureLink(it)
                }
            }
        }

        initTaskData()
    }

    /**
     * 初始话任务高亮高亮
     */
    private fun initTaskData() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            viewModelScope.launch {
                val realm = Realm.getDefaultInstance()
                val results = realm.where(TaskBean::class.java).findAll()
                val list = realm.copyFromRealm(results)
                results.addChangeListener { changes ->
                    val list2 = realm.copyFromRealm(changes)
                    mapController.lineHandler.omdbTaskLinkLayer.removeAll()
                    for (item in list2) {
                        mapController.lineHandler.omdbTaskLinkLayer.addLineList(item.hadLinkDvoList)
                    }
                }
                mapController.lineHandler.omdbTaskLinkLayer.removeAll()
                for (item in list) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mapController.lineHandler.omdbTaskLinkLayer.setLineColor(Color.valueOf(item.color))
                    }
                    mapController.lineHandler.omdbTaskLinkLayer.addLineList(item.hadLinkDvoList)
                }
            }
//            realm.close()
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
//                location.longitude = testPoint.longitude
//                location.latitude = testPoint.latitude
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
                mapController.mMapView.vtmMap.updateMap(true)
            }
        }
        //用于定位点捕捉道路
        viewModelScope.launch(Dispatchers.Default) {
            mapController.locationLayerHandler.niLocationFlow.collectLatest { location ->
//                location.longitude = testPoint.longitude
//                location.latitude = testPoint.latitude
                if (!isSelectRoad())
                    captureLink(GeoPoint(location.latitude, location.longitude))
            }
        }

        //显示轨迹图层
        mapController.layerManagerHandler.showNiLocationLayer()

    }

    /**
     * 捕获道路和面板
     */
    private suspend fun captureLink(point: GeoPoint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val linkList = realmOperateHelper.queryLink(
                point = point,
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
                            point,
                            GeometryTools.createGeoPoint(element.geometry)
                        )
                        signList.add(
                            SignBean(
                                iconId = SignUtil.getSignIcon(element),
                                iconText = SignUtil.getSignIconText(element),
                                distance = distance.toInt(),
                                elementId = element.id,
                                linkId = linkId,
                                geometry = element.geometry,
                                bottomText = SignUtil.getSignBottomText(element)
                            )
                        )
                    }

                }
            }
            liveDataSignList.postValue(signList)
            Log.e("jingo", "自动捕捉数据 共${signList.size}条")
        }
    }

    /**
     * 点击我的位置，回到我的位置
     */
    fun onClickLocationButton() {
        mapController.locationLayerHandler.animateToCurrentPosition()
    }

    /**
     * 点击菜单
     */
    fun onClickMenu() {
        menuState = !menuState
        liveDataMenuState.postValue(menuState)
    }

    override fun onCleared() {
        super.onCleared()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mapController.lineHandler.removeLine()
        }
    }

    //点击相机按钮
    fun onClickCameraButton(context: Context) {

        Log.e("qj", LibVlcUtil.hasCompatibleCPU(context).toString())

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

    fun startSoundMetter(context: Context, v: View) {

        if (mSpeakMode == null) {
            mSpeakMode = SpeakMode(context as Activity?)
        }

        //语音识别动画
        if (pop == null) {
            pop = PopupWindow()
            pop!!.width = ViewGroup.LayoutParams.MATCH_PARENT
            pop!!.height = ViewGroup.LayoutParams.WRAP_CONTENT
            pop!!.setBackgroundDrawable(BitmapDrawable())
            val view = View.inflate(context, R.layout.cv_card_voice_rcd_hint_window, null)
            pop!!.contentView = view
            volume = view.findViewById(R.id.volume)
        }

        pop!!.update()

        Constant.IS_VIDEO_SPEED = true
        //录音动画
        //录音动画
        if (pop != null) {
            pop!!.showAtLocation(v, Gravity.CENTER, 0, 0)
        }
        volume!!.setBackgroundResource(R.drawable.pop_voice_img)
        val animation = volume!!.background as AnimationDrawable
        animation.start()

        val name: String = DateTimeUtil.getTimeSSS().toString() + ".m4a"
        if (mSoundMeter == null) {
            mSoundMeter = SoundMeter()
        }
        mSoundMeter!!.setmListener(object : SoundMeter.OnSoundMeterListener {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onSuccess(filePath: String?) {
                if (!TextUtils.isEmpty(filePath) && File(filePath).exists()) {
                    if (File(filePath) == null || File(filePath).length() < 1600) {
                        ToastUtils.showLong("语音时间太短，无效！")
                        mSpeakMode!!.speakText("语音时间太短，无效")
                        stopSoundMeter()
                        return
                    }
                }
                mSpeakMode!!.speakText("结束录音")
                //获取右侧fragment容器
                val naviController =
                    (context as Activity).findNavController(R.id.main_activity_right_fragment)
                val bundle = Bundle()
                bundle.putString("filePath", filePath)
                naviController.navigate(R.id.EvaluationResultFragment, bundle)
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            override fun onfaild(message: String?) {
                ToastUtils.showLong("录制失败！")
                mSpeakMode!!.speakText("录制失败")
                stopSoundMeter()
            }
        })

        mSoundMeter!!.start(Constant.USER_DATA_ATTACHEMNT_PATH + name)
        ToastUtils.showLong("开始录音")
        mSpeakMode!!.speakText("开始录音")
    }

    //停止语音录制
    @RequiresApi(api = Build.VERSION_CODES.Q)
    fun stopSoundMeter() {
        //先重置标识，防止按钮抬起时触发语音结束
        Constant.IS_VIDEO_SPEED = false
        if (mSoundMeter != null && mSoundMeter!!.isStartSound()) {
            mSoundMeter!!.stop()
        }
        if (pop != null && pop!!.isShowing) pop!!.dismiss()
    }

    /**
     * 刷新OMDB图层显隐
     * */
    fun refreshOMDBLayer(layerConfigList: List<ImportConfig>) {
        // 根据获取到的配置信息，筛选未勾选的图层名称
        if (layerConfigList != null && !layerConfigList.isEmpty()) {
            val omdbVisibleList = layerConfigList.filter { importConfig ->
                importConfig.tableGroupName == "OMDB数据"
            }.first().tables.filter { tableInfo ->
                !tableInfo.checked
            }.map { tableInfo ->
                tableInfo.table
            }.toList()
            com.navinfo.collect.library.system.Constant.HAD_LAYER_INVISIABLE_ARRAY =
                omdbVisibleList.toTypedArray()
            // 刷新地图
            mapController.mMapView.vtmMap.clearMap()
        }
    }

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

    /**
     * 开启线选择
     */
    fun setSelectRoad(select: Boolean) {
        bSelectRoad = select
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mapController.lineHandler.removeLine()
            liveDataSignList.value = mutableListOf()
        }
    }

    fun isSelectRoad(): Boolean {
        return bSelectRoad
    }

}