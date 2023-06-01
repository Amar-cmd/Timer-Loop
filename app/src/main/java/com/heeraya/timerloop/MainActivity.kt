package com.heeraya.timerloop

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

    private var loopCount = 0
    private var initialTimeInMilliseconds: Long = 0
    private var timeLeftInMilliseconds: Long = 0

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
        loopPicker.maxValue = 10
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
        window.decorView.systemUiVisibility = if (isDarkTheme()) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }


//    private fun startTimer() {
//        val hoursToMilliseconds = hourPicker.value * 60 * 60 * 1000
//        val minutesToMilliseconds = minutePicker.value * 60 * 1000
//        val secondsToMilliseconds = secondPicker.value * 1000
//        initialTimeInMilliseconds = hoursToMilliseconds + minutesToMilliseconds + secondsToMilliseconds.toLong()
//        timeLeftInMilliseconds = initialTimeInMilliseconds
//        loopCount = loopPicker.value - 1  // Decrease loopCount by 1 before the first run
//
//        // Hide number pickers and show progress bar
//        hourPicker.visibility = View.GONE
//        minutePicker.visibility = View.GONE
//        secondPicker.visibility = View.GONE
//        loopPicker.visibility = View.GONE
//        progressBar.visibility = View.VISIBLE
//        progressBar.max = (initialTimeInMilliseconds / 1000).toInt()
//
//        // Reset the handler and runnable if they have been initialized previously
//        if (this::handler.isInitialized && this::runnable.isInitialized) {
//            handler.removeCallbacks(runnable)
//        }
//
//        // Initialize the handler
//        handler = Handler(Looper.getMainLooper())
//
//        startCountDownTimer()
//    }
//
//    private fun startCountDownTimer() {
//        runnable = Runnable {
//            if (timeLeftInMilliseconds > 0) {
//                updateTimerText(timeLeftInMilliseconds)
//                progressBar.progress = (timeLeftInMilliseconds / 1000).toInt()
//                timeLeftInMilliseconds -= 1000
//                handler.postDelayed(runnable, 1000)
//            } else if (loopCount-- > 0) {
//                timeLeftInMilliseconds = (hourPicker.value * 60 * 60 * 1000 + minutePicker.value * 60 * 1000 + secondPicker.value).toLong() * 1000
//                startCountDownTimer()
//            } else {
//                timerTextView.text = "Finished!"
//                // Hide progress bar and show number pickers
//                progressBar.visibility = View.GONE
//                hourPicker.visibility = View.VISIBLE
//                minutePicker.visibility = View.VISIBLE
//                secondPicker.visibility = View.VISIBLE
//                loopPicker.visibility = View.VISIBLE
//            }
//        }
//        handler.postDelayed(runnable, 0)
//    }


    private fun startTimer() {
        val hoursToMilliseconds = hourPicker.value * 60 * 60 * 1000
        val minutesToMilliseconds = minutePicker.value * 60 * 1000
        val secondsToMilliseconds = secondPicker.value * 1000
        initialTimeInMilliseconds = hoursToMilliseconds + minutesToMilliseconds + secondsToMilliseconds.toLong()
        timeLeftInMilliseconds = initialTimeInMilliseconds
        loopCount = loopPicker.value - 1  // Decrease loopCount by 1 before the first run

        // Hide number pickers and show progress bar
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

        progressBar.max = initialTimeInMilliseconds.toInt()  // The max progress corresponds to the total time

        // Reset the handler and runnable if they have been initialized previously
        if (this::handler.isInitialized && this::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }

        // Initialize the handler
        handler = Handler(Looper.getMainLooper())

        startCountDownTimer()
    }

    private fun startCountDownTimer() {
        val updateInterval: Long = 50  // We update every 100 milliseconds

        runnable = Runnable {
            if (timeLeftInMilliseconds > 0) {
                updateTimerText(timeLeftInMilliseconds)
                progressBar.progress = timeLeftInMilliseconds.toInt()  // The current progress corresponds to the time left
                timeLeftInMilliseconds -= updateInterval
                handler.postDelayed(runnable, updateInterval)  // Update every 100 milliseconds
            } else if (loopCount-- > 0) {
                timeLeftInMilliseconds = initialTimeInMilliseconds
                startCountDownTimer()
            } else {
                timerTextView.text = "Finished!"
                // Hide progress bar and show number pickers
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

            }
        }
        handler.postDelayed(runnable, 0)
    }

    private fun cancelTimer() {
        stopTimer()
//        resetTimer()
        updateTimerText(0)
        // Hide progress bar and Cancel, Pause buttons
        progressBar.visibility = View.GONE
        cancelButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
        hourPicker.visibility = View.VISIBLE
        minutePicker.visibility = View.VISIBLE
        secondPicker.visibility = View.VISIBLE
        loopPicker.visibility = View.VISIBLE
        // Show start button
        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.VISIBLE
        resetButton.visibility = View.VISIBLE
    }

    private fun pauseOrResumeTimer() {
        if (pauseButton.text == "Pause") {
            // Pause the timer
            handler.removeCallbacks(runnable)
            pauseButton.text = "Resume"
        } else {
            // Resume the timer
            handler.postDelayed(runnable, 0)
            pauseButton.text = "Pause"
        }
    }

    private fun stopTimer() {
        if (this::handler.isInitialized && this::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }
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
        // Hide Cancel and Pause buttons
        cancelButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
        // Show start button
        startButton.visibility = View.VISIBLE
        startButton.isEnabled = false
    }

//    private fun updateTimerText(timeLeftInMilliseconds: Long) {
//        val hours = TimeUnit.MILLISECONDS.toHours(timeLeftInMilliseconds).toInt() % 24
//        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftInMilliseconds).toInt() % 60
//        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftInMilliseconds).toInt() % 60
//
//        timerTextView.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
//    }

    private fun updateTimerText(timeLeftInMilliseconds: Long) {
        val hours = TimeUnit.MILLISECONDS.toHours(timeLeftInMilliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftInMilliseconds - TimeUnit.HOURS.toMillis(hours))
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftInMilliseconds - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(minutes))
        val milliseconds = TimeUnit.MILLISECONDS.toMillis(timeLeftInMilliseconds - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(minutes) - TimeUnit.SECONDS.toMillis(seconds))

        timerTextView.text = String.format(Locale.getDefault(), "%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds)
    }


    override fun onDestroy() {
        super.onDestroy()
        if (this::handler.isInitialized && this::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }
    }
}
