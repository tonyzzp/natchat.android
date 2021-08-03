package me.izzp.natchatandroid

import android.util.TypedValue

fun runOnMainThread(func: () -> Unit) {
    App.app.runOnMainThread(func)
}

fun Int.dp2px() =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        App.app.resources.displayMetrics
    )
        .toInt()

fun Int.px2dp() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_PX,
    this.toFloat(),
    App.app.resources.displayMetrics
)
