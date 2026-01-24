package com.nothingsecure

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import android.view.inputmethod.InputMethodManager
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
import androidx.core.view.isVisible
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
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import kotlin.text.toByteArray

class configurationActivity : AppCompatActivity() {
    companion object {
        lateinit var dialog_conf: Dialog
        val backup_list = listOf("Never", "When adding a password", "When editing a password", "When deleting a password", "When putting the app in the background", "When modifying the key handling", "When exporting", "When importing in any mode", "When importing in 'Add' mode", "When importing in 'Preview' mode", "When importing in 'Replace' mode")
    }


    private enum class states () {
        secure_question_create,
        secure_question,
        very_pass,
        new_pass
    }

    private var id_plus: Int = 0
    private var states_change = states.secure_question

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
        val donate = findViewById<ShapeableImageView>(R.id.dona)
        val version_info = findViewById<TextView>(R.id.version_info)

        // edit pass part
        val all_pass_change = findViewById<ConstraintLayout>(R.id.all_pass_change)

        // edit icon part
        val see_pass = findViewById<AppCompatButton>(R.id.see_pass)
        val log_out_pass = findViewById<ShapeableImageView>(R.id.log_out_pass)
        val pass_all = findViewById<TextInputLayout>(R.id.pass_all)
        val icons_all = findViewById<ConstraintLayout>(R.id.icons_all)
        val edit_icon = findViewById<ShapeableImageView>(R.id.edit_icon)
        pass_all.visibility = View.INVISIBLE
        log_out_pass.visibility = View.INVISIBLE

