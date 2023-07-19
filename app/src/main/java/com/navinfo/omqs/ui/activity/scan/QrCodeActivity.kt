package com.navinfo.omqs.ui.activity.scan

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.camera.core.ImageCapture
import androidx.camera.view.LifecycleCameraController
import androidx.databinding.DataBindingUtil
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.ActivityQrCodeBinding
import com.navinfo.omqs.ui.activity.BaseActivity
import com.navinfo.omqs.ui.listener.QRCodeAnalyser
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.lifecycle.Observer
import com.navinfo.omqs.ui.activity.login.LoginStatus

/**
 * date:2023/6/18
 * author:qj
 * description:二维码扫描
 */
@AndroidEntryPoint
class QrCodeActivity : BaseActivity() {
    private lateinit var binding: ActivityQrCodeBinding
    private lateinit var lifecycleCameraController: LifecycleCameraController
    private lateinit var cameraExecutor: ExecutorService
    private val viewModel by viewModels<QrCodeViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_qr_code)

        binding.qrCodeModel = viewModel
        binding.lifecycleOwner = this
        binding.activity = this

        initView()
        initController()
    }

    private fun initView() {
        //登录校验，初始化成功
        viewModel.qrCodeStatus.observe(this, qrCodeObserve)
    }

    /*
    * 监听扫描结果
    * */
    private val qrCodeObserve = Observer<QrCodeStatus> {
        when (it) {
            QrCodeStatus.QR_CODE_STATUS_SUCCESS -> {
                finish()
            }
            QrCodeStatus.QR_CODE_STATUS_NET_FAILURE -> {

            }
            QrCodeStatus.QR_CODE_STATUS_SERVER_INFO_SUCCESS -> {

            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "UnsafeOptInUsageError")
    private fun initController() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        lifecycleCameraController = LifecycleCameraController(this)
        lifecycleCameraController.bindToLifecycle(this)
        lifecycleCameraController.imageCaptureFlashMode = ImageCapture.FLASH_MODE_AUTO
        lifecycleCameraController.setImageAnalysisAnalyzer(
            cameraExecutor,
            QRCodeAnalyser { barcodes, imageWidth, imageHeight ->
                if (barcodes.isEmpty()) {
                    return@QRCodeAnalyser
                }
                initScale(imageWidth, imageHeight)
                val list = ArrayList<RectF>()
                val strList = ArrayList<String>()

                barcodes.forEach { barcode ->
                    barcode.boundingBox?.let { rect ->
                        val translateRect = translateRect(rect)
                        list.add(translateRect)
                        Log.e(
                            "ztzt", "left：${translateRect.left}  +" +
                                    "  top：${translateRect.top}  +  right：${translateRect.right}" +
                                    "  +  bottom：${translateRect.bottom}"
                        )
                        Log.e("ztzt", "barcode.rawValue：${barcode.rawValue}")
                        strList.add(barcode.rawValue ?: "No Value")
                    }
                }
                judgeIntent(strList)
                binding.scanView.setRectList(list)

            })
        binding.previewView.controller = lifecycleCameraController
    }

    fun judgeIntent(list: ArrayList<String>) {
        val sb = StringBuilder()
        list.forEach {
            sb.append(it)
            sb.append("\n")
        }
        intentToResult(sb.toString())
    }

    private fun intentToResult(result: String) {

        Log.e("qj", "QRCodeActivity === $result")

        viewModel.connect(this, result)

        /*        val intent = Intent(this, QRCodeResultActivity::class.java)
                intent.putExtra(QRCodeResultActivity.RESULT_KEY, result)
                startActivity(intent)
                finish()*/
    }


    private var scaleX = 0f
    private var scaleY = 0f

    private fun translateX(x: Float): Float = x * scaleX
    private fun translateY(y: Float): Float = y * scaleY

    //将扫描的矩形换算为当前屏幕大小
    private fun translateRect(rect: Rect) = RectF(
        translateX(rect.left.toFloat()),
        translateY(rect.top.toFloat()),
        translateX(rect.right.toFloat()),
        translateY(rect.bottom.toFloat())
    )

    //初始化缩放比例
    private fun initScale(imageWidth: Int, imageHeight: Int) {
        Log.e("ztzt", "imageWidth：${imageWidth} + imageHeight：${imageHeight}")
        scaleY = binding.scanView.height.toFloat() / imageWidth.toFloat()
        scaleX = binding.scanView.width.toFloat() / imageHeight.toFloat()
        Log.e("ztzt", "scaleX：${scaleX} + scaleY：${scaleY}")
    }
}