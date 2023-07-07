package com.navinfo.omqs.ui.fragment.note

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.FragmentCanvasBinding
import com.navinfo.omqs.databinding.FragmentNoteBinding
import com.navinfo.omqs.databinding.FragmentProblemLinkBinding
import com.navinfo.omqs.ui.fragment.BaseFragment
import com.navinfo.omqs.ui.fragment.note.CanvasView.CanvasStyle
import com.navinfo.omqs.ui.fragment.note.CanvasView.OnCanvasChangeListener
import com.navinfo.omqs.ui.other.shareViewModels

/**
 * @author zhjch
 * @version V1.0
 * @ClassName: CanvasFragment
 * @Date 2016/5/10
 * @Description: ${TODO}(绘制画布)
 */
class CanvasFragment : BaseFragment() {
    /**
     * 获取画布
     *
     * @return
     */
    /**
     * 画布
     */
    private val canvasView by lazy { binding.canvasView }

    /**
     * 画笔线型
     */
    private var mStyle = CanvasStyle.FREE_LINE

    /**
     * 画笔颜色
     */
    private var mColor = -1

    /**
     * 画笔粗细
     */
    private var width = 5


    /**
     * 画布回调接口
     */
    private var listener: OnCanvasChangeListener? = null


    private var _binding: FragmentCanvasBinding? = null
    private val binding get() = _binding!!

    private val viewModel by shareViewModels<NoteViewModel>("note")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCanvasBinding.inflate(inflater, container, false)
        viewModel.initCanvasView(canvasView)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        canvasView.setStyle(mStyle)
        if (mColor == -1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mColor = resources.getColor(R.color.black, null)
            }
        }
        canvasView.setPaintColor(mColor)
        canvasView.setPaintWidth(width)
        if (listener != null) {
            canvasView.setOnCanvasChangeListener(listener)
        }

//         * 开关橡皮擦
//         */
//        viewModel.liveEraserData.observe(viewLifecycleOwner) {
//            canvasView.setEraser(it)
//        }
//        /**
//         * 清除
//         */
//        viewModel.liveClearData.observe(viewLifecycleOwner) {
//            canvasView.removeAllPaint()
//        }
//        /**
//         * 回退上一笔
//         */
//        viewModel.liveBackData.observe(viewLifecycleOwner) {
//            canvasView.back()
//        }
//        /**
//         * 撤销回退
//         */
//        viewModel.liveForward.observe(viewLifecycleOwner) {
//            canvasView.forward()
//        }
//

    }


    /**
     * 将数据转化并绘制在画板上
     *
     * @param value
     */
    fun setDrawPathList(value: MutableList<CanvasView.DrawPath>) {
        if (value != null && value.isNotEmpty()) {
            canvasView.setDrawPathList(value)
        }
    }


    /**
     * 设置草图画笔线型
     */
    fun setStyle(style: CanvasStyle) {
        mStyle = style
        canvasView.setStyle(style)
    }

    /**
     * 设置画笔颜色
     */
    fun setPaintColor(color: Int) {
        mColor = color
        canvasView.setPaintColor(mColor)
    }

    /**
     * 设置画笔粗细
     */
    fun setPaintWidth(width: Int) {
        this.width = width
        canvasView.setPaintWidth(width)
    }
}