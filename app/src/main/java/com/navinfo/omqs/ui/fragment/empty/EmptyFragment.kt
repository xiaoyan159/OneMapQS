package com.navinfo.omqs.ui.fragment.empty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentEmptyBinding
import com.navinfo.omqs.ui.activity.map.MainActivity

class EmptyFragment : Fragment() {
    private var _binding: FragmentEmptyBinding? = null
    private var currentDestinationLabel = ""
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

    override fun onStart() {
        super.onStart()
        val currentDestination = findNavController().currentDestination
        //有右侧面板的时候
        if (currentDestination?.label == "右侧空页面") {
            currentDestinationLabel = "右侧空页面"
            (activity as MainActivity).setRightSwitchButton(View.GONE)
        }
    }

    override fun onStop() {
        super.onStop()
        //没有有右侧面板的时候
        if (currentDestinationLabel == "右侧空页面") {
            (activity as MainActivity).setRightSwitchButton(View.VISIBLE)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}