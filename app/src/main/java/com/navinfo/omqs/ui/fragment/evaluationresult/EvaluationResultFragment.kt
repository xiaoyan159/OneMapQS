package com.navinfo.omqs.ui.fragment.evaluationresult

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.SignBean
import com.navinfo.omqs.databinding.FragmentEvaluationResultBinding
import com.navinfo.omqs.ui.activity.map.MainActivity
import com.navinfo.omqs.ui.dialog.FirstDialog
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class EvaluationResultFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentEvaluationResultBinding
    private var mCameraLauncher: ActivityResultLauncher<Intent>? = null

    /**
     * 和[PhenomenonFragment],[ProblemLinkFragment],[EvaluationResultFragment]共用同一个viewModel
     */
    private val viewModel by shareViewModels<EvaluationResultViewModel>("QsRecode")

    private val pictureAdapter by lazy {
        PictureAdapter()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 处理相机返回的结果
                val extras = result.data!!.extras
                val imageBitmap: Bitmap? = extras!!["data"] as Bitmap?
                // 在这里处理图片数据
                if (imageBitmap != null)
                    viewModel.savePhoto(imageBitmap)
            }
        }
    }

    //    private val args:EmptyFragmentArgs by navArgs()
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
        //// 设置 RecyclerView 的固定大小，避免在滚动时重新计算视图大小和布局，提高性能
        binding.evaluationVoiceRecyclerview.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        binding.evaluationVoiceRecyclerview.layoutManager = layoutManager
        /**
         * 监听左侧栏的点击事件
         */
        val adapter = SoundtListAdapter { _, _ ->

        }

        binding.evaluationVoiceRecyclerview.adapter = adapter

        //返回按钮点击
        binding.evaluationBar.setOnClickListener {
            val mDialog = FirstDialog(context)
            mDialog.setTitle("提示？")
            mDialog.setMessage("是否退出，请确认！")
            mDialog.setPositiveButton(
                "确定"
            ) { _, _ ->
                mDialog.dismiss()
                onBackPressed()
            }
            mDialog.setNegativeButton("取消", null)
            mDialog.show()
        }

        //测距按钮
        binding.evaluationBarMeasuring.setOnClickListener {
            (activity as MainActivity).measuringToolOn()
        }
        //保存事件
        binding.evaluationBarSave.setOnClickListener {
            viewModel.saveData()
        }

        //删除事件
        binding.evaluationBarDelete.setOnClickListener {
            viewModel.deleteData(requireContext())
        }
        /**
         * 照片view
         */
        binding.evaluationPictureViewpager.adapter = pictureAdapter
        val list = mutableListOf("1", "2", "3")
        pictureAdapter.refreshData(list)

        //照片左右选择键点击监听
        binding.evaluationPictureLeft.setOnClickListener(this)
        binding.evaluationPictureRight.setOnClickListener(this)
        binding.evaluationCamera.setOnClickListener(this)
        //设置照片偏移量
        val viewPager = binding.evaluationPictureViewpager
        val vto = viewPager.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val width = viewPager.width
                // 处理View宽度
                // 在回调完成后，需要将监听器从View树中移除，以避免重复调用
                viewPager.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val recyclerView = viewPager.getChildAt(0) as RecyclerView

                recyclerView.setPadding(0, 0, width / 2 - 30, 0)
                recyclerView.clipToPadding = false
            }
        })

        binding.evaluationVoice.setOnTouchListener { _, event ->
            Log.e("qj", event?.action.toString())
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    voiceOnTouchStart()//Do Something
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                    voiceOnTouchStop()//Do Something
                }
            }
            true
        }


        /**
         * 读取元数据
         */
        var id = ""
        var signBean: SignBean? = null
        var autoSave = false
        var filePath = ""
        arguments?.let {
            id = it.getString("QsId", "")
            filePath = it.getString("filePath", "")
            try {
                signBean = it.getParcelable("SignBean")
                autoSave = it.getBoolean("AutoSave")
            } catch (_: java.lang.Exception) {
            }
        }

        if (id.isEmpty()) {
            viewModel.initNewData(signBean, filePath)
            //增加监听，联动列表自动保存
            viewModel.liveDataRightTypeList.observe(viewLifecycleOwner) {
                if (autoSave) {
                    viewModel.saveData()
                }
            }
        } else {
            viewModel.initData(id)
        }

        viewModel.listDataChatMsgEntityList.observe(viewLifecycleOwner) {
            adapter.refreshData(it)
        }

        //监听是否退出当前页面
        viewModel.liveDataFinish.observe(viewLifecycleOwner) {
            onBackPressed()
        }
        //监听要提示的信息
        viewModel.liveDataToastMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
        viewModel.liveDataQsRecordBean.observe(viewLifecycleOwner){
            binding.evaluationId.text = it.id
        }

    }

    override fun onDestroyView() {
        activity?.run {
            findNavController(R.id.main_activity_middle_fragment).navigateUp()
            (this as MainActivity).measuringToolOff()
        }
        super.onDestroyView()
    }

    /**
     * 处理点击事件
     */
    override fun onClick(v: View?) {
        v?.let {
            when (v.id) {
                //照片左侧按钮
                R.id.evaluation_picture_left -> {
                    val currentItem = binding.evaluationPictureViewpager.currentItem
                    if (currentItem > 0) {
                        binding.evaluationPictureViewpager.currentItem = currentItem - 1
                    } else {
                        return
                    }

                }
                //照片右侧按钮
                R.id.evaluation_picture_right -> {
                    val currentItem = binding.evaluationPictureViewpager.currentItem
                    if (currentItem < pictureAdapter.data.size - 1) {
                        binding.evaluationPictureViewpager.currentItem = currentItem + 1
                    } else {
                        return
                    }
                }
                //上三项，打开面板
                R.id.evaluation_class_type, R.id.evaluation_problem_type, R.id.evaluation_phenomenon -> {
                    activity?.run {
                        val controller = findNavController(R.id.main_activity_middle_fragment)
                        controller.currentDestination?.let {
                            //如果之前页面是空fragment，直接打开面板
                            if (it.id == R.id.MiddleEmptyFragment) {
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
                    activity?.run {
                        val controller = findNavController(R.id.main_activity_middle_fragment)
                        controller.currentDestination?.let {
                            //如果之前页面是空fragment，直接打开面板
                            if (it.id == R.id.MiddleEmptyFragment) {
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
                R.id.evaluation_camera -> {
                    takePhoto()
                }
                else -> {
                    return
                }
            }
        }
    }

    private fun voiceOnTouchStart() {
        viewModel.startSoundMetter(requireActivity(), binding.evaluationVoice)
    }

    private fun voiceOnTouchStop() {
        Log.e("qj", "voiceOnTouchStop====${Constant.IS_VIDEO_SPEED}")
        if (Constant.IS_VIDEO_SPEED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                viewModel.stopSoundMeter()
            }
        }
    }

    override fun onBackPressed(): Boolean {
        findNavController().navigateUp()
        return true
    }


    private fun takePhoto() {
        try {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
                mCameraLauncher!!.launch(takePictureIntent)
            }
        } catch (e: Exception) {
            Log.d("TTTT", e.toString())
        }
    }
}