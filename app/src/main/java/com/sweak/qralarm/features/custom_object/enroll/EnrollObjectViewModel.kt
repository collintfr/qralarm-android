package com.sweak.qralarm.features.custom_object.enroll

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweak.qralarm.core.domain.custom_object.CustomObjectsRepository
import com.sweak.qralarm.core.recognition.text.LabelText
import com.sweak.qralarm.core.ui.components.object_scanner.ObjectDetectionBox
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

data class EnrollObjectScreenState(
    val name: String = "",
    val isReEnrolling: Boolean = false,
    val surfaceRequest: SurfaceRequest? = null,
    val shots: List<Bitmap> = emptyList(),
    val isCapturing: Boolean = false,
    val isRecording: Boolean = false,
    /** The last photo or recording had no readable text. */
    val noTextRead: Boolean = false,
    /** Keywords chosen from the photos; the user can remove ones that are not on the item. */
    val keywords: List<String> = emptyList(),
    val step: Step = Step.CAPTURE,
    val testProgress: Float = 0f,
    val testBox: ObjectDetectionBox? = null,
    val isTestPassed: Boolean = false,
    val isSaving: Boolean = false,
    val showNameError: Boolean = false,
    /** Binding the camera failed. */
    val cameraError: Boolean = false,
    /** Reading a photo failed; the message helps report the cause. Cleared on the next try. */
    val processingError: String? = null
) {
    enum class Step { CAPTURE, TEST }

    val keywordsUsable: Boolean get() = LabelText.isUsable(keywords)
    val canContinue: Boolean get() = shots.size >= EnrollmentAnalyzer.MIN_PHOTOS && keywordsUsable
    val canCaptureMore: Boolean get() = shots.size < EnrollmentAnalyzer.MAX_PHOTOS
}

sealed class EnrollObjectBackendEvent {
    data class Saved(val objectId: Long) : EnrollObjectBackendEvent()
    data object SaveFailed : EnrollObjectBackendEvent()
}

