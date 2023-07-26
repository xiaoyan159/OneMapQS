package com.navinfo.omqs.ui.fragment.tasklink

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.navinfo.collect.library.map.NIMapController
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentTaskLinkBinding
import com.navinfo.omqs.databinding.FragmentTaskLinkMiddleBinding
import com.navinfo.omqs.ui.activity.map.MainActivity
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.fragment.evaluationresult.LeftAdapter
import com.navinfo.omqs.ui.other.shareViewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TaskLinkMiddleFragment : BaseFragment(), View.OnClickListener {
    private var _binding: FragmentTaskLinkMiddleBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var mapController: NIMapController

    private val viewModel by shareViewModels<TaskLinkViewModel>("taskLink")

    /**
     * 监听左侧栏的点击事件
     */
    val adapter = TaskLinkMiddleAdapter { _, item ->
        viewModel.setAdapterSelectValve(item)
        if (activity != null) {
            requireActivity().findNavController(R.id.main_activity_middle_fragment).navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskLinkMiddleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.taskLinkMiddleRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.taskLinkMiddleRecyclerview.adapter = adapter
        viewModel.liveDataLeftAdapterList.observe(viewLifecycleOwner) {
            adapter.refreshData(it)
        }
    }


    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onClick(v: View) {
        when (v) {
        }
    }

    override fun onBackPressed(): Boolean {
        findNavController().navigateUp()
        return true
    }
}