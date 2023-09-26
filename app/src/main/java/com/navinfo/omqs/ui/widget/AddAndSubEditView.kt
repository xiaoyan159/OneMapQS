package com.navinfo.omqs.ui.widget

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import com.navinfo.omqs.R

class AddAndSubEditView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), View.OnClickListener {

    private var valueText: Int = 0
    private val editView: EditText

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.view_add_del_editview, this)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AddAndSubEditView)
        valueText = typedArray.getInteger(R.styleable.AddAndSubEditView_textValue, 0)
        editView = view.findViewById(R.id.edit_text)
        editView.setText("$valueText")
        editView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                try {
                    valueText = s.toString().toInt()
                } catch (e: java.lang.Exception) {

                }
            }

        })
        view.findViewById<ImageView>(R.id.del).setOnClickListener(this)
        view.findViewById<ImageView>(R.id.add).setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.del -> {
                valueText--
                editView.setText("$valueText")
            }
            R.id.add -> {
                valueText++
                editView.setText("$valueText")
            }
        }
    }

    fun getValue(): Int {
        return valueText
    }

    fun setValue(value:Int) {
        valueText = value
        editView.setText("$valueText")
    }
}