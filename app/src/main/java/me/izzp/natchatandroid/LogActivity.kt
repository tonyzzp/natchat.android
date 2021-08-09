package me.izzp.natchatandroid

import android.content.Intent
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
    }

    override fun onResume() {
        super.onResume()
        println("onNewIntent:${intent.flags or Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT}")
        reload()
    }

    private fun reload() {
        val tv = findViewById<TextView>(R.id.tv_content)
        tv.text = Logger.joinLogs()
        val sv = findViewById<ScrollView>(R.id.scroll_view)
        tv.post {
            val offset = tv.height - sv.height
            sv.scrollTo(0, offset)
        }
    }

}