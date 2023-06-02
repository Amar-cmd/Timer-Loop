package com.heeraya.timerloop

import android.content.Context
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RingtoneListActivity : AppCompatActivity() {

    private lateinit var ringtoneRecyclerView: RecyclerView
    private lateinit var volumeSlider: SeekBar

    private var ringtoneAdapter: RingtoneAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ringtone_list)

        ringtoneRecyclerView = findViewById(R.id.ringtone_recycler_view)
        volumeSlider = findViewById(R.id.volumeSlider)

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

        // Set up the RecyclerView with the adapter
        ringtoneAdapter = RingtoneAdapter(ringtoneList, this) { selectedRingtone ->
            ringtoneAdapter?.stopCurrentRingtone()
            selectedRingtone.ringtone.play()
            ringtoneAdapter?.currentRingtone = selectedRingtone
        }
        ringtoneRecyclerView.layoutManager = LinearLayoutManager(this)
        ringtoneRecyclerView.adapter = ringtoneAdapter

        // Set the max value and current value of the SeekBar
        volumeSlider.max = 100
        volumeSlider.progress = 100

        volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val volume = progress / 100.0f
                // No way to change volume of Ringtone
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
