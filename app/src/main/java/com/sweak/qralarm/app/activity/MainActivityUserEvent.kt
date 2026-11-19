package com.sweak.qralarm.app.activity

sealed class MainActivityUserEvent {
    data object OnOnboardingFinished : MainActivityUserEvent()
    data object OnWhatsNewDialogDismissed : MainActivityUserEvent()
}