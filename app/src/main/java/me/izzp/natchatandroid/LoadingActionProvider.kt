package me.izzp.natchatandroid

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import androidx.core.view.ActionProvider

class LoadingActionProvider(context: Context) : ActionProvider(context) {

    private lateinit var view: View
    private var menuItem: MenuItem? = null
    private val icon by lazy { view.findViewById<ImageView>(R.id.icon) }
    private val colorNormal by lazy {
        val array = context.obtainStyledAttributes(intArrayOf(R.attr.colorControlNormal))
        val color = array.getColor(0, 0)
        array.recycle()
        color
    }
    private val colorRed = context.getColor(android.R.color.holo_red_light)
    private val tasks = mutableListOf<() -> Unit>()
    private var isInited = false
    var onClick: (() -> Unit)? = null

    override fun onCreateActionView(): View {
        throw  UnsupportedOperationException()
    }

    override fun onCreateActionView(forItem: MenuItem): View {
        menuItem = forItem
        view = LayoutInflater.from(context).inflate(R.layout.action_loading, null, false)
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                println("onPreDraw")
                icon.viewTreeObserver.removeOnPreDrawListener(this)
                tasks.forEach { it() }
                tasks.clear()
                isInited = true
                return true
            }
        }
        icon.viewTreeObserver.addOnPreDrawListener(listener)
        return view
    }

    private fun post(f: () -> Unit) {
        if (isInited) {
            f()
        } else {
            tasks.add(f)
        }
    }

    fun begin() {
        println("begin")
        post {
            println("begin.run")
            menuItem?.isVisible = true
            val anim = RotateAnimation(
                0f,
                360f,
                RotateAnimation.RELATIVE_TO_SELF,
                0.5f,
                RotateAnimation.RELATIVE_TO_SELF,
                0.5f
            )
            anim.interpolator = LinearInterpolator()
            anim.repeatMode = Animation.RESTART
            anim.repeatCount = Animation.INFINITE
            anim.duration = 2000
            icon.animation = anim
            anim.startNow()

        }
    }

    fun stop() {
        println("stop")
        post {
            println("stop.run")
            view.clearAnimation()
            view.animation = null
            view.rotation = 0f
        }
    }

    fun hide() {
        println("hide")
        post {
            println("hide.run")
            view.clearAnimation()
            view.animation = null
            view.rotation = 0f
            menuItem?.isVisible = false
        }
    }

    fun show() {
        println("show")
        post {
            println("show.run")
            menuItem?.isVisible = true
        }
    }

    var err: Boolean = false
        set(value) {
            field = value
            post {
                icon.imageTintList =
                    if (value) ColorStateList.valueOf(colorRed) else ColorStateList.valueOf(
                        colorNormal
                    )
                if (value) {
                    view.setOnClickListener {
                        onClick?.invoke()
                    }
                } else {
                    view.setOnClickListener(null)
                }
            }
        }

}