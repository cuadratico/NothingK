package com.nothingsecure.recy_information

import android.annotation.SuppressLint
import android.app.Dialog
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
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nothingsecure.R
import com.nothingsecure.alia
import com.nothingsecure.db
import com.nothingsecure.db.Companion.pass_list
import com.nothingsecure.entropy
import com.nothingsecure.pass
import com.nothingsecure.pass_update
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

            entropy(edit_pass.text.toString(), edit_progress)
            edit_pass.addTextChangedListener {dato ->
                if (dato.toString() != "") {
                    entropy(dato.toString(), edit_progress)
                }
            }

            var visi = false
            pass_visibility.setOnClickListener {
                if (visi) {
                    visi = false
                    icon_visi.setImageResource(R.drawable.close_eye)
                    edit_pass.transformationMethod = PasswordTransformationMethod.getInstance()
                }else {
                    visi = true
                    icon_visi.setImageResource(R.drawable.open_eye)
                    edit_pass.transformationMethod = null
                }
                edit_pass.setSelection(edit_pass.text.length)
            }

            bottom.setOnClickListener {
                if (edit_pass.text.isNotEmpty() && edit_information.text.isNotEmpty()) {
                    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                    val c = Cipher.getInstance("AES/GCM/NoPadding")
                    c.init(Cipher.ENCRYPT_MODE, ks.getKey(alia, null))

                    db.update_pass(passData.id,
                        Base64.getEncoder().withoutPadding()
                            .encodeToString(c.doFinal(edit_pass.text.toString().toByteArray())),
                        edit_information.text.toString(),
                        Base64.getEncoder().withoutPadding().encodeToString(c.iv)
                    )
                    passData.information = edit_information.text.toString()
                    passData.pass = edit_pass.text.toString()
                    pass_update = true
                    edit_dialog.dismiss()
                }else {
                    Toast.makeText(context, "Missing information to be filled in", Toast.LENGTH_SHORT).show()
                }
            }


            edit_dialog.setContentView(edit_view)
            edit_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            edit_dialog.show()
        }

        delete.setOnClickListener {
            db.delete_pass(passData.id)
            pass_list.remove(passData)
            pass_update = true
        }

        all.setOnLongClickListener {

            val dialog_see = Dialog(context)
            val dialog_view = LayoutInflater.from(context).inflate(R.layout.see_password, null)

            val see_password = dialog_view.findViewById<TextView>(R.id.pass_visible)

            see_password.text = passData.pass

            dialog_see.setContentView(dialog_view)
            dialog_see.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog_see.show()
            true
        }

    }
}