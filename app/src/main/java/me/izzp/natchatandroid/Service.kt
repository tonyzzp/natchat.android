package me.izzp.natchatandroid

import android.os.Build
import com.google.gson.Gson
import org.json.JSONArray
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class User(
    var name: String = "",
    var addr: InetSocketAddress = InetSocketAddress.createUnresolved("127.0.0.1", 0),
)

object Service {

    private val gson by lazy { Gson() }
    private val executor = Executors.newFixedThreadPool(4)
    private lateinit var socket: DatagramSocket
    private lateinit var regTimer: Timer
    private val touchTimers = mutableMapOf<String, Timer>()
    private var running = false
    private val serverAddr by lazy { InetSocketAddress(SERVER_HOST, SERVER_PORT) }
    private val myName by lazy { Build.BOARD }
    private val _users = mutableListOf<User>()
    val users get() = _users.toList()
    var listener: ServiceListener? = null


    fun connect() {
        running = true
        thread {
            socket = DatagramSocket(InetSocketAddress("0.0.0.0", 0))
            while (running) {
                val bytes = ByteArray(1024 * 128)
                val packet = DatagramPacket(bytes, bytes.size)
                try {
                    socket.receive(packet)
                } catch (e: IOException) {
                    println("receive失败:${e}")
                    continue
                }
                processPacket(packet)
            }
        }
        val regTask = object : TimerTask() {
            override fun run() {
                if (running) {
                    send(serverAddr, Msg(Event = "reg", Name = myName))
                } else {
                    regTimer.cancel()
                }
            }
        }
        regTimer = Timer()
        regTimer.schedule(regTask, Date(), 2000)
    }

    fun close() {
        running = false
        if (this::socket.isInitialized) {
            socket.close()
        }
        if (this::regTimer.isInitialized) {
            regTimer.cancel()
        }
        _users.clear()
    }

    private fun addUser(name: String, addr: SocketAddress): User {
        var user = _users.find { it.name == name }
        if (user != null) {
            user.addr = addr as InetSocketAddress
        } else {
            user = User(name, addr as InetSocketAddress)
            synchronized(_users) {
                _users.add(user)
            }
            runOnMainThread { listener?.userEnter(user) }
        }
        return user
    }

    private fun addUser(name: String, ip: String, port: Int) {
        addUser(name, InetSocketAddress(ip, port))
    }

    private fun removeUser(name: String): User? {
        val user = _users.find { it.name == name }
        if (user != null) {
            synchronized(_users) {
                _users.remove(user)
            }
            runOnMainThread { listener?.userExit(user) }
        }
        return user
    }

    private fun processPacket(packet: DatagramPacket) {
        val content = String(packet.data, packet.offset, packet.length)
        println("receive:${packet.socketAddress} $content")
        val msg = gson.fromJson(content, Msg::class.java)
        when (msg.Event) {
            "reg" -> {
                if (msg.Name == myName) {
                    regTimer.cancel()
                    runOnMainThread { listener?.registered() }
                } else {
                    addUser(msg.Name, msg.IP, msg.Port)
                }
            }
            "touch" -> {
                if (msg.Msg == "unreg") {
                    // nothing
                } else if (msg.Msg == "offline") {
                    removeUser(msg.Name)
                } else if (msg.Port > 0) {
                    // 服务器通知对方ip
                    addUser(msg.Name, msg.IP, msg.Port)
                    beginTouch(msg.Name, msg.IP, msg.Port)
                } else if (msg.Name.isNotEmpty()) {
                    // 对方主动联系
                    cancelTouch(msg.Name)
                }
            }
            "unreg" -> {
                removeUser(msg.Name)
            }
            "users" -> {
                val array = JSONArray(msg.Msg)
                synchronized(_users) {
                    _users.clear()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val name = obj.getString("Name")
                        val addr = InetSocketAddress.createUnresolved(
                            obj.getString("IP"),
                            obj.getInt("Port")
                        )
                        _users.add(User(name, addr))
                    }
                }
                runOnMainThread { listener?.users(_users) }
            }
            "chat" -> {
                DB.receive(msg.Name, msg.Msg)
                runOnMainThread { listener?.msg(msg) }
            }
            else -> {
                println("unknown message event")
            }
        }
    }

    private fun send(addr: SocketAddress, msg: Any) {
        val json = gson.toJson(msg)
        executor.execute {
            if (this::socket.isInitialized && !socket.isClosed) {
                val bytes = json.toByteArray()
                val packet = DatagramPacket(bytes, 0, bytes.size, addr)
                socket.send(packet)
            }
        }
    }

    private fun send(ip: String, port: Int, msg: Any) {
        send(InetSocketAddress(ip, port), msg)
    }

    private fun beginTouch(name: String, ip: String, port: Int) {
        var timer = touchTimers[name]
        if (timer == null) {
            timer = Timer()
            touchTimers[name] = timer
        }
        timer!!.schedule(object : TimerTask() {
            private var times = 0
            override fun run() {
                times++
                if (times >= 10) {
                    cancelTouch(name)
                } else {
                    println("touch: $name")
                    send(ip, port, Msg(Event = "touch", Name = myName))
                }
            }
        }, Date(), 1000)
    }

    private fun cancelTouch(name: String) {
        println("touch cancel: $name")
        val timer = touchTimers[name]
        if (timer != null) {
            timer.cancel()
            touchTimers.remove(name)
        }
    }

    fun loadUsers() {
        send(serverAddr, Msg("users"))
    }

    fun requestTouch(name: String) {
        executor.execute {
            send(serverAddr, Msg(Event = "touch", Name = myName, ToName = name))
        }
    }

    fun sendChat(name: String, msg: String) {
        DB.send(name, msg)
        executor.execute {
            val user = _users.find { it.name == name } ?: return@execute
            send(user.addr, Msg(Event = "chat", Name = myName, Msg = msg))
        }
    }
}