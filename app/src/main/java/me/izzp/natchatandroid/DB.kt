package me.izzp.natchatandroid

import java.util.*

enum class MessageType {
    send,
    receive,
}

class Message(
    var type: MessageType,
    var to: String,
    var content: String,
    var time: Date,
)

interface DBListener {
    fun changed(name: String)
}

object DB {

    private val messages = mutableMapOf<String, MutableList<Message>>()
    private val listeners = mutableListOf<DBListener>()

    fun addListener(l: DBListener) {
        listeners.add(l)
    }

    fun removeListener(l: DBListener) {
        listeners.remove(l)
    }

    fun messages(name: String): MutableList<Message> {
        var list = messages[name]
        return if (list != null) {
            list
        } else {
            list = mutableListOf()
            messages[name] = list
            list
        }
    }

    fun send(name: String, content: String) {
        runOnMainThread {
            val list = messages(name)
            val msg = Message(MessageType.send, name, content, Date())
            list.add(msg)
            listeners.forEach {
                it.changed(name)
            }
        }
    }

    fun receive(name: String, content: String) {
        runOnMainThread {
            val list = messages(name)
            val msg = Message(MessageType.receive, name, content, Date())
            list.add(msg)
            listeners.forEach {
                it.changed(name)
            }
        }
    }
}