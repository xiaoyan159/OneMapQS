package com.navinfo.omqs.ui.fragment.note

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentNoteBinding
import com.navinfo.omqs.ui.dialog.FirstDialog
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.other.shareViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NoteFragment : BaseFragment(), View.OnClickListener {
    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!

    private val viewModel by shareViewModels<NoteViewModel>("note")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.sketchEraser.setOnClickListener(this)
        binding.sketchClear.setOnClickListener(this)
        binding.sketchForward.setOnClickListener(this)
        binding.sketchBack.setOnClickListener(this)
        binding.noteBarSave.setOnClickListener(this)
        binding.noteBarCancel.setOnClickListener(this)
        binding.noteBarDelete.setOnClickListener(this)
        binding.noteDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                viewModel.noteBeanDescription = s.toString()
            }
        })
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
    }


    override fun onStart() {
        super.onStart()
        activity?.run {
            findNavController(
                R.id.main_activity_middle_fragment
            ).navigate(R.id.CanvasFragment)
        }
    }

    override fun onStop() {
        super.onStop()
        activity?.run {
            findNavController(
                R.id.main_activity_middle_fragment
            ).navigateUp()
        }
    }

    override fun onClick(v: View) {
        when (v) {
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
        }
    }

    override fun onBackPressed(): Boolean {
        findNavController().navigateUp()
        return true
    }
}