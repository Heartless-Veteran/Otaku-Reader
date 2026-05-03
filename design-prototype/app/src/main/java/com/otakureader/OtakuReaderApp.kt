package com.otakureader

import android.app.Application
import com.otakureader.data.db.AppDatabase

class OtakuReaderApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
