package com.nothingsecure

import android.app.Activity
import android.app.ComponentCaller
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.OpenableColumns
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.component1
import androidx.activity.result.component2
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
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
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nothingsecure.configuration_tools.delete_all_fun
import com.nothingsecure.configuration_tools.description_list
import com.nothingsecure.configuration_tools.icons_list
import com.nothingsecure.funs_extra.add_register
import com.nothingsecure.funs_extra.auth_json
import com.nothingsecure.funs_extra.biometric_auto
import com.nothingsecure.funs_extra.cip_p_v
import com.nothingsecure.funs_extra.create_dialog
import com.nothingsecure.funs_extra.create_material_dialog
import com.nothingsecure.funs_extra.des_es_values
import com.nothingsecure.funs_extra.des_p_l_v
import com.nothingsecure.funs_extra.desen_p
import com.nothingsecure.funs_extra.destroy_info
import com.nothingsecure.funs_extra.email_generator
import com.nothingsecure.funs_extra.entropy
import com.nothingsecure.funs_extra.export
import com.nothingsecure.funs_extra.force
import com.nothingsecure.funs_extra.import
import com.nothingsecure.funs_extra.import_type
import com.nothingsecure.funs_extra.key_conf_dialog
import com.nothingsecure.funs_extra.load
import com.nothingsecure.funs_extra.now
import com.nothingsecure.funs_extra.pass_generator_dialog
import com.nothingsecure.funs_extra.vibra_conf
import com.nothingsecure.funs_extra.visibility
import com.nothingsecure.funs_extra.x_regi
import com.nothingsecure.funs_extra.y_regi
import com.nothingsecure.funs_extra.z_regi
import com.nothingsecure.recy_information.adapter_global
import com.nothingsecure.recy_information.holders_datas
import com.nothingsecure.recy_information.project_class
import com.nothingsecure.recy_information.recy_edit_add
import com.nothingsecure.recy_information.recy_preview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import kotlin.to
import kotlin.toString


const val version_name = "1.3.23-Nikola_Tesla_C"


enum class time_out_state { ready, stop }
var t_state = time_out_state.ready

class MainActivity : AppCompatActivity(), SensorEventListener {
    private enum class pass_states { without_without, desen_pass, en_pass }

    private var logs_list = listOf<project_class.register>()
    private var pass_list = listOf<project_class.pass>()

    private lateinit var logs_adapter: adapter_global
    private lateinit var pass_adapter: adapter_global
    private val db = db(this)

    private lateinit var pref: SharedPreferences

    private lateinit var desencrypt_passwords: ShapeableImageView
    private lateinit var back_b: ConstraintLayout
    private lateinit var color_part: ConstraintLayout
    private lateinit var multi_funtion: ShapeableImageView
    private lateinit var info_exist: TextView
    private lateinit var search_pass: SearchView
    private lateinit var add: ShapeableImageView
    private lateinit var time_out: TextView


    private var time = 0
    private var sensor_manager: SensorManager? = null
    private var pause = false
    private var key_count = 0

    private fun init_action_activity (load_text: String, execute: (Dialog, TextView) -> Unit, error: () -> Unit) {
        biometric_auto(this, {
            t_state = time_out_state.stop
            val (load_dialog, load_input) = load(load_text, this@MainActivity)

            execute(load_dialog, load_input)

        }, {
            error()
        })
    }

    private fun time_out_init () {
        t_state = time_out_state.ready

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                time_out.text = ((pref.getInt("time_out", 30000) - time) / 1000).toString()
            }

            while (true) {
                if (t_state == time_out_state.stop) {
                    break
                }

                delay(1000)
                if (time == pref.getInt("time_out", 30000)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Too much downtime", Toast.LENGTH_SHORT).show()
                    }
                    finishAffinity()
                }

