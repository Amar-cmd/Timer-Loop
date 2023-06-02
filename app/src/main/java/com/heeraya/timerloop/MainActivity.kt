package com.heeraya.timerloop

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var timerTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button
    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var secondPicker: NumberPicker
    private lateinit var loopPicker: NumberPicker
    private lateinit var progressBar: ProgressBar
    private lateinit var cancelButton: Button
    private lateinit var pauseButton: Button
    private lateinit var selectedRingtoneUri: Uri

    private var loopCount = 0
    private var initialTimeInMilliseconds: Long = 0
    private var timeLeftInMilliseconds: Long = 0
    private var currentRingtone: Ringtone? = null
    private var beepRingtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timerTextView = findViewById(R.id.timer_text)
        startButton = findViewById(R.id.start_button)
        stopButton = findViewById(R.id.stop_button)
        resetButton = findViewById(R.id.reset_button)
        hourPicker = findViewById(R.id.hour_picker)
        minutePicker = findViewById(R.id.minute_picker)
        secondPicker = findViewById(R.id.second_picker)
        loopPicker = findViewById(R.id.loop_picker)
        progressBar = findViewById(R.id.progress_bar)
        cancelButton = findViewById(R.id.cancel_button)
        pauseButton = findViewById(R.id.pause_button)

        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"))

        val soundIcon: ImageButton = findViewById(R.id.sound_icon)
        soundIcon.setOnClickListener {
            val intent = Intent(this, RingtoneListActivity::class.java)
            startActivity(intent)
        }

        startButton.setOnClickListener { startTimer() }
        stopButton.setOnClickListener { stopTimer() }
        resetButton.setOnClickListener { resetTimer() }
        cancelButton.setOnClickListener { cancelTimer() }
        pauseButton.setOnClickListener { pauseOrResumeTimer() }

        startButton.isEnabled = false // Start disabled

        val pickerListener = NumberPicker.OnValueChangeListener { _, _, _ ->
            checkStartButton()
        }

        hourPicker.setOnValueChangedListener(pickerListener)
        minutePicker.setOnValueChangedListener(pickerListener)
        secondPicker.setOnValueChangedListener(pickerListener)

        hourPicker.maxValue = 23
        minutePicker.maxValue = 59
        secondPicker.maxValue = 59
        loopPicker.minValue = 1
        loopPicker.maxValue = 99

        val sharedPreferences = getSharedPreferences("ringtone_prefs", Context.MODE_PRIVATE)
        val selectedRingtoneUriString = sharedPreferences.getString("selected_ringtone_uri", null)

        val beepRingtoneUriString = sharedPreferences.getString("beep_ringtone_uri", null)
        beepRingtone = if (beepRingtoneUriString != null) {
            RingtoneManager.getRingtone(this, Uri.parse(beepRingtoneUriString))
        } else {
            null
        }

        selectedRingtoneUri = if (selectedRingtoneUriString != null) {
            Uri.parse(selectedRingtoneUriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }
    }

    private fun checkStartButton() {
        val totalSeconds = hourPicker.value * 3600 + minutePicker.value * 60 + secondPicker.value
        startButton.isEnabled = totalSeconds > 0
    }

    fun isDarkTheme(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onResume() {
        super.onResume()
//        window.decorView.systemUiVisibility =
//            if (isDarkTheme()) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        // Fetch the selected ringtone URI each time the activity resumes
        val sharedPreferences = getSharedPreferences("ringtone_prefs", Context.MODE_PRIVATE)
        val selectedRingtoneUriString = sharedPreferences.getString("selected_ringtone_uri", null)

        val beepRingtoneUriString = sharedPreferences.getString("beep_ringtone_uri", null)
        beepRingtone = if (beepRingtoneUriString != null) {
            RingtoneManager.getRingtone(this, Uri.parse(beepRingtoneUriString))
        } else {
            null
        }

        selectedRingtoneUri = if (selectedRingtoneUriString != null) {
            Uri.parse(selectedRingtoneUriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }
    }


    private fun startTimer() {
        val hoursToMilliseconds = hourPicker.value * 60 * 60 * 1000
        val minutesToMilliseconds = minutePicker.value * 60 * 1000
        val secondsToMilliseconds = secondPicker.value * 1000
        initialTimeInMilliseconds =
            hoursToMilliseconds + minutesToMilliseconds + secondsToMilliseconds.toLong()
        timeLeftInMilliseconds = initialTimeInMilliseconds
        loopCount = loopPicker.value - 1  // Decrease loopCount by 1 before the first run

        hourPicker.visibility = View.GONE
        minutePicker.visibility = View.GONE
        secondPicker.visibility = View.GONE
        loopPicker.visibility = View.GONE
        startButton.visibility = View.GONE
        stopButton.visibility = View.GONE
        resetButton.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        cancelButton.visibility = View.VISIBLE
        pauseButton.visibility = View.VISIBLE
        pauseButton.text = "Pause"

        progressBar.max =
            initialTimeInMilliseconds.toInt()  // The max progress corresponds to the total time

        if (this::handler.isInitialized && this::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }

        handler = Handler(Looper.getMainLooper())

        startCountDownTimer()
    }

//    private fun startCountDownTimer() {
//        val updateInterval: Long = 100  // We update every 100 milliseconds
//        var ringtone = RingtoneManager.getRingtone(this, selectedRingtoneUri)
//
//        runnable = Runnable {
//            if (timeLeftInMilliseconds > 0) {
//                updateTimerText(timeLeftInMilliseconds)
//                progressBar.progress =
//                    timeLeftInMilliseconds.toInt()  // The current progress corresponds to the time left
//                timeLeftInMilliseconds -= updateInterval
//                handler.postDelayed(runnable, updateInterval)  // Update every 100 milliseconds
//                currentRingtone?.stop()
//            } else if (loopCount-- > 0) {
//                timeLeftInMilliseconds = initialTimeInMilliseconds
//                startCountDownTimer()
//            } else {
//                timerTextView.text = "Finished!"
//
//                progressBar.visibility = View.GONE
//                cancelButton.visibility = View.GONE
//                pauseButton.visibility = View.GONE
//                hourPicker.visibility = View.VISIBLE
//                minutePicker.visibility = View.VISIBLE
//                secondPicker.visibility = View.VISIBLE
//                loopPicker.visibility = View.VISIBLE
//                startButton.visibility = View.VISIBLE
//                stopButton.visibility = View.VISIBLE
//                resetButton.visibility = View.VISIBLE
//
//                currentRingtone = RingtoneManager.getRingtone(this, selectedRingtoneUri)
//                currentRingtone?.play()
//                stopButton.visibility = View.VISIBLE
//
////                ringtone.play()
//            }
//        }
//        handler.postDelayed(runnable, 0)
//    }

    private fun startCountDownTimer() {
        val updateInterval: Long = 100  // We update every 100 milliseconds
        var ringtone = RingtoneManager.getRingtone(this, selectedRingtoneUri)
        var beepRingtoneUri = getBeepRingtoneUri()
//        var beepRingtone: Ringtone? = null
        if (beepRingtoneUri != null) {
            beepRingtone = RingtoneManager.getRingtone(this, beepRingtoneUri)
        }

        runnable = Runnable {
            if (timeLeftInMilliseconds > 3000) {
                updateTimerText(timeLeftInMilliseconds)
                progressBar.progress = timeLeftInMilliseconds.toInt()  // The current progress corresponds to the time left
                timeLeftInMilliseconds -= updateInterval
                handler.postDelayed(runnable, updateInterval)  // Update every 100 milliseconds
                currentRingtone?.stop()
            } else if (timeLeftInMilliseconds > 0) {
                beepRingtone?.play()
                updateTimerText(timeLeftInMilliseconds)
                progressBar.progress = timeLeftInMilliseconds.toInt()  // The current progress corresponds to the time left
                timeLeftInMilliseconds -= updateInterval
                handler.postDelayed(runnable, updateInterval)  // Update every 100 milliseconds
            } else if (loopCount-- > 0) {
                beepRingtone?.stop()
                timeLeftInMilliseconds = initialTimeInMilliseconds
                startCountDownTimer()
            } else {
                beepRingtone?.stop()

                timerTextView.text = "Finished!"

                progressBar.visibility = View.GONE
                cancelButton.visibility = View.GONE
                pauseButton.visibility = View.GONE
                hourPicker.visibility = View.VISIBLE
                minutePicker.visibility = View.VISIBLE
                secondPicker.visibility = View.VISIBLE
                loopPicker.visibility = View.VISIBLE
                startButton.visibility = View.VISIBLE
                stopButton.visibility = View.VISIBLE
                resetButton.visibility = View.VISIBLE

                currentRingtone = RingtoneManager.getRingtone(this, selectedRingtoneUri)
                currentRingtone?.play()
                stopButton.visibility = View.VISIBLE
            }
        }
        handler.postDelayed(runnable, 0)
    }



    private fun cancelTimer() {
        handler.removeCallbacks(runnable) // This stops the timer
        stopTimer()
        beepRingtone?.stop() // This stops the beep sound
        updateTimerText(0)
        progressBar.visibility = View.GONE
        cancelButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
        hourPicker.visibility = View.VISIBLE
        minutePicker.visibility = View.VISIBLE
        secondPicker.visibility = View.VISIBLE
        loopPicker.visibility = View.VISIBLE
        startButton.visibility = View.VISIBLE
        resetButton.visibility = View.VISIBLE
    }


    private fun pauseOrResumeTimer() {
        if (pauseButton.text == "Pause") {
            handler.removeCallbacks(runnable)
            pauseButton.text = "Resume"
        } else {
            handler.postDelayed(runnable, 0)
            pauseButton.text = "Pause"
        }
    }

    private fun stopTimer() {
        currentRingtone?.stop()
        stopButton.visibility = View.GONE
    }


    private fun resetTimer() {
        stopTimer()
        hourPicker.value = 0
        minutePicker.value = 0
        secondPicker.value = 0
        loopPicker.value = 1
        timeLeftInMilliseconds = 0
        initialTimeInMilliseconds = 0
        updateTimerText(0)
        cancelButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
        startButton.visibility = View.VISIBLE
        startButton.isEnabled = false
    }

    private fun updateTimerText(timeLeftInMilliseconds: Long) {
        val hours = TimeUnit.MILLISECONDS.toHours(timeLeftInMilliseconds)
        val minutes =
            TimeUnit.MILLISECONDS.toMinutes(timeLeftInMilliseconds - TimeUnit.HOURS.toMillis(hours))
        val seconds = TimeUnit.MILLISECONDS.toSeconds(
            timeLeftInMilliseconds - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(
                minutes
            )
        )
        val milliseconds = TimeUnit.MILLISECONDS.toMillis(
            timeLeftInMilliseconds - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(
                minutes
            ) - TimeUnit.SECONDS.toMillis(seconds)
        )

        timerTextView.text = String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d.%03d",
            hours,
            minutes,
            seconds,
            milliseconds
        )
    }

    private fun getBeepRingtoneUri(): Uri? {
        val manager = RingtoneManager(this)
        manager.setType(RingtoneManager.TYPE_RINGTONE)
        val cursor: Cursor = manager.cursor
        var beepUri: Uri? = null
        while (cursor.moveToNext()) {
            val ringtoneTitle = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            if (ringtoneTitle == "Beep Once") {
                beepUri = manager.getRingtoneUri(cursor.position)
                break
            }
        }
        return beepUri
    }

    override fun onDestroy() {
        super.onDestroy()

        val ringtone = RingtoneManager.getRingtone(this, selectedRingtoneUri)
        if (ringtone.isPlaying) {
            ringtone.stop()
        }

        if (this::handler.isInitialized && this::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }
    }
}
