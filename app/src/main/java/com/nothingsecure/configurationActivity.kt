package com.nothingsecure

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.nothingsecure.recy_information.conf_adapter
import com.nothingsecure.recy_information.conf_adapter.Companion.colors_list
import com.nothingsecure.recy_information.conf_adapter.Companion.mods_all
import com.nothingsecure.recy_information.conf_adapter.Companion.mods_recy

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
            AlertDialog.Builder(this).apply {
                setTitle("Do you want to give your opinion on Nothing K?")
                setPositiveButton("Yes") { _, _ -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/EWnhgBtgu5jCB3Fa9"))) }
                setNegativeButton("No") { _, _ -> }
            }.show()
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