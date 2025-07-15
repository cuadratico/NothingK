package com.nothingsecure.recy_information

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nothingsecure.R
import com.nothingsecure.register

class logs_adapter(var list: List<register>): RecyclerView.Adapter<logs_holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): logs_holder {
        return logs_holder(LayoutInflater.from(parent.context).inflate(R.layout.recy_history, null))
    }

    override fun onBindViewHolder(holder: logs_holder, position: Int) {
        return holder.element(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update (new_list: List<register>) {
        this.list = new_list
        notifyDataSetChanged()
    }
}