package me.izzp.natchatandroid

import me.izzp.upnp.portmapping.PortMapping
import me.izzp.upnp.portmapping.Utils
import me.izzp.upnp.portmapping.defaultIP4Address
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

    private val executor = Executors.newFixedThreadPool(4)
    private lateinit var socket: DatagramSocket
    private lateinit var regTimer: Timer
    private val touchTimers = mutableMapOf<String, Timer>()
    private var running = false
    private lateinit var serverAddr: InetSocketAddress
    private val _users = mutableListOf<User>()
    val listener = MultiServiceListener()
    val users get() = _users.toList()
    val isConnected: Boolean
        get() {
            return running && this::socket.isInitialized && !this.socket.isClosed
        }
    val isConnecting: Boolean
        get() {
            return running && !isConnected
        }

    fun connect() {
        running = true
        thread {
            serverAddr = InetSocketAddress(Prefs.serverHost, Prefs.serverPort)
            socket = DatagramSocket(InetSocketAddress("0.0.0.0", 0))
            Logger.log("Service.connect:${socket.localSocketAddress} -> $serverAddr")
            requestUpnp(socket.localPort)
            while (running) {
                val bytes = ByteArray(1024 * 128)
                val packet = DatagramPacket(bytes, bytes.size)
                try {
                    socket.receive(packet)
                } catch (e: IOException) {
                    Logger.log("receive失败:$e")
                    break
                }
                processPacket(packet)
            }
            close()
        }
        requestReg()
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

    fun reconnect() {
        close()
        connect()
    }

    private fun requestReg() {
        if (this::regTimer.isInitialized) {
            regTimer.cancel()
        }
        val regTask = object : TimerTask() {
            override fun run() {
                if (running) {
                    if (this@Service::serverAddr.isInitialized) {
                        send(serverAddr, Msg(Event = "reg", Name = Prefs.name))
                    }
                } else {
                    regTimer.cancel()
                }
            }
        }
        regTimer = Timer()
        regTimer.schedule(regTask, Date(), 2000)
    }

    private fun requestUpnp(port: Int) {
        if (Prefs.upnpPort != 0 && Prefs.upnpPort != port) {
            PortMapping.del(Prefs.upnpPort, "UDP") {}
        }
        if (port == 0) {
            return
        }
        val localHost = Utils.findDefaultNetworkInterface()?.defaultIP4Address()
        Logger.log("upnp localHost: $localHost")
        PortMapping.add(port, "UDP", localHost?.hostName ?: "0.0.0.0", port, "natchat") {
            println("upnp result: $it")
            if (it == PortMapping.AddPortMappingResult.success) {
                Prefs.upnpPort = port
            }
        }
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
            runOnMainThread { listener.userEnter(user) }
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
            runOnMainThread { listener.userExit(user) }
        }
        return user
    }

    private fun processPacket(packet: DatagramPacket) {
        val content = String(packet.data, packet.offset, packet.length)
        Logger.log("receive:${packet.socketAddress} $content")
        val msg = Msg.fromJson(content)
        when (msg.Event) {
            "reg" -> {
                if (msg.Name == Prefs.name) {
                    regTimer.cancel()
                    runOnMainThread { listener.registered() }
                    loadUsers()
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
                    listener.touchSuccess(msg.Name)
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
                    _users.sortBy { it.name }
                }
                runOnMainThread { listener.users(_users) }
            }
            "chat" -> {
                DB.receive(msg.Name, msg.Msg)
                runOnMainThread { listener.msg(msg) }
            }
            else -> {
                Logger.log("unknown message event")
            }
        }
    }

    private fun send(addr: SocketAddress, msg: Msg) {
        val json = msg.toJson()
        Logger.log("Service.send: $addr , $json")
        executor.execute {
            if (this::socket.isInitialized && !socket.isClosed) {
                val bytes = json.toByteArray()
                val packet = DatagramPacket(bytes, 0, bytes.size, addr)
                try {
                    socket.send(packet)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun send(ip: String, port: Int, msg: Msg) {
        send(InetSocketAddress(ip, port), msg)
    }

    private fun beginTouch(name: String, ip: String, port: Int) {
        var timer = touchTimers[name]
        if (timer == null) {
            timer = Timer()
            touchTimers[name] = timer
        }
        timer.schedule(object : TimerTask() {
            private var times = 0
            override fun run() {
                times++
                if (times >= 10) {
                    cancelTouch(name)
                    listener.touchFail(name)
                } else {
                    send(ip, port, Msg(Event = "touch", Name = Prefs.name))
                }
            }
        }, Date(), 1000)
    }

    private fun cancelTouch(name: String) {
        Logger.log("touch cancel: $name")
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
        listener.touchBegin(name)
        executor.execute {
            send(serverAddr, Msg(Event = "touch", Name = Prefs.name, ToName = name))
        }
    }

    fun sendChat(name: String, msg: String) {
        DB.send(name, msg)
        executor.execute {
            val user = _users.find { it.name == name } ?: return@execute
            send(user.addr, Msg(Event = "chat", Name = Prefs.name, Msg = msg))
        }
    }
}