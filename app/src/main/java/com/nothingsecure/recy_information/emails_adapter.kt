package com.nothingsecure.recy_information

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nothingsecure.R

class emails_adapter(var list: List<String>): RecyclerView.Adapter<emails_holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): emails_holder {
        return emails_holder(LayoutInflater.from(parent.context).inflate(R.layout.emails_see, null))
    }

    override fun onBindViewHolder( holder: emails_holder, position: Int) {
        return holder.element(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update (new_list: List<String>) {
        val diff = DiffUtil.calculateDiff(diff_ui_global(list, new_list))
        list = new_list
        diff.dispatchUpdatesTo(this)
    }
}

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