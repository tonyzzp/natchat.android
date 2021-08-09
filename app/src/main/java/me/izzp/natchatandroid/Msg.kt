package me.izzp.natchatandroid

import org.json.JSONObject


class Msg(
    var Event: String = "",
    var Name: String = "",
    var ToName: String = "",
    var IP: String = "",
    var Port: Int = 0,
    var Msg: String = "",
) {

    companion object {
        fun fromJson(s: String): Msg {
            val json = JSONObject(s)
            return Msg(
                json.optString("Event", ""),
                json.optString("Name", ""),
                json.optString("ToName", ""),
                json.optString("IP", ""),
                json.optInt("Port", 0),
                json.optString("Msg", ""),
            )
        }
    }

    fun toJson() = JSONObject().run {
        if (Event != "") {
            put("Event", Event)
        }
        if (Name != "") {
            put("Name", Name)
        }
        if (ToName != "") {
            put("ToName", ToName)
        }
        if (IP != "") {
            put("IP", IP)
        }
        if (Port != 0) {
            put("Pot", Port)
        }
        if (Msg != "") {
            put("Msg", Msg)
        }
        toString()
    }
}