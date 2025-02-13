package com.superwall.sdk.paywall.presentation.rule_logic

import android.content.Context
import com.superwall.sdk.config.ConfigManager
import com.superwall.sdk.dependencies.RuleAttributesFactory
import com.superwall.sdk.models.assignment.ConfirmableAssignment
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.triggers.Experiment
import com.superwall.sdk.models.triggers.InternalTriggerResult
import com.superwall.sdk.models.triggers.MatchedItem
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.models.triggers.TriggerResult
import com.superwall.sdk.models.triggers.TriggerRule
import com.superwall.sdk.models.triggers.TriggerRuleOccurrence
import com.superwall.sdk.models.triggers.TriggerRuleOutcome
import com.superwall.sdk.models.triggers.UnmatchedRule
import com.superwall.sdk.paywall.presentation.rule_logic.expression_evaluator.ExpressionEvaluator
import com.superwall.sdk.storage.Storage

data class RuleEvaluationOutcome(
    val confirmableAssignment: ConfirmableAssignment? = null,
    val unsavedOccurrence: TriggerRuleOccurrence? = null,
    val triggerResult: InternalTriggerResult
)

sealed class RuleMatchOutcome {
    data class Matched(val item: MatchedItem) : RuleMatchOutcome()
    data class NoMatchingRules(val unmatchedRules: List<UnmatchedRule>) : RuleMatchOutcome()
}

class RuleLogic(
    private val context: Context,
    private val configManager: ConfigManager,
    private val storage: Storage,
    private val factory: RuleAttributesFactory
) {
    suspend fun evaluateRules(event: EventData, triggers: Map<String, Trigger>): RuleEvaluationOutcome {
        val trigger = triggers[event.name] ?: return RuleEvaluationOutcome(triggerResult = InternalTriggerResult.EventNotFound)

        val ruleMatchOutcome = findMatchingRule(event, trigger)

        val matchedRuleItem: MatchedItem = when (ruleMatchOutcome) {
            is RuleMatchOutcome.Matched -> ruleMatchOutcome.item
            is RuleMatchOutcome.NoMatchingRules -> return RuleEvaluationOutcome(triggerResult = InternalTriggerResult.NoRuleMatch(ruleMatchOutcome.unmatchedRules))
        }

        val rule = matchedRuleItem.rule
        val confirmedAssignments = storage.getConfirmedAssignments()
        val variant: Experiment.Variant
        var confirmableAssignment: ConfirmableAssignment? = null

        variant = confirmedAssignments[rule.experiment.id]
            ?: configManager.unconfirmedAssignments[rule.experiment.id]
                    ?: run {
                return RuleEvaluationOutcome(triggerResult = InternalTriggerResult.Error(PaywallNotFoundException()))
            }

        if (variant !in confirmedAssignments.values) {
            confirmableAssignment = ConfirmableAssignment(rule.experiment.id, variant)
        }

        return when (variant.type) {
            Experiment.Variant.VariantType.HOLDOUT -> RuleEvaluationOutcome(
                confirmableAssignment = confirmableAssignment,
                unsavedOccurrence = matchedRuleItem.unsavedOccurrence,
                triggerResult = InternalTriggerResult.Holdout(Experiment(rule.experiment.id, rule.experiment.groupId, variant))
            )
            Experiment.Variant.VariantType.TREATMENT -> RuleEvaluationOutcome(
                confirmableAssignment = confirmableAssignment,
                unsavedOccurrence = matchedRuleItem.unsavedOccurrence,
                triggerResult = InternalTriggerResult.Paywall(Experiment(rule.experiment.id, rule.experiment.groupId, variant))
            )
        }
    }

    private suspend fun findMatchingRule(event: EventData, trigger: Trigger): RuleMatchOutcome {
        val expressionEvaluator = ExpressionEvaluator(context, storage, factory)

        val unmatchedRules = mutableListOf<UnmatchedRule>()

        for (rule in trigger.rules) {
            val outcome = expressionEvaluator.evaluateExpression(rule, event)

            when (outcome) {
                is TriggerRuleOutcome.Match -> return RuleMatchOutcome.Matched(outcome.matchedItem)
                is TriggerRuleOutcome.NoMatch -> unmatchedRules.add(outcome.unmatchedRule)
            }
        }

        return RuleMatchOutcome.NoMatchingRules(unmatchedRules)
    }

}

class PaywallNotFoundException :
    Exception("There isn't a paywall configured to show in this context")
