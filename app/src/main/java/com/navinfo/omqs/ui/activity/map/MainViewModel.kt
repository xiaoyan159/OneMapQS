package com.navinfo.omqs.ui.activity.map

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import com.blankj.utilcode.util.ToastUtils
import com.google.gson.Gson
import com.navinfo.collect.library.data.dao.impl.TraceDataBase
import com.navinfo.collect.library.data.entity.*
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.collect.library.garminvirbxe.HostBean
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.OnGeoPointClickListener
import com.navinfo.collect.library.map.handler.*
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import com.navinfo.collect.library.utils.MapParamUtils
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.*
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.db.RoomAppDatabase
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.http.NetworkService
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.ui.dialog.CommonDialog
import com.navinfo.omqs.ui.manager.TakePhotoManager
import com.navinfo.omqs.ui.other.BaseToast
import com.navinfo.omqs.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmSet
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import org.locationtech.jts.geom.Geometry
import org.oscim.core.GeoPoint
import org.oscim.core.MapPosition
import org.oscim.map.Map
import org.videolan.libvlc.LibVlcUtil
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer

/**
 * 创建Activity全局viewmode
 */

enum class LoadDataStatus {
    /**
     * 加载开始
     */
    LOAD_DATA_STATUS_BEGIN,

    /**
     * 加载结束
     */
    LOAD_DATA_STATUS_FISISH,
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mapController: NIMapController,
    private val traceDataBase: TraceDataBase,
    private val realmOperateHelper: RealmOperateHelper,
    private val networkService: NetworkService,
    private val sharedPreferences: SharedPreferences,
    val roomAppDatabase: RoomAppDatabase
) : ViewModel(), SocketServer.OnConnectSinsListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val TAG = "MainViewModel"

    private var mCameraDialog: CommonDialog? = null

    //路径计算
    val liveDataNaviStatus = MutableLiveData<NaviStatus>()

    //地图点击捕捉到的质检数据ID列表
    val liveDataQsRecordIdList = MutableLiveData<List<String>>()

    //地图点击捕捉到的标签ID列表
    val liveDataNoteId = MutableLiveData<String>()

    //地图点击捕捉到的轨迹列表
    val liveDataNILocationList = MutableLiveData<NiLocation>()

    //加载数据
    val liveDataLoadData = MutableLiveData<LoadDataStatus>()

    //左侧看板数据
    val liveDataSignList = MutableLiveData<List<SignBean>>()

    //顶部看板数据
    val liveDataTopSignList = MutableLiveData<List<SignBean>>()

    //道路名
    val liveDataRoadName = MutableLiveData<RenderEntity?>()

    //捕捉到新增的link
    val liveDataTaskLink = MutableLiveData<String>()

    /**
     * 当前选中的要展示的详细信息的要素
     */
    val liveDataSignMoreInfo = MutableLiveData<SignBean>()

    /**
     * 捕捉到的itemList
     */
    val liveDataItemList = MutableLiveData<List<RenderEntity>>()

    /**
     * 提示信息
     */
    val liveDataMessage = MutableLiveData<String>()


    private var traceTag: String = "TRACE_TAG"

    /**
     * 右上角菜单状态
     */
    val liveDataMenuState = MutableLiveData<Boolean>()

    /**
     * 地图中心坐标
     */
    val liveDataCenterPoint = MutableLiveData<MapPosition>()

    /**
     * 是否自动定位
     */
    val liveDataAutoLocation = MutableLiveData<Boolean>()

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

    var currentTaskBean: TaskBean? = null

    //状态
    val liveIndoorToolsResp: MutableLiveData<IndoorToolsResp> = MutableLiveData()

    //状态
    val liveIndoorToolsCommand: MutableLiveData<IndoorToolsCommand> = MutableLiveData()

    //播报语音流
    private var voiceFlow = MutableSharedFlow<String>()

    /**
     * 是不是线选择模式
     */
    private var bSelectRoad = false

    /**
     * 是不是高亮任务线
     */
    private var bHighRoad = true

    /**
     * 是不是捕捉线
     */
    private var bCatchRoad = false

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

    /**
     * 是不是开启测距
     */
    private var bMeasuringTool = false

    /**
     * 测量类型
     */
    var measuringType: MeasureLayerHandler.MEASURE_TYPE = MeasureLayerHandler.MEASURE_TYPE.DISTANCE

    /**
     * 捕捉到的上一条link
     */
    var linkIdCache = ""

    private var lastNiLocaion: NiLocation? = null

    private var currentIndexNiLocation: Int = 0

    private var socketServer: SocketServer? = null

    var indoorToolsCommand: IndoorToolsCommand? = null

    private var shareUtil: ShareUtil? = null

    private var timer: Timer? = null

    //自动定位
    private var autoLocationTimer: Timer? = null

    private var disAutoLocationTime: Long = 10000

    private var disTime: Long = 1000

    private var currentMapZoomLevel: Int = 0

    //导航轨迹回顾
    private var naviLocationTest = false
    private var naviLocationTestJob: Job? = null

    //导航信息
    private var naviEngine: NaviEngine? = null

