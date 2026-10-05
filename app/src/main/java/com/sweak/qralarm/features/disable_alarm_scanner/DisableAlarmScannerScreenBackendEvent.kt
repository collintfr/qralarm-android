package com.sweak.qralarm.features.disable_alarm_scanner

sealed class DisableAlarmScannerScreenBackendEvent {
    data class ChallengeCompleted(
        val uriStringToOpen: String?
    ) : DisableAlarmScannerScreenBackendEvent()

    data object CameraInitializationError : DisableAlarmScannerScreenBackendEvent()
}