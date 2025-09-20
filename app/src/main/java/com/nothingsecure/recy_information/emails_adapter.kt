package com.nothingsecure.recy_information

import android.view.LayoutInflater
import android.view.ViewGroup
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
        this.list = new_list
        notifyDataSetChanged()
    }
}