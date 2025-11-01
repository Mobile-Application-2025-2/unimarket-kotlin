package com.example.unimarket

import android.app.Application
import com.example.unimarket.workers.PrefetchWork

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Encolar al iniciar la app (si ya se encoló, KEEP evita duplicado)
        PrefetchWork.run(this)
    }
}