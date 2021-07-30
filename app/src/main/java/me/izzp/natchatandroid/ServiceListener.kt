package me.izzp.natchatandroid

abstract class ServiceListener {
    abstract fun registered()
    abstract fun userEnter(user: User)
    abstract fun userExit(user: User)
    abstract fun users(users: List<User>)
    abstract fun msg(msg: Msg)
}