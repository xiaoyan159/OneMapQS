package com.navinfo.omqs.ui.fragment.personalcenter

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.forEach
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.UriUtils
import com.github.k1rakishou.fsaf.FileChooser
import com.github.k1rakishou.fsaf.callback.FSAFActivityCallbacks
import com.github.k1rakishou.fsaf.callback.FileChooserCallback
import com.navinfo.collect.library.enums.DataLayerEnum
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.collect.library.utils.MapParamUtils
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentPersonalCenterBinding
import com.navinfo.omqs.db.ImportOMDBHelper
import com.navinfo.omqs.hilt.ImportOMDBHiltFactory
import com.navinfo.omqs.tools.CoroutineUtils
import com.navinfo.omqs.ui.activity.map.MainViewModel
import com.navinfo.omqs.ui.activity.scan.QrCodeActivity
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.AndroidEntryPoint
import org.oscim.core.GeoPoint
import org.oscim.core.MapPosition
import java.io.File
import javax.inject.Inject

/**
 * 个人中心
 */
@AndroidEntryPoint
class PersonalCenterFragment(private var indoorDataListener: ((Boolean) -> Unit?)? = null) :
    BaseFragment(),
    FSAFActivityCallbacks {

    private var _binding: FragmentPersonalCenterBinding? = null
    private val binding get() = _binding!!
    private val fileChooser by lazy { FileChooser(requireContext()) }
    private val viewModel by lazy { viewModels<PersonalCenterViewModel>().value }
    private val viewMainModel by activityViewModels<MainViewModel>()

    @Inject
    lateinit var importOMDBHiltFactory: ImportOMDBHiltFactory

    @Inject
    lateinit var niMapController: NIMapController


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalCenterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.personal_center_menu_offline_map ->
                    findNavController().navigate(R.id.OfflineMapFragment)

                R.id.personal_center_menu_obtain_data -> { // 生成数据，根据sqlite文件生成对应的zip文件
                    fileChooser.openChooseFileDialog(object : FileChooserCallback() {
                        override fun onCancel(reason: String) {
                        }

                        @RequiresApi(Build.VERSION_CODES.N)
                        override fun onResult(uri: Uri) {
                            val file = UriUtils.uri2File(uri)
                            // 开始导入数据
                            // 656e6372797000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
                            CoroutineUtils.launchWithLoading(
                                requireContext(),
                                loadingMessage = "生成数据..."
                            ) {
                                val importOMDBHelper: ImportOMDBHelper =
                                    importOMDBHiltFactory.obtainImportOMDBHelper(
                                        requireContext(),
                                        file
                                    )
                                viewModel.obtainOMDBZipData(importOMDBHelper)
                            }
                        }
                    })
                }

                R.id.personal_center_menu_import_data -> { // 导入zip数据
                    fileChooser.openChooseFileDialog(object : FileChooserCallback() {
                        override fun onCancel(reason: String) {
                        }

                        override fun onResult(uri: Uri) {
                            val file = UriUtils.uri2File(uri)
                            val importOMDBHelper: ImportOMDBHelper =
                                importOMDBHiltFactory.obtainImportOMDBHelper(
                                    requireContext(),
                                    file
                                )
                            viewModel.importOMDBData(importOMDBHelper)
                        }
                    })
                }

                R.id.personal_center_menu_import_yuan_data -> {
                    // 用户选中导入数据，打开文件选择器，用户选择导入的数据文件目录
                    fileChooser.openChooseFileDialog(object : FileChooserCallback() {
                        override fun onCancel(reason: String) {
                        }

                        override fun onResult(uri: Uri) {
                            viewModel.importScProblemData(uri)
                        }
                    })
                }

                R.id.personal_center_menu_open_auto_location -> {
                    Constant.AUTO_LOCATION = !Constant.AUTO_LOCATION
                    if (Constant.AUTO_LOCATION) {
                        it.title = "关闭自动定位"
                        viewMainModel.startAutoLocationTimer()
                    } else {
                        it.title = "开启10S自动定位"
                        viewMainModel.cancelAutoLocation()
                    }
                }

                R.id.personal_center_menu_rotate_over_look -> {
                    niMapController.mMapView.vtmMap.eventLayer.enableTilt(Constant.MapRotateEnable)
                    niMapController.mMapView.vtmMap.eventLayer.enableRotation(Constant.MapRotateEnable)
                    Constant.MapRotateEnable = !Constant.MapRotateEnable
                     if (Constant.MapRotateEnable) {
                        val mapPosition: MapPosition =
                            niMapController.mMapView.vtmMap.getMapPosition()
                        mapPosition.setBearing(0f) // 锁定角度，自动将地图旋转到正北方向
                        niMapController.mMapView.vtmMap.setMapPosition(mapPosition)
                        it.title = "开启地图旋转及视角"
                    } else {
                        it.title = "锁定地图旋转及视角"
                    }
                }
                R.id.personal_center_menu_marker -> {
                    niMapController.mMapView.vtmMap.eventLayer.enableTilt(Constant.MapRotateEnable)
                    Constant.MapMarkerCloseEnable = !Constant.MapMarkerCloseEnable
                    //增加开关控制
                    niMapController.markerHandle.setQsRecordMarkEnable(!Constant.MapMarkerCloseEnable)
                    if (Constant.MapMarkerCloseEnable) {
                        it.title = "显示Marker"
                    } else {
                        it.title = "隐藏Marker"
                    }
                }
                R.id.personal_center_menu_catch_all -> {
                    Constant.CATCH_ALL = !Constant.CATCH_ALL
                    if (Constant.CATCH_ALL) {
                        it.title = "关闭全要素捕捉"
                    } else {
                        it.title = "开启全要素捕捉"
                    }
                }
                R.id.personal_center_menu_test -> {
                    viewModel.readRealmData()
                    //116.25017070328308 40.061730653134696
                    // 定位到指定位置
                    niMapController.mMapView.vtmMap.animator()
//                        .animateTo(GeoPoint( 40.05108004733645, 116.29187746293708    ))
                        .animateTo(GeoPoint(40.5016054261786, 115.82381251427815))
                }

                R.id.personal_center_menu_open_all_layer -> {
                    MapParamUtils.setDataLayerEnum(DataLayerEnum.SHOW_ALL_LAYERS)
                    niMapController.layerManagerHandler.updateOMDBVectorTileLayer()
                    viewModel.realmOperateHelper.updateRealmDefaultInstance()
                }

                R.id.personal_center_menu_close_hide_layer -> {
                    MapParamUtils.setDataLayerEnum(DataLayerEnum.ONLY_ENABLE_LAYERS)
                    niMapController.layerManagerHandler.updateOMDBVectorTileLayer()
                    viewModel.realmOperateHelper.updateRealmDefaultInstance()
                }

                R.id.personal_center_menu_scan_qr_code -> {
                    //跳转二维码扫描界面
                    checkPermission()
                }

                R.id.personal_center_menu_scan_indoor_data -> {
                    indoorDataListener?.invoke(true)
                }
            }
            true
        }

        viewModel.liveDataMessage.observe(viewLifecycleOwner) {
            ToastUtils.showShort(it)
        }
        fileChooser.setCallbacks(this@PersonalCenterFragment)
        binding.root.menu.forEach {
            when (it.itemId) {
                R.id.personal_center_menu_open_auto_location -> {
                    if (Constant.AUTO_LOCATION) {
                        it.title = "关闭自动定位"
                    } else {
                        it.title = "开启10S自动定位"
                    }
                }
                R.id.personal_center_menu_rotate_over_look -> {
                    if (Constant.MapRotateEnable) {
                        it.title = "开启地图旋转及视角"
                    } else {
                        it.title = "锁定地图旋转及视角"
                    }
                }
                R.id.personal_center_menu_catch_all->{
                    if (Constant.CATCH_ALL) {
                        it.title = "关闭全要素捕捉"
                    } else {
                        it.title = "开启全要素捕捉"
                    }
                }
                R.id.personal_center_menu_marker -> {
                    if (Constant.MapMarkerCloseEnable) {
                        it.title = "显示Marker"
                    } else {
                        it.title = "隐藏Marker"
                    }
                }
            }
        }
    }

    private fun intentTOQRCode() {
        var intent = Intent(context, QrCodeActivity::class.java);
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun fsafStartActivityForResult(intent: Intent, requestCode: Int) {
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        fileChooser.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkPermission() {
        PermissionX.init(this)
            .permissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    //所有权限已经授权
                    Toast.makeText(context, "授权成功", Toast.LENGTH_LONG).show()
                    intentTOQRCode()
                } else {
                    Toast.makeText(context, "拒绝权限: $deniedList", Toast.LENGTH_LONG).show()
                }
            }
    }
}