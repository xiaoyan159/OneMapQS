package com.navinfo.omqs.ui.fragment.evaluationresult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentEvaluationResultBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EvaluationResultFragment : Fragment() {
    private var _binding: FragmentEvaluationResultBinding? = null

    private val binding get() = _binding!!
    private val viewModel by lazy { viewModels<EvaluationResultViewModel>().value }
    private var phenomenonFragmentAdapter: EvaluationResultAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvaluationResultBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //返回按钮
        binding.evaluationBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        //监听数据变化
        viewModel.classTypeListLiveData.observe(viewLifecycleOwner) {
            if (it == null || it.isEmpty()) {
                Toast.makeText(requireContext(), "还没有导入元数据！", Toast.LENGTH_SHORT).show()
            } else {
                binding.evaluationClassType.adapter =
                    ArrayAdapter(requireContext(), R.layout.text_item_select, it)
            }
        }
        //选择问题分类的回调
        binding.evaluationClassType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    viewModel.getProblemTypeList(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        /**
         * 监听联动选择的内容
         */
        viewModel.problemTypeListLiveData.observe(viewLifecycleOwner) {
            binding.evaluationClassTabLayout.let { tabLayout ->
                tabLayout.removeAllTabs()
                val fragmentList = mutableListOf<Fragment>()
                for (item in it) {
                    val tab = tabLayout.newTab()
                    tab.text = item
                    tabLayout.addTab(tab)
                    fragmentList.add(PhenomenonFragment(viewModel.currentClassType,item))
                }
                phenomenonFragmentAdapter =
                    activity?.let { a -> EvaluationResultAdapter(a, fragmentList) }
                binding.evaluationViewpager.adapter = phenomenonFragmentAdapter

                TabLayoutMediator(
                    binding.evaluationClassTabLayout,
                    binding.evaluationViewpager
                ) { tab, position ->
                    tab.text = it[position]
                }.attach()
                updateHeight(0)
            }

        }
        //获取数据
        viewModel.getClassTypeList()
        binding.evaluationViewpager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateHeight(position)
            }
        })
    }


    private fun updateHeight(position: Int) {
        phenomenonFragmentAdapter?.let {
            if (it.fragmentList.size > position) {
                val fragment: Fragment = it.fragmentList[position]
                if (fragment.view != null) {
                    val viewWidth = View.MeasureSpec.makeMeasureSpec(
                        fragment.requireView().width, View.MeasureSpec.EXACTLY
                    )
                    val viewHeight =
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    fragment.requireView().measure(viewWidth, viewHeight)
                    binding.evaluationViewpager.let { viewpager ->
                        if (viewpager.layoutParams.height != fragment.requireView().measuredHeight) {
                            //必须要用对象去接收，然后修改该对象再采用该对象，否则无法生效...
                            val layoutParams: ViewGroup.LayoutParams =
                                viewpager.layoutParams
                            layoutParams.height = fragment.requireView().measuredHeight
                            viewpager.layoutParams = layoutParams
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