package app.isharah.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject

/**
 * النافذة العائمة. تُرسم بنوع TYPE_ACCESSIBILITY_OVERLAY، وهو متاح لخدمة إمكانية الوصول
 * دون إذن "العرض فوق التطبيقات". الأفاتار نفسه صفحة HTML محلية داخل WebView.
 */
class OverlayController(private val service: AccessibilityService) {

    private val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = service.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    private var root: LinearLayout? = null
    private var web: WebView? = null
    private var frame: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null

    private var ready = false
    private val pending = ArrayList<String>()
    private var widthDp = 200
    private var collapsed = false

    fun show() {
        if (root != null) return

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(140)
        }
        params = p

        val r = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xE60F1A1C.toInt())
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(6), dp(4), dp(6), dp(6))
        }

        val grip = chip("⠿ " + service.getString(R.string.app_name), 13f).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
        }
        val sizeBtn = chip("⤢", 18f)
        val minBtn = chip("—", 18f)
        val bar = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(grip)
            addView(sizeBtn)
            addView(minBtn)
        }

        val w = WebView(service).apply {
            settings.javaScriptEnabled = true
            setBackgroundColor(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    ready = true
                    pending.forEach { view?.evaluateJavascript(it, null) }
                    pending.clear()
                }
            }
            loadUrl("file:///android_asset/overlay/index.html")
        }
        web = w

        val catcher = View(service)
        val f = FrameLayout(service).apply {
            addView(w, FrameLayout.LayoutParams(dp(widthDp), dp(widthDp * 5 / 4)))
            addView(catcher, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        frame = f

        val drag = dragListener()
        grip.setOnTouchListener(drag)
        catcher.setOnTouchListener(drag)

        sizeBtn.setOnClickListener {
            widthDp = when (widthDp) { 200 -> 260; 260 -> 140; else -> 200 }
            w.layoutParams = FrameLayout.LayoutParams(dp(widthDp), dp(widthDp * 5 / 4))
            update()
        }
        minBtn.setOnClickListener {
            collapsed = !collapsed
            f.visibility = if (collapsed) View.GONE else View.VISIBLE
            minBtn.text = if (collapsed) "+" else "—"
            update()
        }

        r.addView(bar)
        r.addView(f)
        root = r

        try {
            wm.addView(r, p)
        } catch (e: Exception) {
            Log.e("Isharah", "addView failed", e)
            root = null
        }
    }

    fun toggle() {
        val r = root ?: return
        r.visibility = if (r.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    fun sendCaption(text: String) = send("window.onCaption(${JSONObject.quote(text)})")

    fun sendHint(text: String) = send("window.onHint(${JSONObject.quote(text)})")

    fun hide() {
        try {
            root?.let { wm.removeView(it) }
        } catch (_: Exception) {
        }
        web?.destroy()
        root = null
        web = null
        frame = null
        ready = false
        pending.clear()
    }

    private fun send(js: String) {
        val w = web ?: return
        if (ready) w.evaluateJavascript(js, null) else pending.add(js)
    }

    private fun update() {
        val r = root ?: return
        val p = params ?: return
        try {
            wm.updateViewLayout(r, p)
        } catch (_: Exception) {
        }
    }

    private fun chip(label: String, size: Float) = TextView(service).apply {
        text = label
        textSize = size
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        minWidth = dp(40)
        minHeight = dp(36)
        setPadding(dp(6), 0, dp(6), 0)
    }

    private fun dragListener(): View.OnTouchListener {
        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f
        return View.OnTouchListener { _, ev ->
            val p = params ?: return@OnTouchListener false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = p.x; startY = p.y
                    downX = ev.rawX; downY = ev.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = (startX + (ev.rawX - downX)).toInt().coerceAtLeast(0)
                    p.y = (startY + (ev.rawY - downY)).toInt().coerceAtLeast(0)
                    update()
                }
            }
            true
        }
    }
}
