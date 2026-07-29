package es.c0n1j.blocfone.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import es.c0n1j.blocfone.domain.BlockingMode
import es.c0n1j.blocfone.domain.PhoneNumberCanonicalizer
import es.c0n1j.blocfone.domain.ScreeningRules
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

private val Context.settingsDataStore by preferencesDataStore(name = "screening_settings")

data class ContactRef(
    val lookupKey: String,
    val contactId: Long,
    val displayName: String,
)

data class ContactContribution(
    val contact: ContactRef,
    val numbers: Set<String>,
)

data class ContactExceptions(
    val manualNumbers: Set<String> = emptySet(),
    val contactRefs: Set<ContactRef> = emptySet(),
    val contactContributions: Set<ContactContribution> = emptySet(),
    val allowedNumbers: Set<String> = emptySet(),
    val contactsRevision: Long = 0L,
) {
    val contacts: Set<ContactRef> = contactRefs + contactContributions.mapTo(mutableSetOf()) { it.contact }
}

data class SettingsSnapshot(
    val rules: ScreeningRules,
    val contactExceptions: ContactExceptions,
)

sealed interface SettingsReadState {
    val lastValid: SettingsSnapshot?

    data class Loading(override val lastValid: SettingsSnapshot?) : SettingsReadState
    data class Data(val value: SettingsSnapshot) : SettingsReadState {
        override val lastValid: SettingsSnapshot = value
    }
    data class Error(
        override val lastValid: SettingsSnapshot?,
        val cause: Throwable,
    ) : SettingsReadState
}

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore
    private val readRetryController = ReadRetryController()

    val settings: Flow<SettingsReadState> = readRetryController.attempts.flatMapLatest {
        dataStore.data
            .map { preferences ->
                val state: SettingsReadState = SettingsReadState.Data(updateLastValid(preferences))
                state
            }
            .catch { cause ->
                if (cause is CancellationException) throw cause
                emit(SettingsReadState.Error(LAST_VALID_SETTINGS.current(), cause))
            }
            .onStart { emit(SettingsReadState.Loading(LAST_VALID_SETTINGS.current())) }
    }

    fun retrySettingsRead() {
        readRetryController.retry()
    }

    suspend fun snapshot(): ScreeningRules = try {
        updateLastValid(dataStore.data.first()).rules
    } catch (cause: IOException) {
        LAST_VALID_SETTINGS.current()?.rules ?: throw cause
    }

    suspend fun contactExceptionsSnapshot(): ContactExceptions = try {
        updateLastValid(dataStore.data.first()).contactExceptions
    } catch (cause: IOException) {
        LAST_VALID_SETTINGS.current()?.contactExceptions ?: throw cause
    }

    private fun updateLastValid(preferences: Preferences): SettingsSnapshot =
        LAST_VALID_SETTINGS.update(
            SettingsSnapshot(
                rules = toRules(preferences),
                contactExceptions = toContactExceptions(preferences),
            ),
        )

    private suspend fun editAndRefresh(transform: suspend (MutablePreferences) -> Unit) {
        updateLastValid(dataStore.edit(transform))
    }

    suspend fun setMode(mode: BlockingMode) {
        editAndRefresh { preferences ->
            preferences[MODE_KEY] = mode.name
        }
    }

    suspend fun setBlockingEnabled(enabled: Boolean) {
        editAndRefresh { preferences ->
            preferences[BLOCKING_ENABLED_KEY] = enabled
        }
    }

    suspend fun addNumber(rawNumber: String): Boolean {
        val canonical = PhoneNumberCanonicalizer.canonicalize(rawNumber) ?: return false
        editAndRefresh { preferences ->
            val current = PhoneNumberCanonicalizer.canonicalizeStored(
                preferences[SELECTED_NUMBERS_KEY].orEmpty(),
            )
            preferences[SELECTED_NUMBERS_KEY] = current + canonical
        }
        return true
    }

    suspend fun removeNumber(number: String) {
        editAndRefresh { preferences ->
            preferences[SELECTED_NUMBERS_KEY] = PhoneNumberCanonicalizer.removeIdentity(
                preferences[SELECTED_NUMBERS_KEY].orEmpty(),
                number,
            )
        }
    }

    suspend fun addManualAllowedNumber(rawNumber: String): Boolean {
        val canonical = PhoneNumberCanonicalizer.canonicalize(rawNumber) ?: return false
        editAndRefresh { preferences ->
            val manualNumbers = PhoneNumberCanonicalizer.canonicalizeStored(
                preferences[MANUAL_ALLOWED_NUMBERS_KEY].orEmpty(),
            ) + canonical
            preferences[MANUAL_ALLOWED_NUMBERS_KEY] = manualNumbers
            preferences[ALLOWED_NUMBERS_KEY] = manualNumbers + contributionNumbers(preferences)
        }
        return true
    }

    suspend fun removeManualAllowedNumber(number: String) {
        editAndRefresh { preferences ->
            val manualNumbers = PhoneNumberCanonicalizer.removeIdentity(
                preferences[MANUAL_ALLOWED_NUMBERS_KEY].orEmpty(),
                number,
            )
            preferences[MANUAL_ALLOWED_NUMBERS_KEY] = manualNumbers
            preferences[ALLOWED_NUMBERS_KEY] = manualNumbers + contributionNumbers(preferences)
        }
    }

    suspend fun addContactRef(contact: ContactRef) {
        require(contact.lookupKey.isNotBlank()) { "Contact lookup key cannot be blank" }
        require(contact.contactId >= 0) { "Contact ID cannot be negative" }
        editAndRefresh { preferences ->
            val refs = decodedContactRefs(preferences)
                .filterNot { it.lookupKey == contact.lookupKey }
                .toSet() + contact
            preferences[CONTACT_REFS_KEY] = refs.mapTo(mutableSetOf(), ContactExceptionCodec::encodeRef)
            preferences[CONTACTS_REVISION_KEY] = currentRevision(preferences) + 1
        }
    }

    suspend fun removeContactRef(lookupKey: String) {
        editAndRefresh { preferences ->
            val refs = decodedContactRefs(preferences).filterNot { it.lookupKey == lookupKey }.toSet()
            val contributions = preferences[CONTACT_CONTRIBUTIONS_KEY]
                .orEmpty()
                .filterNot { ContactExceptionCodec.decodeContribution(it)?.first == lookupKey }
                .toSet()
            preferences[CONTACT_REFS_KEY] = refs.mapTo(mutableSetOf(), ContactExceptionCodec::encodeRef)
            preferences[CONTACT_CONTRIBUTIONS_KEY] = contributions
            val manualNumbers = PhoneNumberCanonicalizer.canonicalizeStored(
                preferences[MANUAL_ALLOWED_NUMBERS_KEY].orEmpty(),
            )
            preferences[MANUAL_ALLOWED_NUMBERS_KEY] = manualNumbers
            preferences[ALLOWED_NUMBERS_KEY] =
                manualNumbers + contributionNumbers(contributions)
            preferences[CONTACTS_REVISION_KEY] = currentRevision(preferences) + 1
        }
    }

    suspend fun applyContactContributions(
        expectedRevision: Long,
        contributions: Set<ContactContribution>,
    ): Boolean {
        var applied = false
        editAndRefresh { preferences ->
            val currentRevision = currentRevision(preferences)
            if (currentRevision != expectedRevision) return@editAndRefresh

            val normalizedContributions = contributions.map { contribution ->
                require(contribution.contact.lookupKey.isNotBlank()) {
                    "Contact lookup key cannot be blank"
                }
                require(contribution.contact.contactId >= 0) { "Contact ID cannot be negative" }
                ContactContribution(
                    contact = contribution.contact,
                    numbers = PhoneNumberCanonicalizer.canonicalizeStored(contribution.numbers),
                )
            }.toSet()
            val refs = normalizedContributions.mapTo(mutableSetOf()) { it.contact }
            val encodedContributions = normalizedContributions
                .flatMapTo(mutableSetOf()) { ContactExceptionCodec.encodeContribution(it) }
            val manualNumbers = PhoneNumberCanonicalizer.canonicalizeStored(
                preferences[MANUAL_ALLOWED_NUMBERS_KEY].orEmpty(),
            )

            preferences[MANUAL_ALLOWED_NUMBERS_KEY] = manualNumbers
            preferences[CONTACT_REFS_KEY] = refs.mapTo(mutableSetOf(), ContactExceptionCodec::encodeRef)
            preferences[CONTACT_CONTRIBUTIONS_KEY] = encodedContributions
            preferences[ALLOWED_NUMBERS_KEY] = manualNumbers + contributionNumbers(encodedContributions)
            preferences[CONTACTS_REVISION_KEY] = currentRevision + 1
            applied = true
        }
        return applied
    }

    private fun toRules(preferences: Preferences): ScreeningRules {
        val mode = preferences[MODE_KEY]
            ?.let { stored -> BlockingMode.entries.firstOrNull { it.name == stored } }
            ?: BlockingMode.UNKNOWN_NUMBERS
        val numbers = PhoneNumberCanonicalizer.canonicalizeStored(
            preferences[SELECTED_NUMBERS_KEY].orEmpty(),
        )
        return ScreeningRules(
            isBlockingEnabled = preferences[BLOCKING_ENABLED_KEY] ?: true,
            mode = mode,
            selectedNumbers = numbers,
            allowedNumbers = PhoneNumberCanonicalizer.canonicalizeStored(
                preferences[ALLOWED_NUMBERS_KEY].orEmpty(),
            ),
        )
    }

    private fun toContactExceptions(preferences: Preferences): ContactExceptions {
        val refsByLookupKey = decodedContactRefs(preferences).associateBy(ContactRef::lookupKey)
        val numbersByLookupKey = preferences[CONTACT_CONTRIBUTIONS_KEY]
            .orEmpty()
            .mapNotNull(ContactExceptionCodec::decodeContribution)
            .groupBy({ it.first }, { it.second })
        val contributions = refsByLookupKey.values.mapTo(mutableSetOf()) { contact ->
            ContactContribution(
                contact = contact,
                numbers = numbersByLookupKey[contact.lookupKey].orEmpty().toSet(),
            )
        }
        return ContactExceptions(
            manualNumbers = PhoneNumberCanonicalizer.canonicalizeStored(
                preferences[MANUAL_ALLOWED_NUMBERS_KEY].orEmpty(),
            ),
            contactRefs = refsByLookupKey.values.toSet(),
            contactContributions = contributions,
            allowedNumbers = PhoneNumberCanonicalizer.canonicalizeStored(
                preferences[ALLOWED_NUMBERS_KEY].orEmpty(),
            ),
            contactsRevision = currentRevision(preferences),
        )
    }

    private fun decodedContactRefs(preferences: Preferences): Set<ContactRef> =
        preferences[CONTACT_REFS_KEY]
            .orEmpty()
            .mapNotNull(ContactExceptionCodec::decodeRef)
            .toSet()

    private fun contributionNumbers(preferences: Preferences): Set<String> =
        contributionNumbers(preferences[CONTACT_CONTRIBUTIONS_KEY].orEmpty())

    private fun contributionNumbers(encodedContributions: Set<String>): Set<String> =
        encodedContributions.mapNotNull(ContactExceptionCodec::decodeContribution)
            .mapTo(mutableSetOf()) { it.second }

    private fun currentRevision(preferences: Preferences): Long =
        preferences[CONTACTS_REVISION_KEY] ?: 0L

    companion object {
        private val BLOCKING_ENABLED_KEY = booleanPreferencesKey("blocking_enabled")
        private val MODE_KEY = stringPreferencesKey("blocking_mode")
        private val SELECTED_NUMBERS_KEY = stringSetPreferencesKey("selected_numbers")
        private val MANUAL_ALLOWED_NUMBERS_KEY = stringSetPreferencesKey("manual_allowed_numbers")
        private val CONTACT_REFS_KEY = stringSetPreferencesKey("allowed_contact_refs")
        private val CONTACT_CONTRIBUTIONS_KEY = stringSetPreferencesKey("contact_allowed_numbers")
        private val ALLOWED_NUMBERS_KEY = stringSetPreferencesKey("allowed_numbers")
        private val CONTACTS_REVISION_KEY = longPreferencesKey("allowed_contacts_revision")
        // Shared by UI and short-lived screening-service repository instances.
        private val LAST_VALID_SETTINGS = LastValidSnapshot<SettingsSnapshot>()

    }
}

