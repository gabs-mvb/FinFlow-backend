package com.finflow.planning.adapter.outbound.ai

import com.finflow.planning.domain.PlanContent
import com.finflow.shared.domain.AiPlanningException

/** Validates generated text before it can reach persistence. Manual edits retain their existing contract. */
internal object AiPlanOutputPolicy {
    // Defense in depth, not a semantic classifier: scope is also enforced by the planner instructions.
    private val codeOrMarkup =
        Regex(
            """```|~~~|`|</?[a-z][^>]*>|https?://|\b(?:print|exec|eval|console\.log)\s*\(|\bdef\s+\w+\s*\(|\bimport\s+\w+|\bfrom\s+\w+\s+import\b|\b(?:const|let|var)\s+\w+\s*=|\bSELECT\s+.+\s+FROM\b|\b(?:pip|npm)\s+install\b""",
            RegexOption.IGNORE_CASE,
        )

    fun validate(plan: PlanContent) {
        val texts =
            listOf(plan.summary, plan.analysis) + plan.categoryBudgets.map { it.reason } +
                plan.actions.map { it.rationale } + plan.warnings
        if (texts.any { codeOrMarkup.containsMatchIn(it) }) {
            throw AiPlanningException(
                "A resposta da IA não respeitou o escopo do plano financeiro. Tente novamente.",
                "AI_PLAN_OUT_OF_SCOPE",
            )
        }
        val bullets = plan.analysis.lines()
        val valid =
            singleLine(plan.summary, 350) &&
                bullets.size in 3..5 && bullets.all { it.startsWith("- ") && singleLine(it.removePrefix("- "), 178) } &&
                plan.categoryBudgets.size <= 8 && plan.categoryBudgets.all { singleLine(it.reason, 120) } &&
                plan.actions.size <= 5 && plan.actions.all { singleLine(it.rationale, 160) } &&
                plan.warnings.size <= 3 && plan.warnings.all { singleLine(it, 160) }
        if (!valid) {
            throw AiPlanningException("A IA retornou um plano fora do formato objetivo esperado. Tente novamente.", "AI_PLAN_INVALID")
        }
    }

    private fun singleLine(
        text: String,
        limit: Int,
    ): Boolean = text.isNotBlank() && text.length <= limit && '\n' !in text && '\r' !in text
}
