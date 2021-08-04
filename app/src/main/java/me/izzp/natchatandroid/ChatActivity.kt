package me.izzp.natchatandroid

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat

private val sdf =
    SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.FULL, SimpleDateFormat.MEDIUM)

class ChatActivity : AppCompatActivity(), TextView.OnEditorActionListener {

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val root by lazy { itemView as LinearLayout }
        val card by lazy { itemView.findViewById<CardView>(R.id.cardView) }
        val content by lazy { itemView.findViewById<TextView>(R.id.tv_content) }
        val time by lazy { itemView.findViewById<TextView>(R.id.tv_time) }
    }

    inner class Adapter : RecyclerView.Adapter<Holder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = layoutInflater.inflate(R.layout.chat_listitem, parent, false)
            val holder = Holder(view)
            return holder
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val msg = messages[position]
            holder.root.gravity =
                if (msg.type == MessageType.receive) Gravity.START else Gravity.END
            val array = obtainStyledAttributes(intArrayOf(R.attr.colorSurface))
            val color = array.getColor(0, 0)
            array.recycle()
            holder.card.setCardBackgroundColor(
                if (msg.type == MessageType.receive) color else getColor(
                    R.color.theme_blue
                )
            )
            holder.content.text = msg.content
            holder.time.text = sdf.format(msg.time)
            val params = holder.card.layoutParams as ViewGroup.MarginLayoutParams
            if (msg.type == MessageType.receive) {
                params.marginStart = 0
                params.marginEnd = 50.dp2px()
            } else {
                params.marginStart = 50.dp2px()
                params.marginEnd = 0
            }
        }

        override fun getItemCount() = messages.size
    }


    private val name by lazy { intent.getStringExtra("name") ?: "" }
    private val messages by lazy { DB.messages(name) }
    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.recyclerView) }
    private val et by lazy { findViewById<EditText>(R.id.et_message) }
    private val dbListener = object : DBListener {
        override fun changed(name: String) {
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        recyclerView.layoutManager = LinearLayoutManager(this)
        supportActionBar?.also {
            it.title = name
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
        et.setOnEditorActionListener(this)
        refresh()
        DB.addListener(dbListener)
        Service.requestTouch(name)
    }

    override fun onDestroy() {
        super.onDestroy()
        DB.removeListener(dbListener)
    }

    private fun refresh() {
        var adapter = recyclerView.adapter as Adapter?
        if (adapter != null) {
            adapter.notifyDataSetChanged()
        } else {
            adapter = Adapter()
            recyclerView.adapter = adapter
        }
        recyclerView.scrollToPosition(messages.size - 1)
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        println("onEditorAction ${v.text} $actionId ${event?.keyCode}")
        val s = v.text.toString()
        if (s.isEmpty()) {
            return false
        }
        if (actionId == EditorInfo.IME_ACTION_SEND || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
            Service.sendChat(name, s)
            et.setText("")
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
