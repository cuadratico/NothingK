package com.nothingsecure

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.graphics.toColorInt
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
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

fun pass_generator (ini_list: List<List<Char>>, size: Int): String {

    val bool_list = mutableListOf<Boolean>()

    for (i in 0..ini_list.size - 1) {
        bool_list.add(false)
    }

    var final_list = ""

    for (position in ini_list) {
        final_list += position.joinToString("")
    }

    var pass = ""

    while (true) {
        pass = ""
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

@RequiresApi(Build.VERSION_CODES.O)
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


@RequiresApi(Build.VERSION_CODES.O)
fun derived_Key ( password: String, salt: String): SecretKey {
    val spec = PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), 600_00, 256)
    val gen = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded

    return SecretKeySpec(gen, "AES")
}


@RequiresApi(Build.VERSION_CODES.O)
fun deri_expressed (context: Context, password: String, salt: String): Key {
    val mk = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    val pref = EncryptedSharedPreferences.create (context, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)


    if (pref.getBoolean("deri", false)) {
        return derived_Key(password, salt)
    }else {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        Log.e("clave", pref.getString("key_u", "").toString())
        return ks.getKey(pref.getString("key_u", ""), null)
    }
}


fun visibility (visi: Boolean, icon: ShapeableImageView, input: EditText) {
    if (visi) {
        icon.setImageResource(R.drawable.close_eye)
        input.transformationMethod = PasswordTransformationMethod.getInstance()
    }else {
        icon.setImageResource(R.drawable.open_eye)
        input.transformationMethod = null
    }
    input.setSelection(input.text.length)
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