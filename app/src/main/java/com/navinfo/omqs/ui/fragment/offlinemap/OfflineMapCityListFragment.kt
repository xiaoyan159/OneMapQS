package com.navinfo.omqs.ui.fragment.offlinemap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.navinfo.omqs.databinding.FragmentOfflineMapCityListBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 离线地图城市列表
 */
@AndroidEntryPoint
class OfflineMapCityListFragment : Fragment() {
    private var _binding: FragmentOfflineMapCityListBinding? = null
    private val viewModel by viewModels<OfflineMapCityListViewModel>()
    private val binding get() = _binding!!
    private val adapter: OfflineMapCityListAdapter by lazy { OfflineMapCityListAdapter() }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfflineMapCityListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(context)
        _binding!!.offlineMapCityListRecyclerview.layoutManager = layoutManager
        _binding!!.offlineMapCityListRecyclerview.adapter = adapter
        viewModel.cityListLiveData.observe(viewLifecycleOwner) {
            adapter.refreshData(it)
        }
        viewModel.getCityList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.e("jingo","OfflineMapCityListFragment onDestroyView")
    }
}