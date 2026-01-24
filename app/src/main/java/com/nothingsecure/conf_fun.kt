package com.nothingsecure

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import com.nothingsecure.db.Companion.pass_list
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64


fun delete_all_fun (contex: Context, pref: SharedPreferences) {
    val db = db(contex)
    pref.edit().clear().commit()
    db.delete_all()
    pass_list = listOf()
    contex.cacheDir.deleteRecursively()
    contex.externalCacheDir?.deleteRecursively()

    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    ks.deleteEntry(pref.getString("key_u", ""))
}