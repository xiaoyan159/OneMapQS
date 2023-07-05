package com.navinfo.omqs.ui.fragment.signMoreInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentSignInfoBinding
import com.navinfo.omqs.ui.activity.map.MainViewModel
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.widget.SignUtil


class SignMoreInfoFragment : BaseFragment() {
    private var _binding: FragmentSignInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<MainViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context)
        //// 设置 RecyclerView 的固定大小，避免在滚动时重新计算视图大小和布局，提高性能
        binding.signInfoRecyclerview.setHasFixedSize(true)
        binding.signInfoRecyclerview.layoutManager = layoutManager
        viewModel.liveDataSignMoreInfo.observe(viewLifecycleOwner) {
            binding.signInfoTitle.text = it.name
            val drawable = resources.getDrawable(R.drawable.icon_main_moreinfo_text_left, null);
            drawable.setBounds(
                0,
                0,
                drawable.minimumWidth,
                drawable.minimumHeight
            );//必须设置图片大小，否则不显示
            binding.signInfoTitle.setCompoundDrawables(
                drawable, null, null, null
            )

            when (it.code) {
                //道路名
                2011 -> {
                    val adapter = RoadNameInfoAdapter()
                    binding.signInfoRecyclerview.adapter = adapter
                    adapter.refreshData(SignUtil.getRoadNameList(it))
                }
                //常规点限速
                4002->{
                    val adapter = ElectronicEyeInfoAdapter()
                    binding.signInfoRecyclerview.adapter = adapter
                    adapter.refreshData(SignUtil.getSpeedLimitMoreInfoText(it))
                }
                //条件点限速
                4003 -> {
                    val adapter = ElectronicEyeInfoAdapter()
                    binding.signInfoRecyclerview.adapter = adapter
                    adapter.refreshData(SignUtil.getConditionLimitMoreInfoText(it))
                }
                //电子眼
                4010
                -> {
                    val drawable = resources.getDrawable(R.drawable.icon_electronic_eye_left, null);
                    drawable.setBounds(
                        0,
                        0,
                        drawable.minimumWidth,
                        drawable.minimumHeight
                    );//必须设置图片大小，否则不显示
                    binding.signInfoTitle.setCompoundDrawables(
                        drawable, null, null, null
                    )
                    val adapter = ElectronicEyeInfoAdapter()
                    binding.signInfoRecyclerview.adapter = adapter
                    adapter.refreshData(SignUtil.getElectronicEyeMoreInfo(it))
                }

            }
        }

        binding.signInfoCancel.setOnClickListener {
            activity?.run {
                supportFragmentManager.beginTransaction().remove(this@SignMoreInfoFragment)
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}