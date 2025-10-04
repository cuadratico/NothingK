package com.nothingsecure.recy_information

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.LayoutInflaterCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nothingsecure.R
import com.nothingsecure.add_register
import com.nothingsecure.backup
import com.nothingsecure.db
import com.nothingsecure.db.Companion.pass_list
import com.nothingsecure.deri_expressed
import com.nothingsecure.entropy
import com.nothingsecure.pass
import com.nothingsecure.pass_update
import com.nothingsecure.visibility
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher

class pass_holder(view: View): RecyclerView.ViewHolder(view) {

    val all = view.findViewById<View>(R.id.all_click)
    val title = view.findViewById<TextView>(R.id.title)
    val password = view.findViewById<TextView>(R.id.password)
    val progress = view.findViewById<LinearProgressIndicator>(R.id.progress)
    val edit = view.findViewById<AppCompatButton>(R.id.edit)
    val delete = view.findViewById<AppCompatButton>(R.id.delete)
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    fun element (passData: pass){
        title.text = passData.information
        password.text = passData.pass
        entropy(password.text.toString(), progress)

        val context = title.context
        val db = db(context)

        val mk = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val pref = EncryptedSharedPreferences.create(context, "ap", mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        edit.setOnClickListener {
            val edit_dialog = Dialog(context)
            val edit_view = LayoutInflater.from(context).inflate(R.layout.add_edit_dialog, null)

            val information_extra = edit_view.findViewById<TextView>(R.id.information)
            val edit_information = edit_view.findViewById<EditText>(R.id.information_pass)
            val edit_pass = edit_view.findViewById<EditText>(R.id.input_password)
            val edit_progress = edit_view.findViewById<LinearProgressIndicator>(R.id.progress)
            val pass_visibility = edit_view.findViewById<ConstraintLayout>(R.id.password_visibility)
            val icon_visi = edit_view.findViewById<ShapeableImageView>(R.id.visibility_icon)
            val bottom = edit_view.findViewById<AppCompatButton>(R.id.multi_bottom)
            information_extra.text = "Edit your password"
            edit_information.setText(passData.information)
            edit_pass.setText(passData.pass)
            bottom.text = "Edit"

            edit_pass.isSelected = false

            edit_pass.isLongClickable = false

            entropy(edit_pass.text.toString(), edit_progress)
            edit_pass.addTextChangedListener {dato ->
                if (dato.toString() != "") {
                    entropy(dato.toString(), edit_progress)
                }
            }

            var visi = false
            pass_visibility.setOnClickListener {
                visibility(visi, icon_visi, edit_pass)
                visi = !visi
            }

            bottom.setOnClickListener {
                if (edit_pass.text.trim().isNotEmpty() && edit_information.text.trim().isNotEmpty()) {
                    try {
                        val c = Cipher.getInstance("AES/GCM/NoPadding")
                        c.init(Cipher.ENCRYPT_MODE, deri_expressed(context, pref.getString("key_u", "")!!, pref.getString("salt", "")!!))

                        if (pref.getBoolean("db_sus", true)) {
                            backup(context, 2)
                            db.update_pass(passData.id, Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(edit_pass.text.toString().toByteArray())), edit_information.text.toString(), Base64.getEncoder().withoutPadding().encodeToString(c.iv))
                            add_register(context, "A password has been edited")
                        }
                        passData.information = edit_information.text.toString()
                        passData.pass = edit_pass.text.toString()
                        pass_update = true
                        edit_dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Editing error", Toast.LENGTH_SHORT).show()
                    }
                }else {
                    Toast.makeText(context, "Missing information to be filled in", Toast.LENGTH_SHORT).show()
                }
            }


            edit_dialog.setContentView(edit_view)
            edit_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            edit_dialog.show()
        }

        delete.setOnLongClickListener( object: View.OnLongClickListener {
            override fun onLongClick(p0: View?): Boolean {
                try {
                    if (pref.getBoolean("db_sus", true)) {
                        backup(context, 3)
                        db.delete_pass(passData.id)
                        add_register(context, "A password has been deleted")
                    }
                    pass_list.removeIf { it.id == passData.id }
                    pass_update = true
                } catch (e: Exception) {
                    Log.e("Deletion error", e.toString())
                    Toast.makeText(context, "Deletion error", Toast.LENGTH_SHORT).show()
                }

                return true
            }
        })

        delete.setOnClickListener {
            Toast.makeText(context, "You must maintain", Toast.LENGTH_SHORT).show()
        }

        all.setOnLongClickListener {

            val dialog_see = Dialog(context)
            val dialog_view = LayoutInflater.from(context).inflate(R.layout.see_password, null)

            val see_password = dialog_view.findViewById<TextView>(R.id.pass_visible)
            val copy = dialog_view.findViewById<AppCompatButton>(R.id.copy)
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
            true
        }

    }
}