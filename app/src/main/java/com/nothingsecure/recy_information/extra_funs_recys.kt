package com.nothingsecure.recy_information

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nothingsecure.MainActivity
import com.nothingsecure.R
import com.nothingsecure.add_register
import com.nothingsecure.backup
import com.nothingsecure.db
import com.nothingsecure.db.Companion.pass_list
import com.nothingsecure.deri_expressed
import com.nothingsecure.entropy
import com.nothingsecure.load
import com.nothingsecure.pass
import com.nothingsecure.pass_generator_dialog
import com.nothingsecure.recy_information.pass_recy.pass_adapter
import com.nothingsecure.visibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Base64
import javax.crypto.Cipher


class pass_funs (val context: MainActivity, val pref: SharedPreferences, val passData: pass) {

    fun edit_pass(pass_adapter: pass_adapter) {

        val db = db(context)

        val edit_dialog = Dialog(context)
        val edit_view = LayoutInflater.from(context).inflate(R.layout.add_edit_dialog, null)

        val information_extra = edit_view.findViewById<TextView>(R.id.information)
        val edit_information = edit_view.findViewById<EditText>(R.id.information_pass)
        val edit_pass = edit_view.findViewById<EditText>(R.id.input_password)
        val edit_progress = edit_view.findViewById<LinearProgressIndicator>(R.id.progress)
        val pass_visibility = edit_view.findViewById<ConstraintLayout>(R.id.secure_visibility)
        val icon_visi = edit_view.findViewById<ShapeableImageView>(R.id.visibility_icon)
        val bottom = edit_view.findViewById<AppCompatButton>(R.id.multi_bottom)
        val pass_generator = edit_view.findViewById<ConstraintLayout>(R.id.pass_generator)

        information_extra.text = "Edit your password"
        edit_information.setText(passData.information)
        edit_pass.setText(passData.pass)
        bottom.text = "Edit"

        edit_pass.isSelected = false

        edit_pass.isLongClickable = false

        entropy(edit_pass.text.toString(), edit_progress)
        edit_pass.addTextChangedListener { dato ->
            if (dato.toString() != "") {
                entropy(dato.toString(), edit_progress)
            }
        }

        visibility(pref, icon_visi, edit_pass)
        pass_visibility.setOnClickListener {
            pref.edit().putBoolean("prims", !pref.getBoolean("prims", false)).commit()
            visibility(pref, icon_visi, edit_pass)
        }

        bottom.setOnClickListener {
            if (edit_pass.text.trim().isNotEmpty() && edit_information.text.trim().isNotEmpty()) {
                val load_dialog = load("Encrypting the password...", context)

                context.lifecycleScope.launch (Dispatchers.IO){
                    try {
                        if (pref.getBoolean("db_sus", true)) {
                            val c = Cipher.getInstance("AES/GCM/NoPadding")
                            c.init(Cipher.ENCRYPT_MODE, deri_expressed(context, pref.getString("key_u", "")!!, pref.getString("salt", "").toString(), pref.getInt("it_def", 60000)))
                            backup(context, 2)
                            db.update_pass(passData.id, Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(edit_pass.text.toString().toByteArray())), edit_information.text.toString(), Base64.getEncoder().withoutPadding().encodeToString(c.iv))
                            add_register(context, "A password has been edited")
                        }
                        pass_list = pass_list.map { if (it.id == passData.id) { passData.copy(information = edit_information.text.toString(), pass = edit_pass.text.toString()) } else { it } }
                        withContext(Dispatchers.Main) {
                            pass_adapter.update(pass_list)
                            edit_dialog.dismiss()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Editing error", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            load_dialog.dismiss()
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Missing information to be filled in", Toast.LENGTH_SHORT).show()
            }
        }

        pass_generator.setOnClickListener {
            pass_generator_dialog(context, null, edit_pass)
        }

        edit_dialog.setContentView(edit_view)
        edit_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        edit_dialog.show()
    }

    fun delete_pass(pass_adapter: pass_adapter) {
        try {
            if (pref.getBoolean("db_sus", true)) {
                val db = db(context)
                backup(context, 3)
                db.delete_pass(passData.id)
                add_register(context, "A password has been deleted")
            }
            pass_list = pass_list.minus(passData)
            pass_adapter.update(pass_list)
        } catch (e: Exception) {
            Log.e("Deletion error", e.toString())
            Toast.makeText(context, "Deletion error", Toast.LENGTH_SHORT).show()
        }
    }

    fun click_all () {
        val dialog_see = Dialog(context)
        val dialog_view = LayoutInflater.from(context).inflate(R.layout.see_password, null)

        val see_password = dialog_view.findViewById<TextView>(R.id.pass_visible)
        val copy = dialog_view.findViewById<ShapeableImageView>(R.id.copy)
        val info_copy = dialog_view.findViewById<TextView>(R.id.info_copy)

        fun copy() {
            if (pref.getInt("copy_in", 0) >= 3) {
                copy.visibility = View.INVISIBLE
                info_copy.text = "You cannot copy passwords"
            }else {
                info_copy.text = "You can copy ${3 - pref.getInt("copy_in", 0)} more passwords"
            }
        }
        copy()

        copy.setOnClickListener {
            val manage = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("pass", see_password.text.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manage.clearPrimaryClip()
            }
            manage.setPrimaryClip(clip)
            pref.edit().putInt("copy_in", pref.getInt("copy_in", 0) + 1).commit()
            add_register(context, "A password has been copied")
            copy()
        }

        see_password.text = passData.pass

        dialog_see.setContentView(dialog_view)
        dialog_see.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog_see.show()
    }
}