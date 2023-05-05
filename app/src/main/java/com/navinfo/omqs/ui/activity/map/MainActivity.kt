package com.navinfo.omqs.ui.activity.map

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.databinding.ActivityMainBinding
import com.navinfo.omqs.http.offlinemapdownload.OfflineMapDownloadManager
import com.navinfo.omqs.ui.activity.BaseActivity
import com.navinfo.omqs.util.FlowEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 地图主页面
 */
@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<MainViewModel>()

    //注入地图控制器
    @Inject
    lateinit var mapController: NIMapController

    @Inject
    lateinit var offlineMapDownloadManager: OfflineMapDownloadManager

    private val signAdapter by lazy { SignAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        //初始化地图
        mapController.init(
            this,
            binding.mainActivityMap,
            null,
            Constant.MAP_PATH,
            Constant.USER_DATA_PATH + "/trace.sqlite"
        )
        //关联生命周期
        binding.lifecycleOwner = this
        //给xml转递对象
        binding.mainActivity = this
        //给xml传递viewModel对象
        binding.viewModel = viewModel

        binding.mainActivityVoice.setOnTouchListener(object : View.OnTouchListener {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                Log.e("qj",event?.action.toString())
                when (event?.action) {
                    MotionEvent.ACTION_DOWN ->{
                        voiceOnTouchStart()//Do Something
                        Log.e("qj","voiceOnTouchStart")
                    }
                    MotionEvent.ACTION_UP ->{
                        voiceOnTouchStop()//Do Something
                        Log.e("qj","voiceOnTouchStop")
                    }
                }


                return v?.onTouchEvent(event) ?: true
            }
        })

        viewModel.liveDataQsRecordIdList.observe(this) {
            //处理页面跳转
            viewModel.navigation(this, it)
        }
        binding.mainActivitySignRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.mainActivitySignRecyclerview.adapter = signAdapter
        viewModel.liveDataSignList.observe(this) {
            signAdapter.refreshData(it)
        }

        lifecycleScope.launch {
            // 初始化地图图层控制接收器
            FlowEventBus.subscribe<List<ImportConfig>>(lifecycle, Constant.EVENT_LAYER_MANAGER_CHANGE) {
                // 根据获取到的配置信息，筛选未勾选的图层名称
                val omdbVisibleList = it.filter { importConfig->
                    importConfig.tableGroupName == "OMDB数据"
                }.first().tables.filter { tableInfo ->
                    !tableInfo.checked
                }.map {
                    tableInfo -> tableInfo.table
                }.toList()
                com.navinfo.collect.library.system.Constant.HAD_LAYER_INVISIABLE_ARRAY = omdbVisibleList.toTypedArray()
                // 刷新地图
                mapController.mMapView.vtmMap.clearMap()
            }
        }
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
     * 点击录音按钮
     */
    fun voiceOnclick() {
/*        val naviController = findNavController(R.id.main_activity_right_fragment)
        naviController.navigate(R.id.EvaluationResultFragment)*/
    }

    fun voiceOnTouchStart(){
        viewModel!!.startSoundMetter(this,binding.mainActivityVoice)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun voiceOnTouchStop(){
        if(Constant.IS_VIDEO_SPEED){
            viewModel!!.stopSoundMeter()
        }
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//    }
}