package com.nothingsecure.recy_information.logs_recy

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.nothingsecure.R
import com.nothingsecure.db
import com.nothingsecure.db.Companion.register_list
import com.nothingsecure.register

class logs_adapter(var list: List<register>, val delete: (register) -> Unit): RecyclerView.Adapter<logs_holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): logs_holder {
        return logs_holder(LayoutInflater.from(parent.context).inflate(R.layout.recy_history, null))
    }

    override fun onBindViewHolder(holder: logs_holder, position: Int) {
        return holder.element(list[position], delete)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update (new_list: List<register>) {
        val diff_ui = DiffUtil.calculateDiff(diff_ui_logs(list, new_list))
        list = new_list
        diff_ui.dispatchUpdatesTo(this)
    }
}

class logs_holder(view: View): RecyclerView.ViewHolder(view) {


    val input_time = view.findViewById<TextView>(R.id.time)
    val information_history = view.findViewById<TextView>(R.id.information_history)
    val color_information = view.findViewById<View>(R.id.color_information)

    val delete = view.findViewById<ShapeableImageView>(R.id.delete)
    @SuppressLint("ResourceType")
    fun element(register: register, delete_call_back: (com.nothingsecure.register) -> kotlin.Unit) {

        input_time.text = register.time
        information_history.text = register.information
        color_information.backgroundTintList = ColorStateList.valueOf(register.color.toColorInt())


        delete.setOnClickListener {
            delete_call_back(register)
        }
    }
}