        val edit_button = findViewById<ShapeableImageView>(R.id.edit_icons_button)
        val fun_spinner = findViewById<AppCompatSpinner>(R.id.fun_spinner)
        val check_dialog = findViewById<AppCompatCheckBox>(R.id.dialog_show_check)
        fun_spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mods_recy)
        check_dialog.isChecked = pref.getBoolean("mod_dialog", true)

        // pass modify part
        val input_pass = findViewById<EditText>(R.id.input_pass)
        val progress_pass = findViewById<LinearProgressIndicator>(R.id.progress)
        val pass_info = findViewById<TextView>(R.id.key_type)
        progress_pass.visibility = View.INVISIBLE

        // edit color part
        val edit_color_button = findViewById<ShapeableImageView>(R.id.edit_color)
        val back_color = findViewById<ConstraintLayout>(R.id.back_color)
        val color_code = findViewById<TextView>(R.id.color_code)

        // edit pass part
        val pass_type = findViewById<ShapeableImageView>(R.id.pass_type_modify)
        val mody_expre = findViewById<TextView>(R.id.mody_expre)
        val info_type = findViewById<ShapeableImageView>(R.id.info_key_type)


        // Edit accelerometer detection
        val info_acele = findViewById<ShapeableImageView>(R.id.info_acele_mode)
        val acele_switch = findViewById<MaterialSwitch>(R.id.acele_switch)


        // Edit the backup time
        val info_backup_mode = findViewById<ShapeableImageView>(R.id.info_backup_modes)
        val back_up_show = findViewById<TextView>(R.id.backup_spinner)
        val back_up_settings = findViewById<ShapeableImageView>(R.id.backup_settings)
        val edit_back_up = findViewById<ShapeableImageView>(R.id.edit_back_up)

        // Vibration
        val vibra = findViewById<MaterialSwitch>(R.id.vibration)

        // Login button
        val log_button = findViewById<MaterialSwitch>(R.id.veryfication)

        // time out
        val info_time_out = findViewById<ShapeableImageView>(R.id.info_time_out)
        val time_out = findViewById<ConstraintLayout>(R.id.time_out_back)


        version_info.text = version_name
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

            recy.adapter = conf_adapter(if (type == 0) { mods_recy } else { colors_list }, view, type,  {
                if (type == 0) {
                    pref.edit().putString("multi_but_icon", it).commit()
                }else {
                    pref.edit().putString("color_back", it).commit()
                }
                dialog_conf.dismiss()
            }, {
                if (type != 0) {
                    val manager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    manager.setPrimaryClip(ClipData.newPlainText("color", it.toString()))
                }
            })
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

        fun info_dialog(title: String, message: String) {
            MaterialAlertDialogBuilder(this).apply {
                setTitle(title)
                setMessage(message)
                setPositiveButton("Ok") {_, _ ->}
            }.show()
        }

        // edit pass part

        fun migration (key: Key, id_final: Int) {
            val db = db(this)
            for (position in 0..pass_list.size - 1) {
                val (id, pass, information, iv) = pass_list[position]
                val c = Cipher.getInstance("AES/GCM/NoPadding")
                c.init(Cipher.ENCRYPT_MODE, key)
                db.add_pass(Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(pass_list[position].pass.toByteArray())), information, Base64.getEncoder().withoutPadding().encodeToString(c.iv))
                id_plus ++
            }
            db.delete_speci(pass_list[0].id, id_final)
            id_plus = 0
        }

        val security_questions = listOf(
            "What was the name of your first pet?",
            "In what city were you born?",
            "What is the name of the first school you attended?",
            "What was the model of your first mobile phone?",
            "What is the name of the street where you grew up?",
            "What was the name of your childhood best friend?",
            "What is your favorite book or movie?",
            "What was your first job?",
            "What is the name of the place where you had your first vacation?",
            "What city and place do you most associate with a memorable childhood experience?"
        )


        fun dialog_material (title: String, message: String): MaterialAlertDialogBuilder {

            return MaterialAlertDialogBuilder(this).apply {
                setTitle(title)
                setMessage(message)
            }

        }

        fun recalculate_secure_info (input: String, alias_salt: String, alias_hash: String) {
            pref.edit().putString(alias_salt, Base64.getEncoder().withoutPadding().encodeToString(
                SecureRandom().generateSeed(16))).commit()

            pref.edit().putString(alias_hash, Base64.getEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA256").digest(
                    input.toByteArray() + Base64.getDecoder().decode(pref.getString(alias_salt, "")))
            )).commit()
        }

        all_pass_change.setOnClickListener {

            val dialog_change = Dialog(this)
            val view_change = LayoutInflater.from(this).inflate(R.layout.secure_question, null)


            val question_all = view_change.findViewById<ConstraintLayout>(R.id.question_all)
            val question = view_change.findViewById<TextView>(R.id.question)
            val edit_q = view_change.findViewById<ShapeableImageView>(R.id.edit_q)
            val info_states = view_change.findViewById<TextView>(R.id.info_pass_states)

            val input_secure_info = view_change.findViewById<EditText>(R.id.input_secure_info)
            val secure_progress = view_change.findViewById<LinearProgressIndicator>(R.id.progress)

            val auth = view_change.findViewById<ShapeableImageView>(R.id.auth)
            val close = view_change.findViewById<ShapeableImageView>(R.id.close)

            dialog_change.setContentView(view_change)
            dialog_change.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog_change.setCancelable(false)


            if (pass_list.isNotEmpty() && pref.getBoolean("desen_pass", false)) {
                if (pref.getString("secure_a", "")!!.isEmpty()) {
                    dialog_material("Do you want to create a security question?", "By creating a security question, you can authenticate yourself to modify your MasterKey whenever you want.").apply {
                        setPositiveButton("Create security question") { _, _ ->
                            states_change = states.secure_question_create
                            dialog_change.show()
                            question.text = security_questions.random()
                        }
                        setNegativeButton("Maybe later") { _, _ -> }
                    }.show()
                } else {
                    dialog_material("Do you want to change your MasterKey?", "To change your MasterKey, you'll need to specify your security question and your MasterKey (your login password). Specifically, changing your MasterKey changes your login password and therefore the cryptographic key used to encrypt your passwords (regardless of the operating mode you're using). Make sure you keep all your information safe.").apply {
                        setPositiveButton("Change my MasterKey") { _, _ ->
                            dialog_change.show()
                            edit_q.visibility = View.INVISIBLE
                            question.text = pref.getString("secure_q", "")
                        }
                        setNegativeButton("Maybe later") { _, _ -> }
                    }.show()
                }
            } else {
                Toast.makeText(this@configurationActivity, "The passwords are not decrypted", Toast.LENGTH_SHORT).show()
            }


            input_secure_info.addTextChangedListener {
                entropy(it.toString(), secure_progress)
            }

            edit_q.setOnClickListener {
                MaterialAlertDialogBuilder(this).apply {
                    setTitle("Choose your security question")
                    setAdapter(ArrayAdapter(this@configurationActivity, android.R.layout.simple_list_item_1, security_questions), object: DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            question.text = security_questions[which]
                        }
                    })
                }.show()
            }

            fun exit () {
                Toast.makeText(this@configurationActivity, "NothingK is going to restart", Toast.LENGTH_SHORT).show()
                finishAffinity()
            }

            close.setOnClickListener {

                dialog_material("Do you want to stop the process?", "If you stop the process, the app will have to close because your credentials have been compromised.").apply {
                    setPositiveButton("Restart to restore normal") {_, _ ->
                        BiometricPrompt(this@configurationActivity, ContextCompat.getMainExecutor(this@configurationActivity), object: BiometricPrompt.AuthenticationCallback() {

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                dialog_change.dismiss()
                                exit()
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                Toast.makeText(this@configurationActivity, "Authentication failure", Toast.LENGTH_SHORT).show()
                            }
                        }).authenticate(promt())
                    }
                    setNegativeButton("Maybe later") {_, _ ->}
                }.show()

            }

            fun input_states (text: String) {
                input_secure_info.setText("")
                input_secure_info.setHint(text)

                val service_method = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                service_method.hideSoftInputFromInputMethod(input_secure_info.windowToken, 0)
            }

            auth.setOnClickListener {
                BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        val db = db(this@configurationActivity)

                        when (states_change) {

                            states.secure_question_create ->
                                dialog_material("Do you want to create your 2FA?", "Once created, you won't be able to modify it, nor will you be able to safely retrieve it if you forget. So write it down in a safe place.").apply {

                                    setPositiveButton("Create 2FA") {_, _ ->
                                        recalculate_secure_info(input_secure_info.text.toString(), "a_salt", "secure_a")
                                        pref.edit().putString("secure_q", question.text.toString()).commit()
                                        states_change = states.secure_question
                                        dialog_change.dismiss()
                                    }
                                    setNegativeButton("Better not") {_, _ -> dialog_change.dismiss()}

                                    setCancelable(false)
                                }.show()

                            states.secure_question ->
                                    if (very_hash(input_secure_info.text.toString(), Base64.getDecoder().decode(pref.getString("a_salt", "")), Base64.getDecoder().decode(pref.getString("secure_a", "")))) {
                                        recalculate_secure_info(input_secure_info.text.toString(), "a_salt", "secure_a")

                                        dialog_material("What's the next step?", "In the next step you will be asked for the password in order to decrypt the passwords.").apply {

                                            setPositiveButton("Continue with the process") {_, _ ->
                                                pref.edit().putString("key_u", "").commit()
                                                pass_list = listOf()
                                                states_change = states.very_pass
                                                question_all.visibility = View.INVISIBLE
                                                input_states("Password")
                                            }
                                            setNegativeButton("Not now") {_, _ -> exit()}
                                            setCancelable(false)

                                        }.show()

                                    } else {
                                        input_secure_info.setText("")
                                        Toast.makeText(this@configurationActivity, "The security response is invalid", Toast.LENGTH_SHORT).show()
                                    }

                            states.very_pass ->
                                if (very_hash(input_secure_info.text.toString(), Base64.getDecoder().decode(pref.getString("salt_very", "")), Base64.getDecoder().decode(pref.getString("hash", "")))) {
                                    pref.edit().putString("key_u", input_secure_info.text.toString()).commit()
                                    recalculate_secure_info(input_secure_info.text.toString(), "salt_very", "hash")

                                    val dialog_load_pass = load("Decrypting the passwords", this@configurationActivity)

                                    lifecycleScope.launch (Dispatchers.IO){
                                        val key = deri_expressed(applicationContext, pref.getString("key_u", "")!!, pref.getString("salt", "")!!, pref.getInt("it_def", 60000))
                                        try {
                                            db.select_pass()
                                            for (position in 0..pass_list.size - 1) {
                                                val (id, pass, information, iv) = pass_list[position]
                                                val c = Cipher.getInstance("AES/GCM/NoPadding")
                                                c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.getDecoder().decode(iv)))

                                                pass_list[position].pass = String(c.doFinal(Base64.getDecoder().decode(pass)))
                                            }
                                            withContext(Dispatchers.Main) {
                                                dialog_load_pass.dismiss()
                                                Toast.makeText(this@configurationActivity, "Passwords decrypted", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("error", e.toString())
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(applicationContext, "The passwords could not be decrypted", Toast.LENGTH_SHORT).show()
                                                finishAffinity()
                                            }

                                        } finally {
                                            if (key.encoded != null) {
                                                key.encoded.fill(0)
                                            }
                                        }

                                        withContext(Dispatchers.Main) {
                                            dialog_material("It's time to create your new password", "After creating your new MasterKey, your passwords will be re-encrypted, NothingK will close, and your initial MasterKey will change (remember this, it's very important).").apply {

                                                setPositiveButton("Continue with the process") {_, _ ->
                                                    input_states("New password")
                                                    states_change = states.new_pass
                                                    info_states.text = "Create your new MasterKey"
                                                }
                                                setNegativeButton("Cancel the process") {_, _ -> exit()}

                                                setCancelable(false)
                                            }.show()
                                        }
                                    }

                                } else {
                                    Toast.makeText(this@configurationActivity, "The password is incorrect", Toast.LENGTH_SHORT).show()
                                    input_secure_info.setText("")
                                }

                            states.new_pass ->
                                if (input_secure_info.text.isNotEmpty() && input_secure_info.text.length >= 8) {

                                    val dialog_load_migration = load("Running the migration", this@configurationActivity)

                                    lifecycleScope.launch {
                                        if (pref.getBoolean("deri", false)) {
                                            pref.edit().putString("salt", Base64.getEncoder().withoutPadding().encodeToString(SecureRandom().generateSeed(16)))
                                        } else {

                                            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                                            ks.deleteEntry(pref.getString("key_u", ""))
                                            pref.edit().putString("key_u", "").commit()

                                            val kg = KeyGenParameterSpec.Builder(input_secure_info.text.toString(), KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT).apply {
                                                setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                            }.build()
                                            val k_gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                                            k_gen.init(kg)
                                            k_gen.generateKey()
                                        }
                                        pref.edit().putString("key_u", input_secure_info.text.toString()).commit()

                                        recalculate_secure_info(input_secure_info.text.toString(), "salt_very", "hash")

                                        val key = deri_expressed(applicationContext, pref.getString("key_u", "")!!, pref.getString("salt", "")!!, pref.getInt("it_def", 60000))
                                        val id_final = pass_list[pass_list.size - 1].id
                                        try {
                                            migration(key, id_final)
                                            withContext(Dispatchers.Main) {
                                                dialog_load_migration.dismiss()
                                                dialog_change.dismiss()
                                                Toast.makeText(this@configurationActivity, "Operation completed", Toast.LENGTH_SHORT).show()
                                                exit()
                                            }
                                            add_register(this@configurationActivity, "Your MasterKey has been changed")
                                        } catch (e: Exception) {
                                            Log.e("Mode modification error", e.toString())
                                            db.delete_speci(id_final + 1, pass_list[pass_list.size - 1].id + (pass_list.size - 1))
                                            withContext(Dispatchers.Main) {
                                                exit()
                                            }
                                        } finally {
                                            if (key.encoded != null) {
                                                key.encoded.fill(0)
                                            }
                                        }
                                    }

                                } else {
                                    Toast.makeText(this@configurationActivity, "Incorrect length or missing data", Toast.LENGTH_SHORT).show()
                                }

                            else -> exit()
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(this@configurationActivity, "Authentication error", Toast.LENGTH_SHORT).show()
                    }
                }).authenticate(promt())

            }


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
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                pref.edit().putString("multi_but_text", mods_recy[position]).commit()
                add_register(applicationContext, "The button mode has been changed to \"${pref.getString("multi_but_text", "Delete all")}\" ")
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // edit pass part code
        input_pass.addTextChangedListener {
            entropy(it.toString(), progress_pass)
        }

        fun see (pass_very: String, see: Int) {
            log_out_pass.visibility = see
            progress_pass.visibility = see
            pass_all.visibility = see
            input_pass.setText(pass_very)
        }

        see_pass.setOnClickListener {
            BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    see_pass.visibility = View.INVISIBLE
                    see(pref.getString("key_def", pref.getString("key_u", "")).toString(), View.VISIBLE)
                    entropy(input_pass.text.toString(), progress_pass)
                }
            }).authenticate(promt())
        }

        log_out_pass.setOnClickListener {

            MaterialAlertDialogBuilder(this@configurationActivity).apply {
                setTitle("Do you want to rewrite your default password?")
                setMessage("If you re-enter your default password, it will replace the password you use to autofill the allowed fields.")
                setPositiveButton("Yes, I want to replace it.") {_, _ ->
                    BiometricPrompt(this@configurationActivity, ContextCompat.getMainExecutor(this@configurationActivity), object: BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                                    if (input_pass.text.isNotEmpty()) {
                                        pref.edit().putString("key_def", input_pass.text.toString()).commit()
                                        see_pass.visibility = View.VISIBLE
                                        see("", View.INVISIBLE)
                                    } else {
                                        Toast.makeText(this@configurationActivity, "The field is empty", Toast.LENGTH_SHORT).show()
                                    }
                        }
                    }).authenticate(promt())
                }
                setNegativeButton("No, maybe later"){_, _ ->
                    see_pass.visibility = View.VISIBLE
                    see("", View.INVISIBLE)
                }
            }.show()
        }

        // edit color part code

        edit_color_button.setOnClickListener {
            dialog(R.layout.dialog_config_color, 1)
        }

        fun info_buttons (text: String, url: String) {
            MaterialAlertDialogBuilder(this).apply {
                setTitle(text)
                setPositiveButton("Yes") { _, _ -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                setNegativeButton("No") { _, _ -> }
            }.show()
        }

        info.setOnClickListener {
            info_buttons("Do you want to give your opinion on Nothing K?", "https://forms.gle/EWnhgBtgu5jCB3Fa9")
        }
        donate.setOnClickListener {
            info_buttons("Do you want to help animals through an animal shelter?", "https://darnaanimalrescue.org/es/")
        }

        // pass type part code

        pass_info.text = "Modify the treatment of your encryption key\n\n You are currently in \"${ if (pref.getBoolean("deri", false)) { "Derived Key" } else { "Android KeyStore" } }\" mode"
        fun modi_type_ui () {
            if (pref.getBoolean("deri", false)) {
                mody_expre.text = "Derived Key"
            }
        }
        modi_type_ui()

        info_type.setOnClickListener {
            info_dialog("Where is my key stored?", "Currently, your key is an ${if (pref.getBoolean("deri", false)) { "Derived Key" } else { "Android KeyStore" }}.\n" +
                    "When I talk about Android KeyStore, I'm referring to Android's cryptographic key storage. Your cryptographic key is stored there and protected by your password. When I talk about derived keys, I'm referring to the use of the PBKDF2 algorithm. This algorithm derives the key from your password. Basically, the cryptographic key is created from your password, which means it's never stored.")
        }
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
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        val load_dialog = load("Changing key type...", this@configurationActivity)

                        lifecycleScope.launch(Dispatchers.IO) {

                            val db = db(applicationContext)
                            pass_modi()
                            if (pass_list.isNotEmpty()) {
                                val key = deri_expressed(applicationContext, pref.getString("key_u", pref.getString("key_u_r", "")).toString(), pref.getString("salt", "").toString(), pref.getInt("it_def", 60000))
                                val id_final = pass_list[pass_list.size - 1].id
                                try {
                                    migration(key, id_final)
                                } catch (e: Exception) {
                                    Log.e("Mode modification error", e.toString())
                                    pass_modi()
                                    db.delete_speci(id_final + 1, pass_list[pass_list.size - 1].id + (pass_list.size - 1))
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
                            withContext(Dispatchers.Main) {
                                modi_type_ui()
                            }
                            add_register(applicationContext, "Your password's operating mode has been changed to: ${mody_expre.text}")
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
            info_dialog("What is the accelerometer used for in Nothing K?", "The accelerometer is used to detect forceful acts. Basically, when a sudden movement is detected, the app closes and the movement is recorded.\n" +
                    "This way, thefts involving forceful acts can be prevented.")
        }

        acele_switch.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(p0: CompoundButton, check: Boolean) {

                BiometricPrompt(this@configurationActivity, ContextCompat.getMainExecutor(applicationContext), object: BiometricPrompt.AuthenticationCallback() {
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
            info_dialog("What are backup moments?", "These are scheduled times when a backup of your entire database will be made. It will be encrypted with your default password and the file will be named NothingK-backup.nk (you can change all of this with the \"Settings\" button in the backup menu)")
        }
        back_up_show.text = backup_list[pref.getInt("backup_ins", 0)]

        edit_back_up.setOnClickListener {
            BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    MaterialAlertDialogBuilder(this@configurationActivity)
                        .setTitle("Select a Back-up instance")
                        .setAdapter(ArrayAdapter(this@configurationActivity, android.R.layout.simple_spinner_item, backup_list), object: DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                        pref.edit().putInt("backup_ins", which).commit()
                                        back_up_show.text = backup_list[pref.getInt("backup_ins", 0)]
                            }
                        })
                        .show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@configurationActivity, "Authentication error", Toast.LENGTH_SHORT).show()
                }
            }).authenticate(promt())

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


        // vibration part
        vibra.isChecked = pref.getBoolean("vibra", true)

        vibra.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, check: Boolean) {
                pref.edit().putBoolean("vibra", check).commit()
            }

        })


        // login button part

        log_button.isChecked = pref.getBoolean("log_very", false)

        log_button.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, check: Boolean) {
                pref.edit().putBoolean("log_very", check).commit()
            }

        })

        // time out

        info_time_out.setOnClickListener {
            info_dialog("Do you want to change your time out?", "The timeout is the amount of time it takes for NothingK to close after your last interaction; this time is a selection of 5, 10, 20, 30 and 60 seconds.")
        }

        time_out.setOnClickListener {

            BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val time_list = listOf("5 secons", "10 secons", "20 secons", "30 secons", "60 secons")

                    MaterialAlertDialogBuilder(this@configurationActivity)
                        .setTitle("Your current timeout is ${pref.getInt("time_out", 30).toString()} seconds")
                        .setAdapter(ArrayAdapter(this@configurationActivity, android.R.layout.simple_list_item_1, time_list), object: DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                pref.edit().putInt("time_out", time_list[which].split(" ")[0].toInt()).commit()
                            }

                        }).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@configurationActivity, "Error", Toast.LENGTH_SHORT).show()
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