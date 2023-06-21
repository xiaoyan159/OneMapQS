package com.navinfo.omqs.ui.fragment.sign

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.navinfo.omqs.databinding.FragmentSignRoadnameBinding
import com.navinfo.omqs.ui.activity.map.MainViewModel
import com.navinfo.omqs.ui.fragment.BaseFragment


class RoadNameInfoFragment : BaseFragment() {
    private var _binding: FragmentSignRoadnameBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<MainViewModel>()
    private val adapter by lazy { RoadNameInfoAdapter() }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignRoadnameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context)
        //// 设置 RecyclerView 的固定大小，避免在滚动时重新计算视图大小和布局，提高性能
        binding.roadnameRecyclerview.setHasFixedSize(true)
        binding.roadnameRecyclerview.layoutManager = layoutManager
        binding.roadnameRecyclerview.adapter = adapter
        viewModel.liveDataRoadName.observe(viewLifecycleOwner) {
            if (it != null && it.isNotEmpty()) {
                adapter.refreshData(it)
            } else {
                activity?.run {
                    supportFragmentManager.beginTransaction().remove(this@RoadNameInfoFragment)
                        .commit()
                }
            }
        }
        binding.roadnameCancel.setOnClickListener {
            activity?.run {
                supportFragmentManager.beginTransaction().remove(this@RoadNameInfoFragment)
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}