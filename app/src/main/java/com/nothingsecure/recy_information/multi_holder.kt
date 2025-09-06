package com.nothingsecure.recy_information

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.imageview.ShapeableImageView
import com.nothingsecure.R
import com.nothingsecure.recy_information.conf_adapter.Companion.mods_all
import com.nothingsecure.configurationActivity.Companion.dialog_conf

class multi_holder(val view: View, val type: Int): RecyclerView.ViewHolder(view) {

    val back = view.findViewById<View>(R.id.back)
    private lateinit var multi_text: TextView


    fun elements (multiData: String) {
        if (type == 0) {
            val icon = view.findViewById<ShapeableImageView>(R.id.icon)
            multi_text = view.findViewById(R.id.text_type)

            icon.setImageResource(mods_all.get(multiData)!!)
            multi_text.text = multiData
        }else {
            val back_color = view.findViewById<ConstraintLayout>(R.id.back_color_expressed)
            multi_text = view.findViewById(R.id.color_code_output)

            back_color.backgroundTintList = ColorStateList.valueOf(multiData.toColorInt())
            multi_text.text = multiData
        }

        val context = multi_text.context

        val mk = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val pref = EncryptedSharedPreferences.create(context, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        back.setOnClickListener {
            if (type == 0) {
                pref.edit().putString("multi_but_icon", multiData).commit()
            }else {
                pref.edit().putString("color_back", multiData).commit()
            }
            dialog_conf.dismiss()
        }

    }
}