//    private var naviEngineNew: NaviEngineNew = NaviEngineNew(realmOperateHelper)

    // 0:不导航 1：导航 2：暂停
    private var naviEngineStatus = 0

    // 定义一个互斥锁
    private val naviMutex = Mutex()
    private var testRealm: Realm? = null;

    init {
        mapController.mMapView.vtmMap.events.bind(Map.UpdateListener { e, mapPosition ->
            when (e) {
                Map.SCALE_EVENT, Map.MOVE_EVENT, Map.ROTATE_EVENT -> liveDataCenterPoint.value =
                    mapPosition
            }

            currentMapZoomLevel = mapController.mMapView.vtmMap.mapPosition.zoomLevel
        })

        currentMapZoomLevel = mapController.mMapView.vtmMap.mapPosition.zoomLevel

        shareUtil = ShareUtil(mapController.mMapView.context, 1)

        //初始化
        realmOperateHelper.niMapController = mapController

        initLocation()
        /**
         * 处理点击道路捕捉回调功能
         */
        mapController.mMapView.addOnNIMapClickListener(TAG,
            //处理地图点击操作
            object : OnGeoPointClickListener {
                override fun onMapClick(tag: String, point: GeoPoint, other: String) {
                    if (tag == TAG) {
                        //数据安装时不允许操作数据
                        if (Constant.INSTALL_DATA) {
                            return
                        }
                        if (bMeasuringTool) {
                            mapController.measureLayerHandler.addPoint(measuringType, point)
                        } else {
                            viewModelScope.launch(Dispatchers.IO) {
                                //线选择状态
                                if (bSelectRoad) {
                                    captureLink(point)
                                } else {
                                    captureItem(point)
                                }
                            }
                        }
                    }
                }
            },
            /**
             * 处理之间数据的点击
             */
            object : OnQsRecordItemClickListener {
                override fun onQsRecordList(tag: String, list: MutableList<String>) {
                    if (tag == TAG) liveDataQsRecordIdList.value = list
                }
            },
            /**
             * 处理新增link线点击编辑
             */
            object : OnTaskLinkItemClickListener {
                override fun onTaskLink(tag: String, taskLinkId: String) {
                    if (tag == TAG) liveDataTaskLink.value = taskLinkId
                }
            },
            /**
             * 处理便签点击
             */
            object : ONNoteItemClickListener {
                override fun onNote(tag: String, noteId: String) {
                    if (tag == TAG) liveDataNoteId.value = noteId
                }

            },
            /**
             * 处理定位点的点击
             */
            object : OnNiLocationItemListener {
                override fun onNiLocation(tag: String, index: Int, it: NiLocation) {
                    if (tag == TAG) liveDataNILocationList.value = it
                }
            })

        viewModelScope.launch(Dispatchers.IO) {
            liveDataLoadData.postValue(LoadDataStatus.LOAD_DATA_STATUS_BEGIN)
            getTaskBean()
            //初始化选中的任务高亮高亮
            if (currentTaskBean != null) {
                mapController.lineHandler.showTaskLines(currentTaskBean!!.hadLinkDvoList)
            }
            initQsRecordData()
            initNoteData()
            initNILocationData()
            liveDataLoadData.postValue(LoadDataStatus.LOAD_DATA_STATUS_FISISH)
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        MapParamUtils.setTaskId(sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1))
        Constant.currentSelectTaskFolder =
            File(Constant.USER_DATA_PATH + "/${MapParamUtils.getTaskId()}")
        Constant.currentSelectTaskConfig =
            RealmConfiguration.Builder().directory(Constant.currentSelectTaskFolder)
                .name("OMQS.realm").encryptionKey(Constant.PASSWORD)
//                .assetFile("${Constant.currentSelectTaskFolder}/OMQS.realm")
//                .readOnly()
//                .allowQueriesOnUiThread(true)
                .schemaVersion(2).build()
        MapParamUtils.setTaskConfig(Constant.currentSelectTaskConfig)
        socketServer = SocketServer(mapController, traceDataBase, sharedPreferences)
//模拟定位，取屏幕中心点
//        viewModelScope.launch(Dispatchers.IO) {
//
//            naviTestFlow().collect { point ->
//                if (naviEngineStatus == 1) {
//                    naviMutex.lock()
//                    naviEngine?.bindingRoute(null, point)
//                    naviMutex.unlock()
//                }
//            }
//        }
        viewModelScope.launch(Dispatchers.Main) {
            voiceFlow.collect {
                speakMode?.speakText(it)
            }
        }
    }


