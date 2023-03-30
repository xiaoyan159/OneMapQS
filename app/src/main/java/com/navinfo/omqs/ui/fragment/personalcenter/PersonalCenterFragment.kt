package com.navinfo.omqs.ui.fragment.personalcenter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentPersonalCenterBinding

/**
 * 个人中心
 */
class PersonalCenterFragment : Fragment() {

    private var _binding: FragmentPersonalCenterBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalCenterBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("jingo", "NIMapController PersonalCenterFragment onViewCreated")
        binding.root.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.personal_center_menu_offline_map ->
                    findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            }
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}