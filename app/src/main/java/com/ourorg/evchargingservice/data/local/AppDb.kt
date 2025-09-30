package com.ourorg.evchargingservice.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDb(ctx: Context) : SQLiteOpenHelper(ctx, "ev.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE session(
            id INTEGER PRIMARY KEY CHECK (id=1),
            nic TEXT NOT NULL, token TEXT NOT NULL, role TEXT NOT NULL, logged_at INTEGER NOT NULL
        )""")
        db.execSQL("""CREATE TABLE user_location(
            id INTEGER PRIMARY KEY CHECK (id=1),
            lat REAL NOT NULL, lng REAL NOT NULL, captured_at INTEGER NOT NULL
        )""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {}
}