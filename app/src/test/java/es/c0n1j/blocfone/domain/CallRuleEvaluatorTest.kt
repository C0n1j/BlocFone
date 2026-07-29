package es.c0n1j.blocfone.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CallRuleEvaluatorTest {
    @Test
    fun `disabled blocking allows all incoming mode`() {
        val rules = ScreeningRules(
            isBlockingEnabled = false,
            mode = BlockingMode.ALL_INCOMING,
        )

        assertEquals(
            ScreeningDecision.ALLOW,
            CallRuleEvaluator.evaluate(
                rules = rules,
                call = IncomingCall(normalizedNumber = "+541155551234", isInContacts = false),
            ),
        )
    }

    @Test
    fun `disabled blocking allows unknown numbers mode`() {
        val rules = ScreeningRules(
            isBlockingEnabled = false,
            mode = BlockingMode.UNKNOWN_NUMBERS,
        )

        assertEquals(
            ScreeningDecision.ALLOW,
            CallRuleEvaluator.evaluate(
                rules = rules,
                call = IncomingCall(normalizedNumber = "+541155551234", isInContacts = false),
            ),
        )
    }

    @Test
    fun `disabled blocking allows selected numbers mode`() {
        val number = "+541155551234"
        val rules = ScreeningRules(
            isBlockingEnabled = false,
            mode = BlockingMode.SELECTED_NUMBERS,
            selectedNumbers = setOf(number),
        )

        assertEquals(
            ScreeningDecision.ALLOW,
            CallRuleEvaluator.evaluate(
                rules = rules,
                call = IncomingCall(normalizedNumber = number, isInContacts = null),
            ),
        )
    }

    @Test
    fun `enabled blocking keeps configured mode behavior`() {
        val number = "+541155551234"
        val rules = ScreeningRules(
            isBlockingEnabled = true,
            mode = BlockingMode.SELECTED_NUMBERS,
            selectedNumbers = setOf(number),
        )

        assertEquals(
            ScreeningDecision.REJECT,
            CallRuleEvaluator.evaluate(
                rules = rules,
                call = IncomingCall(normalizedNumber = number, isInContacts = null),
            ),
        )
    }

    @Test
    fun `all incoming rejects calls with and without a visible number`() {
        val rules = ScreeningRules(mode = BlockingMode.ALL_INCOMING)

        assertEquals(
            ScreeningDecision.REJECT,
            CallRuleEvaluator.evaluate(rules, IncomingCall("+541155551234", true)),
        )
        assertEquals(
            ScreeningDecision.REJECT,
            CallRuleEvaluator.evaluate(rules, IncomingCall(null, null)),
        )
    }

    @Test
    fun `all incoming compares exact canonical identities`() {
        val allowedNumber = PhoneNumberCanonicalizer.canonicalize("612 345 678")!!
        val rules = ScreeningRules(
            mode = BlockingMode.ALL_INCOMING,
            allowedNumbers = setOf(allowedNumber),
        )

        assertEquals(
            ScreeningDecision.ALLOW,
            CallRuleEvaluator.evaluate(rules, IncomingCall(allowedNumber, null)),
        )
        assertEquals(
            ScreeningDecision.ALLOW,
            CallRuleEvaluator.evaluate(
                rules,
                IncomingCall(PhoneNumberCanonicalizer.canonicalize("+34 612-345-678"), null),
            ),
        )
        assertEquals(
            ScreeningDecision.REJECT,
            CallRuleEvaluator.evaluate(rules, IncomingCall("+34612345679", null)),
        )
        assertEquals(
            ScreeningDecision.REJECT,
            CallRuleEvaluator.evaluate(rules, IncomingCall(null, null)),
        )
    }

    @Test
    fun `allowed exceptions do not change unknown or selected modes`() {
        val number = "+541155551234"

        assertEquals(
            ScreeningDecision.REJECT,
            CallRuleEvaluator.evaluate(
                ScreeningRules(mode = BlockingMode.UNKNOWN_NUMBERS, allowedNumbers = setOf(number)),
                IncomingCall(number, false),
            ),
        )
        assertEquals(
            ScreeningDecision.REJECT,
            CallRuleEvaluator.evaluate(
                ScreeningRules(mode = BlockingMode.SELECTED_NUMBERS, allowedNumbers = setOf(number)),
                IncomingCall(number, null),
            ),
        )
    }

    @Test
    fun `unknown mode rejects only a known number absent from contacts`() {
        val rules = ScreeningRules(mode = BlockingMode.UNKNOWN_NUMBERS)

        assertEquals(
            ScreeningDecision.REJECT,
            CallRuleEvaluator.evaluate(rules, IncomingCall("+541155551234", false)),
        )
        assertEquals(
            ScreeningDecision.ALLOW,
            CallRuleEvaluator.evaluate(rules, IncomingCall("+541155551234", true)),
        )
        assertEquals(
            ScreeningDecision.ALLOW,
            CallRuleEvaluator.evaluate(rules, IncomingCall(null, null)),
        )
        assertEquals(
            ScreeningDecision.ALLOW,
            CallRuleEvaluator.evaluate(rules, IncomingCall("+541155551234", null)),
        )
    }

    @Test
    fun `selected mode requires an exact normalized match`() {
        val rules = ScreeningRules(
            mode = BlockingMode.SELECTED_NUMBERS,
            selectedNumbers = setOf("+541155551234"),
        )

        assertEquals(
            ScreeningDecision.REJECT,
            CallRuleEvaluator.evaluate(rules, IncomingCall("+541155551234", null)),
        )
        assertEquals(
            ScreeningDecision.ALLOW,
            CallRuleEvaluator.evaluate(rules, IncomingCall("1155551234", null)),
        )
        assertEquals(
            ScreeningDecision.ALLOW,
            CallRuleEvaluator.evaluate(rules, IncomingCall("55551234", null)),
        )
    }
}
