package com.nothingsecure

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nothingsecure.db.Companion.pass_list
import org.json.JSONArray
import org.json.JSONObject
import java.security.Key
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

@RequiresApi(Build.VERSION_CODES.O)
fun export (context: Context, password: String, salt: String, arch_name: String, rout: String) {
    var key: Key? = derived_Key(password, salt)
    try {
        val json_array = JSONArray()
        for (position in 0..pass_list.size - 1) {
            val (id, pass, info) = pass_list[position]
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, key)


            val data = JSONObject().apply {
                put(
                    "pass",
                    Base64.getEncoder().withoutPadding()
                        .encodeToString(c.doFinal(pass.toByteArray()))
                )
                put("info", info)
                put("iv", Base64.getEncoder().withoutPadding().encodeToString(c.iv))
            }

            json_array.put(data)
        }

        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key)

        val data_pro = JSONObject().apply {
            put(
                "value",
                Base64.getEncoder().withoutPadding()
                    .encodeToString(c.doFinal("Nothingk".toByteArray()))
            )
            put("iv", Base64.getEncoder().withoutPadding().encodeToString(c.iv))
        }
        val array_pro = JSONArray().apply {
            put(data_pro)
        }
        val archive_json = JSONObject().apply {
            put("pro", array_pro)
            put("salt", salt)
            put("pass_list", json_array)
        }

        val content_archive = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, "$arch_name.nk")
            put(MediaStore.Files.FileColumns.MIME_TYPE, "application/nk")
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, rout)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Files.getContentUri("external"),
            content_archive
        )

        context.contentResolver.openOutputStream(uri!!)!!
            .write(archive_json.toString().toByteArray())
        add_register(context, "Your passwords have been exported")
    }catch (e: Exception) {
        Log.e("Export error", e.toString())
    } finally {
        key = null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun import (context: Context, json: JSONObject, pass: String, rep: Boolean = true) {

    var key: Key? = derived_Key(pass, json.getString("salt"))

    try {
        pass_list.clear()
        val mk = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val pref = EncryptedSharedPreferences.create(context, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)


        val array_pass = json.getJSONArray("pass_list")


        val db = db(context)
        if (pref.getBoolean("db_sus", true)) {
            if (rep) {
                db.delete_prin()
            }
        } else {
            pref.edit().putBoolean("deri", true).commit()
            pref.edit().putString("key_u_r", pref.getString("key_u", "")).commit()
            pref.edit().putString("key_u", pass).commit()
        }

        for (pass in 0..array_pass.length() - 1) {
            val posi = array_pass.getJSONObject(pass)

            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.getDecoder().decode(posi.getString("iv"))))

            val desen_pass = c.doFinal(Base64.getDecoder().decode(posi.getString("pass")))
            if (pref.getBoolean("db_sus", true)) {
                val c_db = Cipher.getInstance("AES/GCM/NoPadding")
                c_db.init(Cipher.ENCRYPT_MODE, deri_expressed(context, pref.getString("key_u", "")!!, pref.getString("salt", "")!!))
                db.add_pass(Base64.getEncoder().withoutPadding().encodeToString(c_db.doFinal(desen_pass)), posi.getString("info"), Base64.getEncoder().withoutPadding().encodeToString(c_db.iv))
            } else {
                pass_list.add(pass(pass, String(desen_pass), posi.getString("info"), posi.getString("iv")))
            }
        }

        add_register(context, "Passwords have been imported")
    } catch (e: Exception) {
        Log.e("Import error", e.toString())
    } finally {
        key = null
    }
}