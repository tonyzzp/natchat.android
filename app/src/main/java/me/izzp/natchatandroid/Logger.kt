package me.izzp.natchatandroid

object Logger {

    private val logs = mutableListOf<String>()

    fun log(s: String) {
        logs.add(s)
        println(s)
    }

    fun clear() {
        logs.clear()
    }

    fun joinLogs() = logs.joinToString("\n")

}