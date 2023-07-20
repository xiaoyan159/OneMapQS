package com.navinfo.omqs.ui.activity.map

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.navinfo.collect.library.data.entity.NiLocation
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.bean.TraceVideoBean
import com.navinfo.omqs.databinding.ActivityMainBinding
import com.navinfo.omqs.http.offlinemapdownload.OfflineMapDownloadManager
import com.navinfo.omqs.tools.LayerConfigUtils
import com.navinfo.omqs.ui.activity.BaseActivity
import com.navinfo.omqs.ui.fragment.console.ConsoleFragment
import com.navinfo.omqs.ui.fragment.offlinemap.OfflineMapFragment
import com.navinfo.omqs.ui.fragment.qsrecordlist.QsRecordListFragment
import com.navinfo.omqs.ui.fragment.signMoreInfo.SignMoreInfoFragment
import com.navinfo.omqs.ui.fragment.tasklist.TaskManagerFragment
import com.navinfo.omqs.ui.other.BaseToast
import com.navinfo.omqs.ui.widget.RecyclerViewSpacesItemDecoration
import com.navinfo.omqs.util.FlowEventBus
import com.navinfo.omqs.util.SpeakMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import org.oscim.renderer.GLViewport
import org.videolan.vlc.Util
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * 地图主页面
 */
