/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.network

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.UiccCardInfo
import android.telephony.UiccSlotInfo
import android.telephony.UiccSlotMapping
import android.telephony.euicc.EuiccManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.ToIntFunction
import java.util.stream.Collectors

class SimOnboardingViewModel(
    private val application: Application,
) : AndroidViewModel(application) {

    data class SimSwitchingInfo(
        val targetSubInfo: SubscriptionInfo,
        val removedSubInfo: SubscriptionInfo?,
    ) {
        var targetPhysicalSlotId: Int = SubscriptionManager.INVALID_SIM_SLOT_INDEX
        var targetLogicalSlotId: Int = SubscriptionManager.INVALID_SIM_SLOT_INDEX
        var targetPortId: Int = TelephonyManager.INVALID_PORT_INDEX
        val isEmbedded = targetSubInfo.isEmbedded == true
        val targetSubId: Int = targetSubInfo.subscriptionId
    }

    private val _uiState = MutableStateFlow(SwitchingState.NOT_STARTED)
    val uiState: StateFlow<SwitchingState> = _uiState.asStateFlow()
    private val _euiccSlotSwitchingState = MutableStateFlow(SwitchingState.NOT_STARTED)
    val euiccSlotSwitchingState: StateFlow<SwitchingState> = _euiccSlotSwitchingState.asStateFlow()
    private val _removableSlotSwitchingState = MutableStateFlow(SwitchingState.NOT_STARTED)
    val removableSlotSwitchingState: StateFlow<SwitchingState> =
        _removableSlotSwitchingState.asStateFlow()

    private var activeSubInfos: List<SubscriptionInfo>? = null
    private var euiccManager = application.getSystemService(EuiccManager::class.java)
    private var telephonyManager = application.getSystemService(TelephonyManager::class.java)
    private var subscriptionManager =
        application.getSystemService(SubscriptionManager::class.java)?.createForAllUserProfiles()
    private var isDuringSimSlotMapping = false
    private var euiccOpId = -1
    private lateinit var currentSimSwitchingInfo: SimSwitchingInfo
    private lateinit var callbackIntent: PendingIntent

    enum class SwitchingState {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
    }

    init {
        viewModelScope.launch {
            euiccSlotSwitchingState.collect {
                // update
                when (_euiccSlotSwitchingState.value) {
                    SwitchingState.COMPLETED -> {
                        // success
                        Log.i(
                            TAG,
                            "Successfully SimSlotMapping. Start to enable/disable esim"
                        )
                        // reset _euiccSlotSwitchingState
                        _euiccSlotSwitchingState.value = SwitchingState.NOT_STARTED
                        // do next action: switchToSubscription
                        switchEuiccSimAfterSlotReady()
                    }

                    SwitchingState.FAILED -> {
                        //error
                        Log.i(
                            TAG,
                            "Failed to set SimSlotMapping"
                        )
                        // reset _euiccSlotSwitchingState
                        _euiccSlotSwitchingState.value = SwitchingState.NOT_STARTED
                        _uiState.value = SwitchingState.FAILED
                    }

                    else -> {
                        // do nothing
                    }
                }
            }
        }

        viewModelScope.launch {
            removableSlotSwitchingState.collect {
                // update
                when (_removableSlotSwitchingState.value) {
                    SwitchingState.COMPLETED -> {
                        // success
                        Log.i(
                            TAG,
                            "Successfully switched to removable slot."
                        )

                        // reset _removableSlotSwitchingState
                        _removableSlotSwitchingState.value = SwitchingState.NOT_STARTED
                        // do next action: switchToSubscription
                        switchRemovableSimAfterSlotReady()
                        _uiState.value = SwitchingState.COMPLETED
                    }

                    SwitchingState.FAILED -> {
                        //error
                        Log.i(
                            TAG,
                            "Failed to switch to removable slot."
                        )

                        // reset _removableSlotSwitchingState
                        _removableSlotSwitchingState.value = SwitchingState.NOT_STARTED
                        _uiState.value = SwitchingState.FAILED
                    }

                    else -> {
                        // do nothing
                    }
                }
            }
        }

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                application.sidecarReceiverFlow().collect { resultCode ->
                    onEuiccSimSwitchingCallback(resultCode)
                }
            }
        }
    }

    fun Application.sidecarReceiverFlow(): Flow<Int> = callbackFlow {
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "onReceive: $intent")
                if (ACTION_SWITCH_TO_SUBSCRIPTION == intent.action
                    && euiccOpId == intent.getIntExtra(EXTRA_OP_ID, -1)
                ) {
                    Log.d(TAG, "onReceive: onEuiccSimSwitchingCallback $intent")
                    /* TODO: This relies on our LUI and LPA to coexist, should think about how
                        to generalize this further. */
                    var detailedCode =
                        intent.getIntExtra(
                            EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE,
                            0 /* defaultValue*/
                        )
                    Log.d(
                        TAG,
                        "Result code : $resultCode; detailed code : $detailedCode"
                    )
                    trySend(resultCode)
                }
            }
        }
        Log.d(TAG, "registerReceiver: $ACTION_SWITCH_TO_SUBSCRIPTION")

        application.registerReceiver(
            broadcastReceiver,
            IntentFilter(ACTION_SWITCH_TO_SUBSCRIPTION),
            Manifest.permission.WRITE_EMBEDDED_SUBSCRIPTIONS,
            null,
            Context.RECEIVER_VISIBLE_TO_INSTANT_APPS
        )

        awaitClose {
            Log.d(TAG, "unregisterReceiver: $broadcastReceiver")
            application.unregisterReceiver(broadcastReceiver)
        }
    }.catch { e ->
        Log.e(SimOnboardingActivity.Companion.TAG, "Error while sidecarReceiverFlow", e)
    }.conflate().flowOn(Dispatchers.Default)

    fun startSimSwitching(targetSubInfo: SubscriptionInfo, removedSubInfo: SubscriptionInfo?) {
        currentSimSwitchingInfo = SimSwitchingInfo(targetSubInfo, removedSubInfo)
        _uiState.value = SwitchingState.IN_PROGRESS
        _euiccSlotSwitchingState.value = SwitchingState.NOT_STARTED
        _removableSlotSwitchingState.value = SwitchingState.NOT_STARTED
        viewModelScope.launch {
            if (targetSubInfo.isEmbedded) {
                startEuiccSimSwitching(
                    targetSubInfo.subscriptionId,
                    UiccSlotUtil.INVALID_PORT_ID,
                    removedSubInfo
                )
            } else {
                startRemovableSimSwitching(
                    UiccSlotUtil.INVALID_PHYSICAL_SLOT_ID,
                    removedSubInfo
                )
            }
        }
    }

    suspend fun startEuiccSimSwitching(subId: Int, portId: Int, removedSubInfo: SubscriptionInfo?) {
        if (!this::currentSimSwitchingInfo.isInitialized) {
            Log.e(TAG, "no init currentSimSwitchingInfo")
        }
        callbackIntent = createCallbackIntent(ACTION_SWITCH_TO_SUBSCRIPTION)

        val targetSlot: Int = UiccSlotUtil.getEsimSlotId(application, subId);
        if (targetSlot < 0) {
            Log.d(
                TAG,
                "There is no esim, the TargetSlot is $targetSlot"
            )
            _uiState.value = SwitchingState.FAILED
            return
        }

        activeSubInfos = SubscriptionUtil.getActiveSubscriptions(subscriptionManager)
        // To check whether the esim slot's port is active. If yes, skip setSlotMapping. If no,
        // set this slot+port into setSimSlotMapping.
        var targetPortId =
            if (portId < 0) getTargetEuiccPortId(targetSlot, removedSubInfo) else portId

        if (!isDuringSimSlotMapping) {
            currentSimSwitchingInfo.targetPhysicalSlotId = targetSlot
            currentSimSwitchingInfo.targetPortId = targetPortId
        }

        Log.d(
            TAG,
            String.format(
                "Set esim into the SubId%d Physical Slot%d:Port%d",
                subId, targetSlot, targetPortId
            )
        )
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            // If the subId is INVALID_SUBSCRIPTION_ID, disable the esim (the default esim slot
            // which is selected by the framework).
            euiccManager?.switchToSubscription(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID, targetPortId,
                callbackIntent
            )
        } else if (
            (telephonyManager?.isMultiSimEnabled() == true && removedSubInfo != null
                    && removedSubInfo.isEmbedded)
            || isEsimEnabledAtTargetSlotPort(targetSlot, targetPortId)
        ) {
            // Case 1: In DSDS mode+MEP, if the replaced esim is active, then the replaced esim
            // should be disabled before changing SimSlotMapping process.
            //
            // Case 2: If the user enables the esim A on the target slot:port which is active
            // and there is an active esim B on target slot:port, then the settings disables the
            // esim B before the settings enables the esim A on the target slot:port.
            //
            // Step:
            // 1) Disables the replaced esim.
            // 2) Switches the SimSlotMapping if the target slot:port is not active.
            // 3) Enables the target esim.
            // Note: Use INVALID_SUBSCRIPTION_ID to disable the esim profile.
            Log.d(
                TAG,
                "Disable the enabled esim before the settings enables the target esim"
            )
            isDuringSimSlotMapping = true
            euiccManager?.switchToSubscription(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID, targetPortId,
                callbackIntent
            )
        } else {
            startEuiccSlotSwitching(targetSlot, targetPortId, removedSubInfo)
        }
    }

    suspend fun startRemovableSimSwitching(physicalSlotId: Int, removedSubInfo: SubscriptionInfo?) {
        if (!this::currentSimSwitchingInfo.isInitialized) {
            Log.e(TAG, "no init currentSimSwitchingInfo")
        }
        currentSimSwitchingInfo.targetPhysicalSlotId = physicalSlotId
        activeSubInfos = SubscriptionUtil.getActiveSubscriptions(subscriptionManager)

        when {
            telephonyManager?.isMultiSimEnabled() == false
                    && activeSubInfos != null
                    && activeSubInfos!!.stream().anyMatch { it.isEmbedded } -> {
                // In SS mode, the esim is active, then inactivate the esim.
                Log.i(
                    TAG,
                    "There is an active eSIM profile. Disable the profile first."
                )

                // Use INVALID_SUBSCRIPTION_ID to disable the only active profile.
                isDuringSimSlotMapping = true
                startEuiccSimSwitching(SubscriptionManager.INVALID_SUBSCRIPTION_ID, 0, null)
            }

            telephonyManager?.isMultiSimEnabled() == true && removedSubInfo != null -> {
                // In DSDS mode+MEP, if the replaced esim is active, then it should disable that
                // esim profile before changing SimSlotMapping process.
                // Use INVALID_SUBSCRIPTION_ID to disable the esim profile.

                Log.i(
                    TAG,
                    "In MEP mode, there is an active eSIM profile. Disable the profile first" +
                            "($removedSubInfo)"
                )

                isDuringSimSlotMapping = true
                startEuiccSimSwitching(
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID,
                    removedSubInfo.portIndex,
                    null
                )
            }

            else -> {
                Log.i(TAG, "Start to switch to removable slot.")
                startRemovableSlotSwitching(physicalSlotId, removedSubInfo)
            }
        }
    }

    suspend fun startEuiccSlotSwitching(
        targetPhysicalSlotId: Int,
        targetPortId: Int,
        removedSubInfo: SubscriptionInfo?
    ) {
        _euiccSlotSwitchingState.value = SwitchingState.IN_PROGRESS
        withContext(Dispatchers.Default) {
            try {
                Log.i(TAG, "Start to switch to euicc slot.")
                UiccSlotUtil.switchToEuiccSlot(
                    application, targetPhysicalSlotId, targetPortId, removedSubInfo
                )
                _euiccSlotSwitchingState.value = SwitchingState.COMPLETED
            } catch (e: UiccSlotsException) {
                Log.e(TAG, "UiccSlotsException", e)
                _euiccSlotSwitchingState.value = SwitchingState.FAILED
            }
        }
    }

    suspend fun startRemovableSlotSwitching(
        targetPhysicalSlotId: Int,
        removedSubInfo: SubscriptionInfo?
    ) {
        _removableSlotSwitchingState.value = SwitchingState.IN_PROGRESS
        withContext(Dispatchers.Default) {
            try {
                Log.i(TAG, "Start to switch to removable slot.")
                UiccSlotUtil.switchToRemovableSlot(
                    application, targetPhysicalSlotId, removedSubInfo
                )
                _removableSlotSwitchingState.value = SwitchingState.COMPLETED
            } catch (e: UiccSlotsException) {
                Log.e(TAG, "UiccSlotsException", e)
                _removableSlotSwitchingState.value = SwitchingState.FAILED
            }
        }
    }

    private fun getTargetEuiccPortId(
        physicalEsimSlotIndex: Int,
        removedSubInfo: SubscriptionInfo?
    ): Int {
        if (!isMultipleEnabledProfilesSupported(physicalEsimSlotIndex)) {
            Log.d(
                TAG,
                "The slotId$physicalEsimSlotIndex is no MEP, port is 0"
            )
            return 0
        }

        val uiccSlotMappings: Collection<UiccSlotMapping?>? =
            telephonyManager?.getSimSlotMapping()
        Log.d(TAG, "The UiccSlotMapping: $uiccSlotMappings")
        telephonyManager?.isMultiSimEnabled()?.let {
            if (!it) {
                // In the 'SS mode'
                // If there is the esim slot is active, the port is from the current esim slot.
                // If there is no esim slot in device, then the esim's port is 0.
                Log.d(
                    TAG,
                    "In SS mode, to find the active esim slot's port."
                            + "If no active esim slot, the port is 0"
                )
                return uiccSlotMappings!!.stream()
                    .filter { i: UiccSlotMapping? ->
                        i!!.physicalSlotIndex == physicalEsimSlotIndex
                    }
                    .mapToInt { i: UiccSlotMapping? -> i!!.getPortIndex() }
                    .findFirst().orElse(0)
            }
        }

        // In the 'DSDS+MEP', if the removedSubInfo is esim, then the port is
        // removedSubInfo's port.
        if (removedSubInfo != null && removedSubInfo.isEmbedded()) {
            return removedSubInfo.getPortIndex()
        }

        // In DSDS+MEP mode, the removedSubInfo is psim or is null, it means this esim needs
        // a new corresponding port in the esim slot.
        // For example:
        // 1) If there is no enabled esim and the user add new esim. This new esim's port is
        // active esim slot's port.
        // 2) If there is one enabled esim in port0 and the user add new esim. This new esim's
        // port is 1.
        // 3) If there is one enabled esim in port1 and the user add new esim. This new esim's
        // port is 0.
        var port = 0
        if (activeSubInfos == null) {
            Log.d(TAG, "activeSubInfos is null.")
            return port
        }
        val activeEsimSubInfos: MutableList<SubscriptionInfo> =
            activeSubInfos!!.stream()
                .filter { i: SubscriptionInfo? -> i!!.isEmbedded }
                .sorted(Comparator.comparingInt<SubscriptionInfo?>(ToIntFunction {
                    obj: SubscriptionInfo? -> obj!!.portIndex }))
                .collect(Collectors.toList())

        // In DSDS+MEP mode, if there is the active esim slot and no active esim at that slot,
        // then using this active esim slot's port.
        // If there is no esim slot in device, then the esim's port is 0.
        if (activeEsimSubInfos.isEmpty()) {
            Log.d(
                TAG,
                "In DSDS+MEP mode, no active esim. return the active esim slot's port."
                        + "If no active esim slot, the port is 0"
            )
            return uiccSlotMappings!!.stream()
                .filter { i: UiccSlotMapping? -> i!!.physicalSlotIndex == physicalEsimSlotIndex }
                .mapToInt { i: UiccSlotMapping? -> i!!.portIndex }
                .sorted()
                .findFirst().orElse(0)
        }

        for (subscriptionInfo in activeEsimSubInfos) {
            if (subscriptionInfo.portIndex == port) {
                port++
            }
        }
        return port
    }

    private fun isMultipleEnabledProfilesSupported(physicalEsimSlotIndex: Int): Boolean {
        val cardInfos: MutableList<UiccCardInfo?> = telephonyManager.getUiccCardsInfo()
        return cardInfos.stream()
            .anyMatch { cardInfo: UiccCardInfo? ->
                cardInfo!!.physicalSlotIndex == physicalEsimSlotIndex
                        && cardInfo.isMultipleEnabledProfilesSupported
            }
    }

    private fun switchEuiccSimAfterSlotReady() {
        // The SimSlotMapping is ready, then to execute activate/inactivate esim.
        if (!this::currentSimSwitchingInfo.isInitialized || !this::callbackIntent.isInitialized) {
            Log.e(TAG, "no init currentSimSwitchingInfo or callbackIntent")
            _uiState.value = SwitchingState.FAILED
            return
        }
        Log.d(
            TAG,
            "switchEuiccSimAfterSlotReady: subId${currentSimSwitchingInfo.targetSubId} " +
                    "port${currentSimSwitchingInfo.targetPortId}"
        )

        euiccManager?.switchToSubscription(
            currentSimSwitchingInfo.targetSubId,
            currentSimSwitchingInfo.targetPortId,
            callbackIntent
        )
    }

    private fun switchRemovableSimAfterSlotReady() {
        // The SimSlotMapping is ready, then to execute activate/inactivate removable sim.
        if (!this::currentSimSwitchingInfo.isInitialized) {
            Log.e(TAG, "no init currentSimSwitchingInfo")
            _uiState.value = SwitchingState.FAILED
            return
        }
        if (subscriptionManager?.canDisablePhysicalSubscription() == true) {
            Log.d(
                TAG, "switchRemovableSimAfterSlotReady: subId${currentSimSwitchingInfo.targetSubId}"
            )
            // TODO: to support disable case.
            subscriptionManager?.setUiccApplicationsEnabled(
                currentSimSwitchingInfo.targetSubId, /*enabled=*/true
            )
        } else {
            Log.i(
                TAG, "The device does not support toggling pSIM. It is enough to just "
                        + "enable the removable slot."
            )
        }
    }

    private fun isEsimEnabledAtTargetSlotPort(physicalSlotIndex: Int, portIndex: Int): Boolean {
        val logicalSlotId = getLogicalSlotIndex(physicalSlotIndex, portIndex)
        if (logicalSlotId == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            return false
        }
        return activeSubInfos != null
                && activeSubInfos!!.stream()
            .anyMatch { i: SubscriptionInfo -> i.isEmbedded && i.simSlotIndex == logicalSlotId }
    }

    private fun getLogicalSlotIndex(physicalSlotIndex: Int, portIndex: Int): Int {
        val slotInfos: Array<UiccSlotInfo?>? = telephonyManager.getUiccSlotsInfo()
        if (slotInfos != null && physicalSlotIndex >= 0
            && physicalSlotIndex < slotInfos.size && slotInfos[physicalSlotIndex] != null
        ) {
            for (portInfo in slotInfos[physicalSlotIndex]!!.ports) {
                if (portInfo.portIndex == portIndex) {
                    return portInfo.logicalSlotIndex
                }
            }
        }

        return SubscriptionManager.INVALID_SIM_SLOT_INDEX
    }

    private fun createCallbackIntent(action: String): PendingIntent {
        euiccOpId = currentOpId.incrementAndGet()
        val intent = Intent(action)
        intent.putExtra(EXTRA_OP_ID, euiccOpId)
        return PendingIntent.getBroadcast(
            application, REQUEST_CODE, intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun onEuiccSimSwitchingCallback(resultCode: Int) {
        Log.d(TAG, "onReceive: onEuiccSimSwitchingCallback $resultCode")

        when {
            resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK
                    && isDuringSimSlotMapping -> {
                // Continue to switch the SimSlotMapping, after the esim is disabled.
                isDuringSimSlotMapping = false
                if (currentSimSwitchingInfo.isEmbedded) {
                    startEuiccSlotSwitching(
                        currentSimSwitchingInfo.targetPhysicalSlotId,
                        currentSimSwitchingInfo.targetPortId,
                        currentSimSwitchingInfo.removedSubInfo
                    )
                } else {
                    startRemovableSlotSwitching(
                        currentSimSwitchingInfo.targetPhysicalSlotId,
                        currentSimSwitchingInfo.removedSubInfo
                    )
                }
            }

            resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK -> {
                _uiState.value = SwitchingState.COMPLETED
            }

            else -> {
                _uiState.value = SwitchingState.FAILED
            }
        }
    }

    companion object {
        private const val TAG = "SimOnboardingVM"
        private const val ACTION_SWITCH_TO_SUBSCRIPTION: String =
            "com.android.settings.network.SWITCH_TO_SUBSCRIPTION"
        private const val REQUEST_CODE: Int = 0
        private const val EXTRA_OP_ID: String = "op_id"
        private val currentOpId: AtomicInteger =
            AtomicInteger(SystemClock.elapsedRealtime().toInt())
    }
}