//    fun naviTestFlow(): Flow<GeoPoint> = flow {
//
//        while (true) {
//            emit(mapController.mMapView.vtmMap.mapPosition.geoPoint)
//            delay(1000)
//        }
//    }

    /**
     * 获取当前任务
     */
    private suspend fun getTaskBean() {
        val id = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)
        val realm = realmOperateHelper.getRealmDefaultInstance()
        val res = realm.where(TaskBean::class.java).equalTo("id", id).findFirst()
        if (res != null) {
            currentTaskBean = realm.copyFromRealm(res)
            Log.e("jingo", "获取任务 状态 ${currentTaskBean!!.status}")
        }
        realm.close()
    }

    /**
     * 规划路径
     */
    fun planningPath() {
        viewModelScope.launch(Dispatchers.Default) {
            naviMutex.lock()
            getTaskBean()
            if (currentTaskBean != null && currentTaskBean!!.status == FileManager.Companion.FileDownloadStatus.DONE) {
                if (currentTaskBean!!.navInfo == null) {
                    liveDataMessage.postValue("还没有设置路径的起终点，请先设置")
                    naviMutex.unlock()
                    return@launch
                } else {
                    currentTaskBean!!.navInfo?.let {
                        if (it.naviStartLinkId.isEmpty() || it.naviStartNode.isEmpty()) {
                            liveDataMessage.postValue("还没有设置路径的起点，请先设置")
                            naviMutex.unlock()
                            return@launch
                        } else if (it.naviEndLinkId.isEmpty() || it.naviEndNode.isEmpty()) {
                            liveDataMessage.postValue("还没有设置路径的终点，请先设置")
                            naviMutex.unlock()
                            return@launch
                        }
                    }
                }
                val naviOption = NaviOption(
                    deviationCount = sharedPreferences.getInt(
                        Constant.NAVI_DEVIATION_COUNT,
                        3
                    ),
                    deviationDistance = sharedPreferences.getInt(
                        Constant.NAVI_DEVIATION_DISTANCE,
                        15
                    ),
                    farthestDisplayDistance = sharedPreferences.getInt(
                        Constant.NAVI_FARTHEST_DISPLAY_DISTANCE,
                        500
                    )
                )
                naviEngine = NaviEngine(niMapController = mapController,
                    realmOperateHelper = realmOperateHelper,
                    naviOption = naviOption,
                    callback = object : OnNaviEngineCallbackListener {

                        override suspend fun planningPathStatus(code: NaviStatus, message: String, linkId: String?, geometry: String?) {
                            Log.e("jingo", "路径计算 ${currentTaskBean!!.id} $code $message $linkId,$geometry")
                            when (code) {
                                NaviStatus.NAVI_STATUS_PATH_PLANNING -> naviEngineStatus = 0
                                NaviStatus.NAVI_STATUS_PATH_SUCCESS -> naviEngineStatus = 1
                                NaviStatus.NAVI_STATUS_DISTANCE_OFF -> {}
                                NaviStatus.NAVI_STATUS_DIRECTION_OFF -> {}
                                NaviStatus.NAVI_STATUS_DATA_ERROR, NaviStatus.NAVI_STATUS_PATH_ERROR_BLOCKED, NaviStatus.NAVI_STATUS_NO_START_OR_END -> {
                                    naviEngineStatus = 0
                                    liveDataMessage.postValue("$message:$linkId")
                                }
                            }
                            liveDataNaviStatus.postValue(code)
                            if (geometry != null) {
                                viewModelScope.launch(Dispatchers.Main) {

                                    val lineString = GeometryTools.createGeometry(geometry)
                                    val envelope = lineString.envelopeInternal
                                    mapController.animationHandler.animateToBox(
                                        envelope.maxX,
                                        envelope.maxY,
                                        envelope.minX,
                                        envelope.minY
                                    )
                                    mapController.lineHandler.showLine(geometry)
                                }
                            }
                        }


                        override suspend fun bindingResults(
                            route: NaviRoute?,
                            list: List<NaviRouteItem>
                        ) {
                            val signList = mutableListOf<SignBean>()
                            for (naviRouteItem in list) {
                                val signBean = SignUtil.createSignBean(
                                    viewModelScope,
                                    roomAppDatabase,
                                    naviRouteItem.data
                                )
                                signBean.distance = naviRouteItem.distance
                                signList.add(signBean)
                            }
                            if (route != null) {
                                liveDataRoadName.postValue(route.name)
                                captureTopSign(route)
                            }
                            liveDataSignList.postValue(signList)
                        }

                        override suspend fun voicePlay(text: String): Boolean {
                            speakMode?.let {
                                if (it.isSpeaking()) {
                                    return false
                                } else {
                                    withContext(Dispatchers.Main) {
                                        it.speakText(text)
                                    }
                                    return true
                                }
                            }
                            return false
                        }
                    })
                naviEngine!!.planningPath(currentTaskBean!!)
            } else {
                liveDataMessage.postValue("请先安装任务数据")
            }
            naviMutex.unlock()
        }
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            Constant.SELECT_TASK_ID -> {
                viewModelScope.launch(Dispatchers.IO) {
                    naviMutex.lock()
                    naviEngineStatus = 0
                    getTaskBean()
                    initQsRecordData()
                    naviMutex.unlock()
                }
            }
            Constant.NAVI_DEVIATION_COUNT,
            Constant.NAVI_FARTHEST_DISPLAY_DISTANCE,
            Constant.NAVI_DEVIATION_DISTANCE -> {
                if (naviEngine != null) {
                    val naviOption = NaviOption(
                        deviationCount = sharedPreferences.getInt(
                            Constant.NAVI_DEVIATION_COUNT,
                            3
                        ),
                        deviationDistance = sharedPreferences.getInt(
                            Constant.NAVI_DEVIATION_DISTANCE,
                            15
                        ),
                        farthestDisplayDistance = sharedPreferences.getInt(
                            Constant.NAVI_FARTHEST_DISPLAY_DISTANCE,
                            500
                        )
                    )
                    naviEngine!!.naviOption = naviOption
                }
            }
        }
    }

    /**
     * 初始化渲染质检数据
     */
    private suspend fun initQsRecordData() {
        if (currentTaskBean != null) {
            var list = mutableListOf<QsRecordBean>()
            val realm = realmOperateHelper.getRealmDefaultInstance()
            val id = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)
            val objects = realm.where(QsRecordBean::class.java).equalTo("taskId", id).findAll()
            list = realm.copyFromRealm(objects)
            realm.close()
            mapController.markerHandle.removeAllQsMarker()
            for (item in list) {
                mapController.markerHandle.addOrUpdateQsRecordMark(item)
            }
        }
    }

    /**
     * 初始化渲染便签数据
     */
    private suspend fun initNoteData() {
        var list = mutableListOf<NoteBean>()
        val realm = realmOperateHelper.getRealmDefaultInstance()
        realm.executeTransaction {
            val objects = realmOperateHelper.getRealmTools(NoteBean::class.java).findAll()
            list = realm.copyFromRealm(objects)
        }

        realm.close()

        for (item in list) {
            mapController.markerHandle.addOrUpdateNoteMark(item)
        }
    }

    private suspend fun initNILocationData() {
        //加载轨迹数据
        val id = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)
        val list: List<NiLocation>? = TraceDataBase.getDatabase(
            mapController.mMapView.context, Constant.USER_DATA_PATH
        ).niLocationDao.findToTaskIdAll(id.toString())
        if (list != null) {
            for (location in list) {
                Constant.TRACE_COUNT++

                if (Constant.TRACE_COUNT % Constant.TRACE_COUNT_MORE_TIME == 0) {
                    mapController.markerHandle.addNiLocationMarkerItemRough(location)
                    Log.e("qj", "${Constant.TRACE_COUNT}===轨迹")
                }

                if (Constant.TRACE_COUNT % Constant.TRACE_COUNT_TIME == 0) {
                    mapController.markerHandle.addNiLocationMarkerItemSimple(location)
                    Log.e("qj", "${Constant.TRACE_COUNT}===轨迹")
                }

                mapController.markerHandle.addNiLocationMarkerItem(location)

            }
        }
    }

    /**
     * 初始化定位信息
     */
    private fun initLocation() {
        val gson = Gson();

        //用于定位点存储到数据库
        viewModelScope.launch(Dispatchers.Default) {
            //用于定位点捕捉道路
            mapController.locationLayerHandler.niLocationFlow.collect { location ->

                //过滤掉无效点
                if (!naviLocationTest && !GeometryTools.isCheckError(
                        location.longitude,
                        location.latitude
                    )
                ) {
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

                    location.taskId =
                        sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1).toString()

                    //判断如果是连接状态并处于录像模式，标记为有效点
                    if (shareUtil?.connectstate == true && shareUtil?.takeCameraMode == 0) {
                        location.media = 1
                    }
                    var disance = 0.0
                    //增加间距判断
                    if (lastNiLocaion != null) {
                        disance = GeometryTools.getDistance(
                            location.latitude,
                            location.longitude,
                            lastNiLocaion!!.latitude,
                            lastNiLocaion!!.longitude
                        )
                    }
                    //室内整理工具时不能进行轨迹存储，判断轨迹间隔要超过6并小于60米
                    if (Constant.INDOOR_IP.isEmpty() && (disance == 0.0 || (disance > 6.0 && disance < 60))) {
//                        Log.e("jingo", "轨迹插入开始")
                        CMLog.writeLogtoFile(MainViewModel::class.java.name, "insertTrace", "开始")
                        traceDataBase.niLocationDao.insert(location)
                        mapController.markerHandle.addNiLocationMarkerItem(location)

                        if (Constant.TRACE_COUNT % Constant.TRACE_COUNT_TIME == 0) {
                            mapController.markerHandle.addNiLocationMarkerItemSimple(location)
                        }
                        if (Constant.TRACE_COUNT % Constant.TRACE_COUNT_MORE_TIME == 0) {
                            mapController.markerHandle.addNiLocationMarkerItemRough(location)
                        }
                        mapController.mMapView.vtmMap.updateMap(true)
                        lastNiLocaion = location
                        CMLog.writeLogtoFile(MainViewModel::class.java.name, "insertTrace", gson.toJson(location))
//                        Log.e("jingo", "轨迹插入结束")
                    }
                }
            }

        }

        /**
         * 导航预警信息
         */
        viewModelScope.launch(Dispatchers.Default) {
            //用于定位点捕捉道路
            mapController.locationLayerHandler.niLocationFlow.collectLatest { location ->

                if (!isSelectRoad() && !GeometryTools.isCheckError(
                        location.longitude, location.latitude
                    )
                ) {
                    if (naviEngine != null && naviEngineStatus == 1) {
                        naviMutex.lock()
                        val point = GeoPoint(location.latitude, location.longitude)
                        naviEngine!!.bindingRoute(location, point)
                        naviMutex.unlock()
                    } else {
                        captureLink(
                            GeoPoint(
                                location.latitude, location.longitude
                            )
                        )
                    }
                }
            }
        }
        //显示轨迹图层
        mapController.layerManagerHandler.showNiLocationLayer()
    }

    /**
     * 捕捉要素数据
     */
    private suspend fun captureItem(point: GeoPoint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            var buffer = 8.0
            if (mapController.mMapView.mapLevel >= 18) {
                buffer = 2.0
            }
            val itemList = realmOperateHelper.queryElement(
                GeometryTools.createPoint(
                    point.longitude, point.latitude
                ),
                buffer = buffer, catchAll = false,
            )
            //增加道路线过滤原则
            val filterResult = itemList.filter {
                if (isHighRoad()) {
                    mapController.mMapView.mapLevel >= it.zoomMin && mapController.mMapView.mapLevel <= it.zoomMax
                } else {
                    //关闭时过滤道路线捕捉s
                    mapController.mMapView.mapLevel >= it.zoomMin && mapController.mMapView.mapLevel <= it.zoomMax && it.code != DataCodeEnum.OMDB_RD_LINK.code
                }
            }.toList()
            if (filterResult.size == 1) {
                val bean = SignUtil.createSignBean(viewModelScope, roomAppDatabase, filterResult[0])
                liveDataSignMoreInfo.postValue(bean)
            } else {
                liveDataItemList.postValue(filterResult)
            }
        }
    }

    /**
     * 获取道路属性
     */
    private suspend fun captureTopSign(route: NaviRoute) {
        try {
            captureLinkState = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //看板数据
                val signList = mutableListOf<SignBean>()
                val topSignList = mutableListOf<SignBean>()
                mapController.lineHandler.linksLayer.clear()
                if (linkIdCache != route.linkId) {
                    val realm = realmOperateHelper.getSelectTaskRealmInstance()
                    mapController.lineHandler.showLine(route.pointList)
                    val elementList = realmOperateHelper.queryLinkByLinkPid(realm, route.linkId)
                    for (element in elementList) {

                        when (element.code) {
                            DataCodeEnum.OMDB_MULTI_DIGITIZED.code,//上下线分离
                            DataCodeEnum.OMDB_CON_ACCESS.code,//全封闭
                            -> {
                                val signBean = SignUtil.createSignBean(
                                    viewModelScope,
                                    roomAppDatabase,
                                    element
                                )
                                if (signBean.iconText != "") {
                                    topSignList.add(
                                        signBean
                                    )
                                }
                            }
                            DataCodeEnum.OMDB_PHY_LANENUM.code,//物理车道数
                            DataCodeEnum.OMDB_LANE_NUM.code, //车道数
                            DataCodeEnum.OMDB_RD_LINK_KIND.code,//种别，
                            DataCodeEnum.OMDB_RD_LINK_FUNCTION_CLASS.code, // 功能等级,
                            DataCodeEnum.OMDB_LINK_SPEEDLIMIT.code, //线限速,
                            DataCodeEnum.OMDB_LINK_DIRECT.code,//道路方向,
                            DataCodeEnum.OMDB_RAMP.code, //匝道
                            DataCodeEnum.OMDB_RAMP_1.code,
                            DataCodeEnum.OMDB_RAMP_2.code,
                            DataCodeEnum.OMDB_RAMP_3.code,
                            DataCodeEnum.OMDB_RAMP_4.code,
                            DataCodeEnum.OMDB_RAMP_5.code,
                            DataCodeEnum.OMDB_RAMP_6.code,
                            DataCodeEnum.OMDB_RAMP_7.code,
                            DataCodeEnum.OMDB_BRIDGE.code,//桥
                            DataCodeEnum.OMDB_BRIDGE_1.code,
                            DataCodeEnum.OMDB_BRIDGE_2.code,
                            DataCodeEnum.OMDB_TUNNEL.code,//隧道
                            DataCodeEnum.OMDB_ROUNDABOUT.code,//环岛
                            DataCodeEnum.OMDB_LINK_ATTRIBUTE_MAIN_SIDE_ACCESS.code,//出入口
                            DataCodeEnum.OMDB_LINK_ATTRIBUTE_FORNTAGE.code,//辅路
                            DataCodeEnum.OMDB_LINK_ATTRIBUTE_SA.code,//SA
                            DataCodeEnum.OMDB_LINK_ATTRIBUTE_PA.code,//PA
                            DataCodeEnum.OMDB_LINK_FORM1_1.code,
                            DataCodeEnum.OMDB_LINK_FORM1_2.code,
                            DataCodeEnum.OMDB_LINK_FORM1_3.code,
                            DataCodeEnum.OMDB_LINK_FORM2_1.code,
                            DataCodeEnum.OMDB_LINK_FORM2_2.code,
                            DataCodeEnum.OMDB_LINK_FORM2_3.code,
                            DataCodeEnum.OMDB_LINK_FORM2_4.code,
                            DataCodeEnum.OMDB_LINK_FORM2_5.code,
                            DataCodeEnum.OMDB_LINK_FORM2_6.code,
                            DataCodeEnum.OMDB_LINK_FORM2_7.code,
                            DataCodeEnum.OMDB_LINK_FORM2_8.code,
                            DataCodeEnum.OMDB_LINK_FORM2_9.code,
                            DataCodeEnum.OMDB_LINK_FORM2_10.code,
                            DataCodeEnum.OMDB_LINK_FORM2_11.code,
                            DataCodeEnum.OMDB_LINK_FORM2_12.code,
                            DataCodeEnum.OMDB_LINK_FORM2_13.code,
                            DataCodeEnum.OMDB_VIADUCT.code,
                            -> {
                                val signBean = SignUtil.createSignBean(
                                    viewModelScope,
                                    roomAppDatabase,
                                    element
                                )
                                topSignList.add(
                                    signBean
                                )
                            }
                        }
                    }

                    liveDataTopSignList.postValue(topSignList.distinctBy { it.name }
                        .sortedBy { it.index })

//                    val speechText = SignUtil.getRoadSpeechText(topSignList)
//                    withContext(Dispatchers.Main) {
//                        speakMode?.speakText(speechText)
//                    }
                    linkIdCache = route.linkId ?: ""
                    realm.close()
                }
            }
        } catch (e: Exception) {

        }
    }

    /**
     * 捕获道路和面板
     */
    private suspend fun captureLink(point: GeoPoint) {

        if (captureLinkState || Constant.INSTALL_DATA) {
            return
        }

        try {
            captureLinkState = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val realm = realmOperateHelper.getSelectTaskRealmInstance()
                val linkList = realmOperateHelper.queryLink(realm, point = point)

                var hisRoadName = false

                if (linkList.isNotEmpty()) {
                    val link = linkList[0]

                    val linkId = link.linkPid
                    //看板数据
                    val signList = mutableListOf<SignBean>()
                    val topSignList = mutableListOf<SignBean>()
                    mapController.lineHandler.linksLayer.clear()
                    if (linkIdCache != linkId) {
                        if (bSelectRoad)
                            mapController.markerHandle.addMarker(point, "selectLink")
                        mapController.lineHandler.showLine(link.geometry)

                        val linePoints = GeometryTools.getGeoPoints(link.geometry)
                        val direct = link.properties["direct"]
                        if (direct == "3") {
                            linePoints.reverse()
                        }

                        val footAndDistance = GeometryTools.pointToLineDistance(
                            point,
                            GeometryTools.createLineString(linePoints)
                        )
                        linePoints.add(
                            footAndDistance.footIndex + 1,
                            GeoPoint(
                                footAndDistance.getCoordinate(0).y,
                                footAndDistance.getCoordinate(0).x
                            )
                        )

                        val newLineString = GeometryTools.createLineString(linePoints)
                        if (linkId.isNotEmpty()) {
                            val time = System.currentTimeMillis()
                            val elementList = realmOperateHelper.queryLinkByLinkPid(realm, linkId)
                            Log.e(
                                "jingo",
                                "捕捉到数据 $linkId ${elementList.size} 个 ${System.currentTimeMillis() - time}"
                            )
                            for (element in elementList) {
                                if (element.code == DataCodeEnum.OMDB_LINK_NAME.code) {
                                    hisRoadName = true
                                    liveDataRoadName.postValue(element)
                                    continue
                                }
                                val signBean = SignUtil.createSignBean(
                                    viewModelScope,
                                    roomAppDatabase,
                                    element
                                )
                                signBean.distance = SignUtil.getDistance(
                                    footAndDistance,
                                    newLineString,
                                    element
                                )
                                Log.e(
                                    "jingo",
                                    "捕捉到的数据code $linkId ${DataCodeEnum.findTableNameByCode(element.code)}"
                                )
                                when (element.code) {
                                    DataCodeEnum.OMDB_MULTI_DIGITIZED.code,//上下线分离
                                    DataCodeEnum.OMDB_CON_ACCESS.code,//全封闭
                                    -> {
                                        if (signBean.iconText != "") {
                                            topSignList.add(
                                                signBean
                                            )
                                        }
                                    }
                                    DataCodeEnum.OMDB_PHY_LANENUM.code,//物理车道数
                                    DataCodeEnum.OMDB_LANE_NUM.code, //车道数
                                    DataCodeEnum.OMDB_RD_LINK_KIND.code,//种别，
                                    DataCodeEnum.OMDB_RD_LINK_FUNCTION_CLASS.code, // 功能等级,
                                    DataCodeEnum.OMDB_LINK_SPEEDLIMIT.code, //线限速,
                                    DataCodeEnum.OMDB_LINK_SPEEDLIMIT_COND.code,//条件线限速
                                    DataCodeEnum.OMDB_LINK_DIRECT.code,//道路方向,
                                    DataCodeEnum.OMDB_RAMP.code, //匝道
                                    DataCodeEnum.OMDB_RAMP_1.code,
                                    DataCodeEnum.OMDB_RAMP_2.code,
                                    DataCodeEnum.OMDB_RAMP_3.code,
                                    DataCodeEnum.OMDB_RAMP_4.code,
                                    DataCodeEnum.OMDB_RAMP_5.code,
                                    DataCodeEnum.OMDB_RAMP_6.code,
                                    DataCodeEnum.OMDB_RAMP_7.code,
                                    DataCodeEnum.OMDB_BRIDGE.code,//桥
                                    DataCodeEnum.OMDB_BRIDGE_1.code,//桥
                                    DataCodeEnum.OMDB_BRIDGE_2.code,//桥

                                    DataCodeEnum.OMDB_TUNNEL.code,//隧道
                                    DataCodeEnum.OMDB_ROUNDABOUT.code,//环岛
                                    DataCodeEnum.OMDB_LINK_ATTRIBUTE_MAIN_SIDE_ACCESS.code,//出入口
                                    DataCodeEnum.OMDB_LINK_ATTRIBUTE_FORNTAGE.code,//辅路
                                    DataCodeEnum.OMDB_LINK_ATTRIBUTE_SA.code,//SA
                                    DataCodeEnum.OMDB_LINK_ATTRIBUTE_PA.code,//PA
                                    DataCodeEnum.OMDB_LINK_FORM1_1.code,
                                    DataCodeEnum.OMDB_LINK_FORM1_2.code,
                                    DataCodeEnum.OMDB_LINK_FORM1_3.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_1.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_2.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_3.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_4.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_5.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_6.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_7.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_8.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_9.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_10.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_11.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_12.code,
                                    DataCodeEnum.OMDB_LINK_FORM2_13.code,
                                    DataCodeEnum.OMDB_VIADUCT.code,
                                    -> topSignList.add(
                                        signBean
                                    )
                                    DataCodeEnum.OMDB_SPEEDLIMIT.code,//常规点限速
                                    DataCodeEnum.OMDB_SPEEDLIMIT_COND.code,//条件点限速
                                    DataCodeEnum.OMDB_SPEEDLIMIT_VAR.code,//可变点限速
                                    DataCodeEnum.OMDB_ELECTRONICEYE.code,//电子眼
                                    DataCodeEnum.OMDB_TRAFFICLIGHT.code,//交通灯
                                    DataCodeEnum.OMDB_LANEINFO.code,//车信
                                    DataCodeEnum.OMDB_WARNINGSIGN.code,//危险信息
                                    DataCodeEnum.OMDB_TOLLGATE.code,//收费站
                                    -> {
                                        signList.add(
                                            signBean
                                        )
                                    }
                                }

                            }

//                            val realm = realmOperateHelper.getSelectTaskRealmInstance()

                            val entityList = realmOperateHelper.getSelectTaskRealmTools(
                                realm, RenderEntity::class.java, true
                            ).equalTo("table", DataCodeEnum.OMDB_RESTRICTION.name)
                                .equalTo(
                                    "linkPid", linkId
                                ).findAll()
                            if (entityList.isNotEmpty()) {
                                val outList = entityList.distinct()
                                for (i in outList.indices) {
                                    val outLink = outList[i].properties["linkOut"]
                                    val linkOutEntity =
                                        realmOperateHelper.getSelectTaskRealmTools(
                                            realm, RenderEntity::class.java,
                                            true
                                        )
                                            .equalTo("table", DataCodeEnum.OMDB_RD_LINK_KIND.name)
                                            .equalTo(
                                                "linkPid",
                                                outLink
                                            ).findFirst()
                                    if (linkOutEntity != null) {
                                        mapController.lineHandler.linksLayer.addLine(
                                            linkOutEntity.geometry, 0x7DFF0000
                                        )
                                    }
                                }
                                mapController.lineHandler.linksLayer.addLine(
                                    link.geometry, Color.BLUE
                                )
                            }

                        }

                        liveDataTopSignList.postValue(topSignList.distinctBy { it.name }
                            .sortedBy { it.index })

                        liveDataSignList.postValue(signList.sortedBy { it.distance })
//                        val speechText = SignUtil.getRoadSpeechText(topSignList)
//                        withContext(Dispatchers.Main) {
//                            speakMode?.speakText(speechText)
//                        }
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
                realm.close()
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
        val mapPosition: MapPosition = mapController.mMapView.vtmMap.getMapPosition()
        mapPosition.setBearing(0f) // 锁定角度，自动将地图旋转到正北方向
        mapController.mMapView.vtmMap.mapPosition = mapPosition
        mapController.locationLayerHandler.animateToCurrentPosition()
//        naviEngineStatus = 1
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
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        mapController.mMapView.removeOnNIMapClickListener(TAG)
        mapController.lineHandler.removeLine()
    }

    //点击相机按钮
    fun onClickCameraButton(context: Context) {

        Log.e("qj", LibVlcUtil.hasCompatibleCPU(context).toString())

        initCameraDialog(context)

        mCameraDialog!!.openCamear(mCameraDialog!!.getmShareUtil().continusTakePhotoState)
        mCameraDialog!!.show()
        mCameraDialog!!.setOnDismissListener(DialogInterface.OnDismissListener {
            mCameraDialog!!.hideLoading()
            mCameraDialog!!.stopVideo()
            try {
                if (!mCameraDialog!!.getmShareUtil().connectstate) {
                    mCameraDialog!!.updateCameraResources(
                        1, mCameraDialog!!.getmDeviceNum()
                    )
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

    private fun initCameraDialog(context: Context) {
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
        if (layerConfigList != null && layerConfigList.isNotEmpty()) {
            val omdbVisibleList = mutableListOf<String>()
            layerConfigList.forEach {
                omdbVisibleList.addAll(it.tableMap.filter { entry ->
                    val tableInfo = entry.value
                    !tableInfo.checked
                }.map { entry ->
                    val tableInfo = entry.value
                    tableInfo.table
                }.toList())
            }
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
        mapController.markerHandle.removeMarker("selectLink")
        if (bSelectRoad && naviEngineStatus == 1) {
            naviEngineStatus = 2
        } else if (naviEngineStatus == 2) {
            naviEngineStatus = 1
        }
    }

    /**
     * 是否开启了线选择
     */
    fun isSelectRoad(): Boolean {
        return bSelectRoad
    }

    /**
     * 开启线高亮
     */
    fun setHighRoad(select: Boolean) {
        bHighRoad = select

    }

    /**
     * 开启捕捉线
     */
    fun setCatchRoad(select: Boolean) {
        bCatchRoad = select
        Constant.MapCatchLine = bCatchRoad
    }

    /**
     * 是否开启线高亮
     */
    fun isHighRoad(): Boolean {
        return bHighRoad
    }

    /**
     * 是否开启捕捉线
     */
    fun isCatchRoad(): Boolean {
        return bCatchRoad
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
        viewModelScope.launch(Dispatchers.IO) {
            liveDataSignMoreInfo.postValue(
                SignUtil.createSignBean(
                    viewModelScope,
                    roomAppDatabase,
                    data
                )
            )
        }

        if (data.wkt != null) {
            mapController.markerHandle.removeMarker("moreInfo")
            mapController.lineHandler.removeLine()
            when (data.wkt!!.geometryType) {
                Geometry.TYPENAME_POINT -> {
                    val geoPoint = GeometryTools.createGeoPoint(data.wkt!!.toText())
                    mapController.markerHandle.addMarker(geoPoint, "moreInfo")
                }

                Geometry.TYPENAME_LINESTRING -> {
                    mapController.lineHandler.showLine(data.wkt!!.toText())
                }
            }
        }
    }

    fun sendServerCommand(
        context: Context, traceVideoBean: TraceVideoBean, indoorToolsCommand: IndoorToolsCommand
    ) {

        if (TextUtils.isEmpty(Constant.INDOOR_IP)) {
            Toast.makeText(context, "获取ip失败！", Toast.LENGTH_LONG).show()
            return
        }

        this.indoorToolsCommand = indoorToolsCommand

        viewModelScope.launch(Dispatchers.Default) {
            val url = "http://${Constant.INDOOR_IP}:8080/sensor/service/${traceVideoBean.command}?"

            when (val result = networkService.sendServerCommand(
                url = url, traceVideoBean = traceVideoBean
            )) {
                is NetResult.Success<*> -> {

                    if (result.data != null) {
                        try {

                            val defaultUserResponse = result.data as QRCodeBean

                            if (defaultUserResponse.errcode == 0) {

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context, "命令成功。", Toast.LENGTH_LONG
                                    ).show()

                                    liveIndoorToolsResp.postValue(IndoorToolsResp.QR_CODE_STATUS_UPDATE_VIDEO_INFO_SUCCESS)

                                    //启动双向控制服务

                                    //启动双向控制服务
                                    if (socketServer != null && socketServer!!.isServerClose) {
                                        socketServer!!.connect(
                                            Constant.INDOOR_IP, this@MainViewModel
                                        )
                                    }

                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "命令无效${defaultUserResponse.errmsg}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                liveIndoorToolsResp.postValue(IndoorToolsResp.QR_CODE_STATUS_UPDATE_VIDEO_INFO_FAILURE)
                            }

                        } catch (e: IOException) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context, "${e.message}", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                is NetResult.Error<*> -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context, "${result.exception.message}", Toast.LENGTH_SHORT
                        ).show()
                    }
                    liveIndoorToolsResp.postValue(IndoorToolsResp.QR_CODE_STATUS_UPDATE_VIDEO_INFO_FAILURE)
                }

                is NetResult.Failure<*> -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context, "${result.code}:${result.msg}", Toast.LENGTH_SHORT
                        ).show()
                    }
                    liveIndoorToolsResp.postValue(IndoorToolsResp.QR_CODE_STATUS_UPDATE_VIDEO_INFO_FAILURE)
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
                        niLocation.latitude, niLocation.longitude
                    ), traceTag, "", niLocation as java.lang.Object
                )
            }
        }
    }

    /**
     * 显示索引位置
     * @param niLocation 轨迹点
     */
    fun setCurrentIndexNiLocation(niLocation: NiLocation) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.e("qj", "开始$currentIndexNiLocation")
            currentIndexNiLocation = mapController.markerHandle.getNILocationIndex(niLocation)!!
            Log.e("qj", "结束$currentIndexNiLocation")
        }
    }

    /**
     * 设置索引位置
     * @param index 索引
     */
    fun setCurrentIndexLoction(index: Int) {
        currentIndexNiLocation = index
    }

    /**
     *
     * @return index 索引
     */
    fun getCurrentNiLocationIndex(): Int {
        return currentIndexNiLocation
    }

    override fun onConnect(success: Boolean) {
        if (!success && socketServer != null) {
            BaseToast.makeText(
                mapController.mMapView.context, "轨迹反向控制服务失败，请确认连接是否正常！", Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onIndexing() {
        //切换为暂停状态
        liveIndoorToolsCommand.postValue(IndoorToolsCommand.INDEXING)
    }

    override fun onStop() {
        liveIndoorToolsCommand.postValue(IndoorToolsCommand.STOP)
    }

    override fun onPlay() {
        liveIndoorToolsCommand.postValue(IndoorToolsCommand.PLAY)
    }

    override fun onParseEnd() {

    }

    override fun onReceiveLocation(mNiLocation: NiLocation?) {
        if (mNiLocation != null) {
            setCurrentIndexNiLocation(mNiLocation)
            showMarker(mapController.mMapView.context, mNiLocation)
            Log.e("qj", "反向控制$currentIndexNiLocation")
        } else {
            BaseToast.makeText(
                mapController.mMapView.context, "没有找到对应轨迹点！", Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun isAutoCamera(): Boolean {

        return shareUtil?.connectstate == true
    }

    fun autoCamera() {
        if (shareUtil?.connectstate == true) {
            val hostBean1 = HostBean()
            hostBean1.ipAddress = shareUtil!!.takeCameraIP
            hostBean1.hardwareAddress = shareUtil!!.takeCameraMac
            onClickCameraButton(mapController.mMapView.context)
            mCameraDialog?.connection(hostBean1)
        }
    }

    fun startTimer() {
        if (timer != null) {
            cancelTrace()
        }
        timer = fixedRateTimer("", false, disTime, disTime) {
            if (currentIndexNiLocation < mapController.markerHandle.getNILocationItemizedLayerSize()) {
                Log.e("qj", "定时器")
                val niLocation = mapController.markerHandle.getNILocation(currentIndexNiLocation)
                val nextNiLocation =
                    mapController.markerHandle.getNILocation(currentIndexNiLocation + 1)
                if (nextNiLocation != null && niLocation != null) {
                    var niLocationDisTime =
                        nextNiLocation.timeStamp.toLong() - niLocation.timeStamp.toLong()
                    disTime = if (niLocationDisTime < 1000) {
                        1000
                    } else {
                        niLocationDisTime
                    }
                    showMarker(mapController.mMapView.context, nextNiLocation)
                    currentIndexNiLocation += 1
                    //再次启动
                    startTimer()
                }
            } else {
                Toast.makeText(mapController.mMapView.context, "无数据了！", Toast.LENGTH_LONG).show()
                cancelTrace()
            }
        }
    }

    /**
     * 结束自动播放
     */
    fun cancelTrace() {
        timer?.cancel()
    }


    /**
     * 开启自动定位
     */
    fun startAutoLocationTimer() {
        if (autoLocationTimer != null) {
            cancelAutoLocation()
        }
        autoLocationTimer = fixedRateTimer("", false, disAutoLocationTime, disAutoLocationTime) {
            liveDataAutoLocation.postValue(true)
            Log.e("qj", "自动定位开始执行")
            startAutoLocationTimer()
        }
    }

    /**
     * 结束自动定位
     */
    fun cancelAutoLocation() {
        autoLocationTimer?.cancel()
    }

    /**
     *  开启测量工具
     */
    fun setMeasuringToolEnable(b: Boolean) {
        bMeasuringTool = b
        mapController.measureLayerHandler.clear()
    }

    /**
     * 测量打点
     */
    fun addPointForMeasuringTool() {
        mapController.measureLayerHandler.addPoint(measuringType)
    }

    /**
     * 测距回退点
     */
    fun backPointForMeasuringTool() {
        mapController.measureLayerHandler.backspacePoint()
    }

    /**
     * 重绘
     */
    fun resetMeasuringTool() {
        mapController.measureLayerHandler.clear()
    }

    /**
     * 设置测量类型 0：距离 2：面积 3：角度
     */
    fun setMeasuringToolType(type: MeasureLayerHandler.MEASURE_TYPE) {
        if (measuringType != type) {
            measuringType = type
            mapController.measureLayerHandler.clear()
        }
    }

    fun click2Dor3D() {
    }

    /**
     * 搜索接口
     * @param searchEnum 枚举类
     * @param msg 搜索内容
     */
    fun search(searchEnum: SearchEnum, msg: String, dialog: DialogInterface) {
        if (searchEnum != null && msg.isNotEmpty() && dialog != null) {
            when (searchEnum) {
                SearchEnum.LINK -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        Log.e("jingo", "查询link $msg")
                        val link = realmOperateHelper.queryLink(linkPid = msg)
                        if (link != null) {
                            Log.e("jingo", "查询link ${link.geometry}")
                            val lineString = GeometryTools.createGeometry(link.geometry)
                            val envelope = lineString.envelopeInternal
                            withContext(Dispatchers.Main) {
                                mapController.animationHandler.animateToBox(
                                    envelope.maxX,
                                    envelope.maxY,
                                    envelope.minX,
                                    envelope.minY
                                )
                                mapController.lineHandler.showLine(link.geometry)
                                dialog.dismiss()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    mapController.mMapView.context, "未查询到数据", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                SearchEnum.MARK -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        val qsRecordBean = realmOperateHelper.queryQcRecordBean(markId = msg)
                        if (qsRecordBean != null) {
                            val naviController =
                                (mapController.mMapView.context as Activity).findNavController(R.id.main_activity_right_fragment)
                            val bundle = Bundle()
                            bundle.putString("QsId", qsRecordBean.id)
                            naviController.navigate(R.id.EvaluationResultFragment, bundle)
                            ToastUtils.showLong(qsRecordBean.classType)
                            dialog.dismiss()
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    mapController.mMapView.context, "未查询到数据", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                SearchEnum.LOCATION -> {
                    val parts = msg.split("[,，\\s]".toRegex())
                    if (parts.size == 2) {
                        val x = parts[0].toDouble()
                        val y = parts[1].toDouble()
                        mapController.animationHandler.animationByLatLon(y, x)
                        mapController.markerHandle.addMarker(GeoPoint(y, x), "location")
                        dialog.dismiss()
                    } else {
                        Toast.makeText(
                            mapController.mMapView.context, "输入格式不正确", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    fun clearMarker() {
        mapController.markerHandle.removeMarker()
    }

    /**
     * 导航测试
     */
    fun setNaviLocationTestStartTime(time: Long) {
        naviLocationTest = true
        if (naviLocationTestJob != null && naviLocationTestJob!!.isActive)
            naviLocationTestJob!!.cancel()
        naviLocationTestJob = viewModelScope.launch(Dispatchers.IO) {
            var b = true
            val limitCount = 20
            var lastTime: Long = time
            while (b) {
                Log.e("jingo", "下一组定位点起始时间 $lastTime")
                val list = traceDataBase.niLocationDao.findListWithStartTime(lastTime, limitCount)

                for (location in list) {
                    if (!naviLocationTest)
                        break
                    val nowTime = location.timeStamp.toLong()
                    if (lastTime != 0L) {
                        val tempTime = nowTime - lastTime
                        if (tempTime > 10000) {
                            liveDataMessage.postValue("下个定位点与当前定位点时间间隔超过10秒(${tempTime})，将直接跳转到下个点")
                            delay(2000)
                        } else {
                            delay(tempTime)
                        }
                    }
                    lastTime = nowTime

                    withContext(Dispatchers.Main) {
                        mapController.animationHandler.animationByLatLon(
                            location.latitude,
                            location.longitude
                        )
                    }

                    mapController.locationLayerHandler.niLocationFlow.emit(location)
                }
                if (list.size < limitCount) {
                    b = false
                }
            }
        }
    }

    /**
     * 停止测试
     */
    fun stopNaviLocationTest() {
        naviLocationTest = false
        if (naviLocationTestJob != null) {
            naviLocationTestJob!!.cancel()
        }
    }

}

