package com.navinfo.omqs.ui.fragment

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View {
//        val view = OnCreateView(inflater, container, savedInstanceState)
//
//        view.isFocusableInTouchMode = true;
//        view.requestFocus();
//        view.setOnKeyListener { _, keyCode, event ->
//            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
//                onBackPressed()
//            }
//            false
//        }
//
//
//        return view
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取OnBackPressedDispatcher
        val dispatcher = requireActivity().onBackPressedDispatcher
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }

        // 添加返回键事件处理逻辑
        dispatcher.addCallback(this, callback)
    }

//    abstract fun OnCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View

    open fun onBackPressed(): Boolean{
//        findNavController().navigateUp()
        return true
    }

}