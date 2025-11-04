package com.example.unimarket.workers

import android.content.Context

object PrefetchWork {
    /** Lanza el prefetch sin duplicarlo si ya está encolado. */
    fun run(context: Context) = PrefetchBusinessesWorker.enqueue(context, replace = false)

    /** Úsalo post-login si quieres forzar reemplazo del job anterior. */
    fun runReplacing(context: Context) = PrefetchBusinessesWorker.enqueue(context, replace = true)
}