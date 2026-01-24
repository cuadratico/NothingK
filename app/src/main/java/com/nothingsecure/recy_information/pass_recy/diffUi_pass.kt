package com.nothingsecure.recy_information.pass_recy

import androidx.recyclerview.widget.DiffUtil
import com.nothingsecure.pass

class diffUi_pass(val old_list: List<pass>, val new_list: List<pass>): DiffUtil.Callback() {

    override fun getOldListSize(): Int = old_list.size

    override fun getNewListSize(): Int = new_list.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old_list[oldItemPosition].id == new_list[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old_list[oldItemPosition] == new_list[newItemPosition]
    }

}