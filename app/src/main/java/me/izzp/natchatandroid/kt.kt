package me.izzp.natchatandroid

fun runOnMainThread(func: () -> Unit) {
    App.app.runOnMainThread(func)
}