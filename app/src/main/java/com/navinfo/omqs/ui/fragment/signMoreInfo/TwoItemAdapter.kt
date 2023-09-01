package com.navinfo.omqs.ui.fragment.signMoreInfo

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import com.navinfo.collect.library.enums.DataCodeEnum
import com.navinfo.omqs.databinding.AdapterTwoItemBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder
import org.oscim.android.canvas.AndroidSvgBitmap

data class TwoItemAdapterItem(
    val title: String,
    val text: String,
    val code: String = "",
)

class TwoItemAdapter : BaseRecyclerViewAdapter<TwoItemAdapterItem>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterTwoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val binding: AdapterTwoItemBinding =
            holder.viewBinding as AdapterTwoItemBinding
        val item = data[position]
        binding.title.text = item.title
        if (item.code == DataCodeEnum.OMDB_WARNINGSIGN.code) {
            try {
                val input =
                    holder.viewBinding.root.context.assets.open("omdb/appendix/1105_${item.text}_0.svg")
                if (input != null) {
                    val bitmap = AndroidSvgBitmap.getResourceBitmap(input, 1.0f, 60.0f, 60, 60, 100)
                    input.close()
                    val drawableLeft = BitmapDrawable(
                        holder.viewBinding.root.context.resources,
                        bitmap
                    )
                    drawableLeft.setBounds(
                        0,
                        0,
                        (drawableLeft.minimumWidth * 1.2).toInt(),
                        (drawableLeft.minimumHeight * 1.2).toInt()
                    )//必须
                    binding.text.setCompoundDrawables(
                        drawableLeft, null, null, null
                    )
                }
            } catch (e: Exception) {
                Log.e("jingo", "危险信息没有${item.text}这个svg")
            }
        }
        binding.text.text = item.text
    }
}