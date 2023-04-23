package com.navinfo.omqs.ui.fragment.evaluationresult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentEvaluationResultBinding
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EvaluationResultFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentEvaluationResultBinding
    private val viewModel by shareViewModels<EvaluationResultViewModel>("QsRecode")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_evaluation_result, container, false)
        binding.fragment = this
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //监听是否退出当前页面
        viewModel.liveDataFinish.observe(viewLifecycleOwner) {
            onBackPressed()
        }
        //返回按钮点击
        binding.evaluationBar.setNavigationOnClickListener {
            onBackPressed()
        }
        //标题栏按钮
        binding.evaluationBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.save -> {
                    viewModel.saveData()
                    true
                }
                R.id.delete -> {
                    viewModel.deleteData()
                    true
                }
                else -> true
            }
        }
        /**
         * 读取元数据
         */
        if (arguments != null) {
            val id = requireArguments().getString("QsId")
            if (id != null) {
                viewModel.loadData(id)
            } else {
                viewModel.initNewData()
            }
        } else {
            viewModel.initNewData()
        }

//        //监听大分类数据变化
//        viewModel.liveDataClassTypeList.observe(viewLifecycleOwner) {
//            if (it == null || it.isEmpty()) {
//                Toast.makeText(requireContext(), "还没有导入元数据！", Toast.LENGTH_SHORT).show()
//            } else {
//                binding.evaluationClassType.adapter =
//                    ArrayAdapter(requireContext(), R.layout.text_item_select, it)
//            }
//        }
//
//        viewModel.liveDataProblemTypeList.observe(viewLifecycleOwner){
//            if (it == null || it.isEmpty()) {
//                Toast.makeText(requireContext(), "还没有导入元数据！", Toast.LENGTH_SHORT).show()
//            }else{
//                binding.evaluationProblemType.adapter =
//                    ArrayAdapter(requireContext(), R.layout.text_item_select, it)
//            }
//        }

//        //选择问题分类的回调
//        binding.evaluationClassType.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(
//                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
//                ) {
//                    viewModel.getProblemTypeList(position)
//                }
//
//                override fun onNothingSelected(parent: AdapterView<*>?) {}
//            }
//        /**
//         * 监听联动选择的内容
//         */
//        viewModel.problemTypeListLiveData.observe(viewLifecycleOwner) {
//            binding.evaluationClassTabLayout.let { tabLayout ->
//                tabLayout.removeAllTabs()
//                val fragmentList = mutableListOf<Fragment>()
//                for (item in it) {
//                    val tab = tabLayout.newTab()
//                    tab.text = item
//                    tabLayout.addTab(tab)
//                    fragmentList.add(PhenomenonFragment(viewModel.currentClassType, item))
//                }
//                phenomenonFragmentAdapter =
//                    activity?.let { a -> EvaluationResultAdapter(a, fragmentList) }
//                binding.evaluationViewpager.adapter = phenomenonFragmentAdapter
//
//                TabLayoutMediator(
//                    binding.evaluationClassTabLayout,
//                    binding.evaluationViewpager
//                ) { tab, position ->
//                    tab.text = it[position]
//                }.attach()
//                updateHeight(0)
//            }
//
//        }

//        binding.evaluationViewpager.registerOnPageChangeCallback(object :
//            ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                super.onPageSelected(position)
//                updateHeight(position)
//            }
//        })
    }


//    private fun updateHeight(position: Int) {
//        phenomenonFragmentAdapter?.let {
//            if (it.fragmentList.size > position) {
//                val fragment: Fragment = it.fragmentList[position]
//                if (fragment.view != null) {
//                    val viewWidth = View.MeasureSpec.makeMeasureSpec(
//                        fragment.requireView().width, View.MeasureSpec.EXACTLY
//                    )
//                    val viewHeight =
//                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//                    fragment.requireView().measure(viewWidth, viewHeight)
//                    binding.evaluationViewpager.let { viewpager ->
//                        if (viewpager.layoutParams.height != fragment.requireView().measuredHeight) {
//                            //必须要用对象去接收，然后修改该对象再采用该对象，否则无法生效...
//                            val layoutParams: ViewGroup.LayoutParams =
//                                viewpager.layoutParams
//                            layoutParams.height = fragment.requireView().measuredHeight
//                            viewpager.layoutParams = layoutParams
//                        }
//                    }
//
//                }
//            }
//        }
//
//    }


    override fun onDestroyView() {
        activity?.apply {
            findNavController(R.id.main_activity_middle_fragment).navigateUp()
        }
        super.onDestroyView()
    }

    /**
     * 处理点击事件
     */
    override fun onClick(v: View?) {
        v?.let {
            when (v.id) {
                //上三项，打开面板
                R.id.evaluation_class_type, R.id.evaluation_problem_type, R.id.evaluation_phenomenon -> {
                    activity?.apply {
                        val controller = findNavController(R.id.main_activity_middle_fragment)
                        controller.currentDestination?.let {
                            //如果之前页面是空fragment，直接打开面板
                            if (it.id == R.id.EmptyFragment) {
                                findNavController(
                                    R.id.main_activity_middle_fragment
                                ).navigate(R.id.PhenomenonFragment)
                            } else if (it.id != R.id.PhenomenonFragment) {//不是空fragment，先弹出之前的fragment
                                findNavController(
                                    R.id.main_activity_middle_fragment
                                ).navigate(
                                    R.id.PhenomenonFragment,
                                    null,
                                    NavOptions.Builder()
                                        .setPopUpTo(it.id, true).build()
                                )
                            }
                        }
                    }
                }
                //下两项，打开面板
                R.id.evaluation_link, R.id.evaluation_cause -> {
                    activity?.apply {
                        val controller = findNavController(R.id.main_activity_middle_fragment)
                        controller.currentDestination?.let {
                            //如果之前页面是空fragment，直接打开面板
                            if (it.id == R.id.EmptyFragment) {
                                findNavController(
                                    R.id.main_activity_middle_fragment
                                ).navigate(R.id.ProblemLinkFragment)
                            } else if (it.id != R.id.ProblemLinkFragment) {//不是空fragment，先弹出之前的fragment
                                findNavController(
                                    R.id.main_activity_middle_fragment
                                ).navigate(
                                    R.id.ProblemLinkFragment,
                                    null,
                                    NavOptions.Builder()
                                        .setPopUpTo(it.id, true).build()
                                )
                            }
                        }

                    }
                }
                else -> {}
            }
        }
    }
}