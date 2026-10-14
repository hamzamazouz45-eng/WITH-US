package app.isharah.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import kotlin.math.abs

/**
 * النافذة العائمة: مربع صغير شبه شفاف فيه الأفاتار فقط، وما حوله شفاف تماماً.
 *  - إصبع واحد: سحب.  إصبعان: تكبير وتصغير.
 *  - لمسة واحدة: تظهر أزرار (−  +  —) لثلاث ثوانٍ.
 * تُرسم بنوع TYPE_ACCESSIBILITY_OVERLAY المتاح لخدمة إمكانية الوصول دون إذن "العرض فوق التطبيقات".
 */
class OverlayController(private val service: AccessibilityService) {

    private val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val density = service.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()
    private val screenW get() = service.resources.displayMetrics.widthPixels
    private val screenH get() = service.resources.displayMetrics.heightPixels

    private var root: FrameLayout? = null
    private var web: WebView? = null
    private var catcher: View? = null
    private var bar: LinearLayout? = null
    private var chip: TextView? = null
    private var params: WindowManager.LayoutParams? = null

    private var ready = false
    private val pending = ArrayList<String>()
    private var sizePx = 0
    private var restoreSizePx = 0
    private var collapsed = false

    private val hideBar = Runnable { bar?.visibility = View.GONE }

    fun show() {
        if (root != null) return
        sizePx = dp(170)

        val p = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            // LEFT وليس START، حتى لا ينقلب الموضع في الواجهات العربية
            gravity = Gravity.TOP or Gravity.LEFT
            x = dp(12)
            y = dp(140)
        }
        params = p

        val r = FrameLayout(service).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = GradientDrawable().apply {
                setColor(0x73101A1C)
                cornerRadius = dp(14).toFloat()
            }
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

        val c = View(service)
        catcher = c
        c.setOnTouchListener(Touch(allowScale = true) { showBarTemporarily() })

        val minus = barButton("−") { setSize((sizePx * 0.8f).toInt()); showBarTemporarily() }
        val plus = barButton("+") { setSize((sizePx * 1.25f).toInt()); showBarTemporarily() }
        val fold = barButton("—") { collapse() }
        val b = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            background = GradientDrawable().apply {
                setColor(0xCC000000.toInt())
                cornerRadius = dp(12).toFloat()
            }
            addView(minus)
            addView(plus)
            addView(fold)
        }
        bar = b

        val ch = TextView(service).apply {
            text = "🤟"
            textSize = 22f
            gravity = Gravity.CENTER
            visibility = View.GONE
            setOnTouchListener(Touch(allowScale = false) { expand() })
        }
        chip = ch

        val match = FrameLayout.LayoutParams.MATCH_PARENT
        r.addView(w, FrameLayout.LayoutParams(match, match))
        r.addView(c, FrameLayout.LayoutParams(match, match))
        r.addView(
            b,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
                .apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; topMargin = dp(4) }
        )
        r.addView(ch, FrameLayout.LayoutParams(match, match))
        root = r

        try {
            wm.addView(r, p)
        } catch (e: Exception) {
            Log.e("WithUs", "addView failed", e)
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
        handler.removeCallbacks(hideBar)
        try {
            root?.let { wm.removeView(it) }
        } catch (_: Exception) {
        }
        web?.destroy()
        root = null; web = null; catcher = null; bar = null; chip = null
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

    private fun setSize(px: Int) {
        val p = params ?: return
        val max = minOf(dp(380), (screenW * 0.95f).toInt())
        sizePx = px.coerceIn(dp(90), max)
        p.width = sizePx
        p.height = sizePx
        p.x = clampX(p.x)
        p.y = clampY(p.y)
        update()
    }

    private fun collapse() {
        val p = params ?: return
        collapsed = true
        restoreSizePx = sizePx
        web?.visibility = View.GONE
        catcher?.visibility = View.GONE
        bar?.visibility = View.GONE
        chip?.visibility = View.VISIBLE
        p.width = dp(44)
        p.height = dp(44)
        update()
    }

    private fun expand() {
        collapsed = false
        web?.visibility = View.VISIBLE
        catcher?.visibility = View.VISIBLE
        chip?.visibility = View.GONE
        setSize(restoreSizePx)
    }

    private fun showBarTemporarily() {
        if (collapsed) return
        bar?.visibility = View.VISIBLE
        handler.removeCallbacks(hideBar)
        handler.postDelayed(hideBar, 3000)
    }

    private fun clampX(x: Int) = x.coerceIn(0, (screenW - (params?.width ?: 0)).coerceAtLeast(0))
    private fun clampY(y: Int) = y.coerceIn(0, (screenH - (params?.height ?: 0)).coerceAtLeast(0))

    private fun barButton(label: String, onClick: () -> Unit) = TextView(service).apply {
        text = label
        textSize = 20f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        minWidth = dp(40)
        minHeight = dp(36)
        setOnClickListener { onClick() }
    }

    /** إصبع واحد يسحب، إصبعان يكبّران ويصغّران، ولمسة سريعة تستدعي onTap. */
    private inner class Touch(
        private val allowScale: Boolean,
        private val onTap: () -> Unit
    ) : View.OnTouchListener {
        private val slop = ViewConfiguration.get(service).scaledTouchSlop
        private var startX = 0
        private var startY = 0
        private var downX = 0f
        private var downY = 0f
        private var moved = false
        private var multi = false

        private val scaler = ScaleGestureDetector(
            service,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    setSize((sizePx * detector.scaleFactor).toInt())
                    return true
                }
            }
        )

        override fun onTouch(v: View, ev: MotionEvent): Boolean {
            val p = params ?: return false
            if (allowScale) scaler.onTouchEvent(ev)
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = p.x; startY = p.y
                    downX = ev.rawX; downY = ev.rawY
                    moved = false; multi = false
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    multi = true; moved = true
                }
                MotionEvent.ACTION_MOVE -> if (!multi) {
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY
                    if (!moved && (abs(dx) > slop || abs(dy) > slop)) moved = true
                    if (moved) {
                        p.x = clampX(startX + dx.toInt())
                        p.y = clampY(startY + dy.toInt())
                        update()
                    }
                }
                MotionEvent.ACTION_UP -> if (!moved && !multi) onTap()
            }
            return true
        }
    }
}
