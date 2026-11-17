package tr.com.olgun.masasaati

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private var brightness = 0.60f
    private var textColor = Color.WHITE
    private var gestureStartY = 0f
    private var gestureStartBrightness = 0.60f
    private var leftEdgeGesture = false
    private var rightEdgeGesture = false

    private val paletteNames = arrayOf(
        "Beyaz", "Turkuaz", "Açık mavi", "Yeşil",
        "Sarı", "Turuncu", "Kırmızı", "Mor"
    )
    private val paletteColors = intArrayOf(
        Color.WHITE,
        Color.rgb(75, 224, 220),
        Color.rgb(91, 174, 255),
        Color.rgb(100, 230, 142),
        Color.rgb(255, 226, 112),
        Color.rgb(255, 166, 89),
        Color.rgb(255, 104, 115),
        Color.rgb(198, 143, 255)
    )

    private val clockUpdater = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val preferences = getSharedPreferences("clock_settings", MODE_PRIVATE)
        brightness = preferences.getFloat("brightness", 0.60f).coerceIn(0.01f, 1f)
        textColor = preferences.getInt("text_color", Color.WHITE)

        clockText = TextView(this).apply {
            setTextColor(textColor)
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = -0.025f
        }
        dateText = TextView(this).apply {
            gravity = Gravity.CENTER
            includeFontPadding = false
            letterSpacing = 0.04f
        }

        val centerGroup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(clockText, LinearLayout.LayoutParams(-1, -2))
            addView(dateText, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(8)
            })
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(centerGroup, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
        }

        val helpText = TextView(this).apply {
            text = "Sol kenar: parlaklık   •   Sağ kenar: yazı rengi"
            setTextColor(Color.rgb(82, 82, 82))
            textSize = 11f
            gravity = Gravity.CENTER
        }
        root.addView(helpText, FrameLayout.LayoutParams(-1, dp(30), Gravity.BOTTOM))

        root.setOnTouchListener { view, event -> handleEdgeGesture(view, event) }
        centerGroup.setOnLongClickListener {
            showColorPicker()
            true
        }

        root.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            val widthDp = (right - left) / resources.displayMetrics.density
            clockText.textSize = (widthDp * 0.22f).coerceIn(72f, 190f)
            dateText.textSize = 18f
        }

        setContentView(root)
        applyBrightness()
        applyTextColor()
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

    private fun handleEdgeGesture(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val edgeWidth = dp(72)
                leftEdgeGesture = event.x <= edgeWidth
                rightEdgeGesture = event.x >= view.width - edgeWidth
                gestureStartY = event.rawY
                gestureStartBrightness = brightness
                return leftEdgeGesture || rightEdgeGesture
            }

            MotionEvent.ACTION_MOVE -> {
                if (leftEdgeGesture) {
                    val screenHeight = view.height.toFloat().coerceAtLeast(1f)
                    val verticalChange = (event.rawY - gestureStartY) / screenHeight
                    brightness = (gestureStartBrightness - verticalChange).coerceIn(0.01f, 1f)
                    applyBrightness()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (leftEdgeGesture) {
                    getSharedPreferences("clock_settings", MODE_PRIVATE).edit()
                        .putFloat("brightness", brightness).apply()
                    leftEdgeGesture = false
                    return true
                }
                if (rightEdgeGesture) {
                    val moved = kotlin.math.abs(event.rawY - gestureStartY)
                    rightEdgeGesture = false
                    if (moved < dp(24)) showColorPicker()
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                leftEdgeGesture = false
                rightEdgeGesture = false
                return true
            }
        }
        return leftEdgeGesture || rightEdgeGesture
    }

    private fun showColorPicker() {
        AlertDialog.Builder(this)
            .setTitle("Yazı rengi")
            .setSingleChoiceItems(paletteNames, paletteColors.indexOf(textColor)) { dialog, which ->
                textColor = paletteColors[which]
                applyTextColor()
                getSharedPreferences("clock_settings", MODE_PRIVATE).edit()
                    .putInt("text_color", textColor).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun applyBrightness() {
        val params = window.attributes
        params.screenBrightness = brightness
        window.attributes = params
    }

    private fun applyTextColor() {
        clockText.setTextColor(textColor)
        dateText.setTextColor((textColor and 0x00FFFFFF) or (0xB8 shl 24))
    }

    private fun updateClock() {
        val now = Date()
        clockText.text = SimpleDateFormat("HH:mm", Locale("tr", "TR")).format(now)
        dateText.text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("tr", "TR"))
            .format(now).replaceFirstChar { it.uppercase(Locale("tr", "TR")) }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
