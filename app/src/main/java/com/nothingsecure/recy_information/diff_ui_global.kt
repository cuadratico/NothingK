package com.nothingsecure.recy_information

import androidx.recyclerview.widget.DiffUtil

class diff_ui_global(val old_list: List<String>, val new_list: List<String>): DiffUtil.Callback() {
    override fun getOldListSize(): Int = old_list.size

    override fun getNewListSize(): Int = new_list.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old_list[oldItemPosition] == new_list[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return return old_list[oldItemPosition] == new_list[newItemPosition]
    }
}