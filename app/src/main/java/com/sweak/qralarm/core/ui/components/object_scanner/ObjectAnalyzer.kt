package com.sweak.qralarm.core.ui.components.object_scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.sweak.qralarm.core.domain.recognition.ObjectConfirmation

/** Coordinates are fractions of the upright image; the preview crops them with the image. */
data class ObjectDetectionBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val imageWidth: Int,
    val imageHeight: Int
)

/** All methods, including close, run on the camera's single worker. */
class ObjectAnalyzer(
    private val context: Context,
    private val categoryId: String,
    private val onResult: (ObjectDetectionBox?, Float, Boolean) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer, AutoCloseable {
    private var detector: ObjectDetector? = null
    private val confirmation = ObjectConfirmation()
    private var failed = false
    private var rotation: Int? = null

    override fun analyze(image: ImageProxy) {
        var bitmap: Bitmap? = null
        var upright: Bitmap? = null
        try {
            if (failed) return
            val frameRotation = image.imageInfo.rotationDegrees
            if (rotation != frameRotation) {
                confirmation.reset()
                rotation = frameRotation
            }
            val timestampMillis = image.imageInfo.timestamp / 1_000_000
            val engine = detector ?: ObjectDetector.createFromOptions(
                context,
                ObjectDetector.ObjectDetectorOptions.builder()
                    .setBaseOptions(BaseOptions.builder()
                        .setModelAssetPath("efficientdet-lite0.tflite")
                        .setDelegate(Delegate.CPU).build())
                    .setRunningMode(RunningMode.IMAGE)
                    .setCategoryAllowlist(listOf(categoryId))
                    .setScoreThreshold(ObjectConfirmation.MIN_CONFIDENCE)
                    .build()
            ).also { detector = it }
            val source = image.toBitmap().also { bitmap = it }
            val rotated = if (frameRotation == 0) source else Bitmap.createBitmap(
                source, 0, 0, source.width, source.height,
                Matrix().apply { postRotate(frameRotation.toFloat()) }, true
            )
            upright = rotated
            val mpImage = BitmapImageBuilder(rotated).build()
            try {
                val result = engine.detect(mpImage)
                val match = result.detections().mapNotNull { detection ->
                    detection.categories().filter { it.categoryName() == categoryId }
                        .maxByOrNull { it.score() }?.let { detection to it.score() }
                }.maxByOrNull { it.second }
                val box = match?.first?.boundingBox()?.let {
                    ObjectDetectionBox(
                        it.left / rotated.width, it.top / rotated.height,
                        it.right / rotated.width, it.bottom / rotated.height,
                        rotated.width, rotated.height
                    )
                }
                val completed = confirmation.observe(match?.second, timestampMillis)
                onResult(box, confirmation.progress, completed)
            } finally {
                mpImage.close()
            }
        } catch (exception: Exception) {
            failed = true
            confirmation.reset()
            onError(exception)
        } finally {
            if (upright !== bitmap) upright?.recycle()
            bitmap?.recycle()
            image.close()
        }
    }

    override fun close() {
        confirmation.reset()
        detector?.close()
        detector = null
    }
}