internal object ContactExceptionCodec {
    fun encodeRef(contact: ContactRef): String = listOf(
        VERSION,
        encode(contact.lookupKey),
        encode(contact.contactId.toString()),
        encode(contact.displayName),
    ).joinToString(SEPARATOR)

    fun decodeRef(value: String): ContactRef? = runCatching {
        val fields = value.split(SEPARATOR)
        if (fields.size != REF_FIELD_COUNT || fields.first() != VERSION) return null
        val lookupKey = decode(fields[1]).takeIf(String::isNotBlank) ?: return null
        val contactId = decode(fields[2]).toLongOrNull()?.takeIf { it >= 0 } ?: return null
        ContactRef(
            lookupKey = lookupKey,
            contactId = contactId,
            displayName = decode(fields[3]),
        )
    }.getOrNull()

    fun encodeContribution(contribution: ContactContribution): Set<String> =
        contribution.numbers.mapTo(mutableSetOf()) { number ->
            listOf(VERSION, encode(contribution.contact.lookupKey), encode(number)).joinToString(SEPARATOR)
        }

    fun decodeContribution(value: String): Pair<String, String>? = runCatching {
        val fields = value.split(SEPARATOR)
        if (fields.size != CONTRIBUTION_FIELD_COUNT || fields.first() != VERSION) return null
        val lookupKey = decode(fields[1]).takeIf(String::isNotBlank) ?: return null
        val number = PhoneNumberCanonicalizer.identity(decode(fields[2])) ?: return null
        lookupKey to number
    }.getOrNull()

    private fun encode(value: String): String = ENCODER.encodeToString(
        value.toByteArray(StandardCharsets.UTF_8),
    )

    private fun decode(value: String): String = String(
        DECODER.decode(value),
        StandardCharsets.UTF_8,
    )

    private const val VERSION = "v1"
    private const val SEPARATOR = "|"
    private const val REF_FIELD_COUNT = 4
    private const val CONTRIBUTION_FIELD_COUNT = 3
    private val ENCODER = Base64.getUrlEncoder().withoutPadding()
    private val DECODER = Base64.getUrlDecoder()
}
