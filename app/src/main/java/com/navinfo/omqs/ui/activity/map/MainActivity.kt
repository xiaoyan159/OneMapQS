package com.navinfo.omqs.ui.activity.map

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.map.handler.NiLocationListener
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.ActivityMainBinding
import com.navinfo.omqs.http.offlinemapdownload.OfflineMapDownloadManager
import com.navinfo.omqs.ui.activity.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
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

        viewModel.liveDataQsRecordIdList.observe(this) {
            //处理页面跳转
            viewModel.navigation(this, it)
        }
        binding.mainActivitySignRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.mainActivitySignRecyclerview.adapter = signAdapter
        viewModel.liveDataSignList.observe(this) {
            signAdapter.refreshData(it)
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
        val naviController = findNavController(R.id.main_activity_right_fragment)
        naviController.navigate(R.id.EvaluationResultFragment)
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//    }
}