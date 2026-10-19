package com.qubix.stream

import android.app.Application
import androidx.room.Room
import com.qubix.stream.db.AppDatabase

class QubixStreamApp : Application() {
    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDatabase::class.java, "qubix-stream.db").build()
        instance = this
    }

    companion object {
        lateinit var instance: QubixStreamApp
            private set
    }
}
