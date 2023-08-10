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
import com.blankj.utilcode.util.ViewUtils.runOnUiThread
import com.navinfo.collect.library.data.dao.impl.TraceDataBase
import com.navinfo.collect.library.data.entity.*
import com.navinfo.collect.library.garminvirbxe.HostBean
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.OnGeoPointClickListener
import com.navinfo.collect.library.map.handler.ONNoteItemClickListener
import com.navinfo.collect.library.map.handler.OnNiLocationItemListener
import com.navinfo.collect.library.map.handler.OnQsRecordItemClickListener
import com.navinfo.collect.library.map.handler.OnTaskLinkItemClickListener
import com.navinfo.collect.library.utils.GeometryTools
import com.navinfo.collect.library.utils.GeometryToolsKt
import com.navinfo.collect.library.utils.RealmDBParamUtils
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.bean.QRCodeBean
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.bean.TraceVideoBean
import com.navinfo.omqs.db.RealmOperateHelper
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.http.NetworkService
import com.navinfo.omqs.ui.dialog.CommonDialog
import com.navinfo.omqs.ui.manager.TakePhotoManager
import com.navinfo.omqs.ui.other.BaseToast
import com.navinfo.omqs.ui.widget.SignUtil
import com.navinfo.omqs.util.DateTimeUtil
import com.navinfo.omqs.util.ShareUtil
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
import org.locationtech.jts.geom.Point
import org.oscim.core.GeoPoint
import org.oscim.core.MapPosition
import org.oscim.layers.marker.MarkerItem
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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mapController: NIMapController,
    private val traceDataBase: TraceDataBase,
    private val realmOperateHelper: RealmOperateHelper,
    private val networkService: NetworkService,
    private val sharedPreferences: SharedPreferences
) : ViewModel(), SocketServer.OnConnectSinsListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val TAG = "MainViewModel"

    private var mCameraDialog: CommonDialog? = null

    //地图点击捕捉到的质检数据ID列表
    val liveDataQsRecordIdList = MutableLiveData<List<String>>()

    //地图点击捕捉到的标签ID列表
    val liveDataNoteId = MutableLiveData<String>()

    //地图点击捕捉到的轨迹列表
    val liveDataNILocationList = MutableLiveData<NiLocation>()

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
    val liveDataSignMoreInfo = MutableLiveData<RenderEntity>()

    private var traceTag: String = "TRACE_TAG"

    /**
     * 右上角菜单状态
     */
    val liveDataMenuState = MutableLiveData<Boolean>()

    /**
     * 地图中心坐标
     */
    val liveDataCenterPoint = MutableLiveData<MapPosition>()

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

    var linkIdCache = ""

    private var lastNiLocaion: NiLocation? = null

    private var currentIndexNiLocation: Int = 0

    private var socketServer: SocketServer? = null

    var indoorToolsCommand: IndoorToolsCommand? = null

    private var shareUtil: ShareUtil? = null

    private var timer: Timer? = null

    private var disTime: Long = 1000

    private var currentMapZoomLevel: Int = 0

    init {

        mapController.mMapView.vtmMap.events.bind(Map.UpdateListener { e, mapPosition ->
            when (e) {
                Map.SCALE_EVENT, Map.MOVE_EVENT, Map.ROTATE_EVENT -> liveDataCenterPoint.value =
                    mapPosition
            }
            if (mapController.mMapView.vtmMap.mapPosition.zoomLevel >= 16) {

            }
            currentMapZoomLevel = mapController.mMapView.vtmMap.mapPosition.zoomLevel
        })

        currentMapZoomLevel = mapController.mMapView.vtmMap.mapPosition.zoomLevel

        shareUtil = ShareUtil(mapController.mMapView.context, 1)

        initLocation()
        /**
         * 处理点击道路捕捉回调功能
         */
        mapController.mMapView.addOnNIMapClickListener(
            TAG,
            //处理地图点击操作
            object : OnGeoPointClickListener {
                override fun onMapClick(tag: String, point: GeoPoint) {
                    if (tag == TAG) {
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
            },
            /**
             * 处理之间数据的点击
             */
            object : OnQsRecordItemClickListener {
                override fun onQsRecordList(tag: String, list: MutableList<String>) {
                    if (tag == TAG)
                        liveDataQsRecordIdList.value = list
                }
            },
            /**
             * 处理新增link线点击编辑
             */
            object : OnTaskLinkItemClickListener {
                override fun onTaskLink(tag: String, taskLinkId: String) {
                    if (tag == TAG)
                        liveDataTaskLink.value = taskLinkId
                }
            },
            /**
             * 处理便签点击
             */
            object : ONNoteItemClickListener {
                override fun onNote(tag: String, noteId: String) {
                    if (tag == TAG)
                        liveDataNoteId.value = noteId
                }

            },
            /**
             * 处理定位点的点击
             */
            object : OnNiLocationItemListener {
                override fun onNiLocation(tag: String, index: Int, it: NiLocation) {
                    if (tag == TAG)
                        liveDataNILocationList.value = it
                }
            }
        )

        viewModelScope.launch(Dispatchers.IO) {
            getTaskBean()
            //初始化选中的任务高亮高亮
            if (currentTaskBean != null) {
                mapController.lineHandler.showTaskLines(currentTaskBean!!.hadLinkDvoList)
            }
            initQsRecordData()
            initNoteData()
            initNILocationData()
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        RealmDBParamUtils.setTaskId(sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1))
        socketServer = SocketServer(mapController, traceDataBase, sharedPreferences)
    }


    /**
     * 获取当前任务
     */
    private suspend fun getTaskBean() {
        val id = sharedPreferences.getInt(Constant.SELECT_TASK_ID, -1)
        val realm = Realm.getDefaultInstance()
        val res = realm.where(TaskBean::class.java).equalTo("id", id).findFirst()
        if (res != null) {
            currentTaskBean = realm.copyFromRealm(res)
        }
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == Constant.SELECT_TASK_ID) {
            viewModelScope.launch(Dispatchers.IO) {
                getTaskBean()
                initQsRecordData()
            }
        }
    }

    /**
     * 初始化渲染质检数据
     */
    private suspend fun initQsRecordData() {
        if (currentTaskBean != null) {
            var list = mutableListOf<QsRecordBean>()
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction {
                val objects =
                    realm.where<QsRecordBean>().equalTo("taskId", currentTaskBean!!.id).findAll()
                list = realm.copyFromRealm(objects)
            }
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
            mapController.mMapView.context, Constant.USER_DATA_PATH
        ).niLocationDao.findToTaskIdAll(id.toString())
        if (list != null) {
            for (location in list) {
                mapController.markerHandle.addNiLocationMarkerItem(location)
            }
        }
    }

    /**
     * 初始化定位信息
     */
    private fun initLocation() {

        //用于定位点存储到数据库
        viewModelScope.launch(Dispatchers.Default) {
            //用于定位点捕捉道路
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
                            location.latitude, location.longitude,
                            lastNiLocaion!!.latitude, lastNiLocaion!!.longitude
                        )
                    }
                    //室内整理工具时不能进行轨迹存储，判断轨迹间隔要超过2.5并小于60米
                    if (Constant.INDOOR_IP.isEmpty() && (disance == 0.0 || (disance > 2.5 && disance < 60))) {
                        traceDataBase.niLocationDao.insert(location)
                        mapController.markerHandle.addNiLocationMarkerItem(location)
                        mapController.mMapView.vtmMap.updateMap(true)
                        lastNiLocaion = location
                    }
                }
            }

        }
        viewModelScope.launch(Dispatchers.Default) {
            //用于定位点捕捉道路
            mapController.locationLayerHandler.niLocationFlow.collectLatest { location ->
                if (!isSelectRoad() && !GeometryTools.isCheckError(
                        location.longitude, location.latitude
                    )
                ) {
                    captureLink(
                        GeoPoint(
                            location.latitude, location.longitude
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    if (Constant.AUTO_LOCATION) {
                        mapController.mMapView.vtmMap.animator()
                            .animateTo(GeoPoint(location.longitude, location.latitude))
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
            val itemList = realmOperateHelper.queryElement(
                GeometryTools.createPoint(
                    point.longitude,
                    point.latitude
                )
            )

            if (itemList.size > 0) {
                liveDataSignMoreInfo.postValue(itemList[0])
            }
        }
    }

    /**
     * 捕获道路和面板
     */
    private suspend fun captureLink(point: GeoPoint) {
        if (captureLinkState) {
            return
        }

        try {
            captureLinkState = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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
        mapController.markerHandle.removeMarker("location")
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

    fun sendServerCommand(
        context: Context,
        traceVideoBean: TraceVideoBean,
        indoorToolsCommand: IndoorToolsCommand
    ) {

        if (TextUtils.isEmpty(Constant.INDOOR_IP)) {
            Toast.makeText(context, "获取ip失败！", Toast.LENGTH_LONG).show()
            return
        }

        this.indoorToolsCommand = indoorToolsCommand

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

                                    liveIndoorToolsResp.postValue(IndoorToolsResp.QR_CODE_STATUS_UPDATE_VIDEO_INFO_SUCCESS)

                                    //启动双向控制服务

                                    //启动双向控制服务
                                    if (socketServer != null && socketServer!!.isServerClose) {
                                        socketServer!!.connect(
                                            Constant.INDOOR_IP,
                                            this@MainViewModel
                                        )
                                    }

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
                                liveIndoorToolsResp.postValue(IndoorToolsResp.QR_CODE_STATUS_UPDATE_VIDEO_INFO_FAILURE)
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
                    liveIndoorToolsResp.postValue(IndoorToolsResp.QR_CODE_STATUS_UPDATE_VIDEO_INFO_FAILURE)
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
                        niLocation.latitude,
                        niLocation.longitude
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
                mapController.mMapView.context,
                "轨迹反向控制服务失败，请确认连接是否正常！",
                Toast.LENGTH_SHORT
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
                mapController.mMapView.context,
                "没有找到对应轨迹点！",
                Toast.LENGTH_SHORT
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
                    var nilocationDisTime =
                        nextNiLocation.timeStamp.toLong() - niLocation.timeStamp.toLong()
                    disTime = if (nilocationDisTime < 1000) {
                        1000
                    } else {
                        nilocationDisTime
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

    fun click2Dor3D(){
        viewModelScope.launch(Dispatchers.IO) {
            Log.e(
                "qj",
                "${
                    Realm.getDefaultInstance().where(RenderEntity::class.java).findAll().size
                }==安装数量"
            )
        }
    }

    /**
     * 搜索接口
     * @param searchEnum 枚举类
     * @param msg 搜索内容
     */
     fun search(searchEnum: SearchEnum,msg:String,dialog:DialogInterface){
        if(searchEnum!=null&&msg.isNotEmpty()&&dialog!=null){
            when (searchEnum) {
                SearchEnum.LINK -> {
                     viewModelScope.launch(Dispatchers.IO) {
                         val link = realmOperateHelper.queryLink(linkPid = msg)
                         if(link!=null){
                             link?.let { l ->
                                 mapController.lineHandler.showLine(l.geometry)
                                 dialog.dismiss()
                             }
                         }else{
                             withContext(Dispatchers.Main){
                                 Toast.makeText(mapController.mMapView.context, "未查询到数据", Toast.LENGTH_SHORT).show()
                             }
                         }
                     }
                }
                SearchEnum.MARK -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        val qsRecordBean = realmOperateHelper.queryQcRecordBean(markId = msg)
                        if(qsRecordBean!=null){
                            qsRecordBean?.let { l ->
                                val naviController = (mapController.mMapView.context as Activity).findNavController(R.id.main_activity_right_fragment)
                                val bundle = Bundle()
                                bundle.putString("QsId", l.id)
                                naviController.navigate(R.id.EvaluationResultFragment, bundle)
                                ToastUtils.showLong(l.classType)
                                dialog.dismiss()
                            }
                        }else{
                            withContext(Dispatchers.Main){
                                Toast.makeText(mapController.mMapView.context, "未查询到数据", Toast.LENGTH_SHORT).show()
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
                        mapController.markerHandle.addMarker(GeoPoint(y,x),"location")
                        dialog.dismiss()
                    } else {
                        Toast.makeText(mapController.mMapView.context, "输入格式不正确", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

