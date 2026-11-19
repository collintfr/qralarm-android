package com.sweak.qralarm.core.ui.components.object_scanner

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** On-device ML Kit text recognition, blocking. Call from a background thread only. */
class LabelTextReader : AutoCloseable {
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    class Line(val text: String, val box: Rect?)

    fun read(image: InputImage): List<Line> =
        Tasks.await(recognizer.process(image)).textBlocks
            .flatMap { it.lines }
            .map { Line(it.text, it.boundingBox) }

    fun read(bitmap: Bitmap): List<Line> = read(InputImage.fromBitmap(bitmap, 0))

    override fun close() = recognizer.close()
}