@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<MainViewModel>()

    /**
     * 左侧fragment
     */
    private var leftFragment: Fragment? = null


    /**
     * 是否开启右侧面板
     */
    private var switchFragment = false

    /**
     * 检测是否含有tts插件
     */
    private val someActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
            } else {
            }
        }

    //注入地图控制器
    @Inject
    lateinit var mapController: NIMapController

    @Inject
    lateinit var offlineMapDownloadManager: OfflineMapDownloadManager

    private val rightController by lazy {
        findNavController(R.id.main_activity_right_fragment)
    }

    /**
     * 提前显示要素看板
     */
    private val signAdapter by lazy {
        SignAdapter(object : OnSignAdapterClickListener {
            //点击看板进去问题反馈面板
            override fun onItemClick(signBean: SignBean) {
                rightController.currentDestination?.let {
                    if (it.id == R.id.RightEmptyFragment) {
                        val bundle = Bundle()
                        bundle.putParcelable("SignBean", signBean)
                        bundle.putBoolean("AutoSave", false)
                        rightController.navigate(R.id.EvaluationResultFragment, bundle)
                    }
                }
            }

            //点击详细信息
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onMoreInfoClick(selectTag: String, tag: String, signBean: SignBean) {
                viewModel.showSignMoreInfo(signBean.renderEntity)
                val fragment =
                    supportFragmentManager.findFragmentById(R.id.main_activity_sign_more_info_fragment)
                if (fragment == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_activity_sign_more_info_fragment, SignMoreInfoFragment())
                        .commit()
                }
            }

            override fun onErrorClick(signBean: SignBean) {
                rightController.currentDestination?.let {
                    if (it.id == R.id.RightEmptyFragment) {
                        val bundle = Bundle()
                        bundle.putParcelable("SignBean", signBean)
                        bundle.putBoolean("AutoSave", true)
                        rightController.navigate(R.id.EvaluationResultFragment, bundle)
                    }
                }
            }
        })
    }

    /**
     * 道路信息看板
     */
    private val topSignAdapter by lazy {
        TopSignAdapter { _, signBean ->
            rightController.currentDestination?.let {
                if (it.id == R.id.RightEmptyFragment) {
                    val bundle = Bundle()
                    bundle.putParcelable("SignBean", signBean)
                    rightController.navigate(R.id.EvaluationResultFragment, bundle)
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val checkIntent = Intent()
        checkIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        someActivityResultLauncher.launch(checkIntent)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        //初始化地图
        mapController.init(
            this,
            binding.mainActivityMap,
            null,
            Constant.MAP_PATH,
            Constant.USER_DATA_PATH + "/trace.sqlite"
        )
        viewModel.speakMode = SpeakMode(this)
        // 在mapController初始化前获取当前OMDB图层显隐
        viewModel.refreshOMDBLayer(LayerConfigUtils.getLayerConfigList())
        mapController.mMapView.vtmMap.viewport().maxZoomLevel = 25
        //关联生命周期
        binding.lifecycleOwner = this
        //给xml转递对象
        binding.mainActivity = this
        //给xml传递viewModel对象
        binding.viewModel = viewModel

        binding.mainActivityVoice.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    voiceOnTouchStart()//Do Something
                }

                MotionEvent.ACTION_UP -> {
                    voiceOnTouchStop()//Do Something
                }
            }
            v?.onTouchEvent(event) ?: true
        }
        //捕捉列表变化回调
        viewModel.liveDataQsRecordIdList.observe(this) {
            //跳转到质检数据页面
            //获取右侧fragment容器
            val naviController = findNavController(R.id.main_activity_right_fragment)

            naviController.currentDestination?.let { navDestination ->
                when (navDestination.id) {
                    R.id.RightEmptyFragment -> {
                        if (it.size == 1) {
                            val bundle = Bundle()
                            bundle.putString("QsId", it[0])
                            naviController.navigate(R.id.EvaluationResultFragment, bundle)
                        }
                    }
                }
            }
        }
        //捕捉列表变化回调
        viewModel.liveDataNoteId.observe(this) {
            //跳转到质检数据页面
            //获取右侧fragment容器
            val naviController = findNavController(R.id.main_activity_right_fragment)

            naviController.currentDestination?.let { navDestination ->
                when (navDestination.id) {
                    R.id.RightEmptyFragment -> {
                        val bundle = Bundle()
                        bundle.putString("NoteId", it)
                        naviController.navigate(R.id.NoteFragment, bundle)
                    }
                }
            }
        }

        viewModel.liveDataTaskLink.observe(this) {
            val bundle = Bundle()
            bundle.putString("TaskLinkId", it)
            findNavController(R.id.main_activity_right_fragment).navigate(
                R.id.TaskLinkFragment,
                bundle
            )
        }

        //捕捉轨迹点
        viewModel.liveDataNILocationList.observe(this) {
            if (viewModel.isSelectTrace()) {
                //Toast.makeText(this,"轨迹被点击了",Toast.LENGTH_LONG).show()
                viewModel.showMarker(this, it)
                viewModel.setCurrentIndexNiLocation(it)
                val traceVideoBean = TraceVideoBean(
                    command = "videotime?",
                    userid = Constant.USER_ID,
                    time = "${it.time}:000"
                )
                viewModel.sendServerCommand(this, traceVideoBean, IndoorToolsCommand.SELECT_POINT)
            }
        }

        //右上角菜单是否被点击
        viewModel.liveDataMenuState.observe(this) {
            binding.mainActivityMenu.isSelected = it
            if (it == true) {
                binding.mainActivityMenuGroup.visibility = View.VISIBLE
            } else {
                binding.mainActivityMenuGroup.visibility = View.INVISIBLE
            }
        }
        //道路绑定，名称变化
        viewModel.liveDataRoadName.observe(this) {
            if (it != null) {
                binding.mainActivityRoadName.text = it.properties["name"]
                if (binding.mainActivityRoadName.visibility != View.VISIBLE) binding.mainActivityRoadName.visibility =
                    View.VISIBLE
            } else {
                if (binding.mainActivityRoadName.visibility != View.GONE) binding.mainActivityRoadName.visibility =
                    View.GONE
            }
        }

        //道路属性面板
        binding.mainActivityTopSignRecyclerview.layoutManager = LinearLayoutManager(
            this, RecyclerView.HORIZONTAL, false
        )
//        binding.mainActivityTopSignRecyclerview.addItemDecoration(
//            RecycleViewDivider(this, LinearLayoutManager.HORIZONTAL)
//        )
        binding.mainActivityTopSignRecyclerview.adapter = topSignAdapter


        //提前显示面板
        binding.mainActivitySignRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.mainActivitySignRecyclerview.adapter = signAdapter
        //增加4dp的间隔
        binding.mainActivitySignRecyclerview.addItemDecoration(
            RecyclerViewSpacesItemDecoration(
                Util.convertDpToPx(
                    this, 4
                )
            )
        )
        //监听要素面板变化
        viewModel.liveDataSignList.observe(this) {
            signAdapter.refreshData(it)
        }
        //监听道路信息变化
        viewModel.liveDataTopSignList.observe(this) {
            topSignAdapter.refreshData(it)
        }

        //监听地图中点变化
        viewModel.liveDataCenterPoint.observe(this) {
            try {
                if (it != null && it.longitude != null && it.latitude != null) {
                    binding.mainActivityGeometry.text = "经纬度:${
                        BigDecimal(it.longitude).setScale(
                            7, RoundingMode.HALF_UP
                        )
                    },${BigDecimal(it.latitude).setScale(7, RoundingMode.HALF_UP)}"
                }
            } catch (e: Exception) {
                Log.e("qj", "异常 $e")
            }
        }

        viewModel.liveDataSignMoreInfo.observe(this) {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.main_activity_sign_more_info_fragment)
            if (fragment == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_activity_sign_more_info_fragment, SignMoreInfoFragment())
                    .commit()
            }
        }

        viewModel.liveIndoorToolsResp.observe(this) {
            when (it) {
                IndoorToolsResp.QR_CODE_STATUS_UPDATE_VIDEO_INFO_SUCCESS -> {

                    if (viewModel.indoorToolsCommand == IndoorToolsCommand.SELECT_POINT) {
                        selectPointFinish(true)
                    }

                }

                IndoorToolsResp.QR_CODE_STATUS_UPDATE_VIDEO_INFO_FAILURE -> {
                    if (viewModel.indoorToolsCommand == IndoorToolsCommand.SELECT_POINT) {
                        selectPointFinish(false)
                    }
                }
            }
        }

        //室内整理工具反向控制
        viewModel.liveIndoorToolsCommand.observe(this) {
            when (it) {
                IndoorToolsCommand.PLAY -> {
                    setPlayStatus()
                }

                IndoorToolsCommand.INDEXING -> {
                    pausePlayTrace()
                }

                IndoorToolsCommand.SELECT_POINT -> {

                }

                IndoorToolsCommand.NEXT -> {
                }

                IndoorToolsCommand.REWIND -> {
                }

                IndoorToolsCommand.STOP -> {
                    //切换为暂停状态
                    pausePlayTrace()
                }
            }
        }

        lifecycleScope.launch {
            // 初始化地图图层控制接收器
            FlowEventBus.subscribe<List<ImportConfig>>(
                lifecycle, Constant.EVENT_LAYER_MANAGER_CHANGE
            ) {
                viewModel.refreshOMDBLayer(it)
            }
        }

        findNavController(R.id.main_activity_right_fragment).addOnDestinationChangedListener { _, destination, arguments ->
            if (destination.id == R.id.RightEmptyFragment) {
                binding.mainActivityRightVisibilityButtonsGroup.visibility = View.VISIBLE
            } else {
                binding.mainActivityRightVisibilityButtonsGroup.visibility = View.GONE
                viewModel.setSelectRoad(false)
                binding.mainActivitySelectLine.isSelected = false
            }
        }

        //自动连接相机
        if (viewModel.isAutoCamera()) {
            viewModel.autoCamera()
        } else {
            supportFragmentManager.beginTransaction()
                .add(R.id.console_fragment_layout, ConsoleFragment()).commit()
        }
    }

    //根据输入的经纬度跳转坐标
    fun jumpPosition() {
        val view = this.layoutInflater.inflate(R.layout.dialog_view_edittext, null)
        val inputDialog = MaterialAlertDialogBuilder(
            this
        ).setTitle("坐标定位").setView(view)
        var editText = view.findViewById<EditText>(R.id.dialog_edittext)
        editText.hint = "请输入经纬度例如：\n116.1234567,39.1234567\n116.1234567 39.1234567"
        inputDialog.setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
        }
        inputDialog.setPositiveButton("确定") { dialog, _ ->
            if (editText.text.isNotEmpty()) {
                try {
                    val parts = editText.text.toString().split("[,，\\s]".toRegex())
                    if (parts.size == 2) {
                        val x = parts[0].toDouble()
                        val y = parts[1].toDouble()
                        mapController.animationHandler.animationByLatLon(y, x)
                    } else {
                        Toast.makeText(this, "输入格式不正确", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "输入格式不正确", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }
        inputDialog.show()
    }


    override fun onStart() {
        super.onStart()

        //开启定位
        mapController.locationLayerHandler.startLocation()
        mapController.mMapView.setLogoVisable(View.GONE)
    }

    override fun onPause() {
        super.onPause()
        mapController.mMapView.onPause()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onDestroy() {
        super.onDestroy()
        viewModel.speakMode?.shutdown()
        mapController.mMapView.onDestroy()
        mapController.locationLayerHandler.stopLocation()
    }

    override fun onResume() {
        super.onResume()
        mapController.mMapView.onResume()
    }

    /**
     * 打开个人中菜单
     */
    fun openMenu() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.console_fragment_layout, ConsoleFragment()).commit()
        if (leftFragment != null) {
            supportFragmentManager.beginTransaction().remove(leftFragment!!).commit()
            leftFragment = null
            binding.mainActivityBottomSheetGroup.visibility = View.GONE
            binding.mainActivityLeftFragment.visibility = View.GONE
        }
//        binding.mainActivityDrawer.open()
    }

    /**
     * 打开相机预览
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun openCamera() {
        //显示轨迹图层
        viewModel.onClickCameraButton(this)
    }

    /**
     * 开关菜单
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun onClickMenu() {
        //显示菜单图层
        viewModel.onClickMenu()
    }

    /**
     * 点击计算
     */
    fun onClickCalcDisance() {

    }

    /**
     * 点击轨迹
     */
    fun onClickTrace() {

    }

    /**
     * 点击搜索
     */
    fun onClickSearch() {

    }

    /**
     * 点击2\3D
     */
    fun onClick2DOr3D() {

    }

    /**
     * zoomin
     */
    fun zoomInOnclick(view: View) {
        mapController.animationHandler.zoomIn()
    }

    /**
     * zoomOut
     */
    fun zoomOutOnclick(view: View) {
        mapController.animationHandler.zoomOut()
    }

    /**
     *展开或收起右侧面板
     */
    fun onSwitchFragment() {
        switchFragment = !switchFragment
        binding.mainActivityFragmentSwitch.isSelected = switchFragment
        if (switchFragment) {
            binding.mainActivityFragmentGroup.visibility = View.GONE
        } else {
            binding.mainActivityFragmentGroup.visibility = View.VISIBLE
        }
    }

    /**
     * 准星的显隐控制
     */
    fun setHomeCenterVisibility(visible: Int) {
        binding.mainActivityHomeCenter.visibility = visible
        binding.mainActivityHomeCenterText.visibility = visible
        if (visible != View.VISIBLE) {
            binding.mainActivityHomeCenterText.text = ""
        }
    }

    /**
     * 设置屏幕中心文字内容
     */
    fun setHomeCenterText(str: String) {
        binding.mainActivityHomeCenterText.text = str
    }

    /**
     * 隐藏或显示右侧展开按钮
     */
    fun setRightSwitchButtonVisibility(visibility: Int) {
        binding.mainActivityFragmentSwitch.visibility = visibility
    }

    /**
     * 顶部菜单按钮
     */
    fun setTopMenuButtonVisibility(visibility: Int) {
        binding.mainActivityMenu.visibility = visibility
        if (visibility != View.VISIBLE) {
            binding.mainActivityMenuGroup.visibility = View.INVISIBLE
            binding.mainActivityMenu.isSelected = false
        }
    }

    /**
     * 点击录音按钮
     */
    fun voiceOnclick() {
        val naviController = findNavController(R.id.main_activity_right_fragment)
        naviController.navigate(R.id.EvaluationResultFragment)
    }

    /**
     * 点击线选择
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun selectLineOnclick() {
        viewModel.setSelectRoad(!viewModel.isSelectRoad())
        binding.mainActivitySelectLine.isSelected = viewModel.isSelectRoad()
    }

    /**
     * 点击线选择
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun tracePointsOnclick() {
        viewModel.setSelectTrace(!viewModel.isSelectTrace())
        binding.mainActivityTraceSnapshotPoints.isSelected = viewModel.isSelectTrace()

        if (viewModel.isSelectTrace()) {
            Toast.makeText(this, "请选择轨迹点!", Toast.LENGTH_LONG).show()
            //调用撤销自动播放
            setViewEnable(false)
            viewModel.cancelTrace()
        }
    }

    /**
     * 点击结束轨迹操作
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun finishTraceOnclick() {
        setIndoorGroupEnable(false)
        viewModel.setSelectTrace(false)
        viewModel.setMediaFlag(false)
        viewModel.setSelectPauseTrace(false)
        binding.mainActivityMenuIndoorGroup.visibility = View.GONE
        binding.mainActivityTraceSnapshotPoints.isSelected = viewModel.isSelectTrace()
        binding.mainActivitySnapshotMediaFlag.isSelected = viewModel.isMediaFlag()
        binding.mainActivitySnapshotPause.isSelected = viewModel.isSelectPauseTrace()
    }

    /**
     * 点击结束轨迹操作
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun mediaFlagOnclick() {
        viewModel.setMediaFlag(!viewModel.isMediaFlag())
        binding.mainActivitySnapshotMediaFlag.isSelected = viewModel.isMediaFlag()
    }

    /**
     * 点击上一个轨迹点播放操作
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun rewindTraceOnclick() {
        pausePlayTrace()
        val item =
            mapController.markerHandle.getNILocation(viewModel.getCurrentNiLocationIndex() - 1)
        if (item != null) {
            viewModel.setCurrentIndexLoction(viewModel.getCurrentNiLocationIndex() - 1)
            viewModel.showMarker(this, (item as MarkerItem).uid as NiLocation)
            val traceVideoBean = TraceVideoBean(
                command = "videotime?",
                userid = Constant.USER_ID,
                time = "${(item.uid as NiLocation).time}:000"
            )
            viewModel.sendServerCommand(this, traceVideoBean, IndoorToolsCommand.REWIND)
        } else {
            dealNoData()
        }
    }

    /**
     * 点击暂停播放轨迹操作
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun pauseTraceOnclick() {
        viewModel.setSelectPauseTrace(!viewModel.isSelectPauseTrace())
        binding.mainActivitySnapshotPause.isSelected = viewModel.isSelectPauseTrace()
        viewModel.setSelectTrace(false)
        binding.mainActivityTraceSnapshotPoints.isSelected = viewModel.isSelectTrace()
        if (viewModel.isSelectPauseTrace()) {
            playVideo()
        } else {
            pauseVideo()
            viewModel.cancelTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun playVideo() {
        if (mapController.markerHandle.getCurrentMark() == null) {
            BaseToast.makeText(this, "请先选择轨迹点！", BaseToast.LENGTH_SHORT).show()
            return
        }
        viewModel.setSelectTrace(false)
        binding.mainActivityTraceSnapshotPoints.isSelected = viewModel.isSelectTrace()
        val traceVideoBean = TraceVideoBean(command = "playVideo?", userid = Constant.USER_ID)
        viewModel.sendServerCommand(this, traceVideoBean, IndoorToolsCommand.PLAY)
    }

    /**
     * 设置为播放状态
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun setPlayStatus() {
        //切换为播放
        viewModel.setSelectPauseTrace(true)
        binding.mainActivitySnapshotPause.isSelected = viewModel.isSelectPauseTrace()
        playVideo()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun pauseVideo() {
        val traceVideoBean = TraceVideoBean(command = "pauseVideo?", userid = Constant.USER_ID)
        viewModel.sendServerCommand(this, traceVideoBean, IndoorToolsCommand.STOP)
    }

    /**
     * 点击下一个轨迹点
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun nextTraceOnclick() {
        pausePlayTrace()
        val item =
            mapController.markerHandle.getNILocation(viewModel.getCurrentNiLocationIndex() + 1)
        if (item != null) {
            viewModel.setCurrentIndexLoction(viewModel.getCurrentNiLocationIndex() + 1)
            viewModel.showMarker(this, (item as MarkerItem).uid as NiLocation)
            val traceVideoBean = TraceVideoBean(
                command = "videotime?",
                userid = Constant.USER_ID,
                time = "${(item.uid as NiLocation).time}:000"
            )
            viewModel.sendServerCommand(this, traceVideoBean, IndoorToolsCommand.NEXT)
        } else {
            dealNoData()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun dealNoData() {
        BaseToast.makeText(this, "无数据了！", Toast.LENGTH_SHORT).show()

        //无数据时自动暂停播放，并停止轨迹
        if (viewModel.isSelectPauseTrace()) {
            pauseVideo()
            viewModel.cancelTrace()
            viewModel.setSelectPauseTrace(false)
            binding.mainActivitySnapshotPause.isSelected = viewModel.isSelectPauseTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun pausePlayTrace() {
        viewModel.setSelectTrace(false)
        binding.mainActivityTraceSnapshotPoints.isSelected = viewModel.isSelectTrace()
        viewModel.setSelectPauseTrace(false)
        binding.mainActivitySnapshotPause.isSelected = viewModel.isSelectPauseTrace()
        viewModel.cancelTrace()
    }

    /**
     * 选点结束
     * @param value true 选点成功 false 选点失败
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun selectPointFinish(value: Boolean) {
        if (value) {
            setViewEnable(true)
            viewModel.setSelectPauseTrace(false)
            binding.mainActivitySnapshotPause.isSelected = viewModel.isSelectPauseTrace()
        }
    }

    private fun setViewEnable(value: Boolean) {
        binding.mainActivitySnapshotRewind.isEnabled = value
        binding.mainActivitySnapshotNext.isEnabled = value
        binding.mainActivitySnapshotPause.isEnabled = value
        binding.mainActivitySnapshotFinish.isEnabled = value
    }


    /**
     * 打开或关闭底部导航栏
     */
    fun onSwitchSheet() {
        if (binding.mainActivityBottomSheetGroup.visibility == View.VISIBLE) {
            leftFragment?.let {
                supportFragmentManager.beginTransaction().remove(it).commit()
                leftFragment = null
                binding.mainActivityLeftFragment.visibility = View.GONE
            }

            binding.mainActivityBottomSheetGroup.visibility = View.GONE

            mapController.mMapView.setScaleBarLayer(GLViewport.Position.BOTTOM_CENTER, 128, 5)
        } else {
            binding.mainActivityBottomSheetGroup.visibility = View.VISIBLE
            mapController.mMapView.setScaleBarLayer(GLViewport.Position.BOTTOM_CENTER, 128, 65)
        }
        mapController.mMapView.vtmMap.animator()
            .animateTo(
                GeoPoint(
                    mapController.mMapView.vtmMap.mapPosition.geoPoint.latitude,
                    mapController.mMapView.vtmMap.mapPosition.geoPoint.longitude
                )
            )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun voiceOnTouchStart() {
        viewModel.startSoundMetter(this, binding.mainActivityVoice)
    }

    private fun voiceOnTouchStop() {
        if (Constant.IS_VIDEO_SPEED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                viewModel.stopSoundMeter()
            }
        }
    }

    /**
     * 打开测评任务面板
     */
    fun onClickTaskFragment() {
        if (leftFragment !is TaskManagerFragment) {
            if (leftFragment == null) {
                binding.mainActivityBottomSheetGroup.visibility = View.VISIBLE
                binding.mainActivityLeftFragment.visibility = View.VISIBLE
            }
            leftFragment = TaskManagerFragment {
                binding.mainActivityLeftFragment.visibility = View.GONE
                supportFragmentManager.beginTransaction().remove(leftFragment!!).commit()
                leftFragment = null
                null
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_activity_left_fragment, leftFragment!!).commit()
        }
    }

    /**
     * 打开测评结果面板
     */
    fun onClickResFragment() {
        if (leftFragment !is QsRecordListFragment) {
            if (leftFragment == null) {
                binding.mainActivityBottomSheetGroup.visibility = View.VISIBLE
                binding.mainActivityLeftFragment.visibility = View.VISIBLE
            }
            leftFragment = QsRecordListFragment {
                binding.mainActivityLeftFragment.visibility = View.GONE
                supportFragmentManager.beginTransaction().remove(leftFragment!!).commit()
                leftFragment = null
                null
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_activity_left_fragment, leftFragment!!).commit()
        }
    }

    /**
     * 显示轨迹回放布局
     */
    fun showIndoorDataLayout() {
        binding.mainActivityMenuIndoorGroup.visibility = View.VISIBLE
        if (Constant.INDOOR_IP.isNotEmpty()) {
            setIndoorGroupEnable(true)
        } else {
            setIndoorGroupEnable(false)
        }
    }

    private fun setIndoorGroupEnable(enable: Boolean) {
        binding.mainActivitySnapshotFinish.isEnabled = enable
        binding.mainActivityTraceSnapshotPoints.isEnabled = enable
        //binding.mainActivitySnapshotMediaFlag.isEnabled = enable
        binding.mainActivitySnapshotRewind.isEnabled = enable
        binding.mainActivitySnapshotPause.isEnabled = enable
        binding.mainActivitySnapshotNext.isEnabled = enable
    }

    /**
     * 路径规划
     */
    fun onClickRouteFragment() {
        Toast.makeText(this, "功能开发中", Toast.LENGTH_SHORT).show()
    }

    /**
     * 打开离线地图
     */
    fun onClickOfflineMapFragment() {
        if (leftFragment !is OfflineMapFragment) {
            if (leftFragment == null) {
                binding.mainActivityBottomSheetGroup.visibility = View.VISIBLE
                binding.mainActivityLeftFragment.visibility = View.VISIBLE
            }
            leftFragment = OfflineMapFragment {
                binding.mainActivityLeftFragment.visibility = View.GONE
                supportFragmentManager.beginTransaction().remove(leftFragment!!).commit()
                leftFragment = null
                null
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_activity_left_fragment, leftFragment!!).commit()
        }
    }

    /**
     * 打开道路名称属性看板，选择的道路在viewmodel里记录，不用
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun openRoadNameFragment() {
        if (viewModel.liveDataRoadName.value != null) {
            viewModel.showSignMoreInfo(viewModel.liveDataRoadName.value!!)
        }
    }

    /**
     * 新增便签,打开便签fragment
     */
    fun onClickNewNote() {
        rightController.navigate(R.id.NoteFragment)
    }

    /**
     * 新增评测link
     */
    fun onClickTaskLink() {
        rightController.navigate(R.id.TaskLinkFragment)
    }

    /**
     * 右侧按钮+经纬度按钮
     */
    fun setRightButtonsVisible(visible: Int) {
        binding.mainActivityRightVisibilityButtonsGroup2.visibility = visible
    }
}