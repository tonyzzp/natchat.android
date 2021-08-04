package me.izzp.natchatandroid

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name by lazy { itemView.findViewById<TextView>(R.id.tv_name) }
        val unread by lazy { itemView.findViewById<TextView>(R.id.tv_unread) }
    }

    inner class Adapter(var users: List<User>) : RecyclerView.Adapter<Holder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = layoutInflater.inflate(R.layout.userlist_item, parent, false)
            val holder = Holder(view)
            view.setOnClickListener {
                onClick(holder)
            }
            return holder
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val user = users[position]
            holder.name.text = user.name
        }

        override fun getItemCount() = users.size

        private fun onClick(holder: Holder) {
            val intent = Intent(this@MainActivity, ChatActivity::class.java)
            intent.putExtra("name", users[holder.adapterPosition].name)
            startActivity(intent)
        }
    }

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.recyclerView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EventBus.regist(this)
        Service.listener = object : ServiceListener() {
            override fun registered() {
                Service.loadUsers()
            }

            override fun userEnter(user: User) {
                refreshList()
            }

            override fun userExit(user: User) {
                refreshList()
            }

            override fun users(users: List<User>) {
                refreshList()
            }

            override fun msg(msg: Msg) {
            }
        }
        Service.connect()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    private fun refreshList() {
        var adapter = recyclerView.adapter as Adapter?
        if (adapter != null) {
            adapter.users = Service.users
            adapter.notifyDataSetChanged()
        } else {
            adapter = Adapter(Service.users)
            recyclerView.adapter = adapter
        }
    }

    @Event
    @Keep
    fun onSettingsChanged(event: String) {
        println("MainActivity.onSettingsChanged: $event")
        if (event == "settings_changed") {
            Service.reconnect()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            EventBus.unregist(this)
            Service.close()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_mainactivity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                Service.loadUsers()
                return true
            }
            R.id.mi_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}