package com.nothingsecure

import android.annotation.SuppressLint
import android.app.ComponentCaller
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.shapes.Shape
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SearchView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nothingsecure.db.Companion.pass_list
import com.nothingsecure.db.Companion.register_list
import com.nothingsecure.recy_information.logs_adapter
import com.nothingsecure.recy_information.pass_adapter
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

var logs_update = false
var pass_update = false
var copy_intent = 0
var db_sus = true
class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var load_corou: Job
    private lateinit var logs_adapter: logs_adapter
    private lateinit var pass_adapter: pass_adapter
    private lateinit var load_dialog: Dialog
    private lateinit var ex_im_coru: Job
    private lateinit var back_b: ConstraintLayout
    private var a_new = true
    private var time = 0
    private var back = false
    private lateinit var sensor_manager: SensorManager
    private var start = false
    private lateinit var mk: MasterKey
    private lateinit var pref: SharedPreferences
    private var pause = false

    private fun load (info: String) {
        load_dialog = Dialog(this)
        val load_view = LayoutInflater.from(this).inflate(R.layout.load, null)

        val progress = load_view.findViewById<ProgressBar>(R.id.progress)
        val load_information = load_view.findViewById<TextView>(R.id.load_information)
        load_information.text = info
        progress.isActivated = true

        load_dialog.setContentView(load_view)
        load_dialog.setCancelable(false)
        load_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        load_dialog.show()
    }

    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        mk = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        pref = EncryptedSharedPreferences.create(this, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        val recy = findViewById<RecyclerView>(R.id.recy)
        val info = findViewById<ShapeableImageView>(R.id.info)
        val history = findViewById<ConstraintLayout>(R.id.logs_history)
        val add = findViewById<ConstraintLayout>(R.id.add)
        val generator = findViewById<ConstraintLayout>(R.id.generator)

        val search_pass = findViewById<android.widget.SearchView>(R.id.search)
        val info_exist = findViewById<TextView>(R.id.info_exist)
        val desencrypt_passwords = findViewById<AppCompatButton>(R.id.import_passwords)
        val delete_all = findViewById<ShapeableImageView>(R.id.delete_all)
        val im_ex = findViewById<ShapeableImageView>(R.id.im_ex)
        back_b = findViewById<ConstraintLayout>(R.id.back_b)

        info_exist.visibility = View.INVISIBLE
        search_pass.visibility = View.INVISIBLE
        recy.visibility = View.INVISIBLE
        add.visibility = View.INVISIBLE
        back_b.visibility = View.INVISIBLE

        val db = db(this)


        pref.edit().putBoolean("desen_pass", false).commit()

        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if(ks.getKey(pref.getString("key_u", ""), null) == null) {
            pref.edit().putBoolean("deri", true).commit()
        }else {
            pref.edit().putBoolean("deri", false).commit()
        }

        fun init_acti() {
            history.isEnabled = true
            search_pass.visibility = View.VISIBLE
            info_exist.visibility = View.INVISIBLE
        }

        pass_adapter = pass_adapter(pass_list)
        recy.adapter = pass_adapter
        recy.layoutManager = LinearLayoutManager(applicationContext)

        lifecycleScope.launch (Dispatchers.IO){

            while (true) {
                if (time == 60000) {
                    withContext(Dispatchers.Main) { Toast.makeText(applicationContext, "Too much downtime", Toast.LENGTH_SHORT).show() }
                    finishAffinity()
                }else {
                    if (logs_update) {
                        logs_update = false
                        withContext(Dispatchers.Main) {
                            logs_adapter.update(register_list)
                        }
                    }
                    if (pass_update) {
                        pass_update = false
                        withContext(Dispatchers.Main) {
                            pass_adapter.update(pass_list)
                            if (pass_list.isEmpty()) {
                                info_exist.visibility = View.VISIBLE
                                search_pass.visibility = View.INVISIBLE
                            }
                        }
                    }
                }
                time += 50
                delay(50)
            }
        }
        if (!db.select_pass()) {
            search_pass.visibility = View.INVISIBLE
            info_exist.visibility = View.VISIBLE
            desencrypt_passwords.visibility = View.INVISIBLE
            add.visibility = View.VISIBLE
            recy.visibility = View.VISIBLE
            pref.edit().putBoolean("desen_pass", true).commit()
        }

        back_b.setOnClickListener {
            pref.edit().putBoolean("deri", false).commit()
            if (!a_new) {
                pref.edit().putString("key_u", pref.getString("key_u_r", "")).commit()
                pref.edit().putString("key_u_r", "").commit()
            }
            a_new = false
            db_sus = true
            back = true
            pause = true
            recreate()
        }

        desencrypt_passwords.setOnClickListener {
            history.isEnabled = true
            load("Loading passwords...")

            load_corou = lifecycleScope.launch (Dispatchers.IO){
                val key = deri_expressed(applicationContext, pref.getString("key_u", "")!!, pref.getString("salt", "")!!)
                for (position in 0..pass_list.size - 1) {
                    val (id, pass, information, iv) = pass_list[position]
                    val c = Cipher.getInstance("AES/GCM/NoPadding")
                    c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.getDecoder().decode(iv)))

                    pass_list[position].pass = String(c.doFinal(Base64.getDecoder().decode(pass)))
                }

                pref.edit().putBoolean("desen_pass", true).commit()
                withContext(Dispatchers.Main) {
                    desencrypt_passwords.visibility = View.INVISIBLE
                    recy.visibility = View.VISIBLE
                    add.visibility = View.VISIBLE
                    pass_adapter.update(pass_list)
                    init_acti()
                    load_dialog.dismiss()
                }
                load_corou.cancel()
            }
        }

        search_pass.setOnQueryTextListener(object: android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String?): Boolean {
                if (query!!.isNotEmpty()) {
                    val newList = pass_list.filter { dato -> dato.information.contains(Regex(".*$query.*")) }
                    pass_adapter.update(newList)
                }else {
                    pass_adapter.update(pass_list)
                }
                return true
            }

            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

        })

        info.setOnClickListener {
            val info_dialog = AlertDialog.Builder(this).apply {

                setTitle("Do you want to give your opinion on Nothing K?")
                setPositiveButton("Yes") {_, _ -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/EWnhgBtgu5jCB3Fa9")))}
                setNegativeButton("No") {_, _ ->}
            }

            info_dialog.show()
        }

        delete_all.setOnClickListener {
            val delete_dialog = AlertDialog.Builder(this)

                .setTitle("You want to delete all information from NothingK?")
                .setPositiveButton("Eliminate"){_, _ ->
                    if (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {


                        val promt = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Authenticate yourself")
                            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            .setConfirmationRequired(true)
                            .build()

                        BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)

                                pref.edit().clear().commit()
                                db.delete_all()
                                pass_list.clear()
                                cacheDir.deleteRecursively()
                                externalCacheDir?.deleteRecursively()

                                val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                                ks.deleteEntry(pref.getString("key_u", ""))

                                recy.visibility = View.INVISIBLE
                                info_exist.visibility = View.VISIBLE
                                info_exist.text = "Bye"
                                Thread.sleep(400)

                                finishAffinity()
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)

                                Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                            }
                        }).authenticate(promt)

                    }
                }
                .setNegativeButton("No"){_, _ ->}

            delete_dialog.show()
        }

        im_ex.setOnClickListener {
            val options_dialog = Dialog(this)
            val options_view = LayoutInflater.from(this).inflate(R.layout.dialog_select_im_ex, null)

            val import_button = options_view.findViewById<ConstraintLayout>(R.id.import_archive)
            val export_button = options_view.findViewById<ConstraintLayout>(R.id.export_archive)

            export_button.setOnClickListener {
                options_dialog.dismiss()

                val export_dialog = Dialog(this)
                val export_view = LayoutInflater.from(this).inflate(R.layout.dialog_export, null)

                val info_export = export_view.findViewById<ShapeableImageView>(R.id.info_export)

                val input_pass_export = export_view.findViewById<EditText>(R.id.input_pass)
                val progress_export = export_view.findViewById<LinearProgressIndicator>(R.id.progress)
                val checkBox_export = export_view.findViewById<AppCompatCheckBox>(R.id.my_check)
                val export_pass_visi = export_view.findViewById<ConstraintLayout>(R.id.password_visibility)
                val export_icon_visi = export_view.findViewById<ShapeableImageView>(R.id.visibility_icon)

                val archive_name = export_view.findViewById<EditText>(R.id.input_name)

                val select_directory = export_view.findViewById<AppCompatSpinner>(R.id.directory_spinner)

                val export_button = export_view.findViewById<AppCompatButton>(R.id.export_buttom)
                val create_new_a = export_view.findViewById<AppCompatButton>(R.id.create_new_a)

                create_new_a.setOnClickListener {
                    back_b.visibility = View.VISIBLE
                    pass_list.clear()
                    pass_adapter.update(pass_list)
                    info_exist.visibility = View.VISIBLE
                    search_pass.visibility = View.INVISIBLE
                    if (!a_new) {
                        pref.edit().putString("key_u", pref.getString("key_u_r", "")).commit()
                        pref.edit().putString("key_u_r", "").commit()
                    }
                    db_sus = false
                    a_new = true
                    export_dialog.dismiss()
                }

                info_export.setOnClickListener {
                    val dialog_info_export = AlertDialog.Builder(this).apply {
                        setTitle("What am I exporting?")
                        setMessage("You're exporting all your passwords. Basically, you're exporting your database to a file with a .nk (NothingK) extension. Once you've exported it, you can re-import it without any problems.")
                        setPositiveButton("Ok") {_, _ ->}
                    }
                    dialog_info_export.show()
                }

                // specify password
                var visibility_export_dialog = false
                export_pass_visi.setOnClickListener {
                    visibility_export_dialog = !visibility_export_dialog
                    visibility(visibility_export_dialog, export_icon_visi, input_pass_export)
                }

                input_pass_export.addTextChangedListener {dato ->
                    entropy(dato.toString(), progress_export)
                }

                checkBox_export.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
                    override fun onCheckedChanged(p0: CompoundButton, isCheck: Boolean) {
                        if (isCheck) {
                           input_pass_export.setText(pref.getString("key_u", ""))
                        }else {
                            input_pass_export.setText("")
                        }
                        input_pass_export.setSelection(input_pass_export.text.length)
                    }

                })

                // specify the direction
                val directions_list = arrayOf("Downloads", "Documents", "DCIM", "Pictures", "Movies", "Music", "Alarms")
                val directions_real = mapOf("Downloads" to Environment.DIRECTORY_DOWNLOADS, "Documents" to Environment.DIRECTORY_DOCUMENTS, "DCIM" to Environment.DIRECTORY_DCIM, "Pictures" to Environment.DIRECTORY_PICTURES, "Movies" to Environment.DIRECTORY_MOVIES, "Music" to Environment.DIRECTORY_MUSIC, "Alarms" to Environment.DIRECTORY_ALARMS)

                select_directory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, directions_list)

                export_button.setOnClickListener {
                    if (input_pass_export.text.isNotEmpty() && archive_name.text.isNotEmpty() && pass_list.isNotEmpty()) {
                        val promt = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Authentication is required")
                            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            .build()

                        BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                export_dialog.dismiss()
                                load("Exporting your passwords...")

                                ex_im_coru = lifecycleScope.launch (Dispatchers.IO){
                                    export(applicationContext, input_pass_export.text.toString(), Base64.getEncoder().withoutPadding().encodeToString(SecureRandom().generateSeed(16)) , archive_name.text.toString(), directions_real[select_directory.selectedItem.toString()].toString())
                                    withContext(Dispatchers.Main) {
                                        load_dialog.dismiss()
                                    }
                                    ex_im_coru.cancel()
                                }
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                Toast.makeText(applicationContext, "Authentication error", Toast.LENGTH_SHORT).show()
                            }
                        }).authenticate(promt)
                    }else {
                        Toast.makeText(this, "Information missing or undecrypted passwords", Toast.LENGTH_SHORT).show()
                    }
                }

                export_dialog.setContentView(export_view)
                export_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                export_dialog.show()
            }

            import_button.setOnClickListener {
                if (!a_new) {
                    pref.edit().putString("key_u", pref.getString("key_u_r", "")).commit()
                    pref.edit().putString("key_u_r", "").commit()
                }
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                recy.visibility = View.VISIBLE
                info_exist.visibility = View.INVISIBLE
                pause = true
                options_dialog.dismiss()
                startActivityForResult(intent, 1001)
            }

            if (pref.getBoolean("desen_pass", false)) {
                options_dialog.setContentView(options_view)
                options_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                options_dialog.show()
            }else {
                Toast.makeText(this, "You need to decrypt your passwords", Toast.LENGTH_SHORT).show()
            }
        }

        add.setOnClickListener {
            val add_dialog = Dialog(this)
            val add_view = LayoutInflater.from(this).inflate(R.layout.add_edit_dialog, null)



            val info_pass = add_view.findViewById<EditText>(R.id.information_pass)
            val input_pass = add_view.findViewById<EditText>(R.id.input_password)
            val progress = add_view.findViewById<LinearProgressIndicator>(R.id.progress)
            val pass_visibility = add_view.findViewById<ConstraintLayout>(R.id.password_visibility)
            val visi_icon = add_view.findViewById<ShapeableImageView>(R.id.visibility_icon)
            val multi = add_view.findViewById<AppCompatButton>(R.id.multi_bottom)

            var visi = false
            input_pass.addTextChangedListener {dato ->
                    entropy(input_pass.text.toString(), progress)
            }
            pass_visibility.setOnClickListener {
                visibility(visi, visi_icon, input_pass)
                visi = !visi
            }

            multi.setOnClickListener {
                if (input_pass.text.isNotEmpty() && info_pass.text.isNotEmpty()) {
                    val c = Cipher.getInstance("AES/GCM/NoPadding")
                    c.init(Cipher.ENCRYPT_MODE, deri_expressed(this, pref.getString("key_u", "")!!, pref.getString("salt", "")!!))

                    if (db_sus) {
                        db.add_pass(Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(input_pass.text.toString().toByteArray())), info_pass.text.toString(), Base64.getEncoder().withoutPadding().encodeToString(c.iv))
                    }
                    pass_list.add(pass(if (pass_list.size != 0) { pass_list[pass_list.size - 1].id + 1 } else { 0 }, input_pass.text.toString(), info_pass.text.toString(), ""))

                    init_acti()
                    pass_adapter.update(pass_list)
                    add_register(this, "A password has been added")
                    add_dialog.dismiss()
                }else {
                    Toast.makeText(this, "Missing information to be filled in", Toast.LENGTH_SHORT).show()
                }
            }

            add_dialog.setContentView(add_view)
            add_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            add_dialog.show()
        }

        history.setOnClickListener {
            val logs_dialog = BottomSheetDialog(this)
            val logs_view = LayoutInflater.from(this).inflate(R.layout.history_interface, null)

            logs_dialog.setOnDismissListener(object: DialogInterface.OnDismissListener {
                override fun onDismiss(p0: DialogInterface?) {
                    register_list.clear()
                }

            })

            val logs_search = logs_view.findViewById<android.widget.SearchView>(R.id.search)
            val logs_recy = logs_view.findViewById<RecyclerView>(R.id.recy)
            val delete_all = logs_view.findViewById<ConstraintLayout>(R.id.delete_all)
            logs_search.isEnabled = false
            delete_all.isEnabled = false

            delete_all.setOnClickListener {
                val information_dialog = AlertDialog.Builder(this)
                    .setTitle("Do you want to delete all logs?")
                    .setPositiveButton("Yes") { _, _ ->

                        val promt = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("You must authenticate to continue")
                            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                            .build()

                        BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                db.delete_register(true)
                                register_list.clear()
                                logs_adapter.update(register_list)
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                            }
                        }).authenticate(promt)

                        logs_dialog.dismiss()

                    }
                    .setNegativeButton("No"){_, _ ->}
                information_dialog.show()
            }

            logs_search.setOnQueryTextListener(object: android.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextChange(query: String): Boolean {
                    if (query.isNotEmpty()) {
                        val reference = if (query.matches(Regex(".*@info.*"))) { true } else { false }

                        val new_list = register_list.filter { dato -> if (!reference) {dato.time.contains(Regex(".*$query.*"))} else {dato.information.contains(Regex(".*${query.split("@info")[1]}.*"))} }
                        logs_adapter.update(new_list)
                    }else {
                        logs_adapter.update(register_list)
                    }
                    return true
                }

                override fun onQueryTextSubmit(p0: String?): Boolean {
                    return false
                }

            })

            if (db.select_register()) {


                load("Loading logs...")

                load_corou = lifecycleScope.launch (Dispatchers.IO, start = CoroutineStart.LAZY) {
                    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

                    for (position in 0..register_list.size - 1) {
                        val (id, time, information, color, iv) = register_list[position]
                        val c = Cipher.getInstance("AES/GCM/NoPadding")
                        c.init(Cipher.DECRYPT_MODE, ks.getKey(pref.getString("key", ""), null), GCMParameterSpec(128, Base64.getDecoder().decode(iv)))
                        register_list[position].time = String(c.doFinal(Base64.getDecoder().decode(time)))
                    }

                    register_list.reverse()
                    logs_adapter = logs_adapter(register_list)
                    withContext(Dispatchers.Main) {
                        logs_search.isEnabled = true
                        delete_all.isEnabled = true
                        logs_recy.adapter = logs_adapter
                        logs_recy.layoutManager = LinearLayoutManager(applicationContext)

                        load_dialog.dismiss()

                        logs_dialog.setContentView(logs_view)
                        logs_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        logs_dialog.show()
                    }
                    load_corou.cancel()
                }

                load_corou.start()

            }else {
                Toast.makeText(this, "There are no logs", Toast.LENGTH_SHORT).show()
            }



        }

        generator.setOnClickListener {
            val total = mutableListOf(minusculas_l)

            val gene_dilaog = BottomSheetDialog(this)
            val gen_view = LayoutInflater.from(this).inflate(R.layout.generator_dialog, null)


            val refresh = gen_view.findViewById<ConstraintLayout>(R.id.refresh)
            val icon_refresh = gen_view.findViewById<ShapeableImageView>(R.id.icon_refresh)
            val result_pass = gen_view.findViewById<TextView>(R.id.result_pass)
            val size = gen_view.findViewById<SeekBar>(R.id.size)
            val information_size = gen_view.findViewById<TextView>(R.id.information_size)
            val progress_calculator = gen_view.findViewById<LinearProgressIndicator>(R.id.progress)
            val capital_l = gen_view.findViewById<MaterialSwitch>(R.id.capital_l)
            val number = gen_view.findViewById<MaterialSwitch>(R.id.numbers)
            val simbol = gen_view.findViewById<MaterialSwitch>(R.id.simbol)
            val copy = gen_view.findViewById<AppCompatButton>(R.id.copy)

            fun gen (bar: SeekBar) {
                result_pass.text = pass_generator(total, bar.progress)
                entropy(result_pass.text.toString(), progress_calculator)
            }

            gen(size)

            size.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, size: Int, p2: Boolean) {
                    information_size.text = size.toString()
                }

                override fun onStartTrackingTouch(bar: SeekBar?) {}

                override fun onStopTrackingTouch(bar: SeekBar) {
                    gen(bar)
                }

            })

            capital_l.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
                override fun onCheckedChanged(p0: CompoundButton, check: Boolean) {
                    if (check) {
                        total.add(mayusculas_l)
                    }else {
                        total.remove(mayusculas_l)
                    }
                    gen(size)
                }

            })
            number.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
                override fun onCheckedChanged(p0: CompoundButton, check: Boolean) {
                    if (check) {
                        total.add(numeros_l)
                    }else {
                        total.remove(numeros_l)
                    }
                    gen(size)
                }
            })
            simbol.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
                override fun onCheckedChanged(p0: CompoundButton, check: Boolean) {
                    if (check) {
                        total.add(simbolos_l)
                    }else {
                        total.remove(simbolos_l)
                    }
                    gen(size)
                }
            })

            refresh.setOnClickListener {
                val anim_rotate = AnimationUtils.loadAnimation(this, R.anim.rotate)

                anim_rotate.setAnimationListener(object: Animation.AnimationListener {
                    override fun onAnimationEnd(p0: Animation?) {
                        refresh.isEnabled = true
                    }

                    override fun onAnimationRepeat(p0: Animation?) {}

                    override fun onAnimationStart(p0: Animation?) {
                        refresh.isEnabled = false
                        gen(size)
                    }

                })

                icon_refresh.startAnimation(anim_rotate)
            }

            copy.setOnClickListener {
                val manage = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("pass", result_pass.text.toString())
                manage.setPrimaryClip(clip)
            }


            gene_dilaog.setContentView(gen_view)
            gene_dilaog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            gene_dilaog.show()
        }

        sensor_manager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        super.onDestroy()
        copy_intent = 0
        pref.edit().putBoolean("deri", false).commit()
        if (!back) {
            pref.edit().putString("key_u", "").commit()
            sensor_manager.unregisterListener(this)
            add_register(this, if (pref.getBoolean("close", false)) { "The app has been closed due to a sudden movement" } else { "The app has been closed" })
        }
        back = false
        pass_list.clear()
    }

    override fun onPause() {
        super.onPause()
        if (!pause) {
            finishAffinity()
        }
        pause = false
    }
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            time = 0
        }
        return super.dispatchTouchEvent(event)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, caller: ComponentCaller) {
        super.onActivityResult(requestCode, resultCode, data, caller)
        if (resultCode == -1) {
            pause = false
            val uri = data!!.data
            val query = contentResolver.query(uri!!, null, null, null, null, null)

            if (query!!.moveToFirst()) {
                val position = query.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val name = query.getString(position)

                if (!name.matches(Regex(".*nk.*"))) {
                    Toast.makeText(this, "The file is not correct", Toast.LENGTH_SHORT).show()
                } else {

                    val info_archive = this.contentResolver.openInputStream(uri)?.bufferedReader()
                        .use { it?.readText() }
                    val json_f = JSONObject(info_archive)

                    if (json_f.has("salt") && json_f.has("pass_list") && json_f.has("pro")) {

                        val import_dialog = Dialog(this)
                        val import_view =
                            LayoutInflater.from(this).inflate(R.layout.dialog_import, null)

                        val import_input_pass = import_view.findViewById<EditText>(R.id.input_pass)
                        val import_visible_button =
                            import_view.findViewById<ConstraintLayout>(R.id.password_visibility)
                        val import_icon_visible =
                            import_view.findViewById<ShapeableImageView>(R.id.visibility_icon)
                        val import_progress =
                            import_view.findViewById<LinearProgressIndicator>(R.id.progress)

                        val import_button =
                            import_view.findViewById<AppCompatButton>(R.id.unlock_buttom)

                        import_input_pass.addTextChangedListener { dato ->
                            entropy(dato.toString(), import_progress)
                        }

                        var visible = false
                        import_visible_button.setOnClickListener {
                            visible = !visible
                            visibility(visible, import_icon_visible, import_input_pass)
                        }

                        import_button.setOnClickListener {
                            val array_pro = json_f.getJSONArray("pro").getJSONObject(0)

                            val c = Cipher.getInstance("AES/GCM/NoPadding")
                            c.init(
                                Cipher.DECRYPT_MODE,
                                derived_Key(
                                    import_input_pass.text.toString(),
                                    json_f.getString("salt")
                                ),
                                GCMParameterSpec(
                                    128,
                                    Base64.getDecoder().decode(array_pro.getString("iv"))
                                )
                            )

                            try {
                                c.doFinal(Base64.getDecoder().decode(array_pro.getString("value")))
                                import_dialog.dismiss()

                                val dialog_db_sus = android.app.AlertDialog.Builder(this)
                                dialog_db_sus.setTitle("You want to replace your db or preview the file.")
                                dialog_db_sus.setMessage("If you replace the DB, all information will be deleted, and if you replace the file, you will be able to modify it without any problems.")
                                dialog_db_sus.setPositiveButton("Yes") { _, _ ->
                                    val promt = BiometricPrompt.PromptInfo.Builder()
                                        .setTitle("Authentication is required")
                                        .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                                        .build()

                                    BiometricPrompt(
                                        this,
                                        ContextCompat.getMainExecutor(this),
                                        object : BiometricPrompt.AuthenticationCallback() {

                                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                super.onAuthenticationSucceeded(result)
                                                db_sus = true
                                                load("Decrypting the file")
                                                ex_im_coru = lifecycleScope.launch(Dispatchers.IO) {
                                                    import(
                                                        applicationContext,
                                                        json_f,
                                                        import_input_pass.text.toString(),
                                                        db_sus
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        load_dialog.dismiss()
                                                        Toast.makeText(
                                                            applicationContext,
                                                            "Passwords loaded",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        pause = true
                                                        back = true
                                                        recreate()
                                                    }
                                                    ex_im_coru.cancel()
                                                }
                                            }

                                            override fun onAuthenticationError(
                                                errorCode: Int,
                                                errString: CharSequence
                                            ) {
                                                super.onAuthenticationError(errorCode, errString)
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Authentication error",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }).authenticate(promt)

                                }
                                dialog_db_sus.setNegativeButton("No") { _, _ ->
                                    a_new = false
                                    db_sus = false
                                    load("Decrypting the file")
                                    ex_im_coru = lifecycleScope.launch(Dispatchers.IO) {
                                        import(
                                            applicationContext,
                                            json_f,
                                            import_input_pass.text.toString(),
                                            db_sus
                                        )
                                        withContext(Dispatchers.Main) {
                                            load_dialog.dismiss()
                                            pass_adapter.update(pass_list)
                                            back_b.visibility = View.VISIBLE
                                        }
                                        back = true
                                        ex_im_coru.cancel()
                                    }
                                }
                                dialog_db_sus.show()


                            } catch (e: Exception) {
                                Toast.makeText(
                                    this,
                                    "The password is not correct",
                                    Toast.LENGTH_SHORT
                                ).show()
                                import_input_pass.setText("")
                            }
                        }

                        import_dialog.setContentView(import_view)
                        import_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        import_dialog.show()

                    } else {
                        Toast.makeText(
                            this,
                            "The file structure is not correct",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

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