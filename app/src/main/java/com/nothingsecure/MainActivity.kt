package com.nothingsecure

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.shapes.Shape
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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
import java.security.KeyStore
import java.time.LocalDateTime
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

var logs_update = false
var pass_update = false
lateinit var alia: String
var copy_intent = 0
class MainActivity : AppCompatActivity() {
    private lateinit var load_corou: Job
    private lateinit var logs_adapter: logs_adapter
    private lateinit var pass_adapter: pass_adapter
    private lateinit var load_dialog: Dialog
    private var time = 0


    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        alia = intent.getStringExtra("ali").orEmpty()
        val recy = findViewById<RecyclerView>(R.id.recy)
        val info = findViewById<ShapeableImageView>(R.id.info)
        val history = findViewById<ConstraintLayout>(R.id.logs_history)
        val add = findViewById<ConstraintLayout>(R.id.add)
        val generator = findViewById<ConstraintLayout>(R.id.generator)

        val search_pass = findViewById<android.widget.SearchView>(R.id.search)
        val info_exist = findViewById<TextView>(R.id.info_exist)
        val delete_all = findViewById<ShapeableImageView>(R.id.delete_all)

        info_exist.visibility = View.INVISIBLE
        search_pass.visibility = View.INVISIBLE

        val db = db(this)

        val mk = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val pref = EncryptedSharedPreferences.create(this, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        fun load (info: String) {
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
                            if (pass_list.size == 0) {
                                info_exist.visibility = View.VISIBLE
                                search_pass.visibility = View.INVISIBLE
                            }
                        }
                        Log.e("lista", "actualizada")
                    }
                }
                time += 50
                delay(50)
            }
        }

        if (db.select_pass()) {
            history.isEnabled = true
            load("Loading passwords...")

            load_corou = lifecycleScope.launch (Dispatchers.IO){
                val key = deri_expressed(applicationContext)
                for (position in 0..pass_list.size - 1) {
                    val (id, pass, information, iv) = pass_list[position]
                    val c = Cipher.getInstance("AES/GCM/NoPadding")
                    c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.getDecoder().decode(iv)))

                    pass_list[position].pass = String(c.doFinal(Base64.getDecoder().decode(pass)))
                }

                withContext(Dispatchers.Main) {
                    pass_adapter.update(pass_list)
                    init_acti()
                    load_dialog.dismiss()
                }
                load_corou.cancel()
            }

        }else {
            search_pass.visibility = View.INVISIBLE
            info_exist.visibility = View.VISIBLE

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
                                ks.deleteEntry(alia)

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
                if (visi) {
                    visi = false
                    visi_icon.setImageResource(R.drawable.close_eye)
                    input_pass.transformationMethod = PasswordTransformationMethod.getInstance()
                }else {
                    visi = true
                    visi_icon.setImageResource(R.drawable.open_eye)
                    input_pass.transformationMethod = null
                }
                input_pass.setSelection(input_pass.text.length)
            }

            multi.setOnClickListener {
                if (input_pass.text.isNotEmpty() && info_pass.text.isNotEmpty()) {
                    val c = Cipher.getInstance("AES/GCM/NoPadding")
                    c.init(Cipher.ENCRYPT_MODE, deri_expressed(this))

                    db.add_pass(Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(input_pass.text.toString().toByteArray())), info_pass.text.toString(), Base64.getEncoder().withoutPadding().encodeToString(c.iv))
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
            val total = mutableListOf<List<Char>>(minusculas_l)

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
        pass_list.clear()
        finishAffinity()
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            time = 0
        }
        return super.dispatchTouchEvent(event)
    }
}