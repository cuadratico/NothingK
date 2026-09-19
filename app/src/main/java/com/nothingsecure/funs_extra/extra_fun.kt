package com.nothingsecure.funs_extra

import android.app.Activity
import android.app.Dialog
import android.content.Context.VIBRATOR_SERVICE
import android.content.SharedPreferences
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.google.android.material.imageview.ShapeableImageView
import com.nothingsecure.R
import com.nothingsecure.db
import com.nothingsecure.recy_information.project_class
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Base64
import javax.crypto.Cipher
import kotlin.math.abs


fun add_register (pref: SharedPreferences, db: db, note: String, color: String = "#1b1b1d") {
    val c = cip_no_cip(pref, Cipher.ENCRYPT_MODE, deri_expressed(pref, pref.getString("key_l", "").toString(), false), "AES")

    db.insert(
        pref,
        "info_r",
        db_create(Base64.getEncoder().withoutPadding().encodeToString(c.doFinal((des_es_values(null, null, null, note, true, color) as project_class.multi_data).data.toByteArray())), Base64.getEncoder().withoutPadding().encodeToString(c.iv),
            "global_time")
    )

    pref.edit().putBoolean("info_r_full", true).commit()

}


var x_regi = 0f
var y_regi = 0f
var z_regi = 0f

fun force (context: Activity, pref: SharedPreferences, x: Float, y: Float, z: Float) {

    if (abs(x_regi - x) > 12 || abs(y_regi - y) > 12 || abs(z_regi - z) > 12) {

        pref.edit().putBoolean("close", true).commit()

        val dialog = Dialog(context)
        val view_force = create_dialog(context, R.layout.alert_dialog, dialog)

        val alert_text = view_force.findViewById<TextView>(R.id.alert_text)
        val alert_view = view_force.findViewById<ShapeableImageView>(R.id.alert_view)

        alert_text.text = "Suspicious movement has been detected"
        alert_view.setImageResource(R.drawable.fall)

        CoroutineScope(Dispatchers.IO).launch {
            delay(300)

            withContext(Dispatchers.Main) {
                dialog.dismiss()
                context.finishAffinity()
            }
        }


    }

}

fun destroy_info (pref: SharedPreferences) {
    pref.edit().remove("key_u").commit()
    pref.edit().remove("it_def").commit()
    pref.edit().remove("destroy").commit()

    if (pref.getBoolean("d_v_h", false)) {
        pref.edit().remove("salt_def").commit()
    }
}


fun visibility (pref: SharedPreferences, icon: ShapeableImageView, input: EditText) {
    if (pref.getBoolean("prims", false)) {
        icon.setImageResource(R.drawable.prism_rell)
        input.setTextColor("#27272A".toColorInt())
    }else {
        icon.setImageResource(R.drawable.prism_no_rell)
        input.setTextColor("#e3e3e3".toColorInt())
    }
}

fun vibra_conf (context: Activity, pref: SharedPreferences, list: LongArray) {
    if (pref.getBoolean("vibra", false)) {
        (context.getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createWaveform(list, -1))
    }
}