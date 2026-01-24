package com.nothingsecure.recy_information.pass_recy

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nothingsecure.R
import com.nothingsecure.entropy
import com.nothingsecure.pass

class pass_adapter(var list: List<pass>, val edit_pass: (pass) -> Unit, val delete_pass: (pass) -> Unit, val all: (pass) -> Unit): RecyclerView.Adapter<pass_holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): pass_holder {
        return pass_holder(LayoutInflater.from(parent.context).inflate(R.layout.recy_password, parent, false))
    }

    override fun onBindViewHolder(holder: pass_holder, position: Int) {
        return holder.element(list[position], edit_pass, delete_pass, all)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update (new_list: List<pass>) {
        val diff = DiffUtil.calculateDiff(diffUi_pass(list, new_list))
        list = new_list
        diff.dispatchUpdatesTo(this)
    }
}

class pass_holder(view: View): RecyclerView.ViewHolder(view){

    val all = view.findViewById<View>(R.id.all_click)
    val title = view.findViewById<TextView>(R.id.title)
    val password = view.findViewById<TextView>(R.id.password)
    val progress = view.findViewById<LinearProgressIndicator>(R.id.progress)
    val edit = view.findViewById<ShapeableImageView>(R.id.edit)
    val delete = view.findViewById<ShapeableImageView>(R.id.delete)
    @SuppressLint("MissingInflatedId")

    fun element (passData: pass, edit_pass: (pass) -> Unit, delete_pass: (pass) -> Unit, all_click: (pass) -> Unit){
        title.text = passData.information
        password.text = passData.pass
        entropy(password.text.toString(), progress)

        edit.setOnClickListener {
           edit_pass(passData)
        }

        delete.setOnLongClickListener( object: View.OnLongClickListener {
            override fun onLongClick(p0: View?): Boolean {
                delete_pass(passData)
                return true
            }
        })

        delete.setOnClickListener {
            Toast.makeText(title.context, "You must maintain", Toast.LENGTH_SHORT).show()
        }

        all.setOnLongClickListener {
            all_click(passData)
            true
        }

    }
}