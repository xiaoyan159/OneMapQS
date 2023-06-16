package com.navinfo.omqs.ui.fragment.personalcenter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.UriUtils
import com.github.k1rakishou.fsaf.FileChooser
import com.github.k1rakishou.fsaf.callback.FSAFActivityCallbacks
import com.github.k1rakishou.fsaf.callback.FileChooserCallback
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentPersonalCenterBinding
import com.navinfo.omqs.db.ImportOMDBHelper
import com.navinfo.omqs.hilt.ImportOMDBHiltFactory
import com.navinfo.omqs.tools.CoroutineUtils
import com.navinfo.omqs.ui.fragment.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import org.oscim.core.GeoPoint
import javax.inject.Inject

/**
 * 个人中心
 */
@AndroidEntryPoint
class PersonalCenterFragment(private var backListener: (() -> Unit?)? = null) : BaseFragment(),
    FSAFActivityCallbacks {

    private var _binding: FragmentPersonalCenterBinding? = null
    private val binding get() = _binding!!
    private val fileChooser by lazy { FileChooser(requireContext()) }
    private val viewModel by lazy { viewModels<PersonalCenterViewModel>().value }

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
                R.id.personal_center_menu_test -> {
                    viewModel.readRealmData()
                    // 定位到指定位置
                    niMapController.mMapView.vtmMap.animator()
                        .animateTo(GeoPoint( 40.034842306317486, 116.31735963074652  ))
                }
                R.id.personal_center_menu_task_list -> {
                    findNavController().navigate(R.id.TaskManagerFragment)
                }
                R.id.personal_center_menu_qs_record_list -> {
                    findNavController().navigate(R.id.QsRecordListFragment)
                }
                R.id.personal_center_menu_layer_manager -> { // 图层管理
                    findNavController().navigate(R.id.QsLayerManagerFragment)
                }
            }
            true
        }

        viewModel.liveDataMessage.observe(viewLifecycleOwner) {
            ToastUtils.showShort(it)
        }

        fileChooser.setCallbacks(this@PersonalCenterFragment)
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
}