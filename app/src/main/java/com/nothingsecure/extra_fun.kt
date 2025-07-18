package com.nothingsecure

import android.annotation.SuppressLint
import android.util.Log
import androidx.core.graphics.toColorInt
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.time.InstantSource.system
import kotlin.math.log2


val mayusculas_l = ('A'..'Z').joinToString("").toList()
val numeros_l = (0..9).joinToString ("").toList()
val minusculas_l = ('a'..'z').joinToString("").toList()

val simbolos_l = listOf("@#$|?¿¡!").joinToString("").toList()
@SuppressLint("ResourceAsColor")
fun entropy(pass: String, porgress: LinearProgressIndicator){



    var simbolos = 0
    var minusculas = 0
    var mayusculas = 0
    var numeros = 0

    for (valor in pass) {

        if (minusculas_l.contains(valor)) {
            if (minusculas != 26 ) {minusculas += 26}
        }else if (mayusculas_l.contains(valor)) {
            if (mayusculas != 26) {mayusculas += 26}
        }else if (numeros_l.contains(valor)) {
            if (numeros != 9) {numeros += 10}
        }else {
            simbolos ++
        }
    }

    val final =  pass.length * log2((simbolos + mayusculas + minusculas + numeros).toDouble())

    porgress.progress = final.toInt()
    if (final in 0.0..40.0) {
        porgress.setIndicatorColor("#aa4040".toColorInt())
    }else if (final in 40.0..60.0) {
        porgress.setIndicatorColor("#c9a23e".toColorInt())
    }else if (final > 60.0){
        porgress.setIndicatorColor("#40aa47".toColorInt())
    }else {
        porgress.setIndicatorColor("#e3e3e3".toColorInt())
    }
}

fun pass_generator (ini_list: List<List<Char>>, size: Int): String {

    val bool_list = mutableListOf<Boolean>()

    for (i in 0..ini_list.size - 1) {
        bool_list.add(false)
    }

    var final_list = ""

    for (position in ini_list) {
        final_list += position.joinToString("")
    }

    var pass = ""

    while (true) {
        pass = ""
        for (i in 0..size - 1) {
            pass += final_list.toList().shuffled()[0]
        }

        for (value in pass) {

            var final = true
            for (position in 0..bool_list.size - 1) {
                if (ini_list[position].contains(value) && !bool_list[position]) {
                        bool_list[position] = true
                }
                final = final and bool_list[position]
            }
            Log.e("bool_list", bool_list.toString())
            if (final) {
                return pass
            }


        }
    }

}