@HiltViewModel(assistedFactory = EnrollObjectViewModel.Factory::class)
class EnrollObjectViewModel @AssistedInject constructor(
    @Assisted private val objectIdToReplace: Long,
    @param:ApplicationContext private val context: Context,
    private val repository: CustomObjectsRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(objectIdToReplace: Long): EnrollObjectViewModel
    }

    private val _state = MutableStateFlow(EnrollObjectScreenState(isReEnrolling = objectIdToReplace != 0L))
    val state = _state.asStateFlow()

    private val backendEventsChannel = Channel<EnrollObjectBackendEvent>()
    val backendEvents = backendEventsChannel.receiveAsFlow()

    private val shots = mutableListOf<EnrollmentAnalyzer.Shot>()
    private val removedKeywords = mutableSetOf<String>()
    private var analyzer: EnrollmentAnalyzer? = null

    init {
        if (objectIdToReplace != 0L) viewModelScope.launch {
            repository.getObject(objectIdToReplace)?.let { existing -> _state.update { it.copy(name = existing.name) } }
        }
    }

    /** The screen owns this coroutine, so leaving or backgrounding it releases the camera. */
    @androidx.annotation.OptIn(androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration::class)
    suspend fun runCamera(lifecycleOwner: LifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val enrollmentAnalyzer = EnrollmentAnalyzer(
            onShot = { shot -> viewModelScope.launch { onShots(listOf(shot), fromClip = false) } },
            onClip = { clip -> viewModelScope.launch { onShots(clip, fromClip = true) } },
            onTestResult = { box, progress, completed ->
                viewModelScope.launch {
                    _state.update {
                        it.copy(testBox = box, testProgress = progress, isTestPassed = it.isTestPassed || completed)
                    }
                    if (completed) analyzer?.startTest(null)
                }
            },
            onError = { exception ->
                Log.e("EnrollObject", "Reading failed", exception)
                viewModelScope.launch {
                    _state.update {
                        it.copy(processingError = exception.message ?: exception.javaClass.simpleName, isCapturing = false)
                    }
                }
            }
        )
        analyzer = enrollmentAnalyzer
        if (state.value.step == EnrollObjectScreenState.Step.TEST && !state.value.isTestPassed) {
            enrollmentAnalyzer.startTest(state.value.keywords)
        }
        var provider: ProcessCameraProvider? = null
        // Text needs more pixels than shape matching did.
        val resolution = ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
        ).build()
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(resolution)
            .build()
        try {
            analysis.setAnalyzer(executor, enrollmentAnalyzer)
            try {
                ProcessCameraProvider.configureInstance(Camera2Config.defaultConfig())
            } catch (_: IllegalStateException) { /* Already configured. */ }
            val cameraProvider = ProcessCameraProvider.awaitInstance(context).also { provider = it }
            cameraProvider.unbindAll()
            val preview = Preview.Builder().setResolutionSelector(resolution).build().apply {
                setSurfaceProvider { request -> _state.update { it.copy(surfaceRequest = request) } }
            }
            cameraProvider.bindToLifecycle(lifecycleOwner, DEFAULT_BACK_CAMERA, preview, analysis)
            _state.update { it.copy(cameraError = false) }
            awaitCancellation()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            Log.e("EnrollObject", "Camera failed", exception)
            _state.update { it.copy(cameraError = true) }
        } finally {
            analysis.clearAnalyzer()
            provider?.unbindAll()
            analyzer = null
            _state.update { it.copy(surfaceRequest = null, isCapturing = false, isRecording = false) }
            executor.execute { enrollmentAnalyzer.close() }
            executor.shutdown()
        }
    }

    fun onNameChanged(name: String) = _state.update { it.copy(name = name.take(40), showNameError = false) }

    fun onCaptureClicked() {
        if (state.value.isCapturing || state.value.isRecording || !state.value.canCaptureMore) return
        _state.update { it.copy(isCapturing = true, noTextRead = false, processingError = null) }
        analyzer?.requestCapture()
    }

    fun onRecordClicked() {
        if (state.value.isCapturing || state.value.isRecording || !state.value.canCaptureMore) return
        _state.update { it.copy(isRecording = true, noTextRead = false, processingError = null) }
        analyzer?.requestClip()
    }

    fun onRemoveShot(index: Int) {
        if (index !in shots.indices) return
        shots.removeAt(index).preview.recycle()
        publish()
    }

    fun onRemoveKeyword(keyword: String) {
        removedKeywords += keyword
        publish()
    }

    private fun onShots(newShots: List<EnrollmentAnalyzer.Shot>, fromClip: Boolean) {
        val readable = newShots.filter { it.tokens.isNotEmpty() }
        newShots.filter { it.tokens.isEmpty() }.forEach { it.preview.recycle() }
        val room = (EnrollmentAnalyzer.MAX_PHOTOS - shots.size).coerceAtLeast(0)
        shots += readable.take(room)
        readable.drop(room).forEach { it.preview.recycle() }
        _state.update {
            it.copy(
                isCapturing = if (fromClip) it.isCapturing else false,
                isRecording = if (fromClip) false else it.isRecording,
                noTextRead = readable.isEmpty()
            )
        }
        publish()
    }

    private fun publish() {
        val keywords = LabelText.selectKeywords(shots.map { it.tokens }).filter { it !in removedKeywords }
        _state.update { it.copy(shots = shots.map { shot -> shot.preview }, keywords = keywords) }
    }

    fun onContinueClicked() {
        if (!state.value.canContinue) return
        _state.update { it.copy(step = EnrollObjectScreenState.Step.TEST, testProgress = 0f, isTestPassed = false) }
        analyzer?.startTest(state.value.keywords)
    }

    fun onBackToCaptureClicked() {
        analyzer?.startTest(null)
        _state.update { it.copy(step = EnrollObjectScreenState.Step.CAPTURE, testBox = null, testProgress = 0f) }
    }

    fun onSaveClicked() = viewModelScope.launch {
        val current = state.value
        if (current.name.isBlank()) {
            _state.update { it.copy(showNameError = true) }
            return@launch
        }
        if (!current.isTestPassed || current.isSaving) return@launch
        _state.update { it.copy(isSaving = true) }
        try {
            val photos = shots.map { it.jpeg }
            val id = if (objectIdToReplace != 0L) {
                repository.replaceReferences(objectIdToReplace, photos, current.keywords)
                repository.renameObject(objectIdToReplace, current.name)
                objectIdToReplace
            } else {
                repository.addObject(current.name, photos, current.keywords)
            }
            backendEventsChannel.send(EnrollObjectBackendEvent.Saved(id))
        } catch (exception: Exception) {
            Log.e("EnrollObject", "Saving failed", exception)
            _state.update { it.copy(isSaving = false) }
            backendEventsChannel.send(EnrollObjectBackendEvent.SaveFailed)
        }
    }

    override fun onCleared() {
        shots.forEach { it.preview.recycle() }
        shots.clear()
    }
}
