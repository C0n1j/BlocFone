package es.c0n1j.blocfone.domain

object NumberInputValidator {
    fun accepts(rawNumber: String): Boolean {
        val trimmed = rawNumber.trim(' ')
        return trimmed.isNotEmpty() && trimmed.all { it.isAllowedNumberCharacter() }
    }

    fun hasMinimumDigits(normalizedNumber: String): Boolean =
        normalizedNumber.count(Char::isDigit) >= MINIMUM_DIGITS

    private fun Char.isAllowedNumberCharacter(): Boolean =
        isDigit() || this == '+' || this == '-' || this == '(' || this == ')' || this == ' '

    private const val MINIMUM_DIGITS = 3
}
