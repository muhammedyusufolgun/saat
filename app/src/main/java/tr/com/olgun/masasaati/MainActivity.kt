package tr.com.olgun.masasaati

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clockText: TextView
    private lateinit var secondsText: TextView
    private lateinit var dateText: TextView
    private lateinit var gestureDetector: GestureDetector
    private val brightnessLevels = floatArrayOf(0.25f, 0.60f, 1.0f)
    private var brightnessIndex = 1
    private var showSeconds = true
    private val clockUpdater = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.rgb(8, 11, 18)
        window.navigationBarColor = Color.rgb(8, 11, 18)

        val preferences = getSharedPreferences("clock_settings", MODE_PRIVATE)
        showSeconds = preferences.getBoolean("show_seconds", true)
        brightnessIndex = preferences.getInt("brightness", 1).coerceIn(0, brightnessLevels.lastIndex)

        clockText = TextView(this).apply {
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = -0.035f
        }
        secondsText = TextView(this).apply {
            setTextColor(Color.rgb(98, 213, 255))
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        dateText = TextView(this).apply {
            setTextColor(Color.rgb(181, 192, 210))
            gravity = Gravity.CENTER
            letterSpacing = 0.06f
        }
        val hintText = TextView(this).apply {
            setTextColor(Color.rgb(111, 125, 148))
            gravity = Gravity.CENTER
            text = "Dokun: parlaklık   •   Çift dokun: saniye"
            textSize = 12f
        }

        val timeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(clockText, LinearLayout.LayoutParams(-2, -2))
            addView(secondsText, LinearLayout.LayoutParams(-2, -2).apply {
                leftMargin = dp(8)
                topMargin = dp(28)
            })
        }

        val title = TextView(this).apply {
            text = "MASA SAATİ"
            setTextColor(Color.rgb(98, 213, 255))
            textSize = 13f
            letterSpacing = 0.24f
            gravity = Gravity.CENTER
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(20))
            addView(title, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(24) })
            addView(timeRow, LinearLayout.LayoutParams(-1, -2))
            addView(dateText, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(22)
                bottomMargin = dp(40)
            })
            addView(hintText, LinearLayout.LayoutParams(-1, -2))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(8, 11, 18))
            addView(content, LinearLayout.LayoutParams(-1, -2))
        }
        setContentView(root)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                brightnessIndex = (brightnessIndex + 1) % brightnessLevels.size
                applyBrightness()
                getSharedPreferences("clock_settings", MODE_PRIVATE).edit()
                    .putInt("brightness", brightnessIndex).apply()
                return true
            }

            override fun onDoubleTap(event: MotionEvent): Boolean {
                showSeconds = !showSeconds
                getSharedPreferences("clock_settings", MODE_PRIVATE).edit()
                    .putBoolean("show_seconds", showSeconds).apply()
                updateClock()
                return true
            }
        })

        root.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            val widthDp = (right - left) / resources.displayMetrics.density
            val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val mainSize = (widthDp * if (landscape) 0.19f else 0.205f).coerceIn(58f, 142f)
            clockText.textSize = mainSize
            secondsText.textSize = (mainSize * 0.34f).coerceAtLeast(22f)
            dateText.textSize = (mainSize * 0.19f).coerceIn(15f, 23f)
        }

        applyBrightness()
        updateClock()
    }

    override fun onResume() {
        super.onResume()
        enterImmersiveMode()
        handler.removeCallbacks(clockUpdater)
        handler.post(clockUpdater)
    }

    override fun onPause() {
        handler.removeCallbacks(clockUpdater)
        super.onPause()
    }

    @Suppress("DEPRECATION")
    private fun enterImmersiveMode() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    private fun applyBrightness() {
        val params = window.attributes
        params.screenBrightness = brightnessLevels[brightnessIndex]
        window.attributes = params
    }

    private fun updateClock() {
        val now = Date()
        val is24Hour = DateFormat.is24HourFormat(this)
        clockText.text = SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm", Locale.getDefault())
            .format(now)
        val secondsPattern = if (is24Hour) "ss" else "ss a"
        secondsText.text = SimpleDateFormat(secondsPattern, Locale.getDefault())
            .format(now).uppercase(Locale.getDefault())
        secondsText.visibility = if (showSeconds) View.VISIBLE else View.GONE
        dateText.text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("tr", "TR"))
            .format(now).uppercase(Locale("tr", "TR"))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return if (::gestureDetector.isInitialized && gestureDetector.onTouchEvent(event)) {
            true
        } else {
            super.dispatchTouchEvent(event)
        }
    }
}
