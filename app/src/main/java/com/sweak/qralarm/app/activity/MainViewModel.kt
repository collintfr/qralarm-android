package com.sweak.qralarm.app.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweak.qralarm.alarm.service.AlarmService
import com.sweak.qralarm.core.domain.alarm.AlarmsRepository
import com.sweak.qralarm.core.domain.alarm.CodesRepository
import com.sweak.qralarm.core.domain.alarm.MigrateCustomRingtonesToDeviceProtectedStorage
import com.sweak.qralarm.core.domain.alarm.RescheduleAlarms
import com.sweak.qralarm.core.domain.user.EMERGENCY_DEFAULT_REQUIRED_MATCHES
import com.sweak.qralarm.core.domain.user.EMERGENCY_PREVIOUS_DEFAULT_REQUIRED_MATCHES
import com.sweak.qralarm.core.domain.user.ShouldShowWhatsNew
import com.sweak.qralarm.core.domain.user.UserDataRepository
import com.sweak.qralarm.core.domain.user.WHATS_NEW_VERSION_CODE
import com.sweak.qralarm.features.emergency.settings.util.EMERGENCY_AVAILABLE_REQUIRED_MATCHES
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val alarmsRepository: AlarmsRepository,
    private val codesRepository: CodesRepository,
    private val rescheduleAlarms: RescheduleAlarms,
    private val shouldShowWhatsNew: ShouldShowWhatsNew,
    private val migrateCustomRingtonesToDeviceProtectedStorage: MigrateCustomRingtonesToDeviceProtectedStorage
) : ViewModel() {

    private var _state = MutableStateFlow(MainActivityState())
    val state = _state.asStateFlow()

    private val backendEventsChannel = Channel<MainActivityBackendEvent>()
    val backendEvents = backendEventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            rescheduleAlarms(rescheduleAlarmsIfMissedByFiveMinutes = false)
            codesRepository.migrateLegacyDefaultAlarmCode()
            migrateCustomRingtonesToDeviceProtectedStorage()
            migrateToNewEmergencyRequiredMatches()

            _state.update { currentState ->
                currentState.copy(
                    shouldShowSplashScreen = false,
                    isIntroductionFinished = userDataRepository.isIntroductionFinished.first(),
                    isWhatsNewDialogVisible = shouldShowWhatsNew()
                )
            }
        }

        viewModelScope.launch {
            userDataRepository.theme.collect { theme ->
                _state.update { currentState ->
                    currentState.copy(theme = theme)
                }
            }
        }
    }

    private suspend fun migrateToNewEmergencyRequiredMatches() {
        val currentEmergencyRequiredMatches = userDataRepository.emergencyRequiredMatches.first()

        if (currentEmergencyRequiredMatches !in EMERGENCY_AVAILABLE_REQUIRED_MATCHES) {
            userDataRepository.setEmergencyRequiredMatches(
                matches = EMERGENCY_DEFAULT_REQUIRED_MATCHES
            )
        }

        // The default was lowered from 50 to 20 matches; move users still on the old default.
        if (!userDataRepository.hasMigratedEmergencyDefaultMatches.first()) {
            if (userDataRepository.emergencyRequiredMatches.first() ==
                EMERGENCY_PREVIOUS_DEFAULT_REQUIRED_MATCHES
            ) {
                userDataRepository.setEmergencyRequiredMatches(
                    matches = EMERGENCY_DEFAULT_REQUIRED_MATCHES
                )
            }
            userDataRepository.setHasMigratedEmergencyDefaultMatches(migrated = true)
        }
    }

    /**
     * Opens the alarm screen for an alarm that is ringing or snoozed. Call only while the
     * activity is resumed and cancel on pause: a watcher left running in the background would
     * queue a stale "go to the alarm" event while the alarm screen updates the alarm, and reopen
     * an alarm screen that can no longer be closed once the alarm has been dismissed.
     */
    suspend fun observeActiveAlarms() {
        alarmsRepository.getAllAlarms().collect { alarms ->
            alarms.firstOrNull { alarm ->
                alarm.isAlarmRunning || alarm.snoozeConfig.isAlarmSnoozed
            }?.let { activeAlarm ->
                if (activeAlarm.isAlarmRunning && !AlarmService.isRunning) {
                    alarmsRepository.setAlarmRunning(
                        alarmId = activeAlarm.alarmId,
                        running = false
                    )
                    return@let
                }

                backendEventsChannel.send(
                    MainActivityBackendEvent.NavigateToActiveAlarm(alarmId = activeAlarm.alarmId)
                )
            }
        }
    }

    fun onEvent(event: MainActivityUserEvent) {
        when (event) {
            is MainActivityUserEvent.OnOnboardingFinished -> viewModelScope.launch {
                userDataRepository.setIntroductionFinished(finished = true)
            }

            is MainActivityUserEvent.OnWhatsNewDialogDismissed -> viewModelScope.launch {
                userDataRepository.setWhatsNewLastShownVersionCode(WHATS_NEW_VERSION_CODE)
                _state.update { it.copy(isWhatsNewDialogVisible = false) }
            }
        }
    }
}
