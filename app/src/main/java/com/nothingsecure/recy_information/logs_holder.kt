package com.nothingsecure.recy_information

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.nothingsecure.R
import com.nothingsecure.db
import com.nothingsecure.db.Companion.register_list
import com.nothingsecure.logs_update
import com.nothingsecure.register

class logs_holder(view: View): RecyclerView.ViewHolder(view) {


    val input_time = view.findViewById<TextView>(R.id.time)
    val information_history = view.findViewById<TextView>(R.id.information_history)
    val color_information = view.findViewById<View>(R.id.color_information)

    val delete = view.findViewById<AppCompatButton>(R.id.delete)
    @SuppressLint("ResourceType")
    fun element(register: register) {

        input_time.text = register.time
        information_history.text = register.information
        color_information.backgroundTintList = ColorStateList.valueOf(register.color.toColorInt())


        delete.setOnClickListener {
            val db = db(delete.context)

            db.delete_register(false, register.time)
            register_list.removeIf { it.time == register.time }

            logs_update = true
        }
    }
}