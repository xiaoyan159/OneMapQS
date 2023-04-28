package com.navinfo.omqs.ui.fragment.layermanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.navinfo.omqs.databinding.FragmentEmptyBinding
import com.navinfo.omqs.databinding.FragmentLayerManagerBinding
import com.navinfo.omqs.ui.fragment.offlinemap.OfflineMapCityListViewModel

class LayermanagerFragment :Fragment(){
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}