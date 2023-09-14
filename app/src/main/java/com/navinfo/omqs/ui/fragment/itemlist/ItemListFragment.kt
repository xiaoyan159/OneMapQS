package com.navinfo.omqs.ui.fragment.itemlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.navinfo.collect.library.data.entity.NoteBean
import com.navinfo.omqs.databinding.FragmentItemListBinding
import com.navinfo.omqs.ui.activity.map.MainViewModel
import com.navinfo.omqs.ui.dialog.FirstDialog
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.widget.RecycleViewDivider
import dagger.hilt.android.AndroidEntryPoint
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@AndroidEntryPoint
class ItemListFragment(private var backListener: ((ItemListFragment) -> Unit?)? = null) :
    BaseFragment() {
    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<MainViewModel>()
    private val adapter by lazy {
        ItemAdapter { _,isLongClick, data ->
          if(!isLongClick){
              viewModel.showSignMoreInfo(data)
          }  else{
              val mDialog = FirstDialog(context)
              mDialog.setTitle("提示？")
              val gson = Gson()
              mDialog.setMessage(gson.toJson(data.properties))
              mDialog.setPositiveButton(
                  "确定"
              ) { dialog, _ ->
                  dialog.dismiss()
              }
              mDialog.setNegativeButton("取消", null)
              mDialog.setCancelVisibility(View.GONE)
              mDialog.show()
          }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.itemListRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.itemListRecyclerview.adapter = adapter
        binding.itemListRecyclerview.addItemDecoration(
            RecycleViewDivider(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        viewModel.liveDataItemList.observe(viewLifecycleOwner) {
            adapter.refreshData(it)
        }
        binding.taskBack.setOnClickListener {
            backListener?.invoke(this)
        }
    }
}
