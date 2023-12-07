package com.navinfo.omqs.ui.fragment.note

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentNoteBinding
import com.navinfo.omqs.ui.dialog.FirstDialog
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.fragment.evaluationresult.EvaluationResultFragment
import com.navinfo.omqs.ui.fragment.evaluationresult.EvaluationResultViewModel
import com.navinfo.omqs.ui.fragment.evaluationresult.PhenomenonFragment
import com.navinfo.omqs.ui.fragment.evaluationresult.PictureAdapter
import com.navinfo.omqs.ui.fragment.evaluationresult.ProblemLinkFragment
import com.navinfo.omqs.ui.fragment.evaluationresult.SoundtListAdapter
import com.navinfo.omqs.ui.other.shareViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NoteFragment : BaseFragment(), View.OnClickListener {
    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!

    private var mCameraLauncher: ActivityResultLauncher<Intent>? = null

    private val viewModel by shareViewModels<NoteViewModel>("note")

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        activity?.run {
            findNavController(
                R.id.main_activity_middle_fragment
            ).navigate(R.id.CanvasFragment)
        }

        //// 设置 RecyclerView 的固定大小，避免在滚动时重新计算视图大小和布局，提高性能
        binding.noteVoiceRecyclerview.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        binding.noteVoiceRecyclerview.layoutManager = layoutManager
        binding.sketchEraser.setOnClickListener(this)
        binding.sketchClear.setOnClickListener(this)
        binding.sketchForward.setOnClickListener(this)
        binding.sketchBack.setOnClickListener(this)
        binding.noteBarSave.setOnClickListener(this)
        binding.noteBarCancel.setOnClickListener(this)
        binding.noteBarDelete.setOnClickListener(this)
        binding.noteCamera.setOnClickListener(this)
        binding.noteDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                viewModel.noteBeanDescription = s.toString()
            }
        })

        viewModel.liveDataNoteBean.observe(viewLifecycleOwner) {
            binding.noteDescription.setText(it.description)
        }

        //监听要提示的信息
        viewModel.liveDataToastMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
        /**
         * 监听左侧栏的点击事件
         */
        val adapter = SoundtListAdapter { _, _ ->

        }
        binding.noteVoiceRecyclerview.adapter = adapter
        /**
         * 照片view
         */
        /**
         * 照片view
         */
        binding.notePictureViewpager.adapter = pictureAdapter

        /**
         * 数据操作结束
         */
        viewModel.liveDataFinish.observe(viewLifecycleOwner) {
            if (it)
                onBackPressed()
        }

        /**
         * 画布初始化完成
         */

        viewModel.liveDataCanvasViewInitFinished.observe(viewLifecycleOwner) {
            if (it)
                arguments?.let { b ->
                    val id = b.getString("NoteId", "")
                    if (id.isNotEmpty()) {
                        viewModel.initData(id)
                    }
                }
        }

        /**
         * 音频view
         */
        viewModel.listDataChatMsgEntityList.observe(viewLifecycleOwner) {
            adapter.refreshData(it)
        }

        /**
         * 照片view
         */
        viewModel.liveDataPictureList.observe(viewLifecycleOwner){
            pictureAdapter.refreshData(it)
        }

        //照片左右选择键点击监听
        binding.notePictureLeft.setOnClickListener(this)
        binding.notePictureRight.setOnClickListener(this)
        binding.noteCamera.setOnClickListener(this)
        //设置照片偏移量
        val viewPager = binding.notePictureViewpager
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

        binding.noteVoice.setOnTouchListener { _, event ->
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
    }

    private fun voiceOnTouchStart() {
        viewModel.startSoundMetter(requireActivity(), binding.noteVoice)
    }

    private fun voiceOnTouchStop() {
        Log.e("qj", "voiceOnTouchStop====${Constant.IS_VIDEO_SPEED}")
        if (Constant.IS_VIDEO_SPEED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                viewModel.stopSoundMeter()
            }
        }
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

    override fun onDestroy() {
        activity?.run {
            val result =  findNavController(R.id.main_activity_middle_fragment).navigateUp()
            Log.e("qj","onStop===$result")
        }
        super.onDestroy()
    }

    override fun onClick(v: View) {
        when (v) {
            binding.notePictureLeft->{
                val currentItem = binding.notePictureViewpager.currentItem
                if (currentItem > 0) {
                    binding.notePictureViewpager.currentItem = currentItem - 1
                } else {
                    return
                }
            }
            binding.notePictureRight->{
                val currentItem = binding.notePictureViewpager.currentItem
                if (currentItem < pictureAdapter.data.size - 1) {
                    binding.notePictureViewpager.currentItem = currentItem + 1
                } else {
                    return
                }
            }
            binding.sketchEraser -> {
                viewModel.onEraser()
                binding.sketchEraser.isSelected = viewModel.isEraser
            }
            binding.sketchBack -> {
                viewModel.onBack()
            }
            binding.sketchForward -> {
                viewModel.onForward()
            }
            binding.sketchClear -> {
                viewModel.onClear()
            }
            binding.noteBarSave -> {
                viewModel.onSaveData()
            }
            binding.noteBarDelete -> {
                viewModel.deleteData(requireContext())
            }
            binding.noteBarCancel -> {
                //返回按钮点击
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
            binding.noteCamera-> {
                takePhoto()
            }
        }
    }

    override fun onBackPressed(): Boolean {
        findNavController().navigateUp()
        return true
    }
}