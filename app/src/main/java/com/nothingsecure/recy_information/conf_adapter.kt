package com.nothingsecure.recy_information

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.nothingsecure.R
import com.nothingsecure.delete_all_fun


class conf_adapter(val list: List<String>, val view: Int, val type: Int = 0): RecyclerView.Adapter<multi_holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): multi_holder {
        return multi_holder(LayoutInflater.from(parent.context).inflate(view, null), type)
    }

    override fun onBindViewHolder(holder: multi_holder, position: Int) {
        holder.elements(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    companion object {
        val mods_all = mapOf<String, Int>("Delete all" to R.drawable.delete_all, "Thief mode" to R.drawable.thief_mode, "Backup mode" to R.drawable.backup, "App lock" to R.drawable.padlock)
        val mods_recy = listOf("Delete all", "Thief mode", "Backup mode", "App lock")

        val colors_list = listOf("#264653", "#556B2F", "#2F4F4F", "#5A2A2A", "#3E2C2C", "#4B2E39", "#34495E", "#3B3C36", "#1b1b1d", "#d71b1f", "#FF000000")

    }

}