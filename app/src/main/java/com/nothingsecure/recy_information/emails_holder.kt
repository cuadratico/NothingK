package com.nothingsecure.recy_information

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.nothingsecure.R

class emails_holder(view: View): RecyclerView.ViewHolder(view) {

    val mail = view.findViewById<TextView>(R.id.mail)
    val all = view.findViewById<ConstraintLayout>(R.id.all)
    val context = all.context
    fun element( value: String) {
        mail.text = value

        all.setOnClickListener {
            val manage = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("email", value)
            manage.setPrimaryClip(clip)
        }
    }
}