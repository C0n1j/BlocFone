package es.c0n1j.blocfone.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CallRuleEvaluatorTest {
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
