package com.nothingsecure

import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.shapes.Shape
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.inputmethodservice.ExtractEditText
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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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

class RegisterActivity : AppCompatActivity(), SensorEventListener {
    private var start = false
    private lateinit var sensor_manager: SensorManager
    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.register_main)

        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES

        val info_close = findViewById<TextView>(R.id.info_close)
        val information = findViewById<TextView>(R.id.information_register)
        val input_pass = findViewById<EditText>(R.id.input_password)
        val pass_visi = findViewById<ConstraintLayout>(R.id.password_visibility)
        val icon = findViewById<ShapeableImageView>(R.id.visibility_icon)
        val progress = findViewById<LinearProgressIndicator>(R.id.progress_pass)
        val create = findViewById<ConstraintLayout>(R.id.create_password)
        val opor = findViewById<TextView>(R.id.opor)
        val derived_check = findViewById<CheckBox>(R.id.derived_check)
        val info_derived = findViewById<ShapeableImageView>(R.id.info_derived)
        derived_check.visibility = View.INVISIBLE
        info_derived.visibility = View.INVISIBLE
        var pass_see = false

        val mk = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val pref = EncryptedSharedPreferences.create(this, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        pref.edit().putString("key_u", "").commit()
        input_pass.isLongClickable = false
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
            info_derived.visibility = View.VISIBLE
            derived_check.visibility = View.VISIBLE
        }


        if (pref.getBoolean("close", false)) {
            pref.edit().putBoolean("close", false).commit()
            info_close.text = "The last time the app closed due to a sudden movement"
        }
        pass_visi.setOnClickListener {
            visibility(pass_see, icon, input_pass)
            pass_see = !pass_see

        }

        input_pass.addTextChangedListener {dato ->
            if (pref.getBoolean("start", false)) {
                try {
                    if (dato?.length == pref.getInt("size", 0)) {
                        if (MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(dato.toString().toByteArray() + Base64.getDecoder().decode(pref.getString("salt_very", ""))), Base64.getDecoder().decode(pref.getString("hash", ""))) ) {
                            if (BiometricManager.from(applicationContext).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {

                                BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                                    object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            super.onAuthenticationSucceeded(result)
                                            add_register(applicationContext, "Successful login", "#40aa47")

                                            pref.edit().putString("key_u", input_pass.text.toString()).commit()
                                           startActivity(Intent(applicationContext, MainActivity::class.java))
                                            finish()
                                        }

                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            super.onAuthenticationError(errorCode, errString)
                                            input_pass.setText("")
                                            recreate()
                                        }
                                    }).authenticate(promt("Who are you?"))
                            }
                        } else {
                            add_register(this, "Login failed", "#aa4040")
                            input_pass.setText("")

                            pref.edit().putInt("opor", pref.getInt("opor", 9) - 1).commit()

                            if (pref.getInt("opor", 9) == 0) {
                                pref.edit().putBoolean("block", true).commit()
                                recreate()
                            } else {
                                opor.text = " *".repeat(pref.getInt("opor", 0))
                            }
                        }
                        pref.edit().putString("salt_very", Base64.getEncoder().withoutPadding().encodeToString(
                            SecureRandom().generateSeed(16))).commit()
                        pref.edit().putString("hash", Base64.getEncoder().withoutPadding().encodeToString(
                            MessageDigest.getInstance("SHA-256").digest(dato.toString().toByteArray() + Base64.getDecoder().decode(pref.getString("salt_very", ""))))).commit()
                    }
                } catch (e: Exception) {
                    pref.edit().putString("key_u", "").commit()
                    Toast.makeText(this, "An error arose", Toast.LENGTH_SHORT).show()
                    input_pass.setText("")
                    recreate()
                }
            }
            entropy(dato.toString(), progress)
            System.gc()
        }


        create.setOnClickListener {
            if (input_pass.text!!.isNotEmpty() && input_pass.text!!.length >= 8) {
                val ali = Random.nextBytes(10)
                try {
                    val animation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.trasnlate)
                    if (!derived_check.isChecked) {
                        val kgs = KeyGenParameterSpec.Builder(input_pass.text.toString(), KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .build()
                        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                        kg.init(kgs)
                        kg.generateKey()
                    }else {
                        pref.edit().putString("salt", Base64.getEncoder().withoutPadding().encodeToString(SecureRandom().generateSeed(16))).commit()
                        pref.edit().putInt("it_def", 600000).commit()
                    }

                    val kgs_2 = KeyGenParameterSpec.Builder(ali.toString(), KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .build()
                    val kg_2 = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                    kg_2.init(kgs_2)
                    kg_2.generateKey()

                    pref.edit().putString("key_u", input_pass.text.toString()).commit()
                    pref.edit().putString("key", ali.toString()).commit()
                    pref.edit().putBoolean("start", true).commit()
                    pref.edit().putInt("size", input_pass.text.toString().length).commit()
                    pref.edit().putString("salt_very", Base64.getEncoder().withoutPadding().encodeToString(SecureRandom().generateSeed(16)))
                    pref.edit().putString("hash", Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(input_pass.text.toString().toByteArray() + Base64.getDecoder().decode(pref.getString("salt_very", ""))))).commit()

                    animation.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationEnd(ani: Animation?) {
                            create.visibility = View.INVISIBLE
                            startActivity(Intent(applicationContext, MainActivity::class.java))
                            finish()
                        }

                        override fun onAnimationRepeat(p0: Animation?) {}

                        override fun onAnimationStart(p0: Animation?) {
                            create.isEnabled = false
                            input_pass.isEnabled = false
                        }


                    })

                    create.startAnimation(animation)
                }catch (e: Exception) {
                    Log.e("Error in key generation", e.toString())
                    pref.edit().putString("key_u", "").commit()
                    pref.edit().putString("salt", "").commit()
                    pref.edit().putString("key", "").commit()
                    pref.edit().putBoolean("start", false).commit()
                    pref.edit().putInt("size", 0).commit()
                    pref.edit().putString("hash", "").commit()

                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                    finishAffinity()
                } finally {
                    ali.fill(0)
                }

            }else {
                Toast.makeText(this, "8 or more characters, please", Toast.LENGTH_SHORT).show()
            }
        }

        info_derived.setOnClickListener {

            val info_dialog = MaterialAlertDialogBuilder(this)

            info_dialog.setTitle("Cryptographic operation of Nothing k")
            info_dialog.setMessage("In Nothing K your password is the alias of a symmetric key stored in AndroidKeyStore, with this option your password will be transformed into a derived key, which means that this key will only be generated if the password is correct.")
            info_dialog.setPositiveButton("Ok"){_, _ ->}
            info_dialog.show()
        }


        sensor_manager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor_manager.registerListener(this, sensor_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)

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

    override fun onDestroy() {
        super.onDestroy()
        sensor_manager.unregisterListener(this)
    }
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

            if (start) {
                force(this, event.values[0], event.values[1], event.values[2])
            }
            x_regi = event.values[0]
            y_regi = event.values[1]
            z_regi = event.values[2]
            start = true
        }
    }
}