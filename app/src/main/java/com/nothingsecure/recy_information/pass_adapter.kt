package com.nothingsecure.recy_information

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.nothingsecure.R
import com.nothingsecure.pass

class pass_adapter(var list: List<pass>): RecyclerView.Adapter<pass_holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): pass_holder {
        return pass_holder(LayoutInflater.from(parent.context).inflate(R.layout.recy_password, null))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: pass_holder, position: Int) {
        return holder.element(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update (newList: List<pass>) {
        this.list = newList
        notifyDataSetChanged()
    }
}