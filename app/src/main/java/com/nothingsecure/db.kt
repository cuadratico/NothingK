package com.nothingsecure

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

data class register (val id: Int, var time: String, val information: String, val color: String, val iv: String)
data class pass (val id: Int, var pass: String, var information: String, var iv: String)

class db (context: Context): SQLiteOpenHelper(context, "information.db", null, 1){
    override fun onCreate(db: SQLiteDatabase?) {

        db?.execSQL("CREATE TABLE time_register (id INTEGER PRIMARY KEY AUTOINCREMENT, time TEXT, information TEXT, color TEXT, iv TEXT)")
        db?.execSQL("CREATE TABLE pass (id INTEGER PRIMARY KEY AUTOINCREMENT, pass TEXT, information TEXT, iv TEXT)")
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {}

    val db = this.writableDatabase

    // time_register db

    fun add_register (time: String, information: String, color: String, iv: String) {

        db.execSQL("INSERT INTO time_register (time, information, color, iv) VALUES (?, ?, ?, ?)", arrayOf(time, information, color, iv))

    }

    fun delete_register (all: Boolean, id: Int = 0): Boolean {

        if (all) {
            db.execSQL("DELETE FROM time_register")
        }else {
            db.execSQL("DELETE FROM time_register WHERE id = ?", arrayOf(id))
        }

        return true
    }

    fun select_register (): Boolean {
        val db_read = this.readableDatabase
        val query = db_read.rawQuery("SELECT * FROM time_register", null, null)

        fun add () {
            register_list = register_list.plus(register(query.getInt(0), query.getString(1), query.getString(2), query.getString(3), query.getString(4)))
        }

        if (query.moveToFirst()) {
            add()

            while (query.moveToNext()) {
                add()
            }
            register_list = register_list.reversed()
            return true
        }else {
            return false
        }

    }


    // pass db

    fun add_pass (pass: String, information: String, iv: String) {

        db.execSQL("INSERT INTO pass (pass, information, iv) VALUES (?, ?, ?)", arrayOf(pass, information, iv))

    }


    fun delete_pass (id: Int): Boolean {

        db.execSQL("DELETE FROM pass WHERE id = ?", arrayOf(id))

        return true

    }

    fun delete_speci (id_ini: Int, id_final: Int) {
        db.execSQL("DELETE FROM pass WHERE id BETWEEN ? AND ?", arrayOf(id_ini, id_final))
    }

    fun update_pass (id: Int, pass: String, information: String, iv: String){

        db.execSQL("UPDATE pass SET pass = ?, information = ?, iv = ? WHERE id = ?", arrayOf(pass, information, iv, id))
    }

    fun select_pass ():Boolean {
        val db_read = this.readableDatabase
        val query = db_read.rawQuery("SELECT * FROM pass", null, null)

        fun add () {
            pass_list = pass_list.plus(pass(query.getInt(0), query.getString(1), query.getString(2), query.getString(3)))
        }

        if (query.moveToFirst()) {
            add()
            while (query.moveToNext()) {
                add()
            }
            return true
        }else {
            return false
        }
    }

    fun delete_prin() {
        val db = this.writableDatabase

        db.execSQL("DELETE FROM pass")
    }

    fun delete_all () {
        val db = this.writableDatabase

        db.execSQL("DELETE FROM pass")
        db.execSQL("DELETE FROM time_register")

    }

    companion object {
        var register_list = listOf<register>()
        var pass_list = listOf<pass>()
    }

}