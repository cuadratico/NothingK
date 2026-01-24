package com.nothingsecure.recy_information

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.nothingsecure.R
import com.nothingsecure.recy_information.conf_adapter.Companion.mods_all


class conf_adapter(val list: List<String>, val view: Int, val type: Int = 0, val click: (String) -> Unit, val long_click: (String) -> Unit): RecyclerView.Adapter<multi_holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): multi_holder {
        return multi_holder(LayoutInflater.from(parent.context).inflate(view, null), type)
    }

    override fun onBindViewHolder(holder: multi_holder, position: Int) {
        holder.elements(list[position], click, long_click)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    companion object {
        val mods_all = mapOf<String, Int>("Delete all" to R.drawable.delete_all, "Honeypot" to R.drawable.honeypot, "Backup mode" to R.drawable.backup, "App lock" to R.drawable.padlock)
        val mods_recy = listOf("Delete all", "Honeypot", "Backup mode", "App lock")

        val colors_list = listOf("#264653", "#556B2F", "#2F4F4F", "#5A2A2A", "#3E2C2C", "#4B2E39", "#34495E", "#3B3C36", "#1b1b1d", "#d71b1f", "#FF000000")

    }

}

class multi_holder(val view: View, val type: Int): RecyclerView.ViewHolder(view) {

    val back = view.findViewById<View>(R.id.back)
    private lateinit var multi_text: TextView


    fun elements (multiData: String, click: (String) -> Unit, long_click: (String) -> Unit) {
        if (type == 0) {
            val icon = view.findViewById<ShapeableImageView>(R.id.icon)
            multi_text = view.findViewById(R.id.text_type)

            icon.setImageResource(mods_all.get(multiData)!!)
            multi_text.text = multiData
        }else {
            val back_color = view.findViewById<ConstraintLayout>(R.id.back_color_expressed)
            multi_text = view.findViewById(R.id.color_code_output)

            back_color.backgroundTintList = ColorStateList.valueOf(multiData.toColorInt())
            multi_text.text = multiData
        }

        back.setOnClickListener {
            click(multiData)
        }

        back.setOnLongClickListener(object: View.OnLongClickListener {
            override fun onLongClick(v: View?): Boolean {
                long_click(multiData)
                return true
            }

        })

    }
}