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
        currentDestination?.let {
            //有右侧面板的时候
            currentDestinationLabel = it.label.toString()
            if (it.label == "右侧空页面") {
                (activity as MainActivity).setRightSwitchButtonVisibility(View.GONE)
                (activity as MainActivity).setTopMenuButtonVisibility(View.VISIBLE)
            } else if (it.label == "中间空页面") {
                (activity as MainActivity).setRightButtonsVisible(View.VISIBLE)
            }
        }

    }

    override fun onStop() {
        super.onStop()
        //没有有右侧面板的时候
        if (currentDestinationLabel == "右侧空页面") {
            (activity as MainActivity).setRightSwitchButtonVisibility(View.VISIBLE)
            (activity as MainActivity).setTopMenuButtonVisibility(View.GONE)
        } else if (currentDestinationLabel == "中间空页面") {
            (activity as MainActivity).setRightButtonsVisible(View.GONE)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}