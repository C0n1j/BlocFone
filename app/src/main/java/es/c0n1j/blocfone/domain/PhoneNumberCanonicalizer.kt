package es.c0n1j.blocfone.domain

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

object PhoneNumberCanonicalizer {
    private const val DEFAULT_REGION = "ES"
    private val phoneNumberUtil = PhoneNumberUtil.getInstance()

    fun canonicalize(rawNumber: String): String? {
        if (!NumberInputValidator.accepts(rawNumber)) return null

        val parsed = try {
            phoneNumberUtil.parse(rawNumber.trim(' '), DEFAULT_REGION)
        } catch (_: NumberParseException) {
            return null
        }
        if (!phoneNumberUtil.isValidNumber(parsed)) return null

        return phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
    }

    fun identity(rawNumber: String): String? = canonicalize(rawNumber) ?: exactSyntax(rawNumber)

    fun canonicalizeStored(numbers: Iterable<String>): Set<String> =
        numbers.mapNotNull(::identity).toSet()

    fun removeIdentity(numbers: Iterable<String>, number: String): Set<String> {
        val identityToRemove = identity(number) ?: return canonicalizeStored(numbers)
        return numbers.mapNotNull(::identity).filterNotTo(mutableSetOf()) { it == identityToRemove }
    }

    private fun exactSyntax(rawNumber: String): String? {
        val exact = rawNumber.trim(' ')
        return exact.takeIf(String::isNotEmpty)
    }
}
