package com.heeraya.timerloop

import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RingtoneListActivity : AppCompatActivity() {

    private lateinit var ringtoneRecyclerView: RecyclerView
    private lateinit var volumeSlider: SeekBar
    private lateinit var audioManager: AudioManager

    private var ringtoneAdapter: RingtoneAdapter? = null
    private val SELECTED_RINGTONE = "selected_ringtone"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ringtone_list)

        ringtoneRecyclerView = findViewById(R.id.ringtone_recycler_view)
        volumeSlider = findViewById(R.id.volumeSlider)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"))
        supportActionBar?.title = "Set Timer Sound"
        // Get a RingtoneManager
        val manager = RingtoneManager(this)
        manager.setType(RingtoneManager.TYPE_RINGTONE)

        val cursor: Cursor = manager.cursor
        val ringtoneList: MutableList<RingtoneWrapper> = mutableListOf()
        while (cursor.moveToNext()) {
            val ringtoneUri = manager.getRingtoneUri(cursor.position)
            val ringtone = RingtoneManager.getRingtone(this, ringtoneUri)
            ringtoneList.add(RingtoneWrapper(ringtone, ringtoneUri))
        }

        // Retrieve saved ringtone from SharedPreferences
        val sharedPreferences = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        val savedRingtoneUri = sharedPreferences.getString(SELECTED_RINGTONE, null)
        if (savedRingtoneUri != null) {
            val savedRingtone = RingtoneManager.getRingtone(this, Uri.parse(savedRingtoneUri))
            // Select the saved ringtone in your adapter
        }

        // Set up the RecyclerView with the adapter
        ringtoneAdapter = RingtoneAdapter(ringtoneList, this) { selectedRingtone ->
            ringtoneAdapter?.stopCurrentRingtone()
            selectedRingtone.ringtone.play()
            ringtoneAdapter?.currentRingtone = selectedRingtone
            sharedPreferences.edit().putString(SELECTED_RINGTONE, selectedRingtone.uri.toString()).apply()
        }
        ringtoneRecyclerView.layoutManager = LinearLayoutManager(this)
        ringtoneRecyclerView.adapter = ringtoneAdapter

        // Set the max value and current value of the SeekBar
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        volumeSlider.max = maxVolume
        volumeSlider.progress = audioManager.getStreamVolume(AudioManager.STREAM_RING)

        volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, progress, 0)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onPause() {
        super.onPause()
        ringtoneAdapter?.stopCurrentRingtone()
    }

}
