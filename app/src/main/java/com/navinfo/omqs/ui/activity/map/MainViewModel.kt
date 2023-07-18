package com.navinfo.omqs.ui.activity.map

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import com.blankj.utilcode.util.ToastUtils
import com.navinfo.collect.library.data.dao.impl.TraceDataBase
import com.navinfo.collect.library.data.entity.NiLocation
import com.navinfo.collect.library.data.entity.NoteBean
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.handler.OnQsRecordItemClickListener
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.bean.QRCodeBean
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.bean.TraceVideoBean
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.http.NetworkService
import com.navinfo.omqs.ui.activity.scan.QrCodeStatus
import com.navinfo.omqs.ui.dialog.CommonDialog
import com.navinfo.omqs.ui.manager.TakePhotoManager
import com.navinfo.omqs.ui.other.BaseToast
import com.navinfo.omqs.ui.widget.SignUtil
import com.navinfo.omqs.util.DateTimeUtil
import com.navinfo.omqs.util.SoundMeter
import com.navinfo.omqs.util.SpeakMode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import io.realm.RealmSet
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.oscim.core.GeoPoint
import org.oscim.core.MapPosition
import org.oscim.map.Map
import org.videolan.libvlc.LibVlcUtil
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

/**
 * 创建Activity全局viewmode
 */

@RequiresApi(Build.VERSION_CODES.N)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val mapController: NIMapController,
    private val traceDataBase: TraceDataBase,
    private val realmOperateHelper: RealmOperateHelper,
    private val networkService: NetworkService,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private var mCameraDialog: CommonDialog? = null

    //地图点击捕捉到的质检数据ID列表
    val liveDataQsRecordIdList = MutableLiveData<List<String>>()

    //地图点击捕捉到的标签ID列表
    val liveDataNoteIdList = MutableLiveData<List<String>>()

    //地图点击捕捉到的轨迹列表
    val liveDataNILocationList = MutableLiveData<NiLocation>()

    //左侧看板数据
    val liveDataSignList = MutableLiveData<List<SignBean>>()

    //顶部看板数据
    val liveDataTopSignList = MutableLiveData<List<SignBean>>()

    //道路名
    val liveDataRoadName = MutableLiveData<RenderEntity?>()

    /**
     * 当前选中的要展示的详细信息的要素
     */
    val liveDataSignMoreInfo = MutableLiveData<RenderEntity>()

    private var traceTag: String = "TRACE_TAG"

