package com.sweak.qralarm.core.ui.components.object_scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.sweak.qralarm.core.domain.recognition.ObjectConfirmation
import com.sweak.qralarm.core.recognition.text.LabelText

/** One item to recognise, by the keywords of its label. */
class LabelTextTarget(val id: Long, val keywords: List<String>)

/**
 * Live recognition of one or more items by the text on their labels; every item has to be
 * confirmed, in any order. Text is read with ML Kit on the camera's single worker thread, which
 * also runs close().
 *
 * [onResult] receives the box around the matched words in this frame (if any), the overall
 * progress (0..1), the ids confirmed so far and whether all are confirmed.
 */
class LabelTextAnalyzer(
    private val targets: List<LabelTextTarget>,
    private val onResult: (ObjectDetectionBox?, Float, Set<Long>, Boolean) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer, AutoCloseable {
    private var reader: LabelTextReader? = null
    private val confirmations = targets.associate { it.id to ObjectConfirmation(ObjectConfirmation.LABEL_TEXT_POLICY) }
    private val confirmed = mutableSetOf<Long>()
    private var failed = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        try {
            if (failed) return
            val media = image.image ?: return
            val rotation = image.imageInfo.rotationDegrees
            val timestampMillis = image.imageInfo.timestamp / 1_000_000
            val lines = (reader ?: LabelTextReader().also { reader = it })
                .read(InputImage.fromMediaImage(media, rotation))
            val tokens = LabelText.tokens(lines.map { it.text })
            // ML Kit reports boxes in the upright image.
            val uprightWidth = if (rotation % 180 == 0) image.width else image.height
            val uprightHeight = if (rotation % 180 == 0) image.height else image.width
            var box: ObjectDetectionBox? = null
            var bestPending = 0f
            targets.filter { it.id !in confirmed }.forEach { target ->
                val verdict = LabelText.verify(target.keywords, tokens)
                if (box == null && verdict.matched.isNotEmpty()) {
                    val matchedLines = lines.filter { line ->
                        LabelText.tokens(listOf(line.text)).any { token -> verdict.matched.any { LabelText.matches(it, setOf(token)) } }
                    }.mapNotNull { it.box }
                    if (matchedLines.isNotEmpty()) {
                        box = ObjectDetectionBox(
                            matchedLines.minOf { it.left }.toFloat() / uprightWidth,
                            matchedLines.minOf { it.top }.toFloat() / uprightHeight,
                            matchedLines.maxOf { it.right }.toFloat() / uprightWidth,
                            matchedLines.maxOf { it.bottom }.toFloat() / uprightHeight,
                            uprightWidth, uprightHeight
                        )
                    }
                }
                val confirmation = confirmations.getValue(target.id)
                if (confirmation.observe(if (verdict.verified) 1f else null, timestampMillis)) {
                    confirmed += target.id
                } else {
                    bestPending = maxOf(bestPending, confirmation.progress)
                }
            }
            val allDone = confirmed.size >= targets.size
            val progress = if (allDone) 1f else (confirmed.size + bestPending) / targets.size.coerceAtLeast(1)
            onResult(box, progress, confirmed.toSet(), allDone)
        } catch (exception: Exception) {
            failed = true
            confirmations.values.forEach { it.reset() }
            onError(exception)
        } finally {
            image.close()
        }
    }

    override fun close() {
        reader?.close()
        reader = null
    }
}
