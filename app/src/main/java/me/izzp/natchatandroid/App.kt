package me.izzp.natchatandroid

import android.app.Application
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

class App : Application() {

    companion object {
        lateinit var app: App
    }

    private val handler = Handler(Looper.myLooper()!!)
    private val executor = Executor { command -> handler.post(command) }

    override fun onCreate() {
        super.onCreate()
        app = this
    }

    fun runOnMainThread(func: () -> Unit) {
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            func()
        } else {
            executor.execute(func)
        }
    }
}