                time += 1000
                withContext(Dispatchers.Main) {
                    time_out.text = ((pref.getInt("time_out", 30000) - time) / 1000).toString()
                }
            }

            withContext(Dispatchers.Main) {
                time_out.text = "Pause"
            }

        }
    }

    private fun very_pass (state: pass_states) {
        when(state) {
            pass_states.without_without -> {
                pass_list = listOf()
                pass_adapter.update(pass_list)

                desencrypt_passwords.visibility = View.GONE
                search_pass.visibility = View.GONE
                info_exist.visibility = View.VISIBLE
                add.visibility = View.VISIBLE
            }

            pass_states.en_pass -> {
                pass_list = listOf()
                pass_adapter.update(pass_list)

                back_b.visibility = View.GONE
                info_exist.visibility = View.GONE
                search_pass.visibility = View.GONE
                add.visibility = View.GONE
                desencrypt_passwords.visibility = View.VISIBLE
            }

            else -> {
                info_exist.visibility = View.GONE
                desencrypt_passwords.visibility = View.GONE
                desencrypt_passwords.setImageResource(R.drawable.padlock_lock)
                add.visibility = View.VISIBLE
                search_pass.visibility = View.VISIBLE
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        pref = EncryptedSharedPreferences.create(
            this, "ap",
            MasterKey.Builder(this).apply {
                setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            }.build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        color_part = findViewById(R.id.color_modofy_part)
        val recy = findViewById<RecyclerView>(R.id.recy)
        val confi = findViewById<ShapeableImageView>(R.id.confi)
        val logs = findViewById<ShapeableImageView>(R.id.logs_history)
        add = findViewById(R.id.add)
        val generator = findViewById<ShapeableImageView>(R.id.generator)
        time_out = findViewById(R.id.time_out)

        search_pass = findViewById(R.id.search)
        info_exist = findViewById(R.id.info_exist)
        desencrypt_passwords = findViewById(R.id.import_passwords)
        multi_funtion = findViewById(R.id.multi_funtion_bot)
        val im_ex = findViewById<ShapeableImageView>(R.id.im_ex)
        back_b = findViewById(R.id.multi_options_archive)

        back_b.visibility = View.GONE

        if (pref.getString("init_info", "0.3.18-Animal_Crossing.1") != version_name) {
            create_material_dialog(this,
                "The \"$version_name\" version includes",
                "- Implementation of \"MiniFy\" to obfuscate the code.\n" +
                        "\n" +
                        "- Implementation of non-deprecated functions.\n" +
                        "\n" +
                        "- Bug fixes.\n" +
                        "\n" +
                        "Thank you for using NowiVault ❤\uFE0F",
                "Thank you",
                "",
                {
                    pref.edit().putString("init_info", version_name).commit()
                    time_out_init()
                },
                {}

            ).setCancelable(false)

        } else {
            time_out_init()
        }

        pref.edit().putBoolean("desen_pass", false).commit()

        if (pref.getBoolean("honeypot_mod", false)) {
            logs.visibility = View.GONE
            confi.visibility = View.GONE
            multi_funtion.visibility = View.GONE
            im_ex.visibility = View.GONE
        }

        if (!pref.getBoolean("gene", true)) {
            generator.setImageResource(R.drawable.mail_generator)
        }


        fun very_log () {
            if (pref.getBoolean("info_r_full", false)) {
                logs.visibility = View.VISIBLE
            } else {
                logs.visibility = View.GONE
            }
        }
        very_log()

        pass_adapter = adapter_global(pass_list, 1, holders_datas.pass_holder_data({ pass_data ->

            recy_edit_add(this, pref, true, { edit_dialog, edit_information, edit_pass, last_modi ->

                init_action_activity("Decrypting your data", { load_dialog, load_input ->
                    lifecycleScope.launch (Dispatchers.IO) {

                        desen_p(this@MainActivity, pref, pass_data.pass_all, db, { p ->

                            edit_information.setText(pass_data.information)
                            last_modi.text = pass_data.time
                            edit_pass.setText(String(p))

                            load_dialog.dismiss()
                        }, {
                            time_out_init()
                        })

                    }

                }, {
                    Toast.makeText(this, "Decryption requires authentication", Toast.LENGTH_SHORT).show()
                    edit_dialog.dismiss()
                })

            }, { edit_dialog, edit_pass, edit_information ->

                init_action_activity("Obtaining the key", { load_dialog, load_input ->

                    lifecycleScope.launch (Dispatchers.IO) {
                        cip_p_v(this@MainActivity, pref, db, load_input, edit_pass, edit_information, { _, pass_glo ->
                            pass_list = pass_list.map {
                                if (it.id == pass_data.id) {
                                    it.copy(
                                        information = edit_information.text.toString(),
                                        pass_all = pass_glo,
                                        time = now()
                                    )
                                } else {
                                    it
                                }
                            }

                        }, {

                            pass_adapter.update(pass_list)
                            very_log()
                            very_pass(pass_states.desen_pass)
                            edit_dialog.dismiss()

                        }, {

                            load_dialog.dismiss()
                            time_out_init()

                        }, false, pass_data.id)
                    }

                }, {})

            })

        }, { pass_data ->

            create_material_dialog(this, "Do you want to delete this data?", "If you delete this record from your database, you will not be able to recover it", "Delete", "better not",
                {
                    init_action_activity("Data removed", { load_dialog, load_input ->

                        lifecycleScope.launch (Dispatchers.IO){
                            if (pref.getBoolean("db_sus", true)) {
                                db.delete("info_s", "id = ?", arrayOf(pass_data.id.toString()))
                            }

                            pass_list = pass_list.minus(pass_data)

                            add_register(pref, db, "A password has been deleted")

                            withContext(Dispatchers.Main) {
                                pass_adapter.update(pass_list)
                                if (pass_list.isEmpty()) {
                                    pref.edit().putBoolean("info_s_full", false).commit()
                                    very_pass(pass_states.without_without)
                                }
                                load_dialog.dismiss()
                                very_log()
                                time_out_init()
                            }
                        }

                    }, {})

                }, {}
            )

        }, { pass_data ->

            recy_preview(this, pref, logs, db) { see_pass, dialog_see ->
                init_action_activity("Decrypting the password", {load_dialog, load_input ->
                    lifecycleScope.launch (Dispatchers.IO) {
                        desen_p(this@MainActivity, pref, pass_data.pass_all, db, { p ->
                            load_dialog.dismiss()
                            see_pass.text = String(p)
                            vibra_conf(this@MainActivity, pref, longArrayOf(0, 50, 0, 50))
                        }, {
                            time_out_init()
                        })
                    }

                }, {
                    dialog_see.dismiss()
                })
            }

        }))
        recy.adapter = pass_adapter
        recy.layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        if (pref.getBoolean("info_s_full", false)) {
            very_pass(pass_states.en_pass)
        } else {
            very_pass(pass_states.without_without)
        }

        desencrypt_passwords.setOnClickListener {
            pass_list = listOf()

            init_action_activity("obtaining the key", {load_dialog, load_input ->
                lifecycleScope.launch (Dispatchers.IO){
                    des_p_l_v(this@MainActivity, pref, load_input, db, {
                        desencrypt_passwords.setImageResource(R.drawable.padlock_unlock)
                        very_pass(pass_states.desen_pass)
                        pass_adapter.update(pass_list)

                        vibra_conf(this@MainActivity, pref, longArrayOf(0, 100, 0, 100))
                        load_dialog.dismiss()
                        very_log()
                        time_out_init()

                    }, {value, id ->

                        pass_list = pass_list.plus((des_es_values(value, id) as project_class.pass))

                    }, { posi, alias, pass ->

                        pass_list = pass_list.plus(project_class.pass(posi, pass, alias, now()))

                    })
                }
            }, {})
        }

        search_pass.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String): Boolean {
                if (query.isNotEmpty()) {
                    pass_adapter.update(pass_list.filter { dato -> Regex(".*$query.*").matches(dato.information) })
                } else {
                    pass_adapter.update(pass_list)
                }
                return true
            }

            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

        })

        add.setOnClickListener {

            recy_edit_add(this, pref, false, {_, _, _, _ -> }, { dialog, input_pass, input_info ->
                init_action_activity("Obtaining the key", { load_dialog, load_input ->

                    lifecycleScope.launch (Dispatchers.IO){
                        cip_p_v(this@MainActivity, pref, db, load_input, input_pass, input_info, { id, pass_all ->

                            pass_list = pass_list.plus(project_class.pass(id, pass_all, input_info.text.toString(), now()))

                        }, {

                            dialog.dismiss()
                            pass_adapter.update(pass_list)
                            very_log()
                            very_pass(pass_states.desen_pass)

                        }, {

                            load_dialog.dismiss()
                            time_out_init()

                        })
                    }

                }, {})
            })

        }

        generator.setOnClickListener {
            if (pref.getBoolean("gene", true)) {
                pass_generator_dialog(this, pref, generator)
            } else {
                email_generator(this, pref, generator)
            }
        }

        logs.setOnClickListener {

            val logs_dialog = BottomSheetDialog(this)
            val view_logs = create_dialog(this, R.layout.logs_interface, logs_dialog)

            val recy_logs = view_logs.findViewById<RecyclerView>(R.id.recy_logs)
            val delete_all_l = view_logs.findViewById<ConstraintLayout>(R.id.delete_all)
            val search_logs = view_logs.findViewById<SearchView>(R.id.search)

            fun dismiss_logs () {
                logs_dialog.dismiss()
                logs.visibility = View.GONE
            }

            logs_adapter = adapter_global(logs_list, 2, holders_datas.register_holer_data { data ->

                init_action_activity("deleting the log", { logs_load, load_input ->
                    lifecycleScope.launch (Dispatchers.IO){

                        db.delete("info_r", "id = ?", arrayOf(data.id.toString()))
                        logs_list = logs_list.minus(data)

                        add_register(pref, db, "A log has been deleted")
                        withContext(Dispatchers.Main) {
                            logs_adapter.update(logs_list)
                            logs_load.dismiss()
                            if (logs_list.isEmpty()) {
                                dismiss_logs()
                            }
                            time_out_init()
                        }
                    }
                }, {})
            })
            recy_logs.adapter = logs_adapter
            recy_logs.layoutManager = LinearLayoutManager(this).apply {
                reverseLayout = true
                stackFromEnd = true
            }

            init_action_activity("obtaining the key", { load_dialog, load_edit ->

                lifecycleScope.launch (Dispatchers.IO){
                    des_p_l_v(this@MainActivity, pref, load_edit, db, {

                        load_dialog.dismiss()
                        logs_adapter.update(logs_list)

                    }, { values, id ->

                        logs_list = logs_list.plus(des_es_values(values, id, null, null, true) as project_class.register)

                    }, {_, _, _ ->}, pref.getString("key_l", "").toString(), "AES", true)

                    time_out_init()
                }

            }, {})

            delete_all_l.setOnClickListener {
                create_material_dialog(this, "Do you want to delete all the logs?", "If you delete all the logs, you will lose full access to them", "I'll keep that in mind", "Better not", {
                    biometric_auto(this, {
                        t_state = time_out_state.stop
                        val (logs_load, loaf_input) = load("deleting the log", this@MainActivity)

                        lifecycleScope.launch (Dispatchers.IO){
                            db.delete("info_r")
                            logs_list = listOf()

                            add_register(pref, db, "All logs have been deleted")
                            withContext(Dispatchers.Main) {
                                logs_adapter.update(logs_list)
                                logs_load.dismiss()
                                dismiss_logs()
                                time_out_init()
                            }
                        }

                    }, {})
                }, {})
            }

            search_logs.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
                override fun onQueryTextChange(text: String?): Boolean {
                    if (logs_list.isNotEmpty()) {
                        logs_adapter.update(logs_list.filter { if (Regex(".*#.*").matches(text!!)) { Regex(".*${text.split("#")[1]}.*").matches(it.information) } else { Regex(".*$text.*").matches(it.time) } })
                    } else {
                        logs_adapter.update(logs_list)
                    }
                    return true
                }

                override fun onQueryTextSubmit(text: String?): Boolean = false

            })

            logs_dialog.setOnDismissListener(object: DialogInterface.OnDismissListener {
                override fun onDismiss(p0: DialogInterface?) {
                    logs_list = listOf()
                }
            })

        }

        im_ex.setOnClickListener {

            val options_dialog = Dialog(this)
            val options_view = create_dialog(this, R.layout.dialog_select_im_ex, options_dialog)

            val import_button = options_view.findViewById<ShapeableImageView>(R.id.import_archive)
            val export_button = options_view.findViewById<ShapeableImageView>(R.id.export_archive)
            val new_archive = options_view.findViewById<ConstraintLayout>(R.id.create_new_a)

            new_archive.setOnClickListener {
                options_dialog.dismiss()
                pref.edit().putBoolean("db_sus", false).commit()
                very_pass(pass_states.without_without)
                back_b.visibility = View.VISIBLE
            }

            export_button.setOnClickListener {
                if (pass_list.isNotEmpty()) {

                    options_dialog.dismiss()

                    val export_dialog = Dialog(this)
                    val export_view = create_dialog(this, R.layout.dialog_export, export_dialog)

                    val pass_ex_input = export_view.findViewById<EditText>(R.id.input_pass)
                    val progress = export_view.findViewById<LinearProgressIndicator>(R.id.progress)

                    val file_ex_input = export_view.findViewById<EditText>(R.id.input_name)

                    val info_export = export_view.findViewById<ShapeableImageView>(R.id.info_export)

                    val visibility_values = export_view.findViewById<ConstraintLayout>(R.id.values_visi)
                    val visi_icon = export_view.findViewById<ShapeableImageView>(R.id.visibility_icon)

                    val export_b = export_view.findViewById<ConstraintLayout>(R.id.export_button)

                    val db_out = export_view.findViewById<CheckBox>(R.id.db_out)

                    info_export.setOnClickListener {
                        val dialog_info_export = MaterialAlertDialogBuilder(this).apply {
                            setTitle("What am I exporting?")
                            setMessage("When you export, you are exporting all your passwords to a \".ns\" file with the cryptographic settings you specified.\n" +
                                    "You can import them back into NowiVault whenever you want.")
                            setPositiveButton("Ok") { _, _ -> }
                        }
                        dialog_info_export.show()
                    }

                    visibility(pref, visi_icon, pass_ex_input)
                    visibility_values.setOnClickListener {
                        pref.edit().putBoolean("prims", !pref.getBoolean("prims", false)).commit()
                        visibility(pref, visi_icon, pass_ex_input)
                    }

                    pass_ex_input.addTextChangedListener { dato ->
                        entropy(dato.toString(), progress)
                    }


                    db_out.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
                        override fun onCheckedChanged(p0: CompoundButton, check: Boolean) {
                            if (check) {
                                create_material_dialog(this@MainActivity,
                                    "Do you want to delete your database after exporting?",
                                    "If you delete your database after exporting, you will no longer be able to access it in any way within NowiVault. You will only be able to access it using your exported file.",
                                    "I am aware",
                                    "better not",
                                    {},
                                    {
                                        db_out.isChecked = false
                                    }
                                )
                            }
                        }

                    })

                    export_b.setOnClickListener {
                        if (pass_ex_input.text.isNotEmpty() && file_ex_input.text.isNotEmpty()) {

                            key_conf_dialog(this@MainActivity, pref, true, true) { algo_expre, size_expre, input_iter, _ ->

                                biometric_auto(this@MainActivity, {
                                    t_state = time_out_state.stop
                                    val (load_export, load_input) = load("Generating the file", this)

                                    lifecycleScope.launch {

                                        export(this@MainActivity, load_input, pref,
                                            pass_ex_input.text.toString(),
                                            Base64.getEncoder().withoutPadding().encodeToString(SecureRandom().generateSeed(16)),
                                            input_iter.text.toString().toInt(),
                                            algo_expre.text.toString(),
                                            size_expre.text.toString().toInt(),
                                            file_ex_input.text.toString(),
                                            db,
                                            pass_list,
                                        )

                                        if (db_out.isChecked) {
                                            withContext(Dispatchers.IO) {
                                                db.delete("info_s")
                                                pref.edit().putBoolean("info_s_full", false).commit()
                                            }
                                        }

                                        time_out_init()

                                        withContext(Dispatchers.Main) {
                                            if (pref.getBoolean("info_s_full", false)) {
                                                very_pass(pass_states.en_pass)
                                            } else {
                                                very_pass(pass_states.without_without)
                                            }

                                            load_export.dismiss()
                                            export_dialog.dismiss()
                                        }
                                    }
                                }, {})
                            }

                        } else {
                            Toast.makeText(this, "Information missing or encrypted passwords", Toast.LENGTH_SHORT).show()
                        }
                    }

                    export_dialog.setOnDismissListener(object: DialogInterface.OnDismissListener {
                        override fun onDismiss(p0: DialogInterface?) {
                            time_out_init()
                        }
                    })


                } else {
                    Toast.makeText(this, "There is nothing to export", Toast.LENGTH_SHORT).show()
                }
            }

            import_button.setOnClickListener{
                t_state = time_out_state.stop
                pause = true
                options_dialog.dismiss()

                import_result.launch(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                )

            }

        }

        back_b.setOnClickListener {

            val back_dialog = Dialog(this)
            val view_back_dialog = create_dialog(this, R.layout.archive_oprtions, back_dialog)

            val save_archive = view_back_dialog.findViewById<ConstraintLayout>(R.id.save_archive)
            val go_back = view_back_dialog.findViewById<ConstraintLayout>(R.id.go_back)

            go_back.setOnClickListener {
                back_dialog.dismiss()
                pref.edit().putBoolean("db_sus", false).commit()
                back_b.visibility = View.GONE
                very_pass(if (pref.getBoolean("info_s_full", false)) { pass_states.en_pass } else { pass_states.without_without })
            }

            save_archive.setOnClickListener {
                if (pass_list.isNotEmpty()) {
                    back_dialog.dismiss()
                    dialog_pr_add_re(null, pass_list, pref.getString("key_u", "").toString())
                } else {
                    Toast.makeText(this, "There is nothing to import", Toast.LENGTH_SHORT).show()
                }
            }
        }

        confi.setOnClickListener {
            t_state = time_out_state.stop
            pause = true
            startActivity(Intent(this@MainActivity, configurationActivity::class.java))
        }

        fun modes_expre () {
            biometric_auto(this, {
                when (pref.getInt("modes_sel", 0)) {

                    0 -> {
                        pref.edit().putBoolean("honeypot_mod", true).commit()
                        pref.edit().putBoolean("db_sus", false).commit()

                        finishAffinity()

                        Toast.makeText(this, "HoneyPot mode has been activated", Toast.LENGTH_SHORT).show()
                    }

                    1 -> {
                        pass_list = listOf()
                        destroy_info(pref)
                        Toast.makeText(this, "Bye", Toast.LENGTH_SHORT).show()
                        finishAffinity()
                    }

                    2 -> {
                        delete_all_fun(this, pref, db)
                        Toast.makeText(this, "No trace remains", Toast.LENGTH_SHORT).show()
                        finishAffinity()
                    }

                    else -> {
                        if (pref.getBoolean("info_s_full", false) && pass_list.isNotEmpty() && pref.getBoolean("db_sus", true)) {
                            very_pass(pass_states.en_pass)
                        }
                    }

                }
            }, {})
        }

        multi_funtion.setOnClickListener {

            if (pref.getBoolean("info_modes", true)) {
                create_material_dialog(this, "", description_list[pref.getInt("model_sel", 0)], "start mode", "", {
                    modes_expre()
                }, {})
            } else {
                modes_expre()
            }

        }

        if (pref.getBoolean("ace_force", false)) {
            sensor_manager = getSystemService(SENSOR_SERVICE) as SensorManager
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

    override fun onDestroy() {
        super.onDestroy()

        if (pref.getBoolean("info_s_full", false) && !pref.getBoolean("honeypot_mod", false)) {
            pref.edit().putBoolean("db_sus", true).commit()
        }

        pref.edit().putInt("copy_in", 0).commit()

        sensor_manager.let {  it?.unregisterListener(this) }

        destroy_info(pref)
        pass_list = listOf()
        logs_list = listOf()
    }

    override fun onPause() {
        super.onPause()
        if (!pause) {
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()

        if (pause) {
            time_out_init()
            pause = false
        }

        color_part.backgroundTintList = ColorStateList.valueOf(pref.getString("color_back", "#FF000000")!!.toColorInt())
        multi_funtion.setImageResource(icons_list[pref.getInt("modes_icon", 0)].icon)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event!!.actionMasked == MotionEvent.ACTION_DOWN) {
            time = 0
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (pref.getBoolean("honeypot_mod", false)) {
            key_count ++
            if (key_count == 2) {
                biometric_auto(this, {
                    pref.edit().putBoolean("honeypot_mod", false).commit()
                    pref.edit().putBoolean("db_sus", pref.getBoolean("info_s_full", false)).commit()
                    Toast.makeText(this, "Back to normal", Toast.LENGTH_SHORT).show()
                    finishAffinity()
                }, {})
            }

            lifecycleScope.launch (Dispatchers.IO){
                delay(500)
                key_count = 0
                cancel()
            }

        }
        return super.onKeyUp(keyCode, event)
    }

    private data class multi_m_dialog (val title: String, val message: String)
    private fun dialog_pr_add_re (json: JSONObject?, list: List<project_class.pass>?, pass: String, nk: Boolean = false) {

        val dialogs_list = mapOf(
            import_type.preview to multi_m_dialog(
                "What does \"preview\" mode do?",
                "Preview mode lets you securely view passwords from a file without having to add them to your database."
            ),
            import_type.add_db to multi_m_dialog(
                "What does \"add_db\" mode do?",
                "This mode allows you to add information to your database without losing the rest of the data."
            ),
            import_type.replace_db to multi_m_dialog(
                "What does \"replace_db\" mode do?",
                "This mode allows you to replace your database with the file to be imported."
            )
        )

        val dialog_im = Dialog(this@MainActivity)
        val view_im = create_dialog(this@MainActivity, R.layout.import_posi, dialog_im)

        val preview = view_im.findViewById<ConstraintLayout>(R.id.preview)

        val add_db = view_im.findViewById<ConstraintLayout>(R.id.add_db)

        val replace_db = view_im.findViewById<ConstraintLayout>(R.id.replace_db)

        if (list != null) {
            preview.visibility = View.GONE
        }

        fun liseners (type: import_type) {

            val (title, message) = dialogs_list.getValue(type)

            create_material_dialog(this@MainActivity,
                title,
                message,
                "import",
                "Better another mode",
                {

                    init_action_activity("Decrypting the file", { load_dialog, load_input ->

                        lifecycleScope.launch (Dispatchers.IO) {
                            pass_list = import(this@MainActivity, this, pref, pass, list, json, type, load_dialog, load_input, db, nk)

                            time_out_init()

                            withContext(Dispatchers.Main) {
                                dialog_im.dismiss()

                                if (pass_list.isNotEmpty()) {
                                    very_pass(pass_states.desen_pass)
                                    if (type == import_type.preview) {
                                        back_b.visibility = View.VISIBLE
                                    }
                                } else {
                                    if (pref.getBoolean("info_s_full", false)) {
                                        very_pass(pass_states.en_pass)
                                    } else {
                                        very_pass(pass_states.without_without)
                                    }
                                }
                                pass_adapter.update(pass_list)
                            }
                        }

                    }, {})

                },
                {}
            )
        }

        preview.setOnClickListener {
            liseners(import_type.preview)
        }

        add_db.setOnClickListener {
            liseners(import_type.add_db)
        }

        replace_db.setOnClickListener {
            liseners(import_type.replace_db)
        }
    }


    private val import_result = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { (code, data) ->

        if (code == RESULT_CANCELED) {
            Toast.makeText(this, "No file", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        val query = contentResolver.query(data?.data!!, null, null, null, null, null)
        if (query!!.moveToFirst()) {

            val position = query.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val name = query.getString(position)

            val nk_very = name.matches(Regex(".*nk.*"))

            if (!name.matches(Regex(".*ns.*")) && !nk_very) {
                Toast.makeText(this, "The extension is incorrect", Toast.LENGTH_SHORT).show()
            } else {

                val json_f = JSONObject(
                    this.contentResolver.openInputStream(data.data!!).use { arch ->
                        arch?.bufferedReader()!!.readText()
                    }
                )

                if (json_f.has("salt") && json_f.has("iter") && ((json_f.has("algo") && json_f.has("k_size") && json_f.has("very_key") && json_f.has("data_list")) || (json_f.has("pass_list") && json_f.has("pro")))) {

                    val import_dialog = Dialog(this)
                    val import_view = create_dialog(this, R.layout.dialog_import, import_dialog)

                    val import_input_pass = import_view.findViewById<EditText>(R.id.input_pass)
                    val import_progress = import_view.findViewById<LinearProgressIndicator>(R.id.progress)

                    val import_visible_button = import_view.findViewById<ConstraintLayout>(R.id.password_visibility)
                    val import_icon_visible = import_view.findViewById<ShapeableImageView>(R.id.visibility_icon)

                    val import_button = import_view.findViewById<ConstraintLayout>(R.id.unlock_buttom)

                    import_input_pass.addTextChangedListener { dato ->
                        entropy(dato.toString(), import_progress)
                    }

                    visibility(pref, import_icon_visible, import_input_pass)
                    import_visible_button.setOnClickListener {
                        pref.edit().putBoolean("prims", !pref.getBoolean("prims", false)).commit()
                        visibility(pref, import_icon_visible, import_input_pass)
                    }

                    if (nk_very) {
                        create_material_dialog(
                            this@MainActivity, "",
                            "The architecture of \".nk\" files is not optimized for the \"NowiVault\" architecture; \"NowiVault\" is only optimized for the \".ns\" architecture, which is why the app freezes for a few seconds.",
                            "Ok",
                            "", {}, {})
                    }

                    import_button.setOnClickListener {

                        if (import_input_pass.text.isNotEmpty()) {

                            val (load_dialog, info_dialog) = load("Checking the legitimacy of the key", this@MainActivity)

                            if (auth_json(json_f, import_input_pass.text.toString(), nk_very)) {

                                import_dialog.dismiss()
                                dialog_pr_add_re(json_f, null, import_input_pass.text.toString(), nk_very)

                            } else {
                                import_input_pass.text.clear()
                                Toast.makeText(this, "The password is not valid", Toast.LENGTH_SHORT).show()
                            }
                            load_dialog.dismiss()


                        } else {
                            Toast.makeText(this, "No password has been specified", Toast.LENGTH_SHORT).show()
                        }

                    }

                } else {
                    Toast.makeText(this, "The file structure is not correct", Toast.LENGTH_SHORT)
                        .show()
                }
            }

        } else {
            Toast.makeText(this, "Empty file", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

            force(this, pref, event.values[0], event.values[1], event.values[2])

            x_regi = event.values[0]
            y_regi = event.values[1]
            z_regi = event.values[2]

        }
    }
}