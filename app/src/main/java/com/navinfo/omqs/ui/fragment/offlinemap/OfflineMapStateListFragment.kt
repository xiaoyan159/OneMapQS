package com.navinfo.omqs.ui.fragment.offlinemap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.navinfo.omqs.databinding.FragmentOfflineMapStateListBinding
import com.navinfo.omqs.http.offlinemapdownload.OfflineMapDownloadManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 离线地图管理页面
 */
@AndroidEntryPoint
class OfflineMapStateListFragment : Fragment() {
    @Inject
    lateinit var downloadManager: OfflineMapDownloadManager
    private var _binding: FragmentOfflineMapStateListBinding? = null
    private val viewModel by viewModels<OfflineMapStateListViewModel>()
    private val binding get() = _binding!!

    private val adapter: OfflineMapCityListAdapter by lazy {
        OfflineMapCityListAdapter(
            downloadManager,
            requireContext()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfflineMapStateListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(context)
        //// 设置 RecyclerView 的固定大小，避免在滚动时重新计算视图大小和布局，提高性能
        binding.offlineMapCityStateListRecyclerview.setHasFixedSize(true)
        binding.offlineMapCityStateListRecyclerview.layoutManager = layoutManager
        binding.offlineMapCityStateListRecyclerview.adapter = adapter
        viewModel.cityListLiveData.observe(viewLifecycleOwner) {
            adapter.refreshData(it)
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.getCityList()
    }

}