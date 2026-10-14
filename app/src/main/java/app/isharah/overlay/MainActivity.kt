package app.isharah.overlay

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var diag: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LOCALE

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }
        col.addView(label(getString(R.string.app_name), 30f, bold = true))
        status = label("", 17f, bold = true)
        col.addView(status)
        col.addView(label(getString(R.string.steps), 15f))
        col.addView(button(R.string.btn_access) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        col.addView(button(R.string.btn_appinfo) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        })
        col.addView(button(R.string.btn_toggle) {
            val s = SignAccessibilityService.instance
            if (s == null) toast(R.string.service_off) else s.toggleOverlay()
        })
        col.addView(label(getString(R.string.privacy), 13f))
        col.addView(label(getString(R.string.diag_title), 20f, bold = true))
        col.addView(label(getString(R.string.diag_hint), 13f))
        col.addView(button(R.string.btn_diag) { refresh() })
        col.addView(button(R.string.btn_copy) { copy() })
        diag = label("", 11f).apply {
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textDirection = View.TEXT_DIRECTION_LTR
        }
        col.addView(diag)

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun serviceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { it.startsWith("$packageName/") }
    }

    private fun diagText(): String {
        if (Diagnostics.lastDump.isEmpty()) return getString(R.string.diag_empty)
        return "source: ${Diagnostics.lastSource}\ncaption: ${Diagnostics.lastCaption}\n\n${Diagnostics.lastDump}"
    }

    private fun refresh() {
        status.setText(if (serviceEnabled()) R.string.status_on else R.string.status_off)
        diag.text = diagText()
    }

    private fun copy() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("diagnostics", diagText()))
        toast(R.string.copied)
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_SHORT).show()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(s: String, size: Float, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = size
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun button(res: Int, onClick: () -> Unit) = Button(this).apply {
        setText(res)
        setOnClickListener { onClick() }
    }
}
