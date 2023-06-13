package com.navinfo.omqs.ui.fragment.layermanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentController
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.blankj.utilcode.util.SPStaticUtils
import com.navinfo.omqs.Constant
import com.navinfo.omqs.databinding.FragmentEmptyBinding
import com.navinfo.omqs.databinding.FragmentLayerManagerBinding
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.fragment.offlinemap.OfflineMapCityListViewModel

class LayermanagerFragment : BaseFragment(){
    private var _binding: FragmentLayerManagerBinding? = null

    private val binding get() = _binding!!
    private val viewModel by viewModels<LayerManagerViewModel>()
//    private val viewModel by lazy { viewModels<EvaluationResultViewModel>().value}
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLayerManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = LayerManagerExpandableListAdapter(requireContext(), viewModel.getLayerConfigList())

        binding.elvLayerManager.setAdapter(adapter)
        // 默认显示第一个父项下的子类
        binding.elvLayerManager.expandGroup(0)
        binding.elvLayerManager.setGroupIndicator(null)
        binding.elvLayerManager.setOnGroupClickListener { expandableListView, view, groupPosition, l ->
            if (expandableListView.isGroupExpanded(groupPosition)) {
                binding.elvLayerManager.collapseGroup(groupPosition)
            } else {
                binding.elvLayerManager.expandGroup(groupPosition)
            }
        }

        binding.imgConfirm.setOnClickListener {
            viewModel.saveLayerConfigList(requireContext(),adapter.parentItems)
        }

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.tvTitle.text = "图层管理"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}