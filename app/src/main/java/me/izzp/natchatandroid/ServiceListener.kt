package me.izzp.natchatandroid

abstract class ServiceListener {
    open fun registered() {}
    open fun userEnter(user: User) {}
    open fun userExit(user: User) {}
    open fun users(users: List<User>) {}
    open fun msg(msg: Msg) {}
    open fun touchBegin(name: String) {}
    open fun touchSuccess(name: String) {}
    open fun touchFail(name: String) {}
}

class MultiServiceListener : ServiceListener() {

    private val listeners = mutableListOf<ServiceListener>()

    fun add(l: ServiceListener) {
        if (!listeners.contains(l)) {
            listeners.add(l)
        }
    }

    fun remove(l: ServiceListener) {
        listeners.remove(l)
    }

    fun clear() {
        listeners.clear()
    }

    override fun registered() {
        listeners.forEach { it.registered() }
    }

    override fun userEnter(user: User) {
        listeners.forEach { it.userEnter(user) }
    }

    override fun userExit(user: User) {
        listeners.forEach { it.userExit(user) }
    }

    override fun users(users: List<User>) {
        listeners.forEach { it.users(users) }
    }

    override fun msg(msg: Msg) {
        listeners.forEach { it.msg(msg) }
    }

    override fun touchBegin(name: String) {
        listeners.forEach { it.touchBegin(name) }
    }

    override fun touchSuccess(name: String) {
        listeners.forEach { it.touchSuccess(name) }
    }

    override fun touchFail(name: String) {
        listeners.forEach { it.touchFail(name) }
    }

}