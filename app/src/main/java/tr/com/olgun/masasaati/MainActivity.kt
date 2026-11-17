package tr.com.olgun.masasaati

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: FrameLayout
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var brightnessPanel: LinearLayout
    private lateinit var colorPanel: LinearLayout
    private lateinit var leftEdge: FrameLayout
    private lateinit var rightEdge: FrameLayout
    private lateinit var leftMark: TextView
    private lateinit var rightMark: TextView
    private lateinit var brightnessValue: TextView
    private val paletteSwatches = mutableListOf<View>()
    private var brightness = 0.60f
    private var textColor = Color.WHITE

    private val palette = intArrayOf(
        0xFFFFFFFF.toInt(), 0xFFF5F0E6.toInt(), 0xFFCFD8DC.toInt(), 0xFF8D99AE.toInt(),
        0xFFFF0000.toInt(), 0xFFB71C1C.toInt(), 0xFFFF6F61.toInt(), 0xFFFF5722.toInt(),
        0xFFFF9800.toInt(), 0xFFFFFF00.toInt(), 0xFFC6FF00.toInt(), 0xFF00FF00.toInt(),
        0xFF8BC34A.toInt(), 0xFF009688.toInt(), 0xFF00BFA5.toInt(), 0xFF00FFFF.toInt(),
        0xFF80DEEA.toInt(), 0xFF81D4FA.toInt(), 0xFF40C4FF.toInt(), 0xFF0000FF.toInt(),
        0xFF0D47A1.toInt(), 0xFF3F51B5.toInt(), 0xFF673AB7.toInt(), 0xFF800080.toInt(),
        0xFFFF00FF.toInt(), 0xFFE040FB.toInt(), 0xFFFF4081.toInt(), 0xFFE91E63.toInt(),
        0xFF795548.toInt(), 0xFFD7CCC8.toInt(), 0xFFFFD700.toInt(), 0xFFCDDC39.toInt(),
        0xFF00E676.toInt(), 0xFFD1C4E9.toInt(), 0xFF607D8B.toInt()
    )

    private val clockUpdater = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000L)
        }
    }

    private val hideTouchUi = Runnable {
        if (::brightnessPanel.isInitialized) brightnessPanel.visibility = View.GONE
        if (::colorPanel.isInitialized) colorPanel.visibility = View.GONE
        if (::leftMark.isInitialized) leftMark.visibility = View.INVISIBLE
        if (::rightMark.isInitialized) rightMark.visibility = View.INVISIBLE
        if (::leftEdge.isInitialized) leftEdge.background = null
        if (::rightEdge.isInitialized) rightEdge.background = null
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
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = -0.025f
        }
        dateText = TextView(this).apply {
            gravity = Gravity.CENTER
            includeFontPadding = false
            letterSpacing = 0.02f
        }

        val centerGroup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(clockText, LinearLayout.LayoutParams(-1, -2))
            addView(dateText, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        }

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true
            addView(centerGroup, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
        }

        brightnessPanel = createBrightnessPanel()
        colorPanel = createColorPanel()
        root.addView(brightnessPanel, FrameLayout.LayoutParams(dp(260), -2, Gravity.START or Gravity.CENTER_VERTICAL).apply {
            leftMargin = dp(54)
        })
        root.addView(colorPanel, FrameLayout.LayoutParams(dp(320), -2, Gravity.END or Gravity.CENTER_VERTICAL).apply {
            rightMargin = dp(54)
        })
        brightnessPanel.visibility = View.GONE
        colorPanel.visibility = View.GONE

        leftMark = edgeMark("☼")
        rightMark = edgeMark("●")
        leftEdge = edgeArea(leftMark, Gravity.START)
        rightEdge = edgeArea(rightMark, Gravity.END)
        root.addView(leftEdge, FrameLayout.LayoutParams(dp(48), -1, Gravity.START))
        root.addView(rightEdge, FrameLayout.LayoutParams(dp(48), -1, Gravity.END))
        leftEdge.setOnClickListener { showBrightnessPanel() }
        rightEdge.setOnClickListener { showColorPanel() }
        root.setOnClickListener { revealEdgeAreas() }

        root.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            val widthDp = (right - left) / resources.displayMetrics.density
            clockText.textSize = (widthDp * 0.165f).coerceIn(64f, 160f)
            dateText.textSize = 16f
        }

        setContentView(root)
        applyBrightness()
        applyTextColor()
        updateClock()
    }

    private fun createBrightnessPanel(): LinearLayout {
        val panel = panelContainer()
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(panelTitle("PARLAKLIK"), LinearLayout.LayoutParams(0, -2, 1f))
        brightnessValue = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            text = (brightness * 100).toInt().toString() + "%"
        }
        heading.addView(brightnessValue, LinearLayout.LayoutParams(-2, -2))

        val seekBar = SeekBar(this).apply {
            max = 100
            progress = (brightness * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    brightness = progress.coerceAtLeast(1) / 100f
                    brightnessValue.text = (brightness * 100).toInt().toString() + "%"
                    applyBrightness()
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) {
                    getSharedPreferences("clock_settings", MODE_PRIVATE).edit()
                        .putFloat("brightness", brightness).apply()
                }
            })
        }
        panel.addView(heading)
        panel.addView(seekBar, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
            bottomMargin = dp(4)
        })
        panel.addView(closeButton { scheduleHide() })
        return panel
    }

    private fun createColorPanel(): LinearLayout {
        val panel = panelContainer()
        panel.addView(panelTitle("YAZI RENGİ • 35 RENK"))
        val grid = GridLayout(this).apply {
            columnCount = 7
            rowCount = 5
            useDefaultMargins = false
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        paletteSwatches.clear()
        for (color in palette) {
            val swatch = View(this).apply {
                contentDescription = "Yazı rengi"
                background = swatchBackground(color, color == textColor)
                setOnClickListener {
                    textColor = color
                    getSharedPreferences("clock_settings", MODE_PRIVATE).edit()
                        .putInt("text_color", textColor).apply()
                    applyTextColor()
                    refreshColorPanel()
                }
            }
            paletteSwatches.add(swatch)
            val params = GridLayout.LayoutParams().apply {
                width = dp(38)
                height = dp(38)
                setMargins(dp(1), dp(1), dp(1), dp(1))
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED)
            }
            grid.addView(swatch, params)
        }

        panel.addView(grid, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(7)
            bottomMargin = dp(3)
        })
        panel.addView(closeButton { scheduleHide() })
        val signature = TextView(this).apply {
            text = "Muhammed Yusuf OLGUN"
            setTextColor(Color.rgb(174, 184, 199))
            textSize = 11f
            gravity = Gravity.CENTER
            letterSpacing = 0.03f
            setPadding(0, dp(5), 0, 0)
        }
        panel.addView(signature, LinearLayout.LayoutParams(-1, -2))
        return panel
    }

    private fun refreshColorPanel() {
        paletteSwatches.forEachIndexed { index, swatch ->
            swatch.background = swatchBackground(palette[index], palette[index] == textColor)
        }
    }

    private fun panelContainer() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(10), dp(14), dp(8))
        background = roundedBackground(Color.argb(238, 19, 25, 37), dp(16))
        elevation = dp(8).toFloat()
    }

    private fun panelTitle(value: String) = TextView(this).apply {
        text = value
        setTextColor(Color.rgb(196, 207, 225))
        textSize = 12f
        letterSpacing = 0.1f
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun closeButton(action: () -> Unit) = TextView(this).apply {
        text = "Kapat"
        setTextColor(Color.rgb(145, 162, 188))
        textSize = 12f
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
        setPadding(0, dp(5), 0, dp(1))
        setOnClickListener { action() }
    }

    private fun edgeMark(label: String) = TextView(this).apply {
        text = label
        textSize = 18f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        visibility = View.INVISIBLE
    }

    private fun edgeArea(mark: TextView, gravity: Int) = FrameLayout(this).apply {
        contentDescription = if (gravity == Gravity.START) "Parlaklık ayarını aç" else "Renk panelini aç"
        isClickable = true
        addView(mark, FrameLayout.LayoutParams(-1, dp(62), gravity))
    }

    private fun showBrightnessPanel() {
        revealEdgeAreas()
        colorPanel.visibility = View.GONE
        brightnessPanel.visibility = View.VISIBLE
    }

    private fun showColorPanel() {
        revealEdgeAreas()
        brightnessPanel.visibility = View.GONE
        colorPanel.visibility = View.VISIBLE
    }

    private fun revealEdgeAreas() {
        handler.removeCallbacks(hideTouchUi)
        leftMark.visibility = View.VISIBLE
        rightMark.visibility = View.VISIBLE
        leftEdge.background = roundedBackground(Color.argb(34, 255, 255, 255), dp(18))
        rightEdge.background = roundedBackground(Color.argb(34, 255, 255, 255), dp(18))
    }

    private fun scheduleHide() {
        handler.removeCallbacks(hideTouchUi)
        handler.postDelayed(hideTouchUi, 1000L)
    }

    private fun swatchBackground(color: Int, selected: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(dp(if (selected) 3 else 1), if (selected) Color.WHITE else Color.argb(110, 255, 255, 255))
    }

    private fun roundedBackground(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacks(hideTouchUi)
                if (::root.isInitialized) revealEdgeAreas()
            }
            MotionEvent.ACTION_MOVE -> handler.removeCallbacks(hideTouchUi)
        }
        val handled = super.dispatchTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> scheduleHide()
        }
        return handled
    }

    override fun onResume() {
        super.onResume()
        enterImmersiveMode()
        handler.removeCallbacks(clockUpdater)
        handler.post(clockUpdater)
    }

    override fun onPause() {
        handler.removeCallbacks(clockUpdater)
        handler.removeCallbacks(hideTouchUi)
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
        params.screenBrightness = brightness.coerceIn(0.01f, 1f)
        window.attributes = params
    }

    private fun applyTextColor() {
        clockText.setTextColor(textColor)
        dateText.setTextColor((textColor and 0x00FFFFFF) or (0xB8 shl 24))
    }

    private fun updateClock() {
        val now = Date()
        clockText.text = SimpleDateFormat("HH:mm:ss", Locale("tr", "TR")).format(now)
        dateText.text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("tr", "TR"))
            .format(now).replaceFirstChar { it.uppercase(Locale("tr", "TR")) }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
