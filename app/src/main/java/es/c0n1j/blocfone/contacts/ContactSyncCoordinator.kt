package es.c0n1j.blocfone.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import es.c0n1j.blocfone.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ContactSyncState {
    data object Idle : ContactSyncState
    data object Syncing : ContactSyncState
    data object Synced : ContactSyncState
    data object PermissionRequired : ContactSyncState
    data class Stale(val cause: Throwable) : ContactSyncState
}

class ContactSyncCoordinator(
    context: Context,
    private val repository: SettingsRepository,
    private val contactsDataSource: ContactsDataSource,
    private val scope: CoroutineScope,
) : DefaultLifecycleObserver {
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver
    private val syncMutex = Mutex()
    private val mutableState = MutableStateFlow<ContactSyncState>(ContactSyncState.Idle)
    private var lifecycleStarted = false
    private var observerRegistered = false
    private var triggerChannel: Channel<Unit>? = null
    private var syncJob: Job? = null

    val state: StateFlow<ContactSyncState> = mutableState.asStateFlow()

    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            requestSync()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        lifecycleStarted = true
        resumeIfPermitted()
    }

    override fun onStop(owner: LifecycleOwner) {
        lifecycleStarted = false
        pause(ContactSyncState.Idle)
    }

    fun onPermissionChanged() {
        if (!lifecycleStarted) return
        resumeIfPermitted()
    }

    fun requestSync() {
        triggerChannel?.trySend(Unit)
    }

    private fun resumeIfPermitted() {
        if (!hasContactsPermission()) {
            pause(ContactSyncState.PermissionRequired)
            return
        }
        if (syncJob?.isActive == true) {
            requestSync()
            return
        }

        try {
            registerObserver()
        } catch (_: SecurityException) {
            pause(ContactSyncState.PermissionRequired)
            return
        } catch (error: Exception) {
            pause(ContactSyncState.Stale(error))
            return
        }
        val channel = Channel<Unit>(Channel.CONFLATED)
        triggerChannel = channel
        syncJob = scope.launch {
            for (ignored in channel) {
                delay(SYNC_DEBOUNCE_MILLIS)
                while (channel.tryReceive().isSuccess) {
                    // Collapse all provider changes observed during the debounce window.
                }
                synchronizeWithRetry()
            }
        }
        channel.trySend(Unit)
    }

    private suspend fun synchronizeWithRetry() {
        var attempt = 0
        while (scope.isActive) {
            try {
                syncMutex.withLock {
                    mutableState.value = ContactSyncState.Syncing
                    val current = repository.contactExceptionsSnapshot()
                    val snapshot = contactsDataSource.resolveAll(current.contacts)
                    val applied = repository.applyContactContributions(
                        expectedRevision = current.contactsRevision,
                        contributions = snapshot.contributions,
                    )
                    if (!applied) {
                        requestSync()
                        return
                    }
                    mutableState.value = ContactSyncState.Synced
                }
                return
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (securityException: SecurityException) {
                pause(ContactSyncState.PermissionRequired)
                return
            } catch (error: Exception) {
                if (!hasContactsPermission()) {
                    pause(ContactSyncState.PermissionRequired)
                    return
                }
                attempt += 1
                if (attempt >= MAX_SYNC_ATTEMPTS) {
                    mutableState.value = ContactSyncState.Stale(error)
                    return
                }
                delay(RETRY_BASE_DELAY_MILLIS * attempt)
            }
        }
    }

    private fun registerObserver() {
        if (observerRegistered) return
        contentResolver.registerContentObserver(Contacts.CONTENT_URI, true, contactsObserver)
        try {
            contentResolver.registerContentObserver(Phone.CONTENT_URI, true, contactsObserver)
            observerRegistered = true
        } catch (error: Exception) {
            contentResolver.unregisterContentObserver(contactsObserver)
            throw error
        }
    }

    private fun pause(nextState: ContactSyncState) {
        if (observerRegistered) {
            contentResolver.unregisterContentObserver(contactsObserver)
            observerRegistered = false
        }
        triggerChannel?.close()
        triggerChannel = null
        syncJob?.cancel()
        syncJob = null
        mutableState.value = nextState
    }

    private fun hasContactsPermission(): Boolean = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.READ_CONTACTS,
    ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val SYNC_DEBOUNCE_MILLIS = 300L
        const val RETRY_BASE_DELAY_MILLIS = 500L
        const val MAX_SYNC_ATTEMPTS = 3
    }
}
