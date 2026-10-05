package com.sweak.qralarm.core.domain.recognition

/** Receives only distinct, processed frames, using monotonic camera timestamps. */
class ObjectConfirmation {
    private var firstMatchMillis = 0L
    private var lastFrameMillis: Long? = null
    private var matches = 0
    private var completed = false

    val progress: Float get() = (matches.toFloat() / REQUIRED_MATCHES).coerceAtMost(1f)

    fun observe(confidence: Float?, timestampMillis: Long): Boolean {
        if (completed) return false
        val previous = lastFrameMillis
        if (previous != null && timestampMillis <= previous) return false
        if (previous != null && timestampMillis - previous > MAX_FRAME_GAP_MILLIS) reset()
        lastFrameMillis = timestampMillis
        if (confidence == null || !confidence.isFinite() || confidence < MIN_CONFIDENCE) {
            matches = 0
            return false
        }
        if (matches == 0) firstMatchMillis = timestampMillis
        matches++
        if (matches >= REQUIRED_MATCHES && timestampMillis - firstMatchMillis >= MIN_SPAN_MILLIS) {
            completed = true
            return true
        }
        return false
    }

    fun reset() {
        matches = 0
        lastFrameMillis = null
        completed = false
    }

    companion object {
        const val MIN_CONFIDENCE = 0.60f
        const val REQUIRED_MATCHES = 3
        const val MIN_SPAN_MILLIS = 150L
        const val MAX_FRAME_GAP_MILLIS = 500L
    }
}
