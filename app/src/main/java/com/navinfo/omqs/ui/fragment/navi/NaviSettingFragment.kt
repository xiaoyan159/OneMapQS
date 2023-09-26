package com.navinfo.omqs.ui.fragment.navi

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.navinfo.omqs.Constant
import com.navinfo.omqs.databinding.FragmentNaviSettingBinding
import com.navinfo.omqs.ui.fragment.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NaviSettingFragment(private var backListener: (() -> Unit?)? = null) : BaseFragment() {
    private var _binding: FragmentNaviSettingBinding? = null

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val binding get() = _binding!!
    private val viewModel by viewModels<NaviSettingViewModel>()

    //    private val viewModel by lazy { viewModels<EvaluationResultViewModel>().value}
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNaviSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.offCount.setValue(sharedPreferences.getInt(Constant.NAVI_DEVIATION_COUNT,3))
        binding.offDistance.setValue(sharedPreferences.getInt(Constant.NAVI_DEVIATION_DISTANCE,15))
        binding.tipsDistance.setValue(sharedPreferences.getInt(Constant.NAVI_FARTHEST_DISPLAY_DISTANCE,500))

        binding.imgConfirm.setOnClickListener{
            sharedPreferences.edit()
                .putInt(Constant.NAVI_DEVIATION_DISTANCE,binding.offDistance.getValue())
                .putInt(Constant.NAVI_DEVIATION_COUNT,binding.offCount.getValue())
                .putInt(Constant.NAVI_FARTHEST_DISPLAY_DISTANCE,binding.tipsDistance.getValue())
                .commit()
            backListener?.invoke()
        }

        binding.imgBack.setOnClickListener {
            backListener?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}