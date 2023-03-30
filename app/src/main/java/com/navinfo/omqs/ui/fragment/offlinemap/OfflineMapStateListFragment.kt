package com.navinfo.omqs.ui.fragment.offlinemap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.navinfo.omqs.databinding.FragmentOfflineMapBinding
import com.navinfo.omqs.databinding.FragmentOfflineMapStateListBinding


class OfflineMapStateListFragment : Fragment() {
    private var _binding: FragmentOfflineMapStateListBinding? = null

    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfflineMapStateListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.e("jingo","OfflineMapStateListFragment onDestroyView")
    }
}