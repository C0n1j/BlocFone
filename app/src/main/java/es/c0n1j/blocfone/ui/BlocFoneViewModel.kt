package es.c0n1j.blocfone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.c0n1j.blocfone.contacts.ContactCandidate
import es.c0n1j.blocfone.contacts.ContactSyncCoordinator
import es.c0n1j.blocfone.contacts.ContactSyncState
import es.c0n1j.blocfone.contacts.ContactsDataSource
import es.c0n1j.blocfone.data.ContactExceptions
import es.c0n1j.blocfone.data.ContactRef
import es.c0n1j.blocfone.data.SettingsRepository
import es.c0n1j.blocfone.data.SettingsReadState
import es.c0n1j.blocfone.domain.BlockingMode
import es.c0n1j.blocfone.domain.ScreeningRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BlocFoneUiState(
    val rules: ScreeningRules = ScreeningRules(),
    val exceptions: ContactExceptions = ContactExceptions(),
    val syncState: ContactSyncState = ContactSyncState.Idle,
    val hasSettings: Boolean = false,
    val isLoading: Boolean = true,
    val settingsError: String? = null,
    val error: String? = null,
    val picker: ContactPickerState = ContactPickerState.Hidden,
)

sealed interface ContactPickerState {
    data object Hidden : ContactPickerState
    data object Loading : ContactPickerState
    data class Results(val contacts: List<ContactCandidate>) : ContactPickerState
    data class Error(val cause: Throwable) : ContactPickerState
}

class BlocFoneViewModel(
    private val repository: SettingsRepository,
    private val contactsDataSource: ContactsDataSource,
    private val contactSyncCoordinator: ContactSyncCoordinator,
) : ViewModel() {
    private val picker = MutableStateFlow<ContactPickerState>(ContactPickerState.Hidden)
    private val operationError = MutableStateFlow<String?>(null)

    val state: StateFlow<BlocFoneUiState> = combine(
        repository.settings,
        contactSyncCoordinator.state,
        picker,
        operationError,
    ) { settingsState, syncState, pickerState, error ->
        val snapshot = settingsState.lastValid
        BlocFoneUiState(
            rules = snapshot?.rules ?: ScreeningRules(),
            exceptions = snapshot?.contactExceptions ?: ContactExceptions(),
            syncState = syncState,
            hasSettings = snapshot != null,
            isLoading = settingsState is SettingsReadState.Loading,
            settingsError = (settingsState as? SettingsReadState.Error)?.cause?.displayMessage(),
            error = error,
            picker = pickerState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlocFoneUiState())

    fun setBlockingEnabled(enabled: Boolean) = update { repository.setBlockingEnabled(enabled) }

    fun setMode(mode: BlockingMode) = update { repository.setMode(mode) }

    fun onContactsPermissionChanged(granted: Boolean) {
        contactSyncCoordinator.onPermissionChanged()
        if (granted) retrySync()
    }

    fun retrySync() {
        operationError.value = null
        contactSyncCoordinator.requestSync()
    }

    fun retrySettingsRead() {
        repository.retrySettingsRead()
    }

    fun openContactPicker() = searchContacts("")

    fun dismissContactPicker() {
        picker.value = ContactPickerState.Hidden
    }

    fun searchContacts(query: String) {
        picker.value = ContactPickerState.Loading
        viewModelScope.launch {
            try {
                val candidates = contactsDataSource.listContacts(query)
                picker.value = ContactPickerState.Results(candidates)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                picker.value = ContactPickerState.Error(error)
            }
        }
    }

    fun selectContact(contact: ContactRef) = update {
        repository.addContactRef(contact)
        contactSyncCoordinator.requestSync()
        picker.value = ContactPickerState.Hidden
    }

    fun removeContact(lookupKey: String) = update {
        repository.removeContactRef(lookupKey)
        contactSyncCoordinator.requestSync()
    }

    fun addManualNumber(rawNumber: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val added = repository.addManualAllowedNumber(rawNumber)
                if (added) operationError.value = null
                onResult(added)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                operationError.value = error.message
                onResult(false)
            }
        }
    }

    fun removeManualNumber(number: String) = update { repository.removeManualAllowedNumber(number) }

    fun addSelectedNumber(rawNumber: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(repository.addNumber(rawNumber))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                operationError.value = error.message
                onResult(false)
            }
        }
    }

    fun removeSelectedNumber(number: String) = update { repository.removeNumber(number) }

    private fun update(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                operationError.value = null
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                operationError.value = error.message
            }
        }
    }

    companion object {
        fun factory(
            repository: SettingsRepository,
            contactsDataSource: ContactsDataSource,
            contactSyncCoordinator: ContactSyncCoordinator,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { BlocFoneViewModel(repository, contactsDataSource, contactSyncCoordinator) }
        }
    }
}

private fun Throwable.displayMessage(): String = message?.takeIf(String::isNotBlank)
    ?: javaClass.simpleName