//    var testPoint = GeoPoint(0, 0)

    //uuid标识，用于记录轨迹组
    val uuid = UUID.randomUUID().toString()

    //语音窗体
    private var pop: PopupWindow? = null

    var speakMode: SpeakMode? = null

    //录音图标
    var volume: ImageView? = null

    var mSoundMeter: SoundMeter? = null

    var menuState: Boolean = false

    var captureLinkState: Boolean = false

    val liveDataMenuState = MutableLiveData<Boolean>()

    val liveDataCenterPoint = MutableLiveData<MapPosition>()

    //状态
    val qrCodeStatus: MutableLiveData<QrCodeStatus> = MutableLiveData()

    /**
     * 是不是线选择模式
     */
    private var bSelectRoad = false

    /**
     * 是不是选择轨迹点
     */
    private var bSelectTrace = false

    /**
     * 是不是选择标题标识
     */
    private var isMediaFlag = false

    /**
     * 是不是暂停
     */
    private var bSelectPauseTrace = false

    private var linkIdCache = ""

    private var lastNiLocaion: NiLocation? = null

    var currentIndexNiLocation: Int = 0;

    init {
        mapController.mMapView.vtmMap.events.bind(Map.UpdateListener { e, mapPosition ->
            when (e) {
                Map.SCALE_EVENT, Map.MOVE_EVENT, Map.ROTATE_EVENT -> liveDataCenterPoint.value =
                    mapPosition
            }
        })

        //处理质检数据点击事件
        mapController.markerHandle.setOnQsRecordItemClickListener(object :
            OnQsRecordItemClickListener {
            override fun onQsRecordList(list: MutableList<String>) {
                liveDataQsRecordIdList.value = list
            }

            override fun onNoteList(list: MutableList<String>) {
                liveDataNoteIdList.value = list
            }

            override fun onNiLocation(index: Int, item: NiLocation) {
                liveDataNILocationList.value = item
                currentIndexNiLocation = index
            }
        })

        initLocation()

        //处理地图点击操作
        viewModelScope.launch(Dispatchers.Default) {
            mapController.onMapClickFlow.collectLatest {
//                testPoint = it
                //线选择状态
                /*                if (bSelectRoad) {
                                    captureLink(it)
                                } else {
                                    captureItem(it)
                                }*/
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            initTaskData()
            initQsRecordData()
            initNoteData()
            initNILocationData()
        }
    }

    /**
     * 初始化选中的任务高亮高亮
     */
    private suspend fun initTaskData() {
        val id = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)
        val realm = Realm.getDefaultInstance()
        val res = realm.where(TaskBean::class.java).equalTo("id", id).findFirst()
        if (res != null) {
            val taskBean = realm.copyFromRealm(res)
            mapController.lineHandler.showTaskLines(taskBean.hadLinkDvoList)
        }

    }

    /**
     * 初始化渲染质检数据
     */
    private suspend fun initQsRecordData() {
        var list = mutableListOf<QsRecordBean>()
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            val objects = realm.where<QsRecordBean>().findAll()
            list = realm.copyFromRealm(objects)
        }
        for (item in list) {
            mapController.markerHandle.addOrUpdateQsRecordMark(item)
        }
    }

    /**
     * 初始化渲染便签数据
     */
    private suspend fun initNoteData() {
        var list = mutableListOf<NoteBean>()
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            val objects = realm.where<NoteBean>().findAll()
            list = realm.copyFromRealm(objects)
        }



        for (item in list) {
            mapController.markerHandle.addOrUpdateNoteMark(item)
        }
    }

    private suspend fun initNILocationData() {
        //加载轨迹数据
        val id = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)
        val list: List<NiLocation>? = TraceDataBase.getDatabase(
            mapController.mMapView.context,
            Constant.USER_DATA_PATH
        ).niLocationDao.findToTaskIdAll(id.toString())
        list!!.forEach {
            mapController.markerHandle.addNiLocationMarkerItem(it)
        }
    }

    /**
     * 初始化定位信息
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun initLocation() {
        //用于定位点存储到数据库
        viewModelScope.launch(Dispatchers.Default) {
            mapController.locationLayerHandler.niLocationFlow.collect { location ->

                //过滤掉无效点
                if (!GeometryTools.isCheckError(location.longitude, location.latitude)) {
                    val geometry = GeometryTools.createGeometry(
                        GeoPoint(
                            location.latitude, location.longitude
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
                    location.groupId = uuid
                    try {
                        location.timeStamp = DateTimeUtil.getTime(location.time).toString()
                    } catch (e: Exception) {

                    }
                    val id = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)
                    location.taskId = id.toString()
                    //增加间距判断
                    if (lastNiLocaion != null) {
                        val disance = GeometryTools.getDistance(
                            location.latitude, location.longitude,
                            lastNiLocaion!!.latitude, lastNiLocaion!!.longitude
                        )
                        //相距差距大于2.5米以上进行存储
                        if (disance > 2.5 && disance < 60) {
                            traceDataBase.niLocationDao.insert(location)
                            mapController.markerHandle.addNiLocationMarkerItem(location)
                            mapController.mMapView.vtmMap.updateMap(true)
                            lastNiLocaion = location
                        }
                    } else {
                        traceDataBase.niLocationDao.insert(location)
                        mapController.markerHandle.addNiLocationMarkerItem(location)
                        mapController.mMapView.vtmMap.updateMap(true)
                        lastNiLocaion = location
                    }
                }
            }
        }
        //用于定位点捕捉道路
        viewModelScope.launch(Dispatchers.Default) {
            mapController.locationLayerHandler.niLocationFlow.collectLatest { location ->
                if (!isSelectRoad() && !GeometryTools.isCheckError(
                        location.longitude,
                        location.latitude
                    )
                ) captureLink(
                    GeoPoint(
                        location.latitude,
                        location.longitude
                    )
                )
            }
        }

        //显示轨迹图层
        mapController.layerManagerHandler.showNiLocationLayer()

    }

    /**
     * 捕捉要素
     */
    private suspend fun captureItem(point: GeoPoint) {

    }

    /**
     * 捕获道路和面板
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun captureLink(point: GeoPoint) {
        if (captureLinkState) {
            return
        }

        try {
            captureLinkState = true

            val linkList = realmOperateHelper.queryLink(
                point = point,
            )
            var hisRoadName = false
            if (linkList.isNotEmpty()) {
                //看板数据
                val signList = mutableListOf<SignBean>()
                val topSignList = mutableListOf<SignBean>()
                mapController.lineHandler.linksLayer.clear()

                val link = linkList[0]

                val linkId = link.properties[RenderEntity.Companion.LinkTable.linkPid]

                if (linkIdCache != linkId) {

                    mapController.lineHandler.showLine(link.geometry)
                    linkId?.let {
                        var elementList = realmOperateHelper.queryLinkByLinkPid(it)
                        for (element in elementList) {

                            if (element.code == 2011) {
                                hisRoadName = true
                                liveDataRoadName.postValue(element)
                                continue
                            }

                            val distance = GeometryTools.distanceToDouble(
                                point, GeometryTools.createGeoPoint(element.geometry)
                            )

                            val signBean = SignBean(
                                iconId = SignUtil.getSignIcon(element),
                                iconText = SignUtil.getSignIconText(element),
                                distance = distance.toInt(),
                                linkId = linkId,
                                name = SignUtil.getSignNameText(element),
                                bottomRightText = SignUtil.getSignBottomRightText(element),
                                renderEntity = element,
                                isMoreInfo = SignUtil.isMoreInfo(element),
                                index = SignUtil.getRoadInfoIndex(element)
                            )
                            Log.e("jingo", "捕捉到的数据code ${element.code}")
                            when (element.code) {
                                //车道数，种别，功能等级,线限速,道路方向
                                2041, 2008, 2002, 2019, 2010 -> topSignList.add(
                                    signBean
                                )

                                4002, 4003, 4004, 4010, 4022, 4601 -> signList.add(
                                    signBean
                                )
                            }

                        }

                        val realm = Realm.getDefaultInstance()
                        val entity = realm.where(RenderEntity::class.java)
                            .equalTo("table", "OMDB_RESTRICTION").and().equalTo(
                                "properties['linkIn']", it
                            ).findFirst()
                        if (entity != null) {
                            val outLink = entity.properties["linkOut"]
                            val linkOutEntity = realm.where(RenderEntity::class.java)
                                .equalTo("table", "OMDB_RD_LINK").and().equalTo(
                                    "properties['${RenderEntity.Companion.LinkTable.linkPid}']",
                                    outLink
                                ).findFirst()
                            if (linkOutEntity != null) {
                                mapController.lineHandler.linksLayer.addLine(
                                    linkOutEntity.geometry, 0x7DFF0000
                                )
                            }
                        }
                    }

                    liveDataTopSignList.postValue(topSignList.distinctBy { it.name }
                        .sortedBy { it.index })

                    liveDataSignList.postValue(signList.sortedBy { it.distance })
                    val speechText = SignUtil.getRoadSpeechText(topSignList)
                    withContext(Dispatchers.Main) {
                        speakMode?.speakText(speechText)
                    }
                    linkIdCache = linkId ?: ""
                }
            } else {
                mapController.lineHandler.removeLine()
                linkIdCache = ""
            }
            //如果没有捕捉到道路名
            if (!hisRoadName) {
                liveDataRoadName.postValue(null)
            }

        } catch (e: Exception) {

        } finally {
            captureLinkState = false
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
                    mCameraDialog!!.updateCameraResources(
                        1,
                        mCameraDialog!!.getmDeviceNum()
                    )
                }
                TakePhotoManager.getInstance()
                    .getCameraVedioClent(mCameraDialog!!.getmDeviceNum())
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
                        speakMode?.speakText("语音时间太短，无效")
                        stopSoundMeter()
                        return
                    }
                }
                speakMode?.speakText("结束录音")
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
                speakMode?.speakText("录制失败")
                stopSoundMeter()
            }
        })

        mSoundMeter!!.start(Constant.USER_DATA_ATTACHEMNT_PATH + name)
        ToastUtils.showLong("开始录音")
        speakMode?.speakText("开始录音")
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
            }.first().tableMap.filter { entry ->
                val tableInfo = entry.value
                !tableInfo.checked
            }.map { entry ->
                val tableInfo = entry.value
                tableInfo.table
            }.toList()
            com.navinfo.collect.library.system.Constant.HAD_LAYER_INVISIABLE_ARRAY =
                omdbVisibleList.toTypedArray()
            // 刷新地图
            mapController.mMapView.vtmMap.clearMap()
        }
    }


    /**
     * 开启线选择
     */
    fun setSelectRoad(select: Boolean) {
        bSelectRoad = select
        //去掉缓存
        linkIdCache = ""
        mapController.lineHandler.removeLine()
        liveDataSignList.value = mutableListOf()
    }

    /**
     * 是否开启了线选择
     */
    fun isSelectRoad(): Boolean {
        return bSelectRoad
    }

    /**
     * 开启轨迹选择
     */
    fun setSelectTrace(select: Boolean) {
        bSelectTrace = select
    }

    /**
     * 是否开启了轨迹选择
     */
    fun isSelectTrace(): Boolean {
        return bSelectTrace
    }

    /**
     * 开启媒体标识
     */
    fun setMediaFlag(select: Boolean) {
        isMediaFlag = select
    }

    /**
     * 是否开启了媒体标识
     */
    fun isMediaFlag(): Boolean {
        return isMediaFlag
    }

    /**
     * 开启轨迹选择
     */
    fun setSelectPauseTrace(select: Boolean) {
        bSelectPauseTrace = select
    }

    /**
     * 是否开启了轨迹选择
     */
    fun isSelectPauseTrace(): Boolean {
        return bSelectPauseTrace
    }

    /**
     * 要展示的要素详细信息
     */

    fun showSignMoreInfo(data: RenderEntity) {
        liveDataSignMoreInfo.value = data
    }

    fun sendServerCommand(context: Context, traceVideoBean: TraceVideoBean) {

        if (TextUtils.isEmpty(Constant.INDOOR_IP)) {
            Toast.makeText(context, "获取ip失败！", Toast.LENGTH_LONG).show()
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val url = "http://${Constant.INDOOR_IP}:8080/sensor/service/${traceVideoBean.command}?"

            when (val result = networkService.sendServerCommand(
                url = url,
                traceVideoBean = traceVideoBean
            )) {
                is NetResult.Success<*> -> {

                    if (result.data != null) {
                        try {

                            val defaultUserResponse = result.data as QRCodeBean

                            if (defaultUserResponse.errcode == 0) {

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "命令成功。",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    qrCodeStatus.postValue(QrCodeStatus.QR_CODE_STATUS_UPDATE_VIDEO_INFO_SUCCESS)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "命令无效${defaultUserResponse.errmsg}",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            }

                        } catch (e: IOException) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                is NetResult.Error<*> -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "${result.exception.message}",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    qrCodeStatus.postValue(QrCodeStatus.QR_CODE_STATUS_NET_FAILURE)
                }

                is NetResult.Failure<*> -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "${result.code}:${result.msg}",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    qrCodeStatus.postValue(QrCodeStatus.QR_CODE_STATUS_NET_FAILURE)
                }

                else -> {}
            }

        }

    }

    /**
     * 显示marker
     * @param trackCollection 轨迹点
     * @param type  1 提示最后一个轨迹点  非1提示第一个轨迹点
     */
    fun showMarker(context: Context, niLocation: NiLocation) {
        if (mapController.markerHandle != null) {
            mapController.markerHandle.removeMarker(traceTag)
            if (niLocation != null) {
                mapController.markerHandle.addMarker(
                    GeoPoint(
                        niLocation.latitude,
                        niLocation.longitude
                    ), traceTag, "", niLocation as java.lang.Object
                )
            }
        }
    }

    /**
     * 结束自动播放
     */
    fun cancelTrace() {

    }
}