package es.c0n1j.blocfone.data

import android.content.Context
import android.telephony.PhoneNumberUtils
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import es.c0n1j.blocfone.domain.BlockingMode
import es.c0n1j.blocfone.domain.NumberInputValidator
import es.c0n1j.blocfone.domain.ScreeningRules
import java.io.IOException
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

private val Context.settingsDataStore by preferencesDataStore(name = "screening_settings")

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    private val resilientData = dataStore.data.retryWhen { cause, attempt ->
        if (cause !is IOException) return@retryWhen false
        delay(min((attempt + 1) * RETRY_DELAY_MILLIS, MAX_RETRY_DELAY_MILLIS))
        true
    }

    val rules: Flow<ScreeningRules> = resilientData.map(::toRules)

    suspend fun snapshot(): ScreeningRules = toRules(resilientData.first())

    suspend fun setMode(mode: BlockingMode) {
        dataStore.edit { preferences ->
            preferences[MODE_KEY] = mode.name
        }
    }

    suspend fun addNumber(rawNumber: String): Boolean {
        val normalized = normalizeNumber(rawNumber) ?: return false
        dataStore.edit { preferences ->
            val current = preferences[SELECTED_NUMBERS_KEY].orEmpty().toSet()
            preferences[SELECTED_NUMBERS_KEY] = current + normalized
        }
        return true
    }

    suspend fun removeNumber(normalizedNumber: String) {
        dataStore.edit { preferences ->
            val current = preferences[SELECTED_NUMBERS_KEY].orEmpty().toSet()
            preferences[SELECTED_NUMBERS_KEY] = current - normalizedNumber
        }
    }

    private fun toRules(preferences: Preferences): ScreeningRules {
        val mode = preferences[MODE_KEY]
            ?.let { stored -> BlockingMode.entries.firstOrNull { it.name == stored } }
            ?: BlockingMode.UNKNOWN_NUMBERS
        val numbers = preferences[SELECTED_NUMBERS_KEY]
            .orEmpty()
            .mapNotNull(::normalizeNumber)
            .toSet()
        return ScreeningRules(mode, numbers)
    }

    companion object {
        private val MODE_KEY = stringPreferencesKey("blocking_mode")
        private val SELECTED_NUMBERS_KEY = stringSetPreferencesKey("selected_numbers")
        private const val RETRY_DELAY_MILLIS = 250L
        private const val MAX_RETRY_DELAY_MILLIS = 2_000L

        fun normalizeNumber(rawNumber: String): String? {
            if (!NumberInputValidator.accepts(rawNumber)) return null

            val normalized = PhoneNumberUtils.normalizeNumber(rawNumber.trim(' '))
            return normalized.takeIf(NumberInputValidator::hasMinimumDigits)
        }
    }
}
