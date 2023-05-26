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
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.databinding.ActivityMainBinding
import com.navinfo.omqs.http.offlinemapdownload.OfflineMapDownloadManager
import com.navinfo.omqs.tools.LayerConfigUtils
import com.navinfo.omqs.ui.activity.BaseActivity
import com.navinfo.omqs.ui.widget.RecyclerViewSpacesItemDecoration
import com.navinfo.omqs.util.FlowEventBus
import com.navinfo.omqs.util.SpeakMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

    var switchFragment = false


    private val someActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                Log.e("jingo", "MainActivity someActivityResultLauncher RESULT_OK")
            } else {
                Log.e("jingo", "MainActivity someActivityResultLauncher ${result.resultCode}")
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
        SignAdapter { position, signBean ->
            rightController.currentDestination?.let {
                if (it.id == R.id.RightEmptyFragment) {
                    val bundle = Bundle()
                    bundle.putParcelable("SignBean", signBean)
                    rightController.navigate(R.id.EvaluationResultFragment, bundle)
                }
            }
        }
    }

    /**
     * 道路信息看板
     */
    private val topSignAdapter by lazy {
        TopSignAdapter { position, signBean ->
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
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

        binding.mainActivityVoice.setOnTouchListener(object : View.OnTouchListener {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                Log.e("qj", event?.action.toString())
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        voiceOnTouchStart()//Do Something
                        Log.e("qj", "voiceOnTouchStart")
                    }
                    MotionEvent.ACTION_UP -> {
                        voiceOnTouchStop()//Do Something
                        Log.e("qj", "voiceOnTouchStop")
                    }
                }

                return v?.onTouchEvent(event) ?: true
            }
        })

        viewModel.liveDataQsRecordIdList.observe(this) {
            //处理页面跳转
            viewModel.navigation(this, it)
        }

        viewModel.liveDataMenuState.observe(this) {
            binding.mainActivityMenu.isSelected = it
            if (it == true) {
                binding.mainActivityMenuGroup.visibility = View.VISIBLE
            } else {
                binding.mainActivityMenuGroup.visibility = View.INVISIBLE
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
            binding.mainActivityGeometry.text = "经纬度:${
                BigDecimal(it.longitude).setScale(
                    7,
                    RoundingMode.HALF_UP
                )
            },${BigDecimal(it.latitude).setScale(7, RoundingMode.HALF_UP)}"
        }

        lifecycleScope.launch {
            // 初始化地图图层控制接收器
            FlowEventBus.subscribe<List<ImportConfig>>(
                lifecycle, Constant.EVENT_LAYER_MANAGER_CHANGE
            ) {
                viewModel.refreshOMDBLayer(it)
            }
        }
    }

    //根据输入的经纬度跳转坐标
    fun jumpPosition() {
        val view = this.layoutInflater.inflate(R.layout.dialog_view_edittext, null)
        val inputDialog = MaterialAlertDialogBuilder(
            this
        ).setTitle("标记原因").setView(view)
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
                        val y = parts[0].toDouble()
                        mapController.animationHandler.animationByLatLon(y, x)
                    }else{
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

        //启动轨迹存储
//        viewModel.startSaveTraceThread(this)

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
        binding.mainActivityDrawer.open()
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
    fun onClickSerach() {

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
     * 隐藏或显示右侧展开按钮
     */
    fun setRightSwitchButton(visibility: Int) {
        binding.mainActivityFragmentSwitch.visibility = visibility
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

    fun voiceOnTouchStart() {
        viewModel.startSoundMetter(this, binding.mainActivityVoice)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun voiceOnTouchStop() {
        if (Constant.IS_VIDEO_SPEED) {
            viewModel.stopSoundMeter()
        }
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//    }
}