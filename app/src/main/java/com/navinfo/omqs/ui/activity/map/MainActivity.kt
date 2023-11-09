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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ClipboardUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.handler.MeasureLayerHandler
import com.navinfo.collect.library.utils.DeflaterUtil
import com.navinfo.collect.library.utils.GeometryTools
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
import com.navinfo.omqs.ui.fragment.itemlist.ItemListFragment
import com.navinfo.omqs.ui.fragment.offlinemap.OfflineMapFragment
import com.navinfo.omqs.ui.fragment.qsrecordlist.QsRecordListFragment
import com.navinfo.omqs.ui.fragment.signMoreInfo.SignMoreInfoFragment
import com.navinfo.omqs.ui.fragment.tasklist.TaskManagerFragment
import com.navinfo.omqs.ui.other.BaseToast
import com.navinfo.omqs.ui.widget.RecyclerViewSpacesItemDecoration
import com.navinfo.omqs.util.FlowEventBus
import com.navinfo.omqs.util.NaviStatus
import com.navinfo.omqs.util.SpeakMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.oscim.core.GeoPoint
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
    private val loadingDialog by lazy {
        MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.MaterialAlertDialog_Material3_Animation
        ).setMessage("正在计算路线中...").setCancelable(false).show()
    }

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val checkIntent = Intent()
            checkIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
            someActivityResultLauncher.launch(checkIntent)
        } catch (e: Exception) {
            Log.e("jingo", "检查TTS失败 $e")
        }

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
        mapController.mMapView.vtmMap.viewport().maxZoomLevel =
            com.navinfo.collect.library.system.Constant.MAX_ZOOM
        //关联生命周期
        binding.lifecycleOwner = this
        //给xml转递对象
        binding.mainActivity = this
        //给xml传递viewModel对象
        binding.viewModel = viewModel

        binding.mainActivityGeometry.setOnLongClickListener {
            var text = (it as TextView).text
            text = text.substring(4)
            ClipboardUtils.copyText(text)
            BaseToast.makeText(this, "坐标已复制到剪切板", BaseToast.LENGTH_SHORT).show()
            true
        }

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
                R.id.TaskLinkFragment, bundle
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
                binding.mainActivityRoadName.visibility = View.VISIBLE
                binding.mainActivityRoadName.text = it.properties["name"]
            } else {
                binding.mainActivityRoadName.visibility = View.INVISIBLE
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
                    if (Constant.AUTO_LOCATION) {
                        viewModel.startAutoLocationTimer()
                    }
                    binding.mainActivityLocation.setImageResource(R.drawable.icon_location)
                }
            } catch (e: Exception) {
                Log.e("qj", "异常 $e")
            }
        }
        viewModel.liveDataAutoLocation.observe(this) {
            if (it == true && Constant.INDOOR_IP == null || Constant.INDOOR_IP == "") {
                onClickLocation()
            }
        }
        viewModel.liveDataSignMoreInfo.observe(this) {

            if (!rightController.backQueue.isEmpty()) {
                rightController.navigateUp()
            }

            lifecycleScope.launch {
                delay(100)

                val bundle = Bundle()
                bundle.putParcelable("SignBean", it)
                bundle.putBoolean("AutoSave", false)
                rightController.navigate(R.id.EvaluationResultFragment, bundle)
                val fragment =
                    supportFragmentManager.findFragmentById(R.id.main_activity_sign_more_info_fragment)
                if (fragment == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_activity_sign_more_info_fragment, SignMoreInfoFragment())
                        .commit()
                } else {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.main_activity_sign_more_info_fragment, SignMoreInfoFragment())
                        .commit()
                }
            }
        }

        viewModel.liveIndoorToolsResp.observe(this) {
            when (it) {
                IndoorToolsResp.QR_CODE_STATUS_UPDATE_VIDEO_INFO_SUCCESS -> {

                    if (viewModel.indoorToolsCommand == IndoorToolsCommand.SELECT_POINT) {
                        selectPointFinish(true)
                    }
                    //启动自动播放
                    if (viewModel.indoorToolsCommand == IndoorToolsCommand.PLAY) {
                        viewModel.startTimer()
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

        viewModel.liveDataMessage.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        viewModel.liveDataNaviStatus.observe(this) {
            when (it) {
                NaviStatus.NAVI_STATUS_PATH_ERROR_BLOCKED -> {
                    Toast.makeText(
                        this,
                        "路径不通，请检查",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (loadingDialog.isShowing)
                        loadingDialog.dismiss()
                }

                NaviStatus.NAVI_STATUS_PATH_PLANNING -> {
                    if (!loadingDialog.isShowing)
                        loadingDialog.show()
                }

                NaviStatus.NAVI_STATUS_PATH_ERROR_NODE -> {
                    Toast.makeText(
                        this,
                        "查询link基本信息表失败（node表）",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadingDialog.dismiss()
                }

                NaviStatus.NAVI_STATUS_PATH_ERROR_DIRECTION -> {
                    Toast.makeText(
                        this,
                        "查询link基本信息表失败（方向表）",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadingDialog.dismiss()
                }

                NaviStatus.NAVI_STATUS_PATH_SUCCESS -> {
                    loadingDialog.dismiss()
                }

                NaviStatus.NAVI_STATUS_DISTANCE_OFF -> {
                    Toast.makeText(
                        this,
                        "偏离路线",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                NaviStatus.NAVI_STATUS_DIRECTION_OFF -> {
                }
            }
        }

        viewModel.liveDataItemList.observe(this) {
            if (it.isNotEmpty()) {
                if (leftFragment == null || leftFragment !is ItemListFragment) {
                    leftFragment = ItemListFragment {
                        binding.mainActivityLeftFragment.visibility = View.GONE
                        supportFragmentManager.beginTransaction().remove(leftFragment!!).commit()
                        leftFragment = null
                        null
                    }
                    binding.mainActivityLeftFragment.visibility = View.VISIBLE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_activity_left_fragment, leftFragment!!)
                        .commit()
                } else {
                    supportFragmentManager.beginTransaction()
                        .show(leftFragment!!)
                        .commit()
                }
            }
        }

        viewModel.liveDataLoadData.observe(this) {
            when (it) {
                LoadDataStatus.LOAD_DATA_STATUS_BEGIN -> {
                    showLoadingDialog("正在加载数据...")
                }

                LoadDataStatus.LOAD_DATA_STATUS_FISISH -> {
                    hideLoadingDialog()
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

        findNavController(R.id.main_activity_right_fragment).addOnDestinationChangedListener { _, destination, _ ->
            backSignMoreInfo()
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

        binding.mainActivityCloseLine.isSelected = viewModel.isHighRoad()

        initMeasuringTool()
    }

    /**
     *初始化测量工具栏的点击事件
     */

    private fun initMeasuringTool() {
        val root = binding.mainActivityMeasuringTool.root
        root.findViewById<View>(R.id.measuring_tool_select_point)
            .setOnClickListener(measuringToolClickListener)
        root.findViewById<View>(R.id.measuring_tool_close)
            .setOnClickListener(measuringToolClickListener)
        root.findViewById<View>(R.id.measuring_tool_backspace)
            .setOnClickListener(measuringToolClickListener)
        root.findViewById<View>(R.id.measuring_tool_reset)
            .setOnClickListener(measuringToolClickListener)
        root.findViewById<View>(R.id.measuring_tool_distance)
            .setOnClickListener(measuringToolClickListener)
        root.findViewById<View>(R.id.measuring_tool_area)
            .setOnClickListener(measuringToolClickListener)
        root.findViewById<View>(R.id.measuring_tool_angle)
            .setOnClickListener(measuringToolClickListener)
        root.findViewById<View>(R.id.measuring_tool_value_layout).setOnLongClickListener {
            val value = root.findViewById<TextView>(R.id.measuring_tool_value).text
            val unit = root.findViewById<TextView>(R.id.measuring_tool_value_unit).text
            ClipboardUtils.copyText("$value$unit")
            BaseToast.makeText(this, "测量结果已复制到剪切板", BaseToast.LENGTH_SHORT).show()
            true
        }
    }

    /**
     * 测量工具点击事件
     */
    private val measuringToolClickListener = View.OnClickListener {
        when (it.id) {
            //选点
            R.id.measuring_tool_select_point -> {
                viewModel.addPointForMeasuringTool()
            }
            //关闭
            R.id.measuring_tool_close -> {
                measuringToolOff()
            }
            //上一步
            R.id.measuring_tool_backspace -> {
                viewModel.backPointForMeasuringTool()
            }
            //重绘
            R.id.measuring_tool_reset -> {
                viewModel.resetMeasuringTool()
            }
            //测距
            R.id.measuring_tool_distance -> {
                it.isSelected = true
                viewModel.setMeasuringToolType(MeasureLayerHandler.MEASURE_TYPE.DISTANCE)
                val root = binding.mainActivityMeasuringTool.root
                root.findViewById<View>(R.id.measuring_tool_area).isSelected = false
                root.findViewById<View>(R.id.measuring_tool_angle).isSelected = false
            }
            //测面积
            R.id.measuring_tool_area -> {
                it.isSelected = true
                viewModel.setMeasuringToolType(MeasureLayerHandler.MEASURE_TYPE.AREA)
                val root = binding.mainActivityMeasuringTool.root
                root.findViewById<View>(R.id.measuring_tool_distance).isSelected = false
                root.findViewById<View>(R.id.measuring_tool_angle).isSelected = false
            }
            //测角度
            R.id.measuring_tool_angle -> {
                it.isSelected = true
                viewModel.setMeasuringToolType(MeasureLayerHandler.MEASURE_TYPE.ANGLE)
                val root = binding.mainActivityMeasuringTool.root
                root.findViewById<View>(R.id.measuring_tool_distance).isSelected = false
                root.findViewById<View>(R.id.measuring_tool_area).isSelected = false
            }
        }
    }

    /**
     * 开始测量
     */
    fun measuringToolOn() {
        val root = binding.mainActivityMeasuringTool.root
        val valueView = root.findViewById<TextView>(R.id.measuring_tool_value)
        val unitView = root.findViewById<TextView>(R.id.measuring_tool_value_unit)
        val centerTextView = binding.mainActivityHomeCenterText
        //监听测距值
        mapController.measureLayerHandler.measureValueLiveData.observe(this) {
            valueView.text = it.valueString
            unitView.text = it.unit
            ClipboardUtils.copyText("${it.valueString}${it.unit}")
        }
        mapController.measureLayerHandler.tempMeasureValueLiveData.observe(this)
        {
            centerTextView.text = "${it.valueString}${it.unit}"
        }
        viewModel.setMeasuringToolEnable(true)
        binding.mainActivityHomeCenter.visibility = View.VISIBLE
        binding.mainActivityHomeCenterText.visibility = View.VISIBLE
        viewModel.setMeasuringToolType(MeasureLayerHandler.MEASURE_TYPE.DISTANCE)
        root.visibility = View.VISIBLE
        root.findViewById<View>(R.id.measuring_tool_distance).isSelected = true
        root.findViewById<View>(R.id.measuring_tool_area).isSelected = false
        root.findViewById<View>(R.id.measuring_tool_angle).isSelected = false
    }


    /**
     * 结束测量
     */
    fun measuringToolOff() {
        //监听测距值
        mapController.measureLayerHandler.measureValueLiveData.removeObservers(this)
        mapController.measureLayerHandler.tempMeasureValueLiveData.removeObservers(this)
        viewModel.setMeasuringToolEnable(false)
        binding.mainActivityHomeCenter.visibility = View.GONE
        binding.mainActivityHomeCenterText.visibility = View.GONE
        binding.mainActivityMeasuringTool.root.visibility = View.GONE
    }


    //根据输入的经纬度跳转坐标
    fun jumpPosition() {
        val view = this.layoutInflater.inflate(R.layout.dialog_view_edittext, null)
        val inputDialog = MaterialAlertDialogBuilder(
            this
        ).setTitle("坐标定位").setView(view)
        val editText = view.findViewById<EditText>(R.id.dialog_edittext)
        val tabItemLayout = view.findViewById<TabLayout>(R.id.search_tab_layout)
        editText.hint = "请输入LinkPid例如：12345678"
        var index = 0
        tabItemLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab) {

            }

            override fun onTabUnselected(p0: TabLayout.Tab) {

            }

            override fun onTabSelected(p0: TabLayout.Tab) {
                index = p0.position
                editText.text = null
                //清理已绘制线
                mapController.lineHandler.removeLine()
                mapController.markerHandle.removeMarker("location")
                when (p0.position) {
                    0 -> editText.hint = "请输入LinkPid例如：12345678"
                    1 -> editText.hint = "请输入MarkId例如：123456789"
                    2 -> editText.hint = "请输入经纬度例如：116.1234567,39.1234567"
                }
            }
        })
        inputDialog.setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
        }
        inputDialog.setPositiveButton("确定") { dialog, _ ->
            if (editText.text.isNotEmpty()) {
                try {
                    when (index) {
                        0 -> viewModel.search(SearchEnum.LINK, editText.text.toString(), dialog)
                        1 -> viewModel.search(SearchEnum.MARK, editText.text.toString(), dialog)
                        2 -> viewModel.search(SearchEnum.LOCATION, editText.text.toString(), dialog)
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
            hideMainActivityBottomSheetGroup()
            binding.mainActivityLeftFragment.visibility = View.GONE
        }
//        binding.mainActivityDrawer.open()
    }

    /**
     * 打开相机预览
     */
    fun openCamera() {
        //显示轨迹图层
        viewModel.onClickCameraButton(this)
    }

    /**
     * 开关菜单
     */
    fun onClickMenu() {
        //显示菜单图层
        viewModel.onClickMenu()
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
        jumpPosition()
    }

    /**
     * 点击2\3D
     */
    fun onClick2DOr3D() {
        viewModel.click2Dor3D()
    }

    /**
     * 线捕捉开关
     */
    fun catchLineOnclick(view: View) {
        viewModel.setCatchRoad(!viewModel.isCatchRoad())
        binding.mainActivityMapCatchLine.isSelected = viewModel.isCatchRoad()
    }

    /**
     * zoomin
     */
    fun zoomInOnclick(view: View) {
        //Log.e("qj", "computeline==${GeometryTools.computeLine(175.0/1000000000,"LINESTRING(116.71523523056109 39.92252072202544 16.08, 116.7152446525368 39.922528935578065 16.04, 116.7152492633007 39.92253304221301 16.03, 116.71525397428678 39.922537148987885 16.02, 116.7152585850368 39.92254115561753 16, 116.71526319579864 39.9225452622508 15.99, 116.7152678065473 39.92254926887934 15.99, 116.71527241730776 39.92255337551149 15.97, 116.71527702806752 39.9225574821431 15.96, 116.71528163882658 39.92256158877415 15.94, 116.71528624957249 39.92256559540047 15.92, 116.7152908603302 39.92256970203041 15.91, 116.7152954710872 39.9225738086598 15.9, 116.71529998162082 39.92257791514826 15.88, 116.71530459237646 39.92258202177656 15.87, 116.71530920313141 39.9225861284043 15.86, 116.71531381388567 39.92259023503148 15.85, 116.7153183244166 39.92259434151779 15.84, 116.71532293516951 39.92259844814387 15.82, 116.71532754590925 39.92260245476522 15.82, 116.71533205643817 39.92260656124991 15.8, 116.71533666718902 39.922610667874345 15.79, 116.71534127793919 39.92261477449822 15.78, 116.71534588868865 39.922618881121544 15.77, 116.7153503992149 39.92262298760407 15.76, 116.715355009963 39.92262709422631 15.75, 116.71535962071042 39.922631200847974 15.75, 116.71536423144467 39.9226352074649 15.74, 116.71536884219071 39.922639314085465 15.73, 116.71537335271358 39.922643420565294 15.72, 116.71537796345827 39.92264752718475 15.72, 116.71538257420224 39.92265163380365 15.72, 116.71538718494553 39.92265574042201 15.71, 116.71539179567566 39.92265974703562 15.71, 116.71539640641758 39.92266385365286 15.7, 116.7154010171588 39.92266796026954 15.7, 116.71540562789932 39.92267206688567 15.7, 116.71541023862667 39.92267607349705 15.69, 116.71541484936583 39.922680180112074 15.68, 116.7154194601043 39.922684286726536 15.68, 116.71542407084206 39.92268839334044 15.67, 116.71542868157914 39.92269249995378 15.66, 116.71543339252533 39.922696506702415 15.66, 116.715438003261 39.92270061331464 15.65, 116.71544261399599 39.92270471992631 15.64, 116.7154472247178 39.92270872653324 15.64, 116.71545193567363 39.92271283328377 15.63, 116.71545654640653 39.92271693989375 15.62, 116.71546115712626 39.922720946499005 15.62, 116.71546586806747 39.92272495324363 15.61, 116.71547047879827 39.92272905985193 15.6, 116.71547518973806 39.92273306659542 15.59, 116.71547990067711 39.92273707333831 15.59, 116.71548461161544 39.922741080080634 15.58, 116.71548922233096 39.9227450866825 15.58, 116.71549393326787 39.92274909342366 15.57, 116.71549854398198 39.9227531000244 15.57, 116.71550325491746 39.922757106764415 15.56, 116.71550796583975 39.922761013499674 15.55, 116.71551267677377 39.92276502023853 15.55, 116.71551738770708 39.9227690269768 15.54, 116.71552209863965 39.9227730337145 15.54, 116.71552680957151 39.922777040451614 15.53, 116.71553152050265 39.92278104718814 15.52, 116.71553633164253 39.922784954059665 15.52, 116.7155410425722 39.92278896079502 15.51, 116.71554575350116 39.92279296752979 15.51, 116.71555046442938 39.92279697426398 15.5, 116.71555527556629 39.92280088113311 15.49, 116.71555998648057 39.92280478786194 15.48, 116.71556469740662 39.922808794594374 15.48, 116.71556940831947 39.92281270132205 15.47, 116.7155742194534 39.922816608188796 15.47, 116.71557893037725 39.922820614919466 15.47, 116.71558374150969 39.92282452178501 15.46, 116.71558855264136 39.92282842864994 15.46, 116.71559326356302 39.92283243537885 15.45, 116.71559807469322 39.922836342242576 15.45, 116.71560288582265 39.922840249105704 15.44, 116.7156076969638 39.9228442559724 15.43, 116.71561250809172 39.92284816283432 15.43, 116.71561731921888 39.922852069695615 15.42, 116.7156221303453 39.92285597655631 15.42, 116.71562684124932 39.922859883276885 15.42, 116.71563165237423 39.92286379013638 15.41, 116.71563646349838 39.92286769699527 15.41, 116.7156411744002 39.922871603714064 15.41, 116.71564598552287 39.92287551057175 15.41, 116.71565079664477 39.922879417428824 15.4, 116.71565560776592 39.922883324285294 15.4, 116.71566041888632 39.922887231141154 15.39, 116.71566512978445 39.922891137856986 15.39, 116.71567004112484 39.922895044851046 15.39, 116.71567485224296 39.92289895170508 15.38, 116.71567966334786 39.92290275855433 15.38, 116.71568457468592 39.92290666554651 15.38, 116.71568938580177 39.922910572398706 15.37, 116.71569429712578 39.92291437938545 15.37, 116.7156992084615 39.922918286375754 15.37, 116.71570411978394 39.92292209336123 15.36, 116.71570903110559 39.92292590034607 15.36, 116.7157140426478 39.92292970746958 15.36, 116.71571895396787 39.92293351445314 15.35, 116.71572396550846 39.92293732157533 15.35, 116.71572887682693 39.92294112855762 15.35, 116.71573388835341 39.92294483567432 15.34, 116.71573889989156 39.922948642794545 15.34, 116.71574391142887 39.922952449914106 15.33, 116.7157489229529 39.92295615702883 15.33, 116.71575383426735 39.92295996400789 15.32, 116.71575884578974 39.9229636711213 15.32, 116.7157638573238 39.92296747823824 15.32, 116.71576886884453 39.922971185350335 15.32, 116.71577378015579 39.922974992326814 15.31, 116.7157787916749 39.92297869943759 15.31, 116.71578380320568 39.9229825065519 15.31, 116.71578881472315 39.92298621366136 15.3, 116.7157937260312 39.92299002063527 15.3, 116.71579873754702 39.922993727743425 15.3, 116.71580374907452 39.92299753485509 15.3, 116.71580876058873 39.92300124196192 15.3, 116.7158137721021 39.92300494906809 15.29, 116.71581878362713 39.92300875617778 15.29, 116.71582379513886 39.923012463282625 15.29, 116.71582880664977 39.92301617038681 15.29, 116.7158339183808 39.92301987762931 15.28, 116.71583892987755 39.92302348472798 15.28, 116.71584404160689 39.923027191969105 15.27, 116.7158491533229 39.92303079920537 15.27, 116.71585426505052 39.92303450644512 15.27, 116.71585947698568 39.92303811381891 15.27, 116.71586458869909 39.923041721053096 15.26, 116.7158698006325 39.92304532842547 15.26, 116.715875012565 39.92304893579713 15.26, 116.7158802244966 39.92305254316806 15.26, 116.71588543642733 39.92305615053829 15.25, 116.71589064834468 39.92305965790362 15.25, 116.7158958602736 39.92306326527241 15.25, 116.7159011724099 39.9230667727751 15.25, 116.71590638433705 39.92307038014245 15.25, 116.71591169647151 39.92307388764366 15.24, 116.71591690839685 39.92307749500956 15.24, 116.71592222052946 39.923081002509306 15.24, 116.71592743244052 39.923084509869575 15.23, 116.7159327445713 39.92308801736784 15.23, 116.71593805670116 39.92309152486536 15.23, 116.71594326860951 39.92309503222346 15.23, 116.71594858073753 39.923098539719504 15.23, 116.71595379264407 39.92310204707615 15.23, 116.71595910477025 39.92310555457072 15.22, 116.71596431667498 39.92310906192592 15.22, 116.71596962879933 39.92311256941902 15.22, 116.71597494092273 39.92311607691136 15.22, 116.71598025303274 39.92311948439879 15.21, 116.7159855651543 39.92312299188965 15.21, 116.71599087726246 39.92312639937558 15.21, 116.71599628959011 39.92312980699931 15.21, 116.71600160169638 39.92313321448375 15.2, 116.71600701402213 39.92313662210594 15.2, 116.71601242634688 39.92314002972738 15.2, 116.71601773843786 39.92314333720536 15.2, 116.71602315076072 39.92314674482525 15.2, 116.7160285630826 39.92315015244437 15.2, 116.71603397539106 39.923153460058536 15.19, 116.71603938771102 39.9231568676761 15.19, 116.7160449002378 39.923160175427135 15.19, 116.71605031255581 39.923163583043134 15.19, 116.71605572486038 39.923166890654194 15.19, 116.71606123739667 39.923170298407015 15.19, 116.71606664969929 39.92317360601651 15.18, 116.71607216222111 39.92317691376356 15.18, 116.71607767474192 39.92318022150982 15.18, 116.71608308704162 39.92318352911695 15.18, 116.71608859956046 39.92318683686161 15.18, 116.7160941120783 39.92319014460546 15.18, 116.71609952436255 39.92319335220606 15.18, 116.7161050368784 39.92319665994833 15.18, 116.71611054938076 39.923199867685604 15.18, 116.71611606189461 39.92320317542626 15.17, 116.71612167461498 39.92320638330014 15.17, 116.71612718711431 39.92320959103499 15.17, 116.71613269961263 39.923212798769036 15.17, 116.71613831232992 39.923216006640445 15.17, 116.71614392504615 39.92321921451102 15.17, 116.71614943752895 39.923222322238445 15.17, 116.71615505024315 39.92322553010736 15.16, 116.7161606629438 39.92322863797127 15.16, 116.71616637586328 39.92323174597244 15.16, 116.71617198857432 39.923234953838836 15.16, 116.71617760127184 39.92323806170023 15.15, 116.7161832139683 39.92324116956078 15.15, 116.71618882666372 39.9232442774205 15.15, 116.71619453957788 39.9232473854174 15.15, 116.71620015227123 39.92325049327543 15.14, 116.71620586518323 39.923253601270616 15.14, 116.71621147787447 39.92325670912697 15.14, 116.71621719078435 39.92325981712044 15.14, 116.71622290368066 39.923262825108864 15.13, 116.71622861658837 39.9232659331006 15.13, 116.71623432949501 39.92326904109148 15.12, 116.7162400423881 39.92327204907731 15.12, 116.71624575529258 39.92327515706645 15.12, 116.71625146818349 39.92327816505055 15.12, 116.71625718107332 39.92328117303378 15.12, 116.71626299418162 39.92328418115398 15.12, 116.71626880728878 39.92328718927328 15.12, 116.71627452017535 39.923290197253884 15.12, 116.7162803332803 39.92329320537141 15.11, 116.7162862466036 39.92329621362581 15.11, 116.71629205969381 39.92329912173736 15.11, 116.71629787279538 39.923302129852175 15.11, 116.71630378610277 39.92330503809964 15.11, 116.7163095991896 39.923307946208475 15.11, 116.71631551249467 39.9233108544541 15.11, 116.71632132557926 39.92331376256112 15.11, 116.71632723888203 39.92331667080491 15.11, 116.71633315218365 39.923319579047764 15.11, 116.71633906547164 39.923322387285516 15.11, 116.71634487855167 39.9233252953889 15.11, 116.71635079183736 39.923328103624804 15.11, 116.71635670512187 39.923330911859786 15.12, 116.7163626184177 39.923333820098016 15.12, 116.7163685316999 39.92333662833113 15.12, 116.71637444498094 39.923339436563325 15.12, 116.71638045846748 39.92334214492792 15.13, 116.71638637174618 39.923344953158235 15.13, 116.71639238524283 39.923347761525115 15.13, 116.71639829850669 39.92335046974937 15.13, 116.71640431200096 39.92335327811434 15.13, 116.71641032548153 39.923355986474164 15.13, 116.71641633896091 39.923358694833034 15.14, 116.71642235243908 39.92336140319093 15.14, 116.71642836591604 39.92336411154787 15.14, 116.7164343794043 39.92336691990802 15.14, 116.71644039287887 39.92336962826304 15.14, 116.71644640635223 39.92337233661709 15.14, 116.71645241982439 39.92337504497017 15.14, 116.71645843329536 39.9233777533223 15.14, 116.71646454698399 39.92338046181075 15.15, 116.71647056045252 39.92338317016094 15.14, 116.71647657391985 39.92338587851015 15.15, 116.7164826875923 39.92338848699148 15.15, 116.7164887010572 39.92339119533876 15.15, 116.71649481473968 39.92339390382228 15.15, 116.71650082820216 39.92339661216761 15.15, 116.71650694186967 39.92339922064498 15.15, 116.71651305553594 39.92340182912136 15.15, 116.71651906899477 39.92340453746377 15.15, 116.71652518265857 39.92340714593817 15.15, 116.71653129632114 39.923409754411566 15.15, 116.71653740998245 39.92341236288397 15.15, 116.71654352363004 39.92341487135121 15.16, 116.71654963728885 39.92341747982162 15.16, 116.71655575093396 39.92341998828686 15.16, 116.71656186459028 39.92342259675527 15.16, 116.71656797823289 39.92342510521852 15.16, 116.71657419209275 39.92342761381777 15.16, 116.71658030573283 39.923430122279 15.16, 116.7165864193717 39.923432630739235 15.16, 116.71659263322773 39.92343513933543 15.16, 116.71659874687653 39.92343774779782 15.17, 116.71660496073002 39.92344025639196 15.17, 116.71661107436383 39.92344276484817 15.17, 116.71661728821475 39.92344527344027 15.17, 116.7166235020644 39.92344778203134 15.17, 116.71662971591275 39.923450290621375 15.17, 116.7166359297598 39.923452799210374 15.18, 116.71664214360558 39.92345530779835 15.18, 116.7166482572318 39.92345781624847 15.19, 116.71665447106253 39.923460224830215 15.19, 116.71666068490445 39.92346273341511 15.19, 116.7166668987326 39.92346514199479 15.19, 116.71667311255946 39.92346755057344 15.2, 116.71667932638503 39.92346995915106 15.2, 116.7166855402218 39.92347246773181 15.2, 116.71669175404479 39.92347487630736 15.21, 116.7166979678665 39.92347728488188 15.21, 116.7167041816869 39.92347969345537 15.21, 116.71671039549352 39.92348200202364 15.22, 116.71671660931135 39.92348441059506 15.22, 116.71672292334588 39.92348681930205 15.23, 116.71672913714863 39.9234891278672 15.23, 116.71673545118051 39.92349153657207 15.23, 116.71674176519859 39.9234938452717 15.24, 116.71674807921534 39.92349615397027 15.24, 116.71675439323073 39.92349846266776 15.24, 116.71676070725728 39.923500871368354 15.25, 116.71676702127003 39.92350318006371 15.25, 116.71677333528143 39.923505488757996 15.25, 116.7167796492915 39.92350779745122 15.26, 116.71678596330021 39.923510106143375 15.26, 116.71679227729514 39.923512314830276 15.27, 116.71679849108345 39.92351462338389 15.27, 116.71680480508819 39.92351693207285 15.27, 116.71681121929683 39.923519140892935 15.27, 116.71681753329888 39.92352144957974 15.28, 116.7168238472871 39.92352365826131 15.29, 116.71683026150413 39.92352596708229 15.29, 116.7168366757073 39.923528175898 15.29, 116.71684308990908 39.923530384712606 15.3, 116.7168494038919 39.92353259338984 15.3, 116.71685581810343 39.92353490220642 15.31, 116.71686213208355 39.9235371108815 15.31, 116.71686854627985 39.92353931969171 15.32, 116.71687486025728 39.92354152836462 15.32, 116.71688127445086 39.92354373717266 15.32, 116.71688758842558 39.923545945843415 15.33, 116.7168940026164 39.92354815464925 15.34, 116.71690031658844 39.923550363317844 15.34, 116.71690673076404 39.92355247211731 15.34, 116.7169132451681 39.92355468105595 15.35, 116.7169196593534 39.92355688985735 15.36, 116.71692607352483 39.92355899865349 15.36, 116.71693258791218 39.92356110758456 15.36, 116.7169390020933 39.92356331638264 15.37, 116.71694541626057 39.92356542517543 15.37, 116.7169517302092 39.92356753383113 15.38, 116.71695814437373 39.92356964262173 15.39, 116.71696455854935 39.9235718514154 15.39, 116.71697097271111 39.92357396020379 15.4, 116.71697738685899 39.923575968986896 15.41, 116.71698380101797 39.923578077773065 15.41, 116.71699031539268 39.923580186694025 15.41, 116.71699672954887 39.92358229547797 15.42, 116.71700324390825 39.92358430439248 15.43, 116.71700975826622 39.923586313305854 15.43, 116.71701627262276 39.92358832221809 15.44, 116.71702278697786 39.92359033112917 15.44, 116.71702920111456 39.92359233990333 15.45, 116.71703571545434 39.92359424880797 15.47, 116.71704222980519 39.92359625771565 15.47, 116.7170487441421 39.923598166618014 15.48, 116.7170552584776 39.923600075519225 15.49, 116.71706177281166 39.9236019844193 15.49, 116.71706818692746 39.923603893182545 15.51, 116.71707450081256 39.92360570180485 15.51, 116.71709684999163 39.92361243233016 15.55)")}")
        val geometryStr = "LINESTRING(116.71523523056109 39.92252072202544 16.08, 116.7152446525368 39.922528935578065 16.04, 116.7152492633007 39.92253304221301 16.03, 116.71525397428678 39.922537148987885 16.02, 116.7152585850368 39.92254115561753 16, 116.71526319579864 39.9225452622508 15.99, 116.7152678065473 39.92254926887934 15.99, 116.71527241730776 39.92255337551149 15.97, 116.71527702806752 39.9225574821431 15.96, 116.71528163882658 39.92256158877415 15.94, 116.71528624957249 39.92256559540047 15.92, 116.7152908603302 39.92256970203041 15.91, 116.7152954710872 39.9225738086598 15.9, 116.71529998162082 39.92257791514826 15.88, 116.71530459237646 39.92258202177656 15.87, 116.71530920313141 39.9225861284043 15.86, 116.71531381388567 39.92259023503148 15.85, 116.7153183244166 39.92259434151779 15.84, 116.71532293516951 39.92259844814387 15.82, 116.71532754590925 39.92260245476522 15.82, 116.71533205643817 39.92260656124991 15.8, 116.71533666718902 39.922610667874345 15.79, 116.71534127793919 39.92261477449822 15.78, 116.71534588868865 39.922618881121544 15.77, 116.7153503992149 39.92262298760407 15.76, 116.715355009963 39.92262709422631 15.75, 116.71535962071042 39.922631200847974 15.75, 116.71536423144467 39.9226352074649 15.74, 116.71536884219071 39.922639314085465 15.73, 116.71537335271358 39.922643420565294 15.72, 116.71537796345827 39.92264752718475 15.72, 116.71538257420224 39.92265163380365 15.72, 116.71538718494553 39.92265574042201 15.71, 116.71539179567566 39.92265974703562 15.71, 116.71539640641758 39.92266385365286 15.7, 116.7154010171588 39.92266796026954 15.7, 116.71540562789932 39.92267206688567 15.7, 116.71541023862667 39.92267607349705 15.69, 116.71541484936583 39.922680180112074 15.68, 116.7154194601043 39.922684286726536 15.68, 116.71542407084206 39.92268839334044 15.67, 116.71542868157914 39.92269249995378 15.66, 116.71543339252533 39.922696506702415 15.66, 116.715438003261 39.92270061331464 15.65, 116.71544261399599 39.92270471992631 15.64, 116.7154472247178 39.92270872653324 15.64, 116.71545193567363 39.92271283328377 15.63, 116.71545654640653 39.92271693989375 15.62, 116.71546115712626 39.922720946499005 15.62, 116.71546586806747 39.92272495324363 15.61, 116.71547047879827 39.92272905985193 15.6, 116.71547518973806 39.92273306659542 15.59, 116.71547990067711 39.92273707333831 15.59, 116.71548461161544 39.922741080080634 15.58, 116.71548922233096 39.9227450866825 15.58, 116.71549393326787 39.92274909342366 15.57, 116.71549854398198 39.9227531000244 15.57, 116.71550325491746 39.922757106764415 15.56, 116.71550796583975 39.922761013499674 15.55, 116.71551267677377 39.92276502023853 15.55, 116.71551738770708 39.9227690269768 15.54, 116.71552209863965 39.9227730337145 15.54, 116.71552680957151 39.922777040451614 15.53, 116.71553152050265 39.92278104718814 15.52, 116.71553633164253 39.922784954059665 15.52, 116.7155410425722 39.92278896079502 15.51, 116.71554575350116 39.92279296752979 15.51, 116.71555046442938 39.92279697426398 15.5, 116.71555527556629 39.92280088113311 15.49, 116.71555998648057 39.92280478786194 15.48, 116.71556469740662 39.922808794594374 15.48, 116.71556940831947 39.92281270132205 15.47, 116.7155742194534 39.922816608188796 15.47, 116.71557893037725 39.922820614919466 15.47, 116.71558374150969 39.92282452178501 15.46, 116.71558855264136 39.92282842864994 15.46, 116.71559326356302 39.92283243537885 15.45, 116.71559807469322 39.922836342242576 15.45, 116.71560288582265 39.922840249105704 15.44, 116.7156076969638 39.9228442559724 15.43, 116.71561250809172 39.92284816283432 15.43, 116.71561731921888 39.922852069695615 15.42, 116.7156221303453 39.92285597655631 15.42, 116.71562684124932 39.922859883276885 15.42, 116.71563165237423 39.92286379013638 15.41, 116.71563646349838 39.92286769699527 15.41, 116.7156411744002 39.922871603714064 15.41, 116.71564598552287 39.92287551057175 15.41, 116.71565079664477 39.922879417428824 15.4, 116.71565560776592 39.922883324285294 15.4, 116.71566041888632 39.922887231141154 15.39, 116.71566512978445 39.922891137856986 15.39, 116.71567004112484 39.922895044851046 15.39, 116.71567485224296 39.92289895170508 15.38, 116.71567966334786 39.92290275855433 15.38, 116.71568457468592 39.92290666554651 15.38, 116.71568938580177 39.922910572398706 15.37, 116.71569429712578 39.92291437938545 15.37, 116.7156992084615 39.922918286375754 15.37, 116.71570411978394 39.92292209336123 15.36, 116.71570903110559 39.92292590034607 15.36, 116.7157140426478 39.92292970746958 15.36, 116.71571895396787 39.92293351445314 15.35, 116.71572396550846 39.92293732157533 15.35, 116.71572887682693 39.92294112855762 15.35, 116.71573388835341 39.92294483567432 15.34, 116.71573889989156 39.922948642794545 15.34, 116.71574391142887 39.922952449914106 15.33, 116.7157489229529 39.92295615702883 15.33, 116.71575383426735 39.92295996400789 15.32, 116.71575884578974 39.9229636711213 15.32, 116.7157638573238 39.92296747823824 15.32, 116.71576886884453 39.922971185350335 15.32, 116.71577378015579 39.922974992326814 15.31, 116.7157787916749 39.92297869943759 15.31, 116.71578380320568 39.9229825065519 15.31, 116.71578881472315 39.92298621366136 15.3, 116.7157937260312 39.92299002063527 15.3, 116.71579873754702 39.922993727743425 15.3, 116.71580374907452 39.92299753485509 15.3, 116.71580876058873 39.92300124196192 15.3, 116.7158137721021 39.92300494906809 15.29, 116.71581878362713 39.92300875617778 15.29, 116.71582379513886 39.923012463282625 15.29, 116.71582880664977 39.92301617038681 15.29, 116.7158339183808 39.92301987762931 15.28, 116.71583892987755 39.92302348472798 15.28, 116.71584404160689 39.923027191969105 15.27, 116.7158491533229 39.92303079920537 15.27, 116.71585426505052 39.92303450644512 15.27, 116.71585947698568 39.92303811381891 15.27, 116.71586458869909 39.923041721053096 15.26, 116.7158698006325 39.92304532842547 15.26, 116.715875012565 39.92304893579713 15.26, 116.7158802244966 39.92305254316806 15.26, 116.71588543642733 39.92305615053829 15.25, 116.71589064834468 39.92305965790362 15.25, 116.7158958602736 39.92306326527241 15.25, 116.7159011724099 39.9230667727751 15.25, 116.71590638433705 39.92307038014245 15.25, 116.71591169647151 39.92307388764366 15.24, 116.71591690839685 39.92307749500956 15.24, 116.71592222052946 39.923081002509306 15.24, 116.71592743244052 39.923084509869575 15.23, 116.7159327445713 39.92308801736784 15.23, 116.71593805670116 39.92309152486536 15.23, 116.71594326860951 39.92309503222346 15.23, 116.71594858073753 39.923098539719504 15.23, 116.71595379264407 39.92310204707615 15.23, 116.71595910477025 39.92310555457072 15.22, 116.71596431667498 39.92310906192592 15.22, 116.71596962879933 39.92311256941902 15.22, 116.71597494092273 39.92311607691136 15.22, 116.71598025303274 39.92311948439879 15.21, 116.7159855651543 39.92312299188965 15.21, 116.71599087726246 39.92312639937558 15.21, 116.71599628959011 39.92312980699931 15.21, 116.71600160169638 39.92313321448375 15.2, 116.71600701402213 39.92313662210594 15.2, 116.71601242634688 39.92314002972738 15.2, 116.71601773843786 39.92314333720536 15.2, 116.71602315076072 39.92314674482525 15.2, 116.7160285630826 39.92315015244437 15.2, 116.71603397539106 39.923153460058536 15.19, 116.71603938771102 39.9231568676761 15.19, 116.7160449002378 39.923160175427135 15.19, 116.71605031255581 39.923163583043134 15.19, 116.71605572486038 39.923166890654194 15.19, 116.71606123739667 39.923170298407015 15.19, 116.71606664969929 39.92317360601651 15.18, 116.71607216222111 39.92317691376356 15.18, 116.71607767474192 39.92318022150982 15.18, 116.71608308704162 39.92318352911695 15.18, 116.71608859956046 39.92318683686161 15.18, 116.7160941120783 39.92319014460546 15.18, 116.71609952436255 39.92319335220606 15.18, 116.7161050368784 39.92319665994833 15.18, 116.71611054938076 39.923199867685604 15.18, 116.71611606189461 39.92320317542626 15.17, 116.71612167461498 39.92320638330014 15.17, 116.71612718711431 39.92320959103499 15.17, 116.71613269961263 39.923212798769036 15.17, 116.71613831232992 39.923216006640445 15.17, 116.71614392504615 39.92321921451102 15.17, 116.71614943752895 39.923222322238445 15.17, 116.71615505024315 39.92322553010736 15.16, 116.7161606629438 39.92322863797127 15.16, 116.71616637586328 39.92323174597244 15.16, 116.71617198857432 39.923234953838836 15.16, 116.71617760127184 39.92323806170023 15.15, 116.7161832139683 39.92324116956078 15.15, 116.71618882666372 39.9232442774205 15.15, 116.71619453957788 39.9232473854174 15.15, 116.71620015227123 39.92325049327543 15.14, 116.71620586518323 39.923253601270616 15.14, 116.71621147787447 39.92325670912697 15.14, 116.71621719078435 39.92325981712044 15.14, 116.71622290368066 39.923262825108864 15.13, 116.71622861658837 39.9232659331006 15.13, 116.71623432949501 39.92326904109148 15.12, 116.7162400423881 39.92327204907731 15.12, 116.71624575529258 39.92327515706645 15.12, 116.71625146818349 39.92327816505055 15.12, 116.71625718107332 39.92328117303378 15.12, 116.71626299418162 39.92328418115398 15.12, 116.71626880728878 39.92328718927328 15.12, 116.71627452017535 39.923290197253884 15.12, 116.7162803332803 39.92329320537141 15.11, 116.7162862466036 39.92329621362581 15.11, 116.71629205969381 39.92329912173736 15.11, 116.71629787279538 39.923302129852175 15.11, 116.71630378610277 39.92330503809964 15.11, 116.7163095991896 39.923307946208475 15.11, 116.71631551249467 39.9233108544541 15.11, 116.71632132557926 39.92331376256112 15.11, 116.71632723888203 39.92331667080491 15.11, 116.71633315218365 39.923319579047764 15.11, 116.71633906547164 39.923322387285516 15.11, 116.71634487855167 39.9233252953889 15.11, 116.71635079183736 39.923328103624804 15.11, 116.71635670512187 39.923330911859786 15.12, 116.7163626184177 39.923333820098016 15.12, 116.7163685316999 39.92333662833113 15.12, 116.71637444498094 39.923339436563325 15.12, 116.71638045846748 39.92334214492792 15.13, 116.71638637174618 39.923344953158235 15.13, 116.71639238524283 39.923347761525115 15.13, 116.71639829850669 39.92335046974937 15.13, 116.71640431200096 39.92335327811434 15.13, 116.71641032548153 39.923355986474164 15.13, 116.71641633896091 39.923358694833034 15.14, 116.71642235243908 39.92336140319093 15.14, 116.71642836591604 39.92336411154787 15.14, 116.7164343794043 39.92336691990802 15.14, 116.71644039287887 39.92336962826304 15.14, 116.71644640635223 39.92337233661709 15.14, 116.71645241982439 39.92337504497017 15.14, 116.71645843329536 39.9233777533223 15.14, 116.71646454698399 39.92338046181075 15.15, 116.71647056045252 39.92338317016094 15.14, 116.71647657391985 39.92338587851015 15.15, 116.7164826875923 39.92338848699148 15.15, 116.7164887010572 39.92339119533876 15.15, 116.71649481473968 39.92339390382228 15.15, 116.71650082820216 39.92339661216761 15.15, 116.71650694186967 39.92339922064498 15.15, 116.71651305553594 39.92340182912136 15.15, 116.71651906899477 39.92340453746377 15.15, 116.71652518265857 39.92340714593817 15.15, 116.71653129632114 39.923409754411566 15.15, 116.71653740998245 39.92341236288397 15.15, 116.71654352363004 39.92341487135121 15.16, 116.71654963728885 39.92341747982162 15.16, 116.71655575093396 39.92341998828686 15.16, 116.71656186459028 39.92342259675527 15.16, 116.71656797823289 39.92342510521852 15.16, 116.71657419209275 39.92342761381777 15.16, 116.71658030573283 39.923430122279 15.16, 116.7165864193717 39.923432630739235 15.16, 116.71659263322773 39.92343513933543 15.16, 116.71659874687653 39.92343774779782 15.17, 116.71660496073002 39.92344025639196 15.17, 116.71661107436383 39.92344276484817 15.17, 116.71661728821475 39.92344527344027 15.17, 116.7166235020644 39.92344778203134 15.17, 116.71662971591275 39.923450290621375 15.17, 116.7166359297598 39.923452799210374 15.18, 116.71664214360558 39.92345530779835 15.18, 116.7166482572318 39.92345781624847 15.19, 116.71665447106253 39.923460224830215 15.19, 116.71666068490445 39.92346273341511 15.19, 116.7166668987326 39.92346514199479 15.19, 116.71667311255946 39.92346755057344 15.2, 116.71667932638503 39.92346995915106 15.2, 116.7166855402218 39.92347246773181 15.2, 116.71669175404479 39.92347487630736 15.21, 116.7166979678665 39.92347728488188 15.21, 116.7167041816869 39.92347969345537 15.21, 116.71671039549352 39.92348200202364 15.22, 116.71671660931135 39.92348441059506 15.22, 116.71672292334588 39.92348681930205 15.23, 116.71672913714863 39.9234891278672 15.23, 116.71673545118051 39.92349153657207 15.23, 116.71674176519859 39.9234938452717 15.24, 116.71674807921534 39.92349615397027 15.24, 116.71675439323073 39.92349846266776 15.24, 116.71676070725728 39.923500871368354 15.25, 116.71676702127003 39.92350318006371 15.25, 116.71677333528143 39.923505488757996 15.25, 116.7167796492915 39.92350779745122 15.26, 116.71678596330021 39.923510106143375 15.26, 116.71679227729514 39.923512314830276 15.27, 116.71679849108345 39.92351462338389 15.27, 116.71680480508819 39.92351693207285 15.27, 116.71681121929683 39.923519140892935 15.27, 116.71681753329888 39.92352144957974 15.28, 116.7168238472871 39.92352365826131 15.29, 116.71683026150413 39.92352596708229 15.29, 116.7168366757073 39.923528175898 15.29, 116.71684308990908 39.923530384712606 15.3, 116.7168494038919 39.92353259338984 15.3, 116.71685581810343 39.92353490220642 15.31, 116.71686213208355 39.9235371108815 15.31, 116.71686854627985 39.92353931969171 15.32, 116.71687486025728 39.92354152836462 15.32, 116.71688127445086 39.92354373717266 15.32, 116.71688758842558 39.923545945843415 15.33, 116.7168940026164 39.92354815464925 15.34, 116.71690031658844 39.923550363317844 15.34, 116.71690673076404 39.92355247211731 15.34, 116.7169132451681 39.92355468105595 15.35, 116.7169196593534 39.92355688985735 15.36, 116.71692607352483 39.92355899865349 15.36, 116.71693258791218 39.92356110758456 15.36, 116.7169390020933 39.92356331638264 15.37, 116.71694541626057 39.92356542517543 15.37, 116.7169517302092 39.92356753383113 15.38, 116.71695814437373 39.92356964262173 15.39, 116.71696455854935 39.9235718514154 15.39, 116.71697097271111 39.92357396020379 15.4, 116.71697738685899 39.923575968986896 15.41, 116.71698380101797 39.923578077773065 15.41, 116.71699031539268 39.923580186694025 15.41, 116.71699672954887 39.92358229547797 15.42, 116.71700324390825 39.92358430439248 15.43, 116.71700975826622 39.923586313305854 15.43, 116.71701627262276 39.92358832221809 15.44, 116.71702278697786 39.92359033112917 15.44, 116.71702920111456 39.92359233990333 15.45, 116.71703571545434 39.92359424880797 15.47, 116.71704222980519 39.92359625771565 15.47, 116.7170487441421 39.923598166618014 15.48, 116.7170552584776 39.923600075519225 15.49, 116.71706177281166 39.9236019844193 15.49, 116.71706818692746 39.923603893182545 15.51, 116.71707450081256 39.92360570180485 15.51, 116.71709684999163 39.92361243233016 15.55)"
        val geoByteArray = DeflaterUtil.zipString(geometryStr)
        Log.e("qj","字节转化===${DeflaterUtil.unzipString(geoByteArray)}")
        mapController.animationHandler.zoomIn()
    }

    /**
     * zoomOut
     */
    fun zoomOutOnclick(view: View) {
        val result = mutableListOf<RenderEntity>()
        for (i in 0 until 10) {
            var renderEntity: RenderEntity = RenderEntity()
            renderEntity.geometry = "POINT(116.2694${i}13016946 40.0844${i}5791644373 0)"
            result.add(renderEntity)
        }
        //计算后
        var index = 0
        Log.e("qj","====计算开始")
        var lastRender:RenderEntity = RenderEntity()
        GeometryTools.groupByDistance(DataCodeEnum.OMDB_TRAFFIC_SIGN.code,result, 5.0)?.forEach {
            if(lastRender!=null&&lastRender.geometry!=null&& lastRender.geometry != ""){
                if(it.geometry!=lastRender.geometry){
                    Log.e("qj","${index++}====计算后"+it.geometry)
                }
            }
            lastRender = it
        }
        Log.e("qj","====计算结束")
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
        binding.mainActivityStatusCamera.visibility = visibility
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
    fun selectLineOnclick() {
        viewModel.setSelectRoad(!viewModel.isSelectRoad())
        binding.mainActivitySelectLine.isSelected = viewModel.isSelectRoad()
    }

    /**
     * 点击线高亮
     */
    fun openOrCloseLineOnclick() {
        viewModel.setHighRoad(!viewModel.isHighRoad())
        binding.mainActivityCloseLine.isSelected = viewModel.isHighRoad()
        mapController.lineHandler.taskMarkerLayerEnable(viewModel.isHighRoad())
    }


    /**
     * 点击线选择
     */
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
    fun finishTraceOnclick() {
        setIndoorGroupEnable(false)
        viewModel.setSelectTrace(false)
        viewModel.setMediaFlag(false)
        viewModel.setSelectPauseTrace(false)
        binding.mainActivityMenuIndoorGroup.visibility = View.GONE
        binding.mainActivityTraceSnapshotPoints.isSelected = viewModel.isSelectTrace()
        //binding.mainActivitySnapshotMediaFlag.isSelected = viewModel.isMediaFlag()
        binding.mainActivitySnapshotPause.isSelected = viewModel.isSelectPauseTrace()
    }

    /**
     * 点击结束轨迹操作
     */
    fun mediaFlagOnclick() {
        /*        viewModel.setMediaFlag(!viewModel.isMediaFlag())
                binding.mainActivitySnapshotMediaFlag.isSelected = viewModel.isMediaFlag()*/
    }

    /**
     * 点击上一个轨迹点播放操作
     */
    fun rewindTraceOnclick() {
        pausePlayTrace()
        val item =
            mapController.markerHandle.getNILocation(viewModel.getCurrentNiLocationIndex() - 1)
        if (item != null) {
            viewModel.setCurrentIndexLoction(viewModel.getCurrentNiLocationIndex() - 1)
            viewModel.showMarker(this, item)
            val traceVideoBean = TraceVideoBean(
                command = "videotime?",
                userid = Constant.USER_ID,
                time = "${item.time}:000"
            )
            viewModel.sendServerCommand(this, traceVideoBean, IndoorToolsCommand.REWIND)
        } else {
            dealNoData()
        }
    }

    /**
     * 点击暂停播放轨迹操作
     */
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

    private fun playVideo() {
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
    private fun setPlayStatus() {
        //切换为播放
        viewModel.setSelectPauseTrace(true)
        binding.mainActivitySnapshotPause.isSelected = viewModel.isSelectPauseTrace()
        playVideo()
    }

    private fun pauseVideo() {
        val traceVideoBean = TraceVideoBean(command = "pauseVideo?", userid = Constant.USER_ID)
        viewModel.sendServerCommand(this, traceVideoBean, IndoorToolsCommand.STOP)
    }

    /**
     * 点击下一个轨迹点
     */
    fun nextTraceOnclick() {
        pausePlayTrace()
        val item =
            mapController.markerHandle.getNILocation(viewModel.getCurrentNiLocationIndex() + 1)
        if (item != null) {
            viewModel.setCurrentIndexLoction(viewModel.getCurrentNiLocationIndex() + 1)
            viewModel.showMarker(this, item)
            val traceVideoBean = TraceVideoBean(
                command = "videotime?",
                userid = Constant.USER_ID,
                time = "${item.time}:000"
            )
            viewModel.sendServerCommand(this, traceVideoBean, IndoorToolsCommand.NEXT)
        } else {
            dealNoData()
        }
    }

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
        viewModel.cancelTrace()
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

            hideMainActivityBottomSheetGroup()
        } else {
            showMainActivityBottomSheetGroup()
        }
    }

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
                showMainActivityBottomSheetGroup()
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
                showMainActivityBottomSheetGroup()
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
     * 打开底部导航栏
     */
    private fun showMainActivityBottomSheetGroup() {
        binding.mainActivityBottomSheetGroup.visibility = View.VISIBLE
        mapController.mMapView.setScaleBarLayer(GLViewport.Position.BOTTOM_CENTER, 256, 60)
        mapController.mMapView.vtmMap.animator().animateTo(
            GeoPoint(
                mapController.mMapView.vtmMap.mapPosition.geoPoint.latitude,
                mapController.mMapView.vtmMap.mapPosition.geoPoint.longitude
            )
        )
    }

    /**
     * 关闭底部导航栏
     */
    private fun hideMainActivityBottomSheetGroup() {
        binding.mainActivityBottomSheetGroup.visibility = View.GONE
        mapController.mMapView.setScaleBarLayer(GLViewport.Position.BOTTOM_CENTER, 256, 0)
        mapController.mMapView.vtmMap.animator().animateTo(
            GeoPoint(
                mapController.mMapView.vtmMap.mapPosition.geoPoint.latitude,
                mapController.mMapView.vtmMap.mapPosition.geoPoint.longitude
            )
        )
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
        viewModel.planningPath()
    }

    /**
     * 打开离线地图
     */
    fun onClickOfflineMapFragment() {
        if (leftFragment !is OfflineMapFragment) {
            if (leftFragment == null) {
                showMainActivityBottomSheetGroup()
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
    fun openRoadNameFragment() {
        backSignMoreInfo()
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
     * 点击定位按钮
     */
    fun onClickLocation() {
        binding.mainActivityLocation.setImageResource(R.drawable.icon_location_north)
        viewModel.onClickLocationButton()
    }

    /**
     * 新增评测link
     */
    fun onClickTaskLink() {
        rightController.navigate(R.id.TaskLinkFragment)
    }

    fun setRightButtonsVisible(visible: Int) {
        binding.mainActivityRightVisibilityButtonsGroup2.visibility = visible
    }

    /**
     * 隐藏更多信息面板
     */
    fun backSignMoreInfo() {
        val fragment =
            supportFragmentManager.findFragmentById(R.id.main_activity_sign_more_info_fragment)
        if (fragment != null && !fragment.isHidden) {
            supportFragmentManager.beginTransaction().remove(fragment).commit()
        }
    }
}