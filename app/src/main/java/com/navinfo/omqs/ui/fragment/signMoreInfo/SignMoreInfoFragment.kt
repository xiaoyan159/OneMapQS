package com.navinfo.omqs.ui.fragment.signMoreInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.enum.DataCodeEnum
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.SignBean
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
                DataCodeEnum.OMDB_RD_LINK.code -> {
                    val adapter = RoadNameInfoAdapter()
                    binding.signInfoRecyclerview.adapter = adapter
                    adapter.refreshData(SignUtil.getRoadNameList(it))
                }
                //车道边界类型
                DataCodeEnum.OMDB_LANE_MARK_BOUNDARYTYPE.code -> {
                    val adapter = LaneBoundaryAdapter()
                    binding.signInfoRecyclerview.adapter = adapter
                    adapter.refreshData(SignUtil.getLaneBoundaryTypeInfo(it))
                }
                //可变点限速
                DataCodeEnum.OMDB_LINK_SPEEDLIMIT_VAR.code -> {
                    val adapter = TwoItemAdapter()
                    binding.signInfoRecyclerview.adapter = adapter
                    adapter.refreshData(SignUtil.getChangeLimitSpeedInfo(it))
                }
                //常规点限速
                DataCodeEnum.OMDB_SPEEDLIMIT.code -> {
                    val adapter = TwoItemAdapter()
                    binding.signInfoRecyclerview.adapter = adapter
                    adapter.refreshData(SignUtil.getSpeedLimitMoreInfoText(it))
                }
                //条件点限速
                DataCodeEnum.OMDB_SPEEDLIMIT_COND.code -> {
                    val adapter = TwoItemAdapter()
                    binding.signInfoRecyclerview.adapter = adapter
                    adapter.refreshData(SignUtil.getConditionLimitMoreInfoText(it))
                }
                //电子眼
                DataCodeEnum.OMDB_ELECTRONICEYE.code
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
                    val adapter = TwoItemAdapter()
                    binding.signInfoRecyclerview.adapter = adapter
                    adapter.refreshData(SignUtil.getElectronicEyeMoreInfo(it))
                }
                else -> {
                    val adapter = TwoItemAdapter()
                    binding.signInfoRecyclerview.adapter = adapter
                }
            }
        }

        binding.signInfoCancel.setOnClickListener {
            activity?.run {
                supportFragmentManager.beginTransaction().remove(this@SignMoreInfoFragment)
                    .commit()
            }
        }
        binding.signInfoTitle.setOnClickListener {
            activity?.run {
                val rightController = findNavController(R.id.main_activity_right_fragment)
                rightController.currentDestination?.let {
                    if (it.id == R.id.RightEmptyFragment) {
                        val bundle = Bundle()
                        val element = viewModel.liveDataSignMoreInfo.value
                        if (element != null) {
                            val signBean = SignBean(
                                iconId = SignUtil.getSignIcon(element),
                                iconText = SignUtil.getSignIconText(element),
                                linkId = element.properties[RenderEntity.Companion.LinkTable.linkPid]
                                    ?: "",
                                name = SignUtil.getSignNameText(element),
                                bottomRightText = SignUtil.getSignBottomRightText(element),
                                renderEntity = element,
                                isMoreInfo = SignUtil.isMoreInfo(element),
                                index = SignUtil.getRoadInfoIndex(element)
                            )
                            bundle.putParcelable("SignBean", signBean)
                            bundle.putBoolean("AutoSave", false)
                            rightController.navigate(R.id.EvaluationResultFragment, bundle)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}