package com.nothingsecure

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.shapes.Shape
import android.hardware.Sensor
import android.hardware.SensorManager
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
import android.widget.CheckBox
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
import com.google.android.material.materialswitch.MaterialSwitch
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
        val backup_list = listOf("Never", "When adding a password", "When editing a password", "When deleting a password", "When putting the app in the background", "When modifying the key handling", "When exporting", "When importing in any mode", "When importing in 'Add' mode", "When importing in 'Preview' mode", "When importing in 'Replace' mode")
    }


    @SuppressLint("MissingInflatedId")
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
        val pass_info = findViewById<TextView>(R.id.key_type)

        // edit color part
        val edit_color_button = findViewById<ShapeableImageView>(R.id.edit_color)
        val back_color = findViewById<ConstraintLayout>(R.id.back_color)
        val color_code = findViewById<TextView>(R.id.color_code)

        // edit pass part
        val pass_type = findViewById<AppCompatButton>(R.id.pass_type_modify)
        val info_type = findViewById<ShapeableImageView>(R.id.info_key_type)


        // Edit accelerometer detection
        val info_acele = findViewById<ShapeableImageView>(R.id.info_acele_mode)
        val acele_switch = findViewById<MaterialSwitch>(R.id.acele_switch)


        // Edit the backup time
        val info_backup_mode = findViewById<ShapeableImageView>(R.id.info_backup_modes)
        val back_up_spinner = findViewById<AppCompatSpinner>(R.id.backup_spinner)
        val back_up_settings = findViewById<AppCompatButton>(R.id.backup_settings)

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

        pass_info.text = "Modify the treatment of your encryption key\n\n You are currently in \"${ if (pref.getBoolean("deri", false)) { "Derived Key" } else { "Android KeyStore" } }\" mode"
        fun modi_type_ui () {
            if (pref.getBoolean("deri", false)) {
                pass_type.text = "Android KeyStore"
            }
        }
        modi_type_ui()

        info_type.setOnClickListener {
            val dialog_info_type = MaterialAlertDialogBuilder(this)
                .setTitle("Where is my key stored?")
                .setMessage("Currently, your key is an ${if (pref.getBoolean("deri", false)) { "Derived Key" } else { "Android KeyStore" } }.\n" +
                        "When I talk about Android KeyStore, I'm referring to Android's cryptographic key storage. Your cryptographic key is stored there and protected by your password. When I talk about derived keys, I'm referring to the use of the PBKDF2 algorithm. This algorithm derives the key from your password. Basically, the cryptographic key is created from your password, which means it's never stored.")
                .setPositiveButton("Ok") {_, _ ->}
            dialog_info_type.show()
        }
        @RequiresApi(Build.VERSION_CODES.O)
        fun pass_modi() {
            pref.edit().putBoolean("deri", !pref.getBoolean("deri", false)).commit()
            if (pref.getBoolean("deri", false)) {
                pref.edit().putString("salt", Base64.getEncoder().withoutPadding().encodeToString(SecureRandom().generateSeed(16))).commit()
                val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                ks.deleteEntry(pref.getString("key_u", pref.getString("key_u_r", "")))
                pref.edit().putInt("it_def", 600000).commit()
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
        }

        pass_type.setOnClickListener {

            if (pref.getBoolean("db_sus", true) && pref.getBoolean("desen_pass", false)) {


                BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        val load_dialog = load("Changing key type...", this@configurationActivity)

                        lifecycleScope.launch(Dispatchers.IO) {

                            val db = db(applicationContext)
                            pass_modi()
                            if (pass_list.isNotEmpty()) {
                                val key = deri_expressed(applicationContext, pref.getString("key_u", pref.getString("key_u_r", "")).toString(), pref.getString("salt", "").toString(), pref.getInt("it_def", 60000))
                                val id_final = pass_list[pass_list.size - 1].id
                                var id_plus = 0
                                try {
                                    for (position in 0..pass_list.size - 1) {
                                        val (id, pass, information, iv) = pass_list[position]
                                        val c = Cipher.getInstance("AES/GCM/NoPadding")
                                        c.init(Cipher.ENCRYPT_MODE, key)
                                        db.add_pass(Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(pass_list[position].pass.toByteArray())), information, Base64.getEncoder().withoutPadding().encodeToString(c.iv))
                                        id_plus ++
                                    }
                                    db.delete_speci(pass_list[0].id, id_final)
                                } catch (e: Exception) {
                                    Log.e("Mode modification error", e.toString())
                                    pass_modi()
                                    db.delete_speci(id_final + 1, pass_list[pass_list.size - 1].id + id_plus)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(applicationContext, "Mode modification error", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    if (key.encoded != null) {
                                        key.encoded.fill(0)
                                    }
                                }
                            }
                            withContext(Dispatchers.Main) {
                                load_dialog.dismiss()
                                backup(this@configurationActivity, 5, this@configurationActivity)
                                Toast.makeText(applicationContext, "The app needs to be restarted", Toast.LENGTH_SHORT).show()
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

        // Edit accelerometer detection part code

        acele_switch.isChecked = pref.getBoolean("ace_force", true)

        info_acele.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("What is the accelerometer used for in Nothing K?")
                .setMessage("The accelerometer is used to detect forceful acts. Basically, when a sudden movement is detected, the app closes and the movement is recorded.\n" +
                        "This way, thefts involving forceful acts can be prevented.")
                .setPositiveButton("Ok") {_, _ ->}
                .show()
        }

        acele_switch.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(p0: CompoundButton, check: Boolean) {

                BiometricPrompt(this@configurationActivity, ContextCompat.getMainExecutor(applicationContext), object: BiometricPrompt.AuthenticationCallback() {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        pref.edit().putBoolean("ace_force", check).commit()
                        if(check) {
                            add_register(applicationContext, "Accelerometer recording has been activated")
                        }else {
                            add_register(applicationContext, "Accelerometer recording has been disabled")
                        }

                        MaterialAlertDialogBuilder(this@configurationActivity)
                            .setTitle("Nothing K needs to be restarted to apply changes")
                            .setMessage("If you don't restart the app the changes will be applied the next time you log in.")
                            .setPositiveButton("Reboot") {_, _ ->
                                Toast.makeText(applicationContext, "Changes applied", Toast.LENGTH_SHORT).show()
                                finishAffinity()
                            }
                            .setNegativeButton("Later") {_, _ -> }
                            .show()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(applicationContext, "Authentication error", Toast.LENGTH_SHORT).show()
                        acele_switch.isChecked = !check
                    }
                }).authenticate(promt())
            }

        })

        // Edit the backup time code part

        info_backup_mode.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("What are backup moments?")
                .setMessage("These are scheduled times when a backup of your entire database will be made. It will be encrypted with your default password and the file will be named NothingK-backup.nk (you can change all of this with the \"Settings\" button in the backup menu).")
                .setPositiveButton("Ok"){_, _ ->}
                .show()
        }

        back_up_spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, backup_list)
        back_up_spinner.setSelection(pref.getInt("backup_ins", 0))
        back_up_spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, item: Int, p3: Long) {
                pref.edit().putInt("backup_ins", item).commit()
                add_register(applicationContext, "Now a backup will be made ${backup_list[item]}")
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

        }

        back_up_settings.setOnClickListener {
            BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val settings_dialog = Dialog(this@configurationActivity)
                    val settings_view = LayoutInflater.from(this@configurationActivity).inflate(R.layout.backup_settings, null)

                    val settings_input_pass = settings_view.findViewById<EditText>(R.id.input_pass)
                    val settings_pass_progress = settings_view.findViewById<LinearProgressIndicator>(R.id.progress)
                    val checBox_default = settings_view.findViewById<AppCompatCheckBox>(R.id.my_check)

                    val file_name = settings_view.findViewById<EditText>(R.id.input_name_file)

                    val iter = settings_view.findViewById<EditText>(R.id.input_iter)
                    iter.setText(pref.getInt("it_up", 600000).toString())

                    val apply_button = settings_view.findViewById<AppCompatButton>(R.id.settings_button)

                    settings_input_pass.setText(pref.getString("backup_pass", pref.getString("key_def", pref.getString("key_u", ""))))
                    settings_input_pass.addTextChangedListener {dato ->
                        entropy(dato.toString(), settings_pass_progress)
                    }

                    checBox_default.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
                        override fun onCheckedChanged(p0: CompoundButton, check: Boolean) {
                            if (check) {
                                settings_input_pass.setText(pref.getString("key_def", pref.getString("key_u", "")))
                            }else {
                                settings_input_pass.setText("")
                            }
                        }
                    })

                    file_name.setText(pref.getString("backup_file", "NothingK-backup"))

                    apply_button.setOnClickListener {
                        pref.edit().putString("backup_pass", settings_input_pass.text.toString()).commit()
                        pref.edit().putString("backup_file", file_name.text.toString()).commit()
                        pref.edit().putInt("it_up", iter.text.toString().toInt()).commit()
                        settings_dialog.dismiss()
                    }


                    settings_dialog.setContentView(settings_view)
                    settings_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    settings_dialog.show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "You need to authenticate yourself", Toast.LENGTH_SHORT).show()
                }
            }).authenticate(promt())
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

    override fun onPause() {
        super.onPause()
        finish()
    }
}