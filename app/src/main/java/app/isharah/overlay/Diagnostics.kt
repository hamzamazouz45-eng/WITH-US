package app.isharah.overlay

/** آخر ما رأته الخدمة على الشاشة، لعرضه في شاشة التشخيص. */
object Diagnostics {
    @Volatile var lastDump: String = ""
    @Volatile var lastSource: String = "-"
    @Volatile var lastCaption: String = ""
}
