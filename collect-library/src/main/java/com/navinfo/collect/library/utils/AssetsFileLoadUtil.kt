package com.navinfo.collect.library.utils

import android.content.Context
import java.io.InputStream

class AssetsFileLoadUtil {
    companion object {
        fun loadWarningSvg(context: Context, code: String): InputStream? {
            return context.assets.open("omdb/appendix/1105_${code}_0.svg")
        }
    }
}