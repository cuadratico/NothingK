package com.nothingsecure

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nothingsecure.recy_information.emails_adapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun pass_generator_dialog (context: Context, info_image: ShapeableImageView) {
    val total = mutableListOf(minusculas_l)

    val gene_dilaog = BottomSheetDialog(context)
    val gen_view = LayoutInflater.from(context).inflate(R.layout.generator_dialog, null)


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
    val mail_gen = gen_view.findViewById<ConstraintLayout>(R.id.gen_emails_intent)

    mail_gen.setOnClickListener {
        val mk = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val pref = EncryptedSharedPreferences.create(context, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        pref.edit().putBoolean("gene", false).commit()

        gene_dilaog.dismiss()
        info_image.setImageResource(R.drawable.mail_generator)
        email_generator(context, info_image)
    }
    fun gen (bar: SeekBar) {
        result_pass.text = pass_generator(bar.progress, total)
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
        val anim_rotate = AnimationUtils.loadAnimation(context, R.anim.rotate)

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
        val manage = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("pass", result_pass.text.toString())
        manage.setPrimaryClip(clip)
    }


    gene_dilaog.setContentView(gen_view)
    gene_dilaog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    gene_dilaog.show()
}

fun email_generator (context: Context, info_image: ShapeableImageView) {

    val dialog_email = BottomSheetDialog(context)
    val email_view = LayoutInflater.from(context).inflate(R.layout.email_generator_dialog, null)

    val pass_intent = email_view.findViewById<ConstraintLayout>(R.id.gen_pass_intent)

    val recy_emails = email_view.findViewById<RecyclerView>(R.id.recy_mails)

    val input_name = email_view.findViewById<EditText>(R.id.input_name)
    val input_date = email_view.findViewById<EditText>(R.id.input_date)
    val input_domain = email_view.findViewById<EditText>(R.id.input_domain)

    val but_generator = email_view.findViewById<AppCompatButton>(R.id.generator)


    val adapter = emails_adapter(listOf("alex-1998@hotmail.com", "alex1998@hotmail.com", "1998alex@hotmail.com", "alex.1998@hotmail.com", "1998.alex@hotmail.com", "alex/1998@hotmail.com", "1998/alex@hotmail.com", "alex19981998@hotmail.com", "19981998alex@hotmail.com", "1998.alex@hotmail.com"))

    recy_emails.adapter = adapter
    recy_emails.layoutManager = LinearLayoutManager(context)

    pass_intent.setOnClickListener {
        val mk = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val pref = EncryptedSharedPreferences.create(context, "ap", mk, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        pref.edit().putBoolean("gene", true).commit()

        dialog_email.dismiss()
        info_image.setImageResource(R.drawable.generator)
        pass_generator_dialog(context, info_image)
    }



    but_generator.setOnClickListener {
        if (Regex("@.+\\..+").matches(input_domain.text.toString()) && input_name.text.isNotEmpty() && input_date.text.isNotEmpty()) {

            val load_dialog = load("Generating the emails", context)

            val name = input_name.text.toString()
            val date = input_date.text.toString()
            val domain = input_domain.text.toString()
            CoroutineScope(Dispatchers.IO).launch {
                val emails_list = listOf("$name-$date$domain", "$name$date$domain", "$date$name$domain", "$name.$date$domain", "$date.$name$domain", "$name/$date$domain", "$date/$name$domain", "$name${date}${date}$domain", "$date${date}$name$domain", "$date.$name$domain")

                withContext(Dispatchers.Main) {
                    adapter.update(emails_list)
                    load_dialog.dismiss()
                    cancel()
                }
            }
        }else {
            Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
        }

    }


    dialog_email.setContentView(email_view)
    dialog_email.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog_email.show()


}