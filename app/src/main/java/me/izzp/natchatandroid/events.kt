package me.izzp.natchatandroid

import kotlin.reflect.KCallable

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Event()

object EventBus {

    private val map = mutableMapOf<Any, List<KCallable<*>>>()

    fun regist(receiver: Any) {
        val methods = receiver::class.members.filter {
            it.annotations.any { it is Event }
        }
        if (methods.isNotEmpty()) {
            map[receiver] = methods
        }
    }

    fun unregist(receiver: Any) {
        map.remove(receiver)
    }

    fun post(event: Any) {
        map.forEach { entry ->
            entry.value.forEach { callable ->
                if (callable.parameters[1].type.classifier == event::class) {
                    println("call:$callable")
                    callable.call(entry.key, event)
                }
            }
        }
    }

}