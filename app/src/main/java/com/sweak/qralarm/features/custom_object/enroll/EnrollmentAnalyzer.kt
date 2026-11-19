package com.sweak.qralarm.features.custom_object.enroll

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.sweak.qralarm.core.recognition.text.LabelText
import com.sweak.qralarm.core.ui.components.object_scanner.LabelTextAnalyzer
import com.sweak.qralarm.core.ui.components.object_scanner.LabelTextReader
import com.sweak.qralarm.core.ui.components.object_scanner.LabelTextTarget
import com.sweak.qralarm.core.ui.components.object_scanner.ObjectDetectionBox
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * Reads the label inside the guide box on request (single photos or a recorded turn), and later
 * runs the real alarm-time recogniser as a test.
 */
class EnrollmentAnalyzer(
    private val onShot: (Shot) -> Unit,
    /** The best frame of each time slice of a recorded turn (most words read). */
    private val onClip: (shots: List<Shot>) -> Unit,
    private val onTestResult: (ObjectDetectionBox?, Float, Boolean) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer, AutoCloseable {

    /** One photo of the label: the JPEG kept for thumbnails, and the words read in it. */
    class Shot(val jpeg: ByteArray, val tokens: Set<String>, val preview: Bitmap)

    private val captureRequested = AtomicBoolean(false)
    private val recordRequested = AtomicBoolean(false)
    private var recordingStartMillis: Long? = null
    private val clipSlices = HashMap<Int, Shot>()
    @Volatile private var testKeywords: List<String>? = null
    private var tester: LabelTextAnalyzer? = null
    private var reader: LabelTextReader? = null

    fun requestCapture() = captureRequested.set(true)

    /** Records for [CLIP_MILLIS] while the user turns the item; see [onClip]. */
    fun requestClip() = recordRequested.set(true)

    /** Runs exactly what the alarm will run; null stops testing. */
    fun startTest(keywords: List<String>?) {
        testKeywords = keywords
    }

    override fun analyze(image: ImageProxy) {
        val keywords = testKeywords
        if (keywords != null) {
            val current = tester ?: LabelTextAnalyzer(
                listOf(LabelTextTarget(0L, keywords)),
                onResult = { box, progress, _, done -> onTestResult(box, progress, done) },
                onError = onError
            ).also { tester = it }
            current.analyze(image) // closes the image
            return
        }
        tester?.close()
        tester = null
        if (recordRequested.compareAndSet(true, false)) {
            clipSlices.values.forEach { it.preview.recycle() }
            clipSlices.clear()
            recordingStartMillis = image.imageInfo.timestamp / 1_000_000
        }
        val recordingStart = recordingStartMillis
        if (recordingStart != null) {
            val now = image.imageInfo.timestamp / 1_000_000
            if (now - recordingStart >= CLIP_MILLIS) {
                recordingStartMillis = null
                image.close()
                onClip(clipSlices.toSortedMap().values.toList())
                clipSlices.clear()
                return
            }
            val shot = readShot(image) ?: return
            val slice = ((now - recordingStart) * CLIP_SLICES / CLIP_MILLIS).toInt()
            val kept = clipSlices[slice]
            if (shot.tokens.isNotEmpty() && (kept == null || shot.tokens.size > kept.tokens.size)) {
                kept?.preview?.recycle()
                clipSlices[slice] = shot
            } else {
                shot.preview.recycle()
            }
            return
        }
        if (!captureRequested.compareAndSet(true, false)) {
            image.close()
            return
        }
        readShot(image)?.let(onShot)
    }

    /** Crops the guide box and reads its text. Always closes [image]. */
    private fun readShot(image: ImageProxy): Shot? {
        var source: Bitmap? = null
        var upright: Bitmap? = null
        try {
            val rotation = image.imageInfo.rotationDegrees
            val bitmap = image.toBitmap().also { source = it }
            val rotated = if (rotation == 0) bitmap else Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height,
                Matrix().apply { postRotate(rotation.toFloat()) }, true
            )
            upright = rotated
            // Only the guide box is read, so text elsewhere in the room never becomes a keyword.
            val side = (minOf(rotated.width, rotated.height) * GUIDE_BOX_FRACTION).roundToInt()
            val crop = Bitmap.createBitmap(rotated, (rotated.width - side) / 2, (rotated.height - side) / 2, side, side)
            val lines = (reader ?: LabelTextReader().also { reader = it }).read(crop)
            val preview = if (crop.width > PREVIEW_SIDE) {
                Bitmap.createScaledBitmap(crop, PREVIEW_SIDE, PREVIEW_SIDE, true).also { crop.recycle() }
            } else crop
            val jpeg = ByteArrayOutputStream().use { out ->
                preview.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.toByteArray()
            }
            return Shot(jpeg, LabelText.tokens(lines.map { it.text }), preview)
        } catch (exception: Exception) {
            onError(exception)
            return null
        } finally {
            if (upright !== source) upright?.recycle()
            source?.recycle()
            image.close()
        }
    }

    override fun close() {
        clipSlices.values.forEach { it.preview.recycle() }
        clipSlices.clear()
        tester?.close()
        tester = null
        reader?.close()
        reader = null
    }

    companion object {
        const val CLIP_MILLIS = 5_000L
        const val CLIP_SLICES = 8
        /** Guide box: centred square, this share of the shorter image side. */
        const val GUIDE_BOX_FRACTION = 0.7f
        const val PREVIEW_SIDE = 480
        const val MIN_PHOTOS = 2
        const val MAX_PHOTOS = 10
    }
}
