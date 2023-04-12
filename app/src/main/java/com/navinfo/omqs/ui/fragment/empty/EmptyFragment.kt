package com.navinfo.omqs.ui.fragment.empty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.navinfo.omqs.databinding.FragmentEmptyBinding

class EmptyFragment :Fragment(){
    private var _binding: FragmentEmptyBinding? = null

    private val binding get() = _binding!!
//    private val viewModel by lazy { viewModels<EvaluationResultViewModel>().value}
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmptyBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}