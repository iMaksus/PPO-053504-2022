package com.example.lab2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_edit.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_timer_start.*
import kotlinx.android.synthetic.main.timer_home.*
import kotlinx.android.synthetic.main.timer_home.view.*
import kotlinx.coroutines.launch
import java.util.*


class MainActivity : BaseActivityTheme(), OnSharedPreferenceChangeListener {
//    var db = Room.databaseBuilder(
//        applicationContext,
//        AppDatabase::class.java, "timerapp-database"
//    ).build()

    // the database won’t be created until we call it.
    private val timerDatabase by lazy { AppDatabase.getDatabase(this).timerDao() }
    private lateinit var adapter: TimerAdapter
    private var fontScale: Float = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
//        setStartPreferences()
//        setTheme(R.style.AppTheme)
        setupSharedPreferences()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

//        setSupportActionBar(toolbar)
//        supportActionBar?.title = getString(R.string.main_activity_title)


//        // manually add timer
//        val title = "Hello2 title"
//        val color = "123587"
//        val duration = 30
//        // Add the new note at the top of the list
//        val newNote = Timer(title, color, duration)
//        lifecycleScope.launch {
//            timerDatabase?.addTimer(newNote)
//        }


//        for (i in 1..3){
//            val timerTitle = "Title hello"
//            val timerColor = R.color.title_aqua.toString()
//            val timerDuration = 0
//            // Add the new note at the top of the list
//            val newTimer = Timer(timerTitle, timerColor, timerDuration)
//            lifecycleScope.launch {
//                timerDatabase?.addTimer(newTimer)
//            }
//        }
        setRecyclerView()
        observeTimers()
        lifecycleScope.launch {
            timerDatabase?.getTimers()?.collect { timerList ->
                Log.d("timerList ---- ", timerList.toString())
            }
        }
        btnTimerNew.setOnClickListener { v->
            val name = getString(R.string.default_timer_title)
            val duration = 0
            val color = R.color.title_aqua
            val timer = Timer(name, color.toString(), duration)
            lifecycleScope.launch {
                timerDatabase?.addTimer(timer)
            }
        }

    }

    override fun onResume() {
        Log.d("OnResume", "--------")
        super.onResume()
    }


    private fun setRecyclerView()
    {
        // устанавливаем для списка адаптер
        val recyclerView = findViewById<RecyclerView>(R.id.timer_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        adapter = TimerAdapter(this)

        adapter.setItemListener(object : RecyclerClickListener {

            // Tap the btnDelete to delete the note.
            override fun onItemRemoveClick(position: Int) {
                Log.d("DELETE TIMER", "------------------")
                val timerList = adapter.currentList.toMutableList()
                val title = timerList[position].title
                val color = timerList[position].color
                val duration = timerList[position].duration
                val id = timerList[position].id
                val removeTimer = Timer(title, color, duration, id)
                lifecycleScope.launch {
                    timerDatabase?.deleteTimer(removeTimer)
                }
            }

            // Tap the note to edit.
            override fun onItemClick(position: Int) {
                Log.d("TIMER VIEW", "-----------------")
                val intent = Intent(this@MainActivity, TimerStart::class.java)
                val timerList = adapter.currentList.toMutableList()

                intent.putExtra("timer_id", timerList[position].id)
                intent.putExtra("timer_title", timerList[position].title)
                intent.putExtra("timer_color", timerList[position].color)
                intent.putExtra("timer_duration", timerList[position].duration)
//                if (timerList[position].duration > 0)
                    startActivity(intent)
//                else
//                    showToast("You can't start timer: duration is 0")
            }

            // Tap the btn Edit to edit.
            override fun onItemEditClick(position: Int) {
                val intent = Intent(this@MainActivity, EditActivity::class.java)
                val timerList = adapter.currentList.toMutableList()
                intent.putExtra("timer_id", timerList[position].id)
                intent.putExtra("timer_title", timerList[position].title)
                intent.putExtra("timer_color", timerList[position].color)
                intent.putExtra("timer_duration", timerList[position].duration)
                startActivity(intent)
            }
        })
        recyclerView.adapter = adapter
    }


    private val newNoteResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Get the new note from the AddNoteActivity
                val timerTitle = "Title hello"
                val timerColor = "123567"
                val timerDuration = 50
                // Add the new note at the top of the list
                val newTimer = Timer(timerTitle, timerColor, timerDuration)
                lifecycleScope.launch {
                    timerDatabase?.addTimer(newTimer)
                }
            }
        }
    private fun observeTimers()
    {
        lifecycleScope.launch {
            timerDatabase?.getTimers()?.collect { timerList ->
                Log.d("timer list", timerList.isNotEmpty().toString())
                if (timerList.isNotEmpty()) {
                    adapter.submitList(timerList)
                }
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        if (id == R.id.action_settings) {
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupSharedPreferences() {
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        setStartPreferences()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        Log.d("preferences ----", "CHANGED!!!!")
        if (key == "font_size") {
            val fontSize = sharedPreferences.getString("font_size", "medium").toString()
            Log.d("df", fontSize)
            if (fontSize == "Medium")
            {
                this.fontScale = 1.0f
            }
            else if (fontSize == "Small")
            {
                this.fontScale = 0.5f
            }
            else if (fontSize == "Large")
            {
                this.fontScale = 1.5f
            }
            recreate()
        }
        if (key == "app_theme") {
            val isDarkTheme = sharedPreferences.getBoolean("app_theme", false)
            if (isDarkTheme)
            {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else
            {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        if (key == "language"){
//            val languageCode = sharedPreferences.getString("language", "English").toString()
//            Log.d("languageCode 1", languageCode)
//            if (languageCode == "English")
//            {
//                setLocale("en")
//            }
//            else if (languageCode == "Русский")
//            {
//                setLocale("ru")
//            }
//            recreate()
            finish()
            startActivity(getIntent())
            overridePendingTransition(0, 0);
        }
    }
    override fun attachBaseContext(newBase: Context) {
        val newConfig = Configuration(newBase.resources.configuration)
        newConfig.fontScale = this.fontScale
        applyOverrideConfiguration(newConfig)
        super.attachBaseContext(newBase)
    }

    private fun setLocale(languageCode: String)
    {
        val config = resources.configuration
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            config.setLocale(locale)
        else
            config.locale = locale

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun setStartPreferences()
    {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val language = sharedPreferences.getString("language", "English").toString()
        Log.d("Language 2 --->", language!!)
//        var languageCode = "en"
//        if (language == "English")
//        {
//            languageCode = "en"
//        }
//        else if (language == "Русский")
//        {
//            languageCode = "ru"
//        }
//        setLocale(languageCode)
        val isDarkTheme = sharedPreferences.getBoolean("app_theme", true)
        if (isDarkTheme)
        {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        else
        {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

    }

    override fun onDestroy() {
        Log.d("onDestroy ----", "MAIN ACTIVITY")
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    fun OnClickEdit(view: View)
    {
        val modify_intent = Intent(this, EditActivity::class.java)
        modify_intent.putExtra("id", tvTimerId.text.toString().toInt())
        startActivity(modify_intent)
    }

    private fun showToast(message:String){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show()
    }
}