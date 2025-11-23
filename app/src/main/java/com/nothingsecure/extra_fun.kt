package com.nothingsecure

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Environment
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.graphics.toColorInt
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nothingsecure.configurationActivity.Companion.backup_list
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.Key
import java.security.KeyStore
import java.security.SecureRandom
import java.time.InstantSource.system
import java.time.LocalDateTime
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs
import kotlin.math.log2


val mayusculas_l = ('A'..'Z').joinToString("").toList()
val numeros_l = (0..9).joinToString ("").toList()
val minusculas_l = ('a'..'z').joinToString("").toList()

val simbolos_l = listOf("@#$|?¿¡!").joinToString("").toList()
@SuppressLint("ResourceAsColor")
fun entropy(pass: String, porgress: LinearProgressIndicator){



    var simbolos = 0
    var minusculas = 0
    var mayusculas = 0
    var numeros = 0

    for (valor in pass) {

        if (minusculas_l.contains(valor)) {
            if (minusculas != 26 ) {minusculas += 26}
        }else if (mayusculas_l.contains(valor)) {
            if (mayusculas != 26) {mayusculas += 26}
        }else if (numeros_l.contains(valor)) {
            if (numeros != 9) {numeros += 10}
        }else {
            simbolos ++
        }
    }

    val final =  pass.length * log2((simbolos + mayusculas + minusculas + numeros).toDouble())

    porgress.progress = final.toInt()
    if (final in 0.0..40.0) {
        porgress.setIndicatorColor("#aa4040".toColorInt())
    }else if (final in 40.0..60.0) {
        porgress.setIndicatorColor("#c9a23e".toColorInt())
    }else if (final > 60.0){
        porgress.setIndicatorColor("#40aa47".toColorInt())
    }else {
        porgress.setIndicatorColor("#e3e3e3".toColorInt())
    }
}

fun pass_generator (size: Int, ini_list: List<List<Char>> = listOf(mayusculas_l, minusculas_l, simbolos_l, numeros_l)): String {

    val bool_list = mutableListOf<Boolean>()

    for (i in 0..ini_list.size - 1) {
        bool_list.add(false)
    }

    var final_list = ""

    for (position in ini_list) {
        final_list += position.joinToString("")
    }


    while (true) {
        var pass = ""
        for (i in 0..size - 1) {
            pass += final_list.toList().shuffled()[0]
        }

        for (value in pass) {

            var final = true
            for (position in 0..bool_list.size - 1) {
                if (ini_list[position].contains(value) && !bool_list[position]) {
                        bool_list[position] = true
                }
                final = final and bool_list[position]
            }
            Log.e("bool_list", bool_list.toString())
            if (final) {
                return pass
            }


        }
    }

}

fun add_register (context: Context, note: String, color: String = "#1b1b1d") {
    val mk = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    val pref = EncryptedSharedPreferences.create (context, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    val c = Cipher.getInstance("AES/GCM/NoPadding")
    c.init(Cipher.ENCRYPT_MODE, ks.getKey(pref.getString("key", ""), null))

    val db = db(context)

    db.add_register(Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(LocalDateTime.now().toString().split("T").joinToString(" - ").toByteArray())), note, color,
        Base64.getEncoder().withoutPadding().encodeToString(c.iv))

}


fun derived_Key ( password: String, salt: String, iter: Int): SecretKey {
    val spec = PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), iter, 256)
    val gen = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded

    return SecretKeySpec(gen, "AES")
}


fun deri_expressed (context: Context, password: String, salt: String, iter: Int): Key {
    val mk = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    val pref = EncryptedSharedPreferences.create (context, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)


    if (pref.getBoolean("deri", false)) {
        return derived_Key(password, salt, iter)
    }else {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return ks.getKey(pref.getString("key_u", ""), null)
    }
}


@SuppressLint("ResourceAsColor")
fun visibility (pref: SharedPreferences, icon: ShapeableImageView, input: EditText) {
    if (pref.getBoolean("prims", false)) {
        icon.setImageResource(R.drawable.prism_rell)
        input.setTextColor("#27272A".toColorInt())
    }else {
        icon.setImageResource(R.drawable.prism_no_rell)
        input.setTextColor("#e3e3e3".toColorInt())
    }
}

var x_regi = 0f
var y_regi = 0f
var z_regi = 0f

fun force (context: Activity,  x: Float, y: Float, z: Float) {

    if (abs(x_regi - x) > 12 || abs(y_regi - y) > 12 || abs(z_regi - z) > 12) {
        val mk = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val pref = EncryptedSharedPreferences.create(context, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        pref.edit().putBoolean("close", true).commit()
        context.finishAffinity()
    }

}

fun promt (title: String = "Authenticate yourself"): BiometricPrompt.PromptInfo {

    return BiometricPrompt.PromptInfo.Builder().apply {
        setTitle(title)
        setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
        setConfirmationRequired(true)
    }
        .build()

}

fun load (info: String, context: Context): Dialog {
    val load_dialog = Dialog(context)
    val load_view = LayoutInflater.from(context).inflate(R.layout.load, null)

    val progress = load_view.findViewById<ProgressBar>(R.id.progress)
    val load_information = load_view.findViewById<TextView>(R.id.load_information)
    load_information.text = info
    progress.isActivated = true

    load_dialog.setContentView(load_view)
    load_dialog.setCancelable(false)
    load_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    load_dialog.show()

    return load_dialog
}

fun backup (context: Context, ins: Int, acti: Activity? = null) {
    val mk = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    val pref = EncryptedSharedPreferences.create(context, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

    if (ins == pref.getInt("backup_ins", 0) && pref.getBoolean("desen_pass", false)) {
        val load_dilaog = load("Making the backup", context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                export(context, pref.getString("backup_pass", pref.getString("key_def", pref.getString("key_u", ""))).toString(), Base64.getEncoder().withoutPadding().encodeToString(SecureRandom().generateSeed(16)), pref.getString("backup_file", "NothingK-backup").toString(), Environment.DIRECTORY_DOWNLOADS, pref.getInt("it_up", 600000))
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "The backup could not be performed", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    load_dilaog.dismiss()
                    if (acti != null) {
                        acti.finishAffinity()
                    }
                }
            }
        }
    }else {
        if (acti != null) {
            acti.finishAffinity()
        }
    }
}