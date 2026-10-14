package app.isharah.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * يبحث في نصوص الشاشة عن الترجمة المكتوبة (CC).
 *
 * لا توجد طريقة موحّدة تعرضها بها التطبيقات، لذلك يجرّب ثلاث طرق بالترتيب:
 *  1) عنصر معرّفه يحتوي caption / subtitle
 *  2) نافذة الترجمة الحية في النظام (Live Caption)
 *  3) نص داخل مساحة الفيديو (النصف السفلي منها)
 * وقد لا تنجح أي طريقة في بعض التطبيقات التي ترسم الترجمة كصورة. شاشة التشخيص تبيّن ما رآه التطبيق.
 */
object CaptionReader {

    data class Result(val text: String?, val source: String, val dump: String, val hasVideo: Boolean)

    private class N(val id: String, val cls: String, val text: String, val r: Rect, val clickable: Boolean)

    private val timeRe = Regex("""^\d{1,2}:\d{2}(:\d{2})?(\s*[/•]\s*\d{1,2}:\d{2}(:\d{2})?)?$""")
    private val capIdRe = Regex("caption|subtitle", RegexOption.IGNORE_CASE)
    private val videoClsRe = Regex("SurfaceView|TextureView|VideoView|PlayerView")
    private val videoIdRe = Regex("player|video|surface|exo", RegexOption.IGNORE_CASE)

    fun read(s: AccessibilityService): Result {
        val dm = s.resources.displayMetrics
        val screenArea = dm.widthPixels.toLong() * dm.heightPixels

        val roots = ArrayList<AccessibilityNodeInfo>()
        try {
            s.windows?.forEach { w -> w.root?.let { roots.add(it) } }
        } catch (_: Exception) {
        }
        if (roots.isEmpty()) s.rootInActiveWindow?.let { roots.add(it) }

        val dump = StringBuilder()
        var bestText: String? = null
        var bestSource = "none"
        var bestRank = 0
        var hasVideo = false

        for (root in roots) {
            val pkg = root.packageName?.toString().orEmpty()
            if (pkg == s.packageName) continue

            val nodes = ArrayList<N>()
            collect(root, nodes, intArrayOf(0), 0)
            dump.append("== ").append(pkg).append(" (").append(nodes.size).append(" nodes)\n")

            var lines = 0
            for (n in nodes) {
                if (lines >= 40) break
                if (n.text.isNotEmpty() || videoClsRe.containsMatchIn(n.cls)) {
                    dump.append(n.id.substringAfter(":id/")).append(" | ")
                        .append(n.cls.substringAfterLast('.')).append(" | ")
                        .append(n.text.take(80).replace('\n', ' ')).append(" | ")
                        .append(n.r.flattenToString()).append('\n')
                    lines++
                }
            }

            // الطريقة 1: معرّف العنصر
            val idHits = nodes.filter { it.text.isNotEmpty() && !it.clickable && capIdRe.containsMatchIn(it.id) }
            if (idHits.isNotEmpty() && bestRank < 3) {
                bestText = join(idHits); bestSource = "id:$pkg"; bestRank = 3
            }

            // الطريقة 2: الترجمة الحية في النظام
            if (bestRank < 2 && (pkg.contains("livecaption", true) || pkg == "com.google.android.as")) {
                val hits = nodes.filter { it.text.length >= 2 && !it.clickable }
                if (hits.isNotEmpty()) {
                    bestText = join(hits); bestSource = "livecaption:$pkg"; bestRank = 2
                }
            }

            // الطريقة 3: داخل مساحة الفيديو
            val video = nodes
                .filter { (videoClsRe.containsMatchIn(it.cls) || videoIdRe.containsMatchIn(it.id)) && it.r.width() > 0 }
                .filter { it.r.width().toLong() * it.r.height() >= screenArea * 0.12 }
                .maxByOrNull { it.r.width().toLong() * it.r.height() }
            if (video != null) {
                hasVideo = true
                if (bestRank < 1) {
                    val v = video.r
                    val lowerTop = v.top + (v.height() * 0.45).toInt()
                    val hits = nodes.filter {
                        it.text.length in 2..160 && !it.clickable &&
                            !timeRe.matches(it.text) &&
                            v.contains(it.r.centerX(), it.r.centerY()) &&
                            it.r.top >= lowerTop
                    }
                    if (hits.isNotEmpty()) {
                        bestText = join(hits); bestSource = "video:$pkg"; bestRank = 1
                    }
                }
            }
        }

        return Result(bestText, bestSource, dump.toString().take(6000), hasVideo)
    }

    private fun join(list: List<N>): String =
        list.sortedWith(compareBy({ it.r.top }, { it.r.left }))
            .map { it.text }
            .distinct()
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun collect(n: AccessibilityNodeInfo?, out: MutableList<N>, counter: IntArray, depth: Int) {
        if (n == null || depth > 60 || counter[0] > 2500) return
        counter[0]++
        if (!n.isVisibleToUser) return
        val r = Rect()
        n.getBoundsInScreen(r)
        out.add(
            N(
                id = n.viewIdResourceName.orEmpty(),
                cls = n.className?.toString().orEmpty(),
                text = n.text?.toString()?.trim().orEmpty(),
                r = r,
                clickable = n.isClickable
            )
        )
        for (i in 0 until n.childCount) collect(n.getChild(i), out, counter, depth + 1)
    }
}
