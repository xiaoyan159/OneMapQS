package com.navinfo.omqs.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import com.navinfo.omqs.R

interface OnTimeDialogListener {
}

class TimeDialog(context: Context) : Dialog(context), View.OnClickListener {

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

    override fun onClick(v: View) {
        when (v.id) {
            R.id.time_dialog_ok -> {
//                dataPicker.
                dismiss()
            }
            R.id.time_dialog_cancel -> {
                dismiss()
            }
        }
    }


}