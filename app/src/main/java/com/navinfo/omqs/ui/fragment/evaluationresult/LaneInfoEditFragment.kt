package com.navinfo.omqs.ui.fragment.evaluationresult

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentLineInfoEditBinding
import com.navinfo.omqs.ui.activity.map.LaneInfoItem
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels

class LaneInfoEditFragment : BaseFragment() {
    private var _binding: FragmentLineInfoEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel by shareViewModels<EvaluationResultViewModel>("QsRecode")

    /**
     * 车道类型
     */
    private var laneType = 0
    private var selectView: ImageView? = null
    private lateinit var laneInfoItemsAdapter: LaneInfoItemsAdapter
    private lateinit var laneInfoItemsAdapter2: LaneInfoItems2Adapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLineInfoEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.laneInfoList.observe(viewLifecycleOwner){
            initLaneInfo(it)
            viewModel.laneInfoList.removeObservers(viewLifecycleOwner)
        }
        initFLowLayout()

        binding.laneInfoBackspace.setOnClickListener {
            if (binding.laneInfoTopContainer.childCount < 4) {
                binding.laneInfoTopContainer.removeAllViews()
            } else {
                binding.laneInfoTopContainer.removeViewAt(binding.laneInfoTopContainer.childCount - 1)
                val view =
                    binding.laneInfoTopContainer.getChildAt(binding.laneInfoTopContainer.childCount - 1)
                binding.laneInfoTopContainer.removeView(view)
                if (view == selectView) {
                    selectView = null
                }
            }
            viewModel.backspaceLaneInfo()
        }
        binding.laneInfoRadio1.setOnClickListener {
            laneType = 0
            laneInfoItemsAdapter.setType(laneType)
        }
        binding.laneInfoRadio2.setOnClickListener {
            laneType = 1
            laneInfoItemsAdapter.setType(laneType)
        }
        binding.laneInfoRadio3.setOnClickListener {
            laneType = 2
            laneInfoItemsAdapter.setType(laneType)
        }

    }

    private fun initFLowLayout() {

        val itemList: MutableList<Int> = mutableListOf()

        itemList.add(R.drawable.laneinfo_1)
        itemList.add(R.drawable.laneinfo_2)
        itemList.add(R.drawable.laneinfo_3)
        itemList.add(R.drawable.laneinfo_5)
        itemList.add(R.drawable.laneinfo_6)
        itemList.add(R.drawable.laneinfo_4)
        itemList.add(R.drawable.laneinfo_7)
        itemList.add(R.drawable.laneinfo_1_2)
        itemList.add(R.drawable.laneinfo_1_5)
        itemList.add(R.drawable.laneinfo_2_5)
        itemList.add(R.drawable.laneinfo_2_6)
        itemList.add(R.drawable.laneinfo_1_3)
        itemList.add(R.drawable.laneinfo_1_6)
        itemList.add(R.drawable.laneinfo_3_5)
        itemList.add(R.drawable.laneinfo_3_6)
        itemList.add(R.drawable.laneinfo_2_3)
        itemList.add(R.drawable.laneinfo_5_6)
        itemList.add(R.drawable.laneinfo_1_4)
        itemList.add(R.drawable.laneinfo_4_5)
        itemList.add(R.drawable.laneinfo_2_4)
        itemList.add(R.drawable.laneinfo_3_4)
        itemList.add(R.drawable.laneinfo_4_6)
        itemList.add(R.drawable.laneinfo_1_7)
        itemList.add(R.drawable.laneinfo_1_2_3)
        itemList.add(R.drawable.laneinfo_1_2_4)
        itemList.add(R.drawable.laneinfo_1_2_5)
        itemList.add(R.drawable.laneinfo_1_2_6)
        itemList.add(R.drawable.laneinfo_1_3_4)
        itemList.add(R.drawable.laneinfo_1_3_5)
        itemList.add(R.drawable.laneinfo_1_3_6)
        itemList.add(R.drawable.laneinfo_2_3_4)
        itemList.add(R.drawable.laneinfo_0)

        laneInfoItemsAdapter = LaneInfoItemsAdapter(itemList)

        binding.laneInfoGridview.adapter = laneInfoItemsAdapter
        binding.laneInfoGridview.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                val resId = laneInfoItemsAdapter.getItem(position) as Int
                //如果选中了一个view
                if (selectView != null) {

                    val drawable = requireContext().getDrawable(resId)
                    val color = when (laneType) {
                        1 -> requireContext().resources.getColor(R.color.lane_info_1)
                        2 -> requireContext().resources.getColor(R.color.lane_info_2)
                        else -> requireContext().resources.getColor(R.color.white)
                    }
                    // 创建 PorterDuffColorFilter 对象
                    val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
                    // 将 PorterDuffColorFilter 设置给 Drawable
                    drawable!!.colorFilter = colorFilter
                    selectView!!.scaleType = ImageView.ScaleType.FIT_XY
                    selectView!!.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                    selectView!!.setImageDrawable(drawable)
                    viewModel.updateLaneInfo(selectView!!.tag as Int, resId, laneType)
                } else {
                    //如果一条车道都没有，左侧先加一条虚线
                    if (binding.laneInfoTopContainer.childCount == 0) {
                        val lineViewS = View(context)
                        lineViewS.layoutParams = ViewGroup.LayoutParams(24, 110)
                        lineViewS.background =
                            requireContext().getDrawable(R.drawable.shape_vertical_dashed_line)
                        binding.laneInfoTopContainer.addView(lineViewS, lineViewS.layoutParams)
                    }

                    val imageView = ImageView(context)
                    val drawable =
                        requireContext().getDrawable(laneInfoItemsAdapter.getItem(position) as Int)
                    val color = when (laneType) {
                        1 -> requireContext().resources.getColor(R.color.lane_info_1)
                        2 -> requireContext().resources.getColor(R.color.lane_info_2)
                        else -> requireContext().resources.getColor(R.color.white)
                    }
                    // 创建 PorterDuffColorFilter 对象
                    val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
                    // 将 PorterDuffColorFilter 设置给 Drawable
                    drawable!!.colorFilter = colorFilter
                    imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                    imageView.layoutParams = ViewGroup.LayoutParams(45, 100)
                    imageView.scaleType = ImageView.ScaleType.FIT_XY
                    imageView.setImageDrawable(drawable)
                    imageView.tag = viewModel.addLaneInfo(resId, laneType)
                    binding.laneInfoTopContainer.addView(imageView)
                    imageView.setOnClickListener {
                        selectView = if (selectView == it) {
                            selectView!!.setBackgroundColor(requireContext().resources.getColor(R.color.gray))
                            null
                        } else {
                            if (selectView != null) {
                                selectView!!.setBackgroundColor(
                                    requireContext().resources.getColor(
                                        R.color.gray
                                    )
                                )
                            }
                            imageView.setBackgroundColor(requireContext().resources.getColor(R.color.lane_info_0))
                            it as ImageView
                        }
                    }

                    //右侧加虚线
                    val lineViewE = View(context)
                    lineViewE.layoutParams = ViewGroup.LayoutParams(24, 110)
                    lineViewE.background =
                        requireContext().getDrawable(R.drawable.shape_vertical_dashed_line)
                    binding.laneInfoTopContainer.addView(lineViewE)
                }
            }


        val itemList2: MutableList<List<Int>> = mutableListOf()
        itemList2.add(listOf(R.drawable.laneinfo_2, R.drawable.laneinfo_1, R.drawable.laneinfo_3))
        itemList2.add(listOf(R.drawable.laneinfo_1_2, R.drawable.laneinfo_1, R.drawable.laneinfo_1_3))
        itemList2.add(listOf(R.drawable.laneinfo_2, R.drawable.laneinfo_1, R.drawable.laneinfo_1_3))
        itemList2.add(listOf(R.drawable.laneinfo_2_4, R.drawable.laneinfo_1, R.drawable.laneinfo_3))
        itemList2.add(listOf(R.drawable.laneinfo_1_2, R.drawable.laneinfo_1, R.drawable.laneinfo_3))
        itemList2.add(listOf(R.drawable.laneinfo_2_4, R.drawable.laneinfo_1, R.drawable.laneinfo_1_3))

        itemList2.add(listOf(R.drawable.laneinfo_2, R.drawable.laneinfo_1, R.drawable.laneinfo_1, R.drawable.laneinfo_3))
        itemList2.add(listOf(R.drawable.laneinfo_1_2, R.drawable.laneinfo_1, R.drawable.laneinfo_1, R.drawable.laneinfo_1_3))
        itemList2.add(listOf(R.drawable.laneinfo_2, R.drawable.laneinfo_1, R.drawable.laneinfo_1, R.drawable.laneinfo_1_3))
        itemList2.add(listOf(R.drawable.laneinfo_2_4, R.drawable.laneinfo_1, R.drawable.laneinfo_1, R.drawable.laneinfo_3))
        itemList2.add(listOf(R.drawable.laneinfo_1_2, R.drawable.laneinfo_1, R.drawable.laneinfo_1, R.drawable.laneinfo_3))
        itemList2.add(listOf(R.drawable.laneinfo_2_4, R.drawable.laneinfo_1, R.drawable.laneinfo_1, R.drawable.laneinfo_1_3))

        laneInfoItemsAdapter2 = LaneInfoItems2Adapter(itemList2)

        binding.laneInfoGridview2.adapter = laneInfoItemsAdapter2
        binding.laneInfoGridview2.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                val listIds = laneInfoItemsAdapter2.getItem(position) as List<Int>
                //如果选中了一个view
                if (selectView != null) {
                    selectView = null
                }
                binding.laneInfoTopContainer.removeAllViews()
                viewModel.removeAllLaneInfo()
                for (resId in listIds) {
                    val lineViewS = View(context)
                    lineViewS.layoutParams = ViewGroup.LayoutParams(24, 110)
                    lineViewS.background =
                        requireContext().getDrawable(R.drawable.shape_vertical_dashed_line)
                    binding.laneInfoTopContainer.addView(lineViewS, lineViewS.layoutParams)
                    val imageView = ImageView(context)
                    val drawable =
                        requireContext().getDrawable(resId)
                    val color = when (laneType) {
                        1 -> requireContext().resources.getColor(R.color.lane_info_1)
                        2 -> requireContext().resources.getColor(R.color.lane_info_2)
                        else -> requireContext().resources.getColor(R.color.white)
                    }
                    // 创建 PorterDuffColorFilter 对象
                    val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
                    // 将 PorterDuffColorFilter 设置给 Drawable
                    drawable!!.colorFilter = colorFilter
                    imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                    imageView.layoutParams = ViewGroup.LayoutParams(45, 100)
                    imageView.scaleType = ImageView.ScaleType.FIT_XY
                    imageView.setImageDrawable(drawable)
                    imageView.tag = viewModel.addLaneInfo(resId, laneType)
                    binding.laneInfoTopContainer.addView(imageView)
                    imageView.setOnClickListener {
                        selectView = if (selectView == it) {
                            selectView!!.setBackgroundColor(requireContext().resources.getColor(R.color.gray))
                            null
                        } else {
                            if (selectView != null) {
                                selectView!!.setBackgroundColor(
                                    requireContext().resources.getColor(
                                        R.color.gray
                                    )
                                )
                            }
                            imageView.setBackgroundColor(requireContext().resources.getColor(R.color.lane_info_0))
                            it as ImageView
                        }
                    }
                }

                //右侧加虚线
                val lineViewE = View(context)
                lineViewE.layoutParams = ViewGroup.LayoutParams(24, 110)
                lineViewE.background =
                    requireContext().getDrawable(R.drawable.shape_vertical_dashed_line)
                binding.laneInfoTopContainer.addView(lineViewE)
            }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 初始化车道信息
     */
    private fun initLaneInfo(list:MutableList<LaneInfoItem>) {
        val container = binding.laneInfoTopContainer
        container.removeAllViews()
        val lineViewS = View(context)
        lineViewS.layoutParams = ViewGroup.LayoutParams(24, 110)
        lineViewS.background =
            requireContext().getDrawable(R.drawable.shape_vertical_dashed_line)
        container.addView(lineViewS, lineViewS.layoutParams)
        for (i in list.indices) {
            val laneInfo = list[i]
            val imageView = ImageView(context)
            val drawable = requireContext().getDrawable(laneInfo.id)
            val color = when (laneInfo.type) {
                1 -> requireContext().resources.getColor(R.color.lane_info_1)
                2 -> requireContext().resources.getColor(R.color.lane_info_2)
                else -> requireContext().resources.getColor(R.color.white)
            }
            // 创建 PorterDuffColorFilter 对象
            val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            // 将 PorterDuffColorFilter 设置给 Drawable
            drawable!!.colorFilter = colorFilter
            // 将 Drawable 设置给 ImageView
            imageView.scaleType = ImageView.ScaleType.FIT_XY
            imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            imageView.setImageDrawable(drawable)
            // 将 ImageView 的颜色设置为红色
            imageView.layoutParams = ViewGroup.LayoutParams(45, 100)
            container.addView(imageView, imageView.layoutParams)
            if (i < list.size - 1) {
                val lineView = View(context)
                lineView.layoutParams = ViewGroup.LayoutParams(24, 110)
                lineView.background =
                    requireContext().getDrawable(R.drawable.shape_vertical_dashed_line)
                container.addView(lineView, lineView.layoutParams)
            }
            imageView.tag = i
            imageView.setOnClickListener {
                selectView = if (selectView == it) {
                    selectView!!.setBackgroundColor(requireContext().resources.getColor(R.color.gray))
                    null
                } else {
                    if (selectView != null) {
                        selectView!!.setBackgroundColor(requireContext().resources.getColor(R.color.gray))
                    }
                    imageView.setBackgroundColor(requireContext().resources.getColor(R.color.lane_info_0))
                    it as ImageView
                }
            }
        }
        val lineViewE = View(context)
        lineViewE.layoutParams = ViewGroup.LayoutParams(24, 110)
        lineViewE.background =
            requireContext().getDrawable(R.drawable.shape_vertical_dashed_line)
        container.addView(lineViewE, lineViewE.layoutParams)
    }
}