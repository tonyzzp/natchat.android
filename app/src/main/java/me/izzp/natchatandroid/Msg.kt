package me.izzp.natchatandroid


data class Msg(
    var Event: String = "",
    var Name: String = "",
    var ToName: String = "",
    var IP: String = "",
    var Port: Int = 0,
    var Msg: String = "",
)