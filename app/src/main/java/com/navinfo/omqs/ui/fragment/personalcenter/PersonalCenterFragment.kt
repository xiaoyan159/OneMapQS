package com.navinfo.omqs.ui.fragment.personalcenter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.blankj.utilcode.util.UriUtils
import com.github.k1rakishou.fsaf.FileChooser
import com.github.k1rakishou.fsaf.callback.FSAFActivityCallbacks
import com.github.k1rakishou.fsaf.callback.FileChooserCallback
import com.navinfo.collect.library.data.RealmUtils
import com.navinfo.collect.library.data.entity.OMDBEntity
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentPersonalCenterBinding
import com.navinfo.omqs.db.ImportOMDBHelper
import com.navinfo.omqs.hilt.ImportOMDBHiltFactory
import com.navinfo.omqs.tools.CoroutineUtils
import com.navinfo.omqs.ui.activity.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import io.realm.Realm
import io.realm.RealmDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * 个人中心
 */
@AndroidEntryPoint
class PersonalCenterFragment : Fragment(), FSAFActivityCallbacks {

    private var _binding: FragmentPersonalCenterBinding? = null
    private val binding get() = _binding!!
    private val fileChooser by lazy { FileChooser(requireContext()) }
    private val viewModel by lazy { viewModels<PersonalCenterViewModel>().value }
    @Inject
    lateinit var importOMDBHiltFactory: ImportOMDBHiltFactory


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
                    findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
                R.id.personal_center_menu_obtain_data -> { // 生成数据，根据sqlite文件生成对应的zip文件
                    fileChooser.openChooseFileDialog(object: FileChooserCallback() {
                        override fun onCancel(reason: String) {
                        }

                        override fun onResult(uri: Uri) {
                            val file = UriUtils.uri2File(uri)
                            // 开始导入数据
                            // 656e6372797000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
                            val job = CoroutineUtils.launchWithLoading(requireContext(), loadingMessage = "生成数据...") {
                                val importOMDBHelper: ImportOMDBHelper = importOMDBHiltFactory.obtainImportOMDBHelper(requireContext(), file, File(file.parentFile, "config.json"))
                                viewModel.obtainOMDBZipData(importOMDBHelper)
                            }
                        }
                    })
                }
                R.id.personal_center_menu_import_data -> { // 导入zip数据
                    fileChooser.openChooseFileDialog(object: FileChooserCallback() {
                        override fun onCancel(reason: String) {
                        }

                        override fun onResult(uri: Uri) {
                            val file = UriUtils.uri2File(uri)
                            // 开始导入数据
                            CoroutineUtils.launchWithLoading(requireContext(), loadingMessage = "导入数据...") {
                                val importOMDBHelper: ImportOMDBHelper = importOMDBHiltFactory.obtainImportOMDBHelper(requireContext(), file, File(file.parentFile, "config.json"))
                                viewModel.importOMDBData(importOMDBHelper)
                            }
                        }
                    })
                }
                R.id.personal_center_menu_import_yuan_data->{
                    // 用户选中导入数据，打开文件选择器，用户选择导入的数据文件目录
                    fileChooser.openChooseFileDialog(object: FileChooserCallback() {
                        override fun onCancel(reason: String) {
                        }

                        override fun onResult(uri: Uri) {
                            viewModel.importScProblemData(uri)

                        }
                    })
                }
                R.id.personal_center_menu_test -> {
                    viewModel.readRealmData()
                }
            }
            true
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