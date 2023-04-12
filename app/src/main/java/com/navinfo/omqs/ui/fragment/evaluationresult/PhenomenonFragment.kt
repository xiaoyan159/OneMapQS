package com.navinfo.omqs.ui.fragment.evaluationresult

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.navinfo.omqs.databinding.FragmentPhenomenonBinding
import com.navinfo.omqs.db.RoomAppDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhenomenonFragment(private val classType: String, private val title: String) :
    Fragment() {
    @Inject
    lateinit var roomAppDatabase: RoomAppDatabase
    private var _binding: FragmentPhenomenonBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhenomenonBinding.inflate(inflater, container, false)
        Log.e("jingo", "PhenomenonFragment onCreateView ${hashCode()}")
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.phenomenonRecyclerview.setHasFixedSize(true)
        binding.phenomenonRecyclerview.layoutManager = LinearLayoutManager(context)
        val adapter = PhenomenonAdapter()
        binding.phenomenonRecyclerview.adapter = adapter
        lifecycleScope.launch {
            Log.e("jingo", "$classType $title ")
            val list = roomAppDatabase.getScProblemTypeDao().getPhenomenonList(classType, title)
            Log.e("jingo", "${list.toString()}")
            adapter.refreshData(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.e("jingo", "PhenomenonFragment onDestroyView ${hashCode()}")
    }

    override fun onResume() {
        super.onResume()
    }
}