package es.c0n1j.blocfone.domain

enum class BlockingMode {
    ALL_INCOMING,
    UNKNOWN_NUMBERS,
    SELECTED_NUMBERS,
}

data class ScreeningRules(
    val isBlockingEnabled: Boolean = true,
    val mode: BlockingMode = BlockingMode.UNKNOWN_NUMBERS,
    val selectedNumbers: Set<String> = emptySet(),
    val allowedNumbers: Set<String> = emptySet(),
)

data class IncomingCall(
    val normalizedNumber: String?,
    val isInContacts: Boolean?,
)

enum class ScreeningDecision {
    ALLOW,
    REJECT,
}

object CallRuleEvaluator {
    fun evaluate(rules: ScreeningRules, call: IncomingCall): ScreeningDecision {
        if (!rules.isBlockingEnabled) return ScreeningDecision.ALLOW

        val shouldReject = when (rules.mode) {
            BlockingMode.ALL_INCOMING ->
                call.normalizedNumber == null || call.normalizedNumber !in rules.allowedNumbers
            BlockingMode.UNKNOWN_NUMBERS ->
                call.normalizedNumber != null && call.isInContacts == false
            BlockingMode.SELECTED_NUMBERS ->
                call.normalizedNumber != null && call.normalizedNumber in rules.selectedNumbers
        }

        return if (shouldReject) ScreeningDecision.REJECT else ScreeningDecision.ALLOW
    }
}
