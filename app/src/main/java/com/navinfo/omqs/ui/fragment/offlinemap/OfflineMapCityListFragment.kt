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
import com.navinfo.omqs.http.RetrofitNetworkServiceAPI
import com.navinfo.omqs.http.offlinemapdownload.OfflineMapDownloadManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 离线地图城市列表
 */
@AndroidEntryPoint
class OfflineMapCityListFragment : Fragment() {
    @Inject
    lateinit var downloadManager: OfflineMapDownloadManager
    private var _binding: FragmentOfflineMapCityListBinding? = null
    private val viewModel by viewModels<OfflineMapCityListViewModel>()
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
        _binding = FragmentOfflineMapCityListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(context)
        //// 设置 RecyclerView 的固定大小，避免在滚动时重新计算视图大小和布局，提高性能
        binding.offlineMapCityListRecyclerview.setHasFixedSize(true)
        binding.offlineMapCityListRecyclerview.layoutManager = layoutManager
        binding.offlineMapCityListRecyclerview.adapter = adapter
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

    override fun onResume() {
        super.onResume()
        viewModel.getCityList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}