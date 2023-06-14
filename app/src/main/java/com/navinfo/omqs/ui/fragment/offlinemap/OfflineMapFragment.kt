package com.navinfo.omqs.ui.fragment.offlinemap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.navinfo.omqs.databinding.FragmentOfflineMapBinding
import com.navinfo.omqs.ui.fragment.BaseFragment

/**
 * 离线地图总页面
 */
class OfflineMapFragment(private var backListener: (() -> Unit?)? = null) :
    BaseFragment() {

    private var _binding: FragmentOfflineMapBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfflineMapBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //禁止滑动，因为页面在抽屉里，和抽屉的滑动有冲突
        binding.offlineMapViewpager.isUserInputEnabled = false
        //创建viewpager2的适配器
        binding.offlineMapViewpager.adapter = activity?.let { OfflineMapAdapter(it) }
        //绑定viewpager2与tabLayout
        TabLayoutMediator(
            binding.offlineMapTabLayout,
            binding.offlineMapViewpager
        ) { tab, position ->
            when (position) {
                0 -> tab.text = "下载管理"
                1 -> tab.text = "城市列表"
            }
        }.attach()

        //处理返回按钮
        binding.offlineMapBack.setOnClickListener {
            backListener?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}