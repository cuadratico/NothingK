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
import android.content.res.ColorStateList
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
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.KeyEvent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SearchView
import androidx.biometric.BiometricManager
import java.security.Key
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nothingsecure.db.Companion.pass_list
import com.nothingsecure.db.Companion.register_list
import com.nothingsecure.recy_information.conf_adapter.Companion.mods_all
import com.nothingsecure.recy_information.logs_adapter
import com.nothingsecure.recy_information.pass_adapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import kotlin.concurrent.thread
import kotlin.coroutines.ContinuationInterceptor
import kotlin.system.exitProcess

var logs_update = false
var pass_update = false
class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var logs_adapter: logs_adapter
    private lateinit var pass_adapter: pass_adapter
    private lateinit var load_dialog: Dialog
    private lateinit var back_b: ConstraintLayout
    private var a_new = true
    private var time = 0
    private var back = false
    private var sensor_manager: SensorManager? = null
    private var start = false
    private lateinit var mk: MasterKey
    private lateinit var pref: SharedPreferences
    private lateinit var color_part: View
    private lateinit var multi_funtion: ShapeableImageView
    private var pause = false
    private var key_count = 0
    private lateinit var recy: RecyclerView
    private lateinit var desencrypt_passwords: AppCompatButton
    private lateinit var info_exist: TextView
    private lateinit var vibrator: Vibrator
    private var can_vib = true
    @RequiresApi(Build.VERSION_CODES.O)
    fun vibra_conf (list: LongArray) {
        if (can_vib) {
            vibrator.vibrate(VibrationEffect.createWaveform(list, -1))
        }
    }

    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES

        vibrator = this.getSystemService(VIBRATOR_SERVICE) as Vibrator

        mk = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        pref = EncryptedSharedPreferences.create(this, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        color_part = findViewById(R.id.color_modofy_part)
        recy = findViewById<RecyclerView>(R.id.recy)
        val confi = findViewById<ShapeableImageView>(R.id.confi)
        val history = findViewById<ConstraintLayout>(R.id.logs_history)
        val add = findViewById<ConstraintLayout>(R.id.add)
        val generator = findViewById<ConstraintLayout>(R.id.generator)
        val view_generator = findViewById<ShapeableImageView>(R.id.view_generator)

        val search_pass = findViewById<android.widget.SearchView>(R.id.search)
        info_exist = findViewById<TextView>(R.id.info_exist)
        desencrypt_passwords = findViewById<AppCompatButton>(R.id.import_passwords)
        multi_funtion = findViewById(R.id.multi_funtion_bot)
        val im_ex = findViewById<ShapeableImageView>(R.id.im_ex)
        back_b = findViewById(R.id.back_b)

        info_exist.visibility = View.INVISIBLE
        search_pass.visibility = View.INVISIBLE
        recy.visibility = View.INVISIBLE
        add.visibility = View.INVISIBLE
        back_b.visibility = View.INVISIBLE


        val dialog_feed = MaterialAlertDialogBuilder(this)
            .setTitle("Want to give feedback on Nothing K?")
            .setMessage("In case you didn't know, in the settings menu at the top left, there's a button to give feedback to Nothing K (not for donating). If you log in, you can fill out a form with your new ideas, which I'll gladly read. Thanks for using Nothing K \uD83D\uDE42. \n (This message will not be displayed again.)")
            .setPositiveButton("I'll give an idea") {_, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/EWnhgBtgu5jCB3Fa9")))
            }
            .setNegativeButton("Maybe another time") {_, _ ->}
        dialog_feed.setOnDismissListener(object: DialogInterface.OnDismissListener {
            override fun onDismiss(dialog: DialogInterface?) {
                pref.edit().putBoolean("feed", false).commit()
            }

        })

        if (pref.getBoolean("feed", true)) {
            dialog_feed.show()
        }

        val db = db(this)

        pref.edit().putBoolean("desen_pass", false).commit()
        pref.edit().putBoolean("db_sus", pref.getBoolean("honeypot_mod", true)).commit()

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
        if (pref.getBoolean("honeypot_mod", true) && !db.select_pass()) {
            search_pass.visibility = View.INVISIBLE
            info_exist.visibility = View.VISIBLE
            desencrypt_passwords.visibility = View.INVISIBLE
            add.visibility = View.VISIBLE
            recy.visibility = View.VISIBLE
            pref.edit().putBoolean("desen_pass", true).commit()
        }

        if (!pref.getBoolean("gene", true)) {
            view_generator.setImageResource(R.drawable.mail_generator)
        }

        back_b.setOnClickListener {
            pref.edit().putBoolean("deri", false).commit()
            if (!a_new) {
                pref.edit().putString("key_u", pref.getString("key_u_r", "")).commit()
                pref.edit().putString("key_u_r", "").commit()
            }
            a_new = false
            pref.edit().putBoolean("db_sus", pref.getBoolean("honeypot_mod", true)).commit()
            back = true
            pause = true
            recreate()
        }

        desencrypt_passwords.setOnClickListener {
            history.isEnabled = true
            load_dialog = load("Loading passwords...", this)

            lifecycleScope.launch (Dispatchers.IO){
                if (pref.getBoolean("honeypot_mod", true)) {
                    val key = deri_expressed(applicationContext, pref.getString("key_u", "")!!, pref.getString("salt", "")!!, pref.getInt("it_def", 60000))
                    try {
                        for (position in 0..pass_list.size - 1) {
                            val (id, pass, information, iv) = pass_list[position]
                            val c = Cipher.getInstance("AES/GCM/NoPadding")
                            c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.getDecoder().decode(iv)))

                            pass_list[position].pass = String(c.doFinal(Base64.getDecoder().decode(pass)))
                        }
                    }catch (e: Exception) {
                        Log.e("error", e.toString())
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "The passwords could not be decrypted", Toast.LENGTH_SHORT).show()
                            back = true
                            recreate()
                        }
                    } finally {
                        if (key.encoded != null) {
                            key.encoded.fill(0)
                        }
                    }
                }else {
                    val alias_false = mutableListOf("Microsoft", "Google", "My account", "Bank", "PayPal (the main account)", "Google (work)", "Bank - card account", "Amazon (family)",
                        "Netflix - shared",
                        "Spotify (student)",
                        "Apple ID (iCloud)",
                        "Crypto Wallet (ETH)",
                        "Crypto Wallet (BTC)",
                    )

                    for (position in 0..SecureRandom().nextInt(2, (alias_false.size - 1))) {
                        val alias = alias_false[SecureRandom().nextInt(0, (alias_false.size - 1))]

                        pass_list.add(pass(position, pass_generator(SecureRandom().nextInt(6, 12)), alias, Base64.getEncoder().withoutPadding().encodeToString(SecureRandom().generateSeed(12))))
                        alias_false.remove(alias)
                    }
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
                cancel()
            }
        }

        search_pass.setOnQueryTextListener(object: android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String): Boolean {
                if (query.isNotEmpty()) {
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

        confi.setOnClickListener {
            if (pref.getBoolean("honeypot_mod", true)) {
                time = 60001
                pause = true
                startActivity(Intent(applicationContext, configurationActivity::class.java))
            }else {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
            }
        }

        multi_funtion.setOnClickListener {
            if (pref.getBoolean("honeypot_mod", true)) {
                val value = pref.getString("multi_but_text", "Delete all")

                var load_message = "Deleting your information"
                var title = "You want to delete all information from NothingK?"
                var message = "This mode will erase all Nothing K data from this device without leaving any traces."
                var posi_but = "Eliminate"

                when (value) {
                    "Backup mode" -> {
                        title = "Do you want to backup your passwords?"
                        message = "In this mode Nothing K will make a backup of your passwords naming the file bacjup_n.nk and using your default password to encrypt them."
                        load_message = "Securing your passwords"
                        posi_but = "Ensure"
                    }

                    "Honeypot" -> {
                        title = "Do you want to activate Nothing K's Honeypot mode?"
                        message = "In this mode, Nothing K will use decoy passwords and a random password to export them (in case the thief does so). Some areas, such as the log view, will also be inaccessible. To deactivate it, you need to quickly press the volume up button twice."
                        load_message = "Activating Honeypot mode"
                        posi_but = "Activate it"
                    }
                }

                fun veri_mod() {

                    load_dialog = load(load_message, this)

                    lifecycleScope.launch(Dispatchers.IO) {
                        when (value) {
                            "Honeypot" -> {
                                pref.edit().putBoolean("honeypot_mod", false).commit()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(applicationContext, "Honeypot mode activated", Toast.LENGTH_SHORT).show()
                                    finishAffinity()
                                }
                            }

                            "Backup mode" -> {
                                if (pref.getBoolean("desen_pass", false) && pass_list.isNotEmpty()) {
                                    export(applicationContext, pref.getString("key_def", pref.getString("key_u", "")).toString(), Base64.getEncoder().withoutPadding().encodeToString(SecureRandom().generateSeed(16)), "backup_n", Environment.DIRECTORY_DOWNLOADS, pref.getInt("it_up", 600000))
                                }else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(applicationContext, "Your passwords are not accessible", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            "Delete all" -> {
                                delete_all_fun(applicationContext, pref)
                            }

                            "App lock" -> {
                                finishAffinity()
                            }
                        }
                        delay(500)
                        withContext(Dispatchers.Main) {
                            load_dialog.dismiss()
                        }
                        cancel()
                    }
                }


                if (pref.getBoolean("mod_dialog", true)) {
                    val dialog_mods = MaterialAlertDialogBuilder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(posi_but) { _, _ ->
                            BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        super.onAuthenticationSucceeded(result)
                                        veri_mod()
                                    }
                                }).authenticate(promt())
                        }
                        .setNegativeButton("At another time") { _, _ -> }
                    dialog_mods.show()
                } else {
                    BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                veri_mod()
                            }
                        }).authenticate(promt())
                }
            }else {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
            }

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

                val specify_iter = export_view.findViewById<EditText>(R.id.input_iter)
                specify_iter.setText("600000")

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
                    pref.edit().putBoolean("db_sus", false).commit()
                    a_new = true
                    export_dialog.dismiss()
                }

                info_export.setOnClickListener {
                    val dialog_info_export = MaterialAlertDialogBuilder(this).apply {
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
                           input_pass_export.setText(pref.getString("key_def", pref.getString("key_u", "")))
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

                // export
                export_button.setOnClickListener {
                    if (input_pass_export.text.isNotEmpty() && archive_name.text.isNotEmpty()) {

                        BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                export_dialog.dismiss()
                                backup(this@MainActivity, 6)
                                load_dialog = load("Exporting your passwords...", this@MainActivity)
                                lifecycleScope.launch (Dispatchers.IO){
                                    export(applicationContext, input_pass_export.text.toString(), Base64.getEncoder().withoutPadding().encodeToString(SecureRandom().generateSeed(16)) , archive_name.text.toString(), directions_real[select_directory.selectedItem.toString()].toString(), specify_iter.text.toString().toInt())
                                    withContext(Dispatchers.Main) {
                                        load_dialog.dismiss()
                                        vibra_conf(longArrayOf(0, 20, 10, 100))
                                    }
                                    cancel()
                                }
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                Toast.makeText(applicationContext, "Authentication error", Toast.LENGTH_SHORT).show()
                            }
                        }).authenticate(promt("Authentication is required"))
                    }else {
                        Toast.makeText(this, "Information missing", Toast.LENGTH_SHORT).show()
                    }
                }


                if (pref.getBoolean("desen_pass", false) && pass_list.isNotEmpty()) {
                    export_dialog.setContentView(export_view)
                    export_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    export_dialog.show()
                }else {
                    Toast.makeText(this, "Your passwords are not accessible", Toast.LENGTH_SHORT).show()
                }
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
                pause = true
                options_dialog.dismiss()
                startActivityForResult(intent, 1001)
            }

            if (pref.getBoolean("honeypot_mod", true)) {
                options_dialog.setContentView(options_view)
                options_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                options_dialog.show()
            }else {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
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
                if (input_pass.text.trim().isNotEmpty() && info_pass.text.trim().isNotEmpty()) {

                    try {
                        val c = Cipher.getInstance("AES/GCM/NoPadding")
                        c.init(Cipher.ENCRYPT_MODE, deri_expressed(this, pref.getString("key_u", "")!!, pref.getString("salt", "").toString(), pref.getInt("it_def", 60000)))

                        var iv = ""
                        if (pref.getBoolean("db_sus", true)) {
                            backup(this, 1)
                            db.add_pass(Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(input_pass.text.toString().toByteArray())), info_pass.text.toString(), Base64.getEncoder().withoutPadding().encodeToString(c.iv))
                            iv = Base64.getEncoder().withoutPadding().encodeToString(c.iv)
                            add_register(this, "A password has been added")
                        }
                        pass_list.add(pass(if (pass_list.size != 0) { pass_list[pass_list.size - 1].id + 1 } else { 0 }, input_pass.text.toString(), info_pass.text.toString(), iv))

                        init_acti()
                        pass_adapter.update(pass_list)
                    } catch (e: Exception) {
                        Log.e("Addition error", e.toString())
                        Toast.makeText(this, "Error adding a password", Toast.LENGTH_SHORT).show()
                    } finally {
                        add_dialog.dismiss()
                    }
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
                val information_dialog = MaterialAlertDialogBuilder(this)
                    .setTitle("Do you want to delete all logs?")
                    .setPositiveButton("Yes") { _, _ ->

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
                        }).authenticate(promt("You must authenticate to continue"))

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

            if (db.select_register() && pref.getBoolean("honeypot_mod", true)) {


                load_dialog = load("Loading logs...", this)

                lifecycleScope.launch (Dispatchers.IO) {
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
                        cancel()
                    }
                }

            }else {
                Toast.makeText(this, "There are no logs", Toast.LENGTH_SHORT).show()
            }



        }

        generator.setOnClickListener {
            if (pref.getBoolean("gene", true)) {
                pass_generator_dialog(this, view_generator)
            }else {
                email_generator(this, view_generator)
            }
        }

        if (pref.getBoolean("ace_force", true)) {
            sensor_manager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensor_manager?.registerListener(this, sensor_manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        super.onDestroy()
        pref.edit().putInt("copy_in", 0).commit()
        pref.edit().putBoolean("deri", false).commit()
        if (!back) {
            pref.edit().putString("key_u", "").commit()
            if (sensor_manager != null) {
                sensor_manager?.unregisterListener(this)
            }
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        time = 0
        color_part.backgroundTintList = ColorStateList.valueOf(pref.getString("color_back", "#FF000000")!!.toColorInt())
        multi_funtion.setImageResource(mods_all.get(pref.getString("multi_but_icon", "Delete all"))!!)
        can_vib = pref.getBoolean("vibra", true)
        backup(this, 4)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (!pref.getBoolean("honeypot_mod", true)) {
            key_count ++
            if (key_count == 2) {
                BiometricPrompt(this, ContextCompat.getMainExecutor(this), object: BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        pref.edit().putBoolean("honeypot_mod", true).commit()
                        Toast.makeText(applicationContext, "Back to normal", Toast.LENGTH_SHORT).show()
                        finishAffinity()
                    }
                }).authenticate(promt())
            }

            lifecycleScope.launch (Dispatchers.IO){
                delay(500)
                key_count = 0
                cancel()
            }

        }
        return super.onKeyUp(keyCode, event)
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

                        var iter = 60000
                        if (json_f.has("iter")) {
                            iter = json_f.getInt("iter")
                        }

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

                        val my_check = import_view.findViewById<AppCompatCheckBox>(R.id.my_check)
                        val import_button =
                            import_view.findViewById<AppCompatButton>(R.id.unlock_buttom)

                        my_check.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
                            override fun onCheckedChanged(p0: CompoundButton, check: Boolean) {
                                if (check) {
                                    import_input_pass.setText(pref.getString("key_def", pref.getString("key_u", "")))
                                }else {
                                    import_input_pass.setText("")
                                }
                                import_input_pass.setSelection(import_input_pass.text.length)
                            }

                        })
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
                            val dialog_test = load("Checking the legitimacy of the key", this@MainActivity)
                            try {

                                val dialog_db_sus = MaterialAlertDialogBuilder(this)
                                dialog_db_sus.setTitle("What do you want to do with your file?")
                                dialog_db_sus.setMessage("Well, preview it (which won't affect your DB), replace the DB (which will delete all the information from the DB and replace it with the information from the file), or add the information from the file to the DB (which won't delete anything from your DB).")
                                dialog_db_sus.setPositiveButton("Replace") { _, _ ->
                                    BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {

                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            super.onAuthenticationSucceeded(result)
                                            pref.edit().putBoolean("db_sus", true).commit()
                                            backup(this@MainActivity, 10)
                                            load("Decrypting the file", this@MainActivity)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                import(applicationContext, json_f, import_input_pass.text.toString(), iter)
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(applicationContext, "Passwords loaded", Toast.LENGTH_LONG).show()
                                                    pause = true
                                                    back = true
                                                    vibra_conf(longArrayOf(0, 100, 10, 20))
                                                    recreate()
                                                }
                                            }
                                        }

                                        override fun onAuthenticationError(
                                            errorCode: Int,
                                            errString: CharSequence
                                        ) {
                                            super.onAuthenticationError(errorCode, errString)
                                            Toast.makeText(applicationContext, "Authentication error", Toast.LENGTH_SHORT).show()
                                        }
                                    }).authenticate(promt("Authentication is required"))
                                }
                                dialog_db_sus.setNeutralButton("Preview") { _, _ ->
                                    a_new = false
                                    pref.edit().putBoolean("db_sus", false).commit()
                                    backup(this@MainActivity, 9)
                                    load_dialog = load("Decrypting the file", this)
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        import(applicationContext, json_f, import_input_pass.text.toString(), iter)
                                        withContext(Dispatchers.Main) {
                                            load_dialog.dismiss()
                                            pass_adapter.update(pass_list)
                                            back_b.visibility = View.VISIBLE
                                            recy.visibility = View.VISIBLE
                                            desencrypt_passwords.visibility = View.INVISIBLE
                                            info_exist.visibility = View.INVISIBLE
                                        }
                                        back = true
                                        cancel()
                                    }
                                }
                                dialog_db_sus.setNegativeButton("Add") {_, _ ->

                                    BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {

                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            super.onAuthenticationSucceeded(result)
                                            pref.edit().putBoolean("db_sus", true).commit()
                                            backup(this@MainActivity, 8)
                                            load("Decrypting the file", this@MainActivity)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                import(applicationContext, json_f, import_input_pass.text.toString(), iter, false)
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(applicationContext, "Passwords added", Toast.LENGTH_LONG).show()
                                                    pause = true
                                                    back = true
                                                    vibra_conf(longArrayOf(0, 50, 10, 20))
                                                    recreate()
                                                }
                                            }
                                        }

                                        override fun onAuthenticationError(
                                            errorCode: Int,
                                            errString: CharSequence
                                        ) {
                                            super.onAuthenticationError(errorCode, errString)
                                            Toast.makeText(applicationContext, "Authentication error", Toast.LENGTH_SHORT).show()
                                        }
                                    }).authenticate(promt("Authentication is required"))

                                }

                                CoroutineScope(Dispatchers.IO).launch {
                                    val c = Cipher.getInstance("AES/GCM/NoPadding")
                                    c.init(Cipher.DECRYPT_MODE, derived_Key(import_input_pass.text.toString(), json_f.getString("salt"), iter), GCMParameterSpec(128, Base64.getDecoder().decode(array_pro.getString("iv"))))

                                    c.doFinal(Base64.getDecoder().decode(array_pro.getString("value")))

                                    withContext(Dispatchers.Main) {
                                        import_dialog.dismiss()
                                        dialog_test.dismiss()
                                        dialog_db_sus.show()
                                        backup(applicationContext, 7)
                                        cancel()
                                    }
                                }

                            } catch (e: Exception) {
                                dialog_test.dismiss()
                                Toast.makeText(this, "The password is not correct", Toast.LENGTH_SHORT).show()
                                import_input_pass.setText("")
                                my_check.isChecked = false
                            }
                        }

                        import_dialog.setContentView(import_view)
                        import_dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        import_dialog.show()

                    } else {
                        Toast.makeText(this, "The file structure is not correct", Toast.LENGTH_SHORT).show()
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