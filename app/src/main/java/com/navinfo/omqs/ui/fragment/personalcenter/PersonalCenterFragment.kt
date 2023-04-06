package com.navinfo.omqs.ui.fragment.personalcenter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.blankj.utilcode.util.UriUtils
import com.github.k1rakishou.fsaf.FileChooser
import com.github.k1rakishou.fsaf.callback.FSAFActivityCallbacks
import com.github.k1rakishou.fsaf.callback.FileChooserCallback
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentPersonalCenterBinding

/**
 * 个人中心
 */
class PersonalCenterFragment : Fragment(), FSAFActivityCallbacks {

    private var _binding: FragmentPersonalCenterBinding? = null
    private val binding get() = _binding!!
    private val fileChooser by lazy { FileChooser(requireContext()) }
    private val viewModel by lazy { viewModels<PersonalCenterViewModel>().value }


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
                R.id.personal_center_menu_import_data -> {
                    // 用户选中导入数据，打开文件选择器，用户选择导入的数据文件目录
                    fileChooser.openChooseFileDialog(object: FileChooserCallback() {
                        override fun onCancel(reason: String) {
                        }

                        override fun onResult(uri: Uri) {
                            val file = UriUtils.uri2File(uri)
                            // 开始导入数据
                            viewModel.importOmdbData(file)
                        }
                    })
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