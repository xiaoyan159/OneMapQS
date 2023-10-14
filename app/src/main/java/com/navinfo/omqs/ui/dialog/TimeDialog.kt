package com.navinfo.omqs.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import com.navinfo.omqs.R
import java.text.SimpleDateFormat

interface OnTimeDialogListener {
    fun selectTime(milliseconds: Long)
}

class TimeDialog(context: Context, val listener: OnTimeDialogListener) : Dialog(context),
    View.OnClickListener {

    private lateinit var dataPicker: DatePicker
    private lateinit var timePicker: TimePicker


    private var timeText = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        val customFrame = View.inflate(context, R.layout.dialog_time_layout, null)
        dataPicker = customFrame.findViewById(R.id.time_dialog_data_picker)
        timePicker = customFrame.findViewById(R.id.time_dialog_time_picker)
        customFrame.findViewById<TextView>(R.id.time_dialog_ok).setOnClickListener(this)
        customFrame.findViewById<TextView>(R.id.time_dialog_cancel).setOnClickListener(this)
        setContentView(customFrame)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onClick(v: View) {
        when (v.id) {
            R.id.time_dialog_ok -> {
                val month = if (dataPicker.month > 8) {
                    "${dataPicker.month + 1}"
                } else {
                    "0${dataPicker.month + 1}"
                }
                val hour = if(timePicker.hour < 10){
                    "0${timePicker.hour}"
                }else{
                    "${timePicker.hour}"
                }
                val minute = if(timePicker.minute<10){
                    "0${timePicker.minute}"
                }else{
                    "${timePicker.minute}"
                }
                val time =
                    "${dataPicker.year}${month}${dataPicker.dayOfMonth}${hour}${minute}00"
                Log.e("jingo", "选择的时间 $time")
                val sdf = SimpleDateFormat("yyyyMMddHHmmss")
                listener.selectTime(sdf.parse(time).time)
                dismiss()
            }
            R.id.time_dialog_cancel -> {
                dismiss()
            }
        }
    }


}