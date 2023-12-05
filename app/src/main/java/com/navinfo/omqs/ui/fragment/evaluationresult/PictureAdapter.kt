package com.navinfo.omqs.ui.fragment.evaluationresult

import android.view.LayoutInflater
import android.view.ViewGroup
import com.navinfo.omqs.Constant
import com.navinfo.omqs.R
import com.navinfo.omqs.databinding.AdapterPictureBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder
import com.navinfo.omqs.util.ImageTools
import java.io.File

class PictureAdapter : BaseRecyclerViewAdapter<String>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterPictureBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val bd = holder.viewBinding as AdapterPictureBinding

        val myAppDir = File(Constant.USER_DATA_ATTACHEMNT_PATH)

        if (!myAppDir.exists()) myAppDir.mkdirs() // 确保文件夹已创建

        // 创建一个名为 fileName 的文件
        val file = File(myAppDir, data[position])
        if(file.exists()){
            bd.showImage.setImageBitmap(ImageTools.zoomBitmap(Constant.USER_DATA_ATTACHEMNT_PATH+"/"+data[position],2))
        }else{
            bd.showImage.setBackgroundResource(R.drawable.icon_camera_img)
        }
    }

}