package com.sweak.qralarm.core.recognition

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.sweak.qralarm.core.domain.recognition.ObjectCategories
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class ObjectModelTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun bundledLabelsMatchEverySelectableCategory() {
        val labels = context.assets.open("object-labels.txt").bufferedReader().use { it.readLines() }
            .filter { it.isNotBlank() && it != "???" }.sorted()
        assertEquals(80, labels.size)
        assertEquals(ObjectCategories.ids, labels)
        assertTrue(labels.containsAll(listOf("toilet", "sink", "toothbrush", "bottle")))
    }

    @Test fun pinnedModelLoadsAndRunsOfflineWithoutRecognizingABlankFrame() {
        val bytes = context.assets.open("efficientdet-lite0.tflite").use { it.readBytes() }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals("0720bf247bd76e6594ea28fa9c6f7c5242be774818997dbbeffc4da460c723bb", hash)
        val detector = ObjectDetector.createFromOptions(context,
            ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("efficientdet-lite0.tflite").build())
                .setScoreThreshold(0.6f).build())
        val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        val image = BitmapImageBuilder(bitmap).build()
        try {
            assertTrue(detector.detect(image).detections().isEmpty())
        } finally {
            image.close()
            bitmap.recycle()
            detector.close()
        }
    }
}
