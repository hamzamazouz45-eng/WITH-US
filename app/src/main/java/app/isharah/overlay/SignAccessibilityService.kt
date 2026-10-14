package app.isharah.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

class SignAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: OverlayController? = null
    private var scanPending = false
    private var lastText = ""
    private var lastCaptionAt = 0L
    private var hintShown = false

    private val scan = Runnable {
        scanPending = false
        val res = try {
            CaptionReader.read(this)
        } catch (e: Exception) {
            return@Runnable
        }
        Diagnostics.lastDump = res.dump
        Diagnostics.lastSource = res.source

        val text = res.text?.replace(Regex("\\s+"), " ")?.trim()
        val now = SystemClock.elapsedRealtime()
        if (!text.isNullOrEmpty()) {
            lastCaptionAt = now
            hintShown = false
            if (text != lastText) {
                val fresh = newPart(lastText, text)
                lastText = text
                Diagnostics.lastCaption = text
                if (fresh.isNotBlank()) overlay?.sendCaption(fresh)
            }
        } else if (res.hasVideo && !hintShown && now - lastCaptionAt > 6000) {
            hintShown = true
            overlay?.sendHint(getString(R.string.hint_no_captions))
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlay = OverlayController(this).also { it.show() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName == packageName) return
        if (!scanPending) {
            scanPending = true
            handler.postDelayed(scan, 250)
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacks(scan)
        overlay?.hide()
        overlay = null
        instance = null
        return super.onUnbind(intent)
    }

    fun toggleOverlay() = overlay?.toggle()

    /** الترجمة تتدحرج في بعض التطبيقات: نرسل الكلمات الجديدة فقط. */
    private fun newPart(prev: String, cur: String): String {
        val a = prev.split(' ').filter { it.isNotEmpty() }
        val b = cur.split(' ').filter { it.isNotEmpty() }
        var k = minOf(a.size, b.size)
        while (k > 0) {
            if (a.takeLast(k) == b.take(k)) return b.drop(k).joinToString(" ")
            k--
        }
        return cur
    }

    companion object {
        @Volatile
        var instance: SignAccessibilityService? = null
    }
}
