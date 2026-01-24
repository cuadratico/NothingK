package com.nothingsecure.recy_information.logs_recy

import androidx.recyclerview.widget.DiffUtil
import com.nothingsecure.register

class diff_ui_logs(val old_list: List<register>, val new_list: List<register>): DiffUtil.Callback() {

    override fun getOldListSize(): Int = old_list.size

    override fun getNewListSize(): Int = new_list.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old_list[oldItemPosition].id == new_list[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old_list[oldItemPosition] == new_list[newItemPosition]
    }
}