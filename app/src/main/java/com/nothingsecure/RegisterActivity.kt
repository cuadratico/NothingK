package com.nothingsecure

import android.animation.Animator
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import kotlin.random.Random

class RegisterActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.register_main)

        val information = findViewById<TextView>(R.id.information_register)
        val input_pass = findViewById<EditText>(R.id.input_password)
        val pass_visi = findViewById<ConstraintLayout>(R.id.password_visibility)
        val icon = findViewById<ShapeableImageView>(R.id.visibility_icon)
        val progress = findViewById<LinearProgressIndicator>(R.id.progress_pass)
        val create = findViewById<ConstraintLayout>(R.id.create_password)
        val opor = findViewById<TextView>(R.id.opor)
        var pass_see = false

        val mk = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val pref = EncryptedSharedPreferences.create(this, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        fun block_mode() {
            val dialog = Dialog(this)
            val view = LayoutInflater.from(this).inflate(R.layout.block, null)

            val time = view.findViewById<TextView>(R.id.time)

            val scope = lifecycleScope.launch (Dispatchers.IO, start = CoroutineStart.LAZY){
                for (tim in (60 * pref.getInt("multi", 1)).downTo(0)) {
                    withContext(Dispatchers.Main) {
                        time.text = tim.toString()
                    }
                    delay(1000)
                }
                pref.edit().putBoolean("block", false).commit()
                pref.edit().putInt("opor", 9).commit()
                pref.edit().putInt("multi", pref.getInt("multi", 1) + 1).commit()
                withContext(Dispatchers.Main) {
                    recreate()
                }
            }

            scope.start()

            dialog.setContentView(view)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setCancelable(false)
            dialog.show()
        }

        if (pref.getBoolean("block", false)) {
            block_mode()
        }

        if (pref.getBoolean("start", false)) {
            information.text = "Put your password"
            create.visibility = View.INVISIBLE
            opor.text = " *".repeat(pref.getInt("opor", 9))
        } else {
            opor.visibility = View.INVISIBLE
        }


        pass_visi.setOnClickListener {
            if (!pass_see) {
                pass_see = true
                icon.setImageResource(R.drawable.open_eye)
                input_pass.transformationMethod = null
            }else {
                pass_see = false
                icon.setImageResource(R.drawable.close_eye)
                input_pass.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            input_pass.setSelection(input_pass.text.length)

        }

        input_pass.addTextChangedListener {dato ->


            if (pref.getBoolean("start", false)) {
                val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                val db = db(this)

                if (dato?.length == pref.getInt("size", 0)){
                    if ( ks.getKey(dato.toString(), null) != null && Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(dato.toString().toByteArray())) == pref.getString("hash", "")) {

                        if (BiometricManager.from(applicationContext).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                            val promt = BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Who are you?")
                                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                                .build()

                            BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    val c = Cipher.getInstance("AES/GCM/NoPadding")
                                    c.init(Cipher.ENCRYPT_MODE, ks.getKey(pref.getString("key", ""), null))

                                    add_register(applicationContext, "Successful login", "#40aa47")

                                    val intent = Intent(applicationContext, MainActivity::class.java)
                                        .putExtra("ali", input_pass.text.toString())
                                    startActivity(intent)
                                    finish()
                                }

                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    super.onAuthenticationError(errorCode, errString)
                                    input_pass.setText("")
                                    recreate()
                                }
                            }).authenticate(promt)
                        }
                    }else {
                        val c = Cipher.getInstance("AES/GCM/NoPadding")
                        c.init(Cipher.ENCRYPT_MODE, ks.getKey(pref.getString("key", ""), null))

                        add_register(this, "Login failed", "#aa4040")
                        input_pass.setText("")

                        pref.edit().putInt("opor", pref.getInt("opor", 9) - 1).commit()

                        if (pref.getInt("opor", 9) == 0) {
                            pref.edit().putBoolean("block", true).commit()
                            recreate()
                        }else {
                            opor.text = " *".repeat(pref.getInt("opor", 0))
                        }
                    }
                }


            }

            entropy(dato.toString(), progress)
            System.gc()
        }


        create.setOnClickListener {
            if (input_pass.text.isNotEmpty()) {
                val animation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.trasnlate)

                val kgs = KeyGenParameterSpec.Builder(input_pass.text.toString(), KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .build()
                val kg =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                kg.init(kgs)
                kg.generateKey()


                val ali = Random.nextBytes(10).toString()
                val kgs_2 = KeyGenParameterSpec.Builder(ali, KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .build()
                val kg_2 = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                kg_2.init(kgs_2)
                kg_2.generateKey()

                pref.edit().putString("key", ali).commit()
                pref.edit().putBoolean("start", true).commit()
                pref.edit().putInt("size", input_pass.text.toString().length).commit()
                pref.edit().putString("hash", Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(input_pass.text.toString().toByteArray()))).commit()

                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationEnd(ani: Animation?) {
                        create.visibility = View.INVISIBLE
                        val intent = Intent(applicationContext, MainActivity::class.java)
                            .putExtra("ali", input_pass.text.toString())
                        startActivity(intent)
                        finish()
                    }

                    override fun onAnimationRepeat(p0: Animation?) {}

                    override fun onAnimationStart(p0: Animation?) {
                        create.isEnabled = false
                        input_pass.isEnabled = false
                    }


                })

                create.startAnimation(animation)
            }else {
                Toast.makeText(this, "You must specify a password", Toast.LENGTH_SHORT).show()
            }
        }


        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}