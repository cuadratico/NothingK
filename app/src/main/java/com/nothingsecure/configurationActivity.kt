package com.nothingsecure

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.shapes.Shape
import android.health.connect.datatypes.AppInfo
import android.icu.util.VersionInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatSpinner
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.nothingsecure.db.Companion.pass_list
import com.nothingsecure.recy_information.conf_adapter
import com.nothingsecure.recy_information.conf_adapter.Companion.colors_list
import com.nothingsecure.recy_information.conf_adapter.Companion.mods_all
import com.nothingsecure.recy_information.conf_adapter.Companion.mods_recy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.Key
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

class configurationActivity : AppCompatActivity() {
    companion object {
        lateinit var dialog_conf: Dialog
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_configuration)

        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        val mk = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val pref = EncryptedSharedPreferences.create(this, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)


        val info = findViewById<ShapeableImageView>(R.id.info)

        // edit icon part
        val see_pass = findViewById<AppCompatButton>(R.id.see_pass)
        val pass_all = findViewById<TextInputLayout>(R.id.pass_all)
        val icons_all = findViewById<ConstraintLayout>(R.id.icons_all)
        val edit_icon = findViewById<ShapeableImageView>(R.id.edit_icon)
        pass_all.visibility = View.INVISIBLE

        val edit_button = findViewById<ShapeableImageView>(R.id.edit_icons_button)
        val fun_spinner = findViewById<AppCompatSpinner>(R.id.fun_spinner)
        val check_dialog = findViewById<AppCompatCheckBox>(R.id.dialog_show_check)
        fun_spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mods_recy)
        check_dialog.isChecked = pref.getBoolean("mod_dialog", true)

        // pass modify part
        val input_pass = findViewById<EditText>(R.id.input_pass)
        val progress_pass = findViewById<LinearProgressIndicator>(R.id.progress)

        // edit color part
        val edit_color_button = findViewById<ShapeableImageView>(R.id.edit_color)
        val back_color = findViewById<ConstraintLayout>(R.id.back_color)
        val color_code = findViewById<TextView>(R.id.color_code)

        // edit pass type
        val pass_type = findViewById<AppCompatButton>(R.id.pass_type_modify)
        val info_type = findViewById<ShapeableImageView>(R.id.info_key_type)

        val version = findViewById<TextView>(R.id.version_info)

        fun spec_all () {
            val color = ColorStateList.valueOf(pref.getString("color_back", "#FF000000")!!.toColorInt())
            icons_all.backgroundTintList = color
            edit_icon.setImageResource(mods_all.get(pref.getString("multi_but_icon", "Delete all"))!!)
            fun_spinner.setSelection(mods_recy.indexOf(pref.getString("multi_but_text", "Delete all")))


            back_color.backgroundTintList = color
            color_code.text = pref.getString("color_back", "#FF000000")
        }
        spec_all()

        fun dialog (view: Int, type: Int = 0) {
            dialog_conf = Dialog(this)
            val dialog_view = LayoutInflater.from(this).inflate(R.layout.dialog_config_expressed, null)

            val recy = dialog_view.findViewById<RecyclerView>(R.id.multi_recy)

            recy.adapter = conf_adapter(if (type == 0) { mods_recy } else { colors_list }, view, type)
            recy.layoutManager = LinearLayoutManager(this)

            dialog_conf.setContentView(dialog_view)
            dialog_conf.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog_conf.show()

            dialog_conf.setOnDismissListener (object: DialogInterface.OnDismissListener {
                override fun onDismiss(p0: DialogInterface?) {
                    spec_all()
                }

            })
        }



        // edit icon part code

        edit_button.setOnClickListener {
            dialog(R.layout.dialog_icon_selector)
        }

        check_dialog.setOnCheckedChangeListener( object: CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(p0: CompoundButton, check: Boolean) {
                pref.edit().putBoolean("mod_dialog", check).commit()
            }
        })



        fun_spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                pref.edit().putString("multi_but_text", mods_recy[position]).commit()
                add_register(applicationContext, "The button mode has been changed to \"${pref.getString("multi_but_text", "Delete all")}\" ")
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // edit pass part code
        input_pass.setText(pref.getString("key_def", pref.getString("key_u", "")))

        input_pass.addTextChangedListener {
            entropy(it.toString(), progress_pass)
            if (it!!.isNotEmpty()) {
                pref.edit().putString("key_def", it.toString()).commit()
            }
        }

        see_pass.setOnClickListener {
            BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    see_pass.visibility = View.INVISIBLE
                    pass_all.visibility = View.VISIBLE
                    entropy(input_pass.text.toString(), progress_pass)
                }
            }).authenticate(promt())
        }

        // edit color part code

        edit_color_button.setOnClickListener {
            dialog(R.layout.dialog_config_color, 1)
        }

        info.setOnClickListener {
            MaterialAlertDialogBuilder(this).apply {
                setTitle("Do you want to give your opinion on Nothing K?")
                setPositiveButton("Yes") { _, _ -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/EWnhgBtgu5jCB3Fa9"))) }
                setNegativeButton("No") { _, _ -> }
            }.show()
        }

        // pass type part code

        fun modi_type_ui () {
            if (pref.getBoolean("deri", false)) {
                pass_type.text = "Android KeyStore"
            }
        }
        modi_type_ui()

        pass_type.setOnClickListener {

            if (pref.getBoolean("db_sus", true) && pref.getBoolean("desen_pass", false)) {


                BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        val load_dialog = load("Changing key type...", this@configurationActivity)

                        lifecycleScope.launch(Dispatchers.IO) {

                            pref.edit().putBoolean("deri", !pref.getBoolean("deri", false)).commit()
                            if (pref.getBoolean("deri", false)) {
                                pref.edit().putString("salt", Base64.getEncoder().withoutPadding().encodeToString(
                                    SecureRandom().generateSeed(16))).commit()
                                val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                                ks.deleteEntry(pref.getString("key_u", pref.getString("key_u_r", "")))
                            } else {
                                pref.edit().putString("salt", "").commit()
                                val kgs = KeyGenParameterSpec.Builder(pref.getString("key_u", pref.getString("key_u_r", "")).toString(), KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT).apply {
                                    setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                    setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                }
                                    .build()

                                val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply { init(kgs) }
                                kg.generateKey()
                            }

                            val db = db(applicationContext)
                            if (pass_list.isNotEmpty() || db.select_pass()) {
                                db.delete_prin()
                                var key: Key? = deri_expressed(applicationContext, pref.getString("key_u", pref.getString("key_u_r", "")).toString(), pref.getString("salt", "").toString())
                                try {
                                    for (position in 0..pass_list.size - 1) {
                                        val (id, pass, information, iv) = pass_list[position]
                                        val c = Cipher.getInstance("AES/GCM/NoPadding")
                                        c.init(Cipher.ENCRYPT_MODE, key)
                                        db.add_pass(Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(pass_list[position].pass.toByteArray())), information, Base64.getEncoder().withoutPadding().encodeToString(c.iv))
                                    }
                                } catch (e: Exception) {
                                    Log.e("Mode modification error", e.toString())
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(applicationContext, "Mode modification error", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    key = null
                                    withContext(Dispatchers.Main) {
                                        load_dialog.dismiss()
                                        Toast.makeText(applicationContext, "The app needs to be restarted", Toast.LENGTH_SHORT).show()
                                        finishAffinity()
                                    }
                                }
                            }else {
                                withContext(Dispatchers.Main) {
                                    load_dialog.dismiss()
                                    Toast.makeText(applicationContext, "There are no passwords", Toast.LENGTH_SHORT).show()
                                }
                            }
                            add_register(applicationContext, "Your password's operating mode has been changed to: ${pass_type.text}")
                            withContext(Dispatchers.Main) {
                                modi_type_ui()
                            }
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(applicationContext, "You need authentication", Toast.LENGTH_SHORT).show()
                    }
                }).authenticate(promt("Do you want to change the mode?"))

            }else {
                Toast.makeText(this, "Are your passwords decrypted?", Toast.LENGTH_SHORT).show()
            }
        }

        info_type.setOnClickListener {
            val dialog_info_type = MaterialAlertDialogBuilder(this)
                .setTitle("Where is my key stored?")
                .setMessage("Currently, your key is an ${if (pref.getBoolean("deri", false)) { "Derived Key" } else { "Androi KeyStore" } }.\n" +
                        "When I talk about Android KeyStore, I'm referring to Android's cryptographic key storage. Your cryptographic key is stored there and protected by your password. When I talk about derived keys, I'm referring to the use of the PBKDF2 algorithm. This algorithm derives the key from your password. Basically, the cryptographic key is created from your password, which means it's never stored.")
                .setPositiveButton("Ok") {_, _ ->}
            dialog_info_type.show()
        }

        version.text = this.packageManager.getPackageInfo(packageName, 0).packageName

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

    override fun onPause() {
        super.onPause()
        finish()
    }
}