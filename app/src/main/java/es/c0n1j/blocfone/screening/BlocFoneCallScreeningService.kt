package es.c0n1j.blocfone.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.os.Handler
import android.os.HandlerThread
import es.c0n1j.blocfone.contacts.ContactLookup
import es.c0n1j.blocfone.data.SettingsRepository
import es.c0n1j.blocfone.domain.BlockingMode
import es.c0n1j.blocfone.domain.CallRuleEvaluator
import es.c0n1j.blocfone.domain.IncomingCall
import es.c0n1j.blocfone.domain.ScreeningDecision
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class BlocFoneCallScreeningService : CallScreeningService() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
    private val fallbackThread = HandlerThread("BlocFoneFallback").apply { start() }
    private val fallbackHandler = Handler(fallbackThread.looper)
    private val pendingCallsLock = Any()
    private val pendingCalls = mutableSetOf<PendingCall>()
    private var isDestroying = false

    override fun onScreenCall(callDetails: Call.Details) {
        val pendingCall = PendingCall(callDetails)
        val accepted = synchronized(pendingCallsLock) {
            if (isDestroying) {
                false
            } else {
                pendingCalls.add(pendingCall)
                fallbackHandler.postDelayed(pendingCall.fallback, INTERNAL_DEADLINE_MILLIS)

                val job = serviceScope.launch {
                    try {
                        val response = try {
                            evaluateCall(callDetails)
                        } catch (_: CancellationException) {
                            if (!serviceJob.isActive) return@launch
                            pendingCall.allowResponse
                        } catch (_: Throwable) {
                            pendingCall.allowResponse
                        }
                        runCatching { pendingCall.respondOnce(response) }
                    } finally {
                        pendingCall.cleanup()
                    }
                }
                pendingCall.evaluation.set(job)
                if (pendingCall.hasResponded()) job.cancel()
                true
            }
        }

        if (!accepted) {
            runCatching { pendingCall.respondOnce(pendingCall.allowResponse) }
        }
    }

    private suspend fun evaluateCall(callDetails: Call.Details): CallResponse {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            return CallResponse.Builder().build()
        }

        val rules = SettingsRepository(applicationContext).snapshot()
        val normalizedNumber = callDetails.handle
            ?.takeIf { it.scheme == "tel" }
            ?.schemeSpecificPart
            ?.let(SettingsRepository::normalizeNumber)

        val isInContacts = when {
            rules.mode != BlockingMode.UNKNOWN_NUMBERS -> null
            normalizedNumber == null -> null
            else -> ContactLookup(contentResolver).exists(normalizedNumber)
        }
        val decision = CallRuleEvaluator.evaluate(
            rules = rules,
            call = IncomingCall(normalizedNumber, isInContacts),
        )

        return when (decision) {
            ScreeningDecision.ALLOW -> CallResponse.Builder().build()
            ScreeningDecision.REJECT -> CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .build()
        }
    }

    private companion object {
        const val INTERNAL_DEADLINE_MILLIS = 3_500L
    }

    private inner class PendingCall(
        private val callDetails: Call.Details,
    ) {
        val allowResponse: CallResponse = CallResponse.Builder().build()
        val evaluation = AtomicReference<Job?>(null)
        private val responded = AtomicBoolean(false)
        private val cleanedUp = AtomicBoolean(false)
        val fallback = Runnable {
            runCatching { respondOnce(allowResponse) }
            evaluation.get()?.cancel()
            cleanup()
        }

        fun respondOnce(response: CallResponse): Boolean {
            if (!responded.compareAndSet(false, true)) return false
            respondToCall(callDetails, response)
            return true
        }

        fun hasResponded(): Boolean = responded.get()

        fun cleanup() {
            if (!cleanedUp.compareAndSet(false, true)) return
            fallbackHandler.removeCallbacks(fallback)
            synchronized(pendingCallsLock) {
                pendingCalls.remove(this)
            }
        }
    }

    override fun onDestroy() {
        val callsToFailOpen = synchronized(pendingCallsLock) {
            isDestroying = true
            pendingCalls.toList()
        }
        callsToFailOpen.forEach { pendingCall ->
            runCatching { pendingCall.respondOnce(pendingCall.allowResponse) }
        }
        fallbackHandler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        callsToFailOpen.forEach { it.cleanup() }
        fallbackThread.quitSafely()
        super.onDestroy()
    }
}
