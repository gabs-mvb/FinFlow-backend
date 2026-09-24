package com.finflow.transaction.domain

import java.text.Normalizer

class TransactionCategorizer {
    private val rules: List<Pair<Regex, TransactionCategory>> =
        listOf(
            Regex("salario|pagamento salario|folha") to TransactionCategory.INCOME,
            Regex("aluguel|condominio|energia|eletricidade|agua|gas") to TransactionCategory.HOUSING,
            Regex("ifood|restaurante|mercado|supermercado|padaria|aliment") to TransactionCategory.FOOD,
            Regex("uber|99app|combustivel|posto |estacionamento|pedagio") to TransactionCategory.TRANSPORT,
            Regex("farmacia|hospital|clinica|laboratorio|plano de saude") to TransactionCategory.HEALTH,
            Regex("escola|faculdade|curso|livraria") to TransactionCategory.EDUCATION,
            Regex("netflix|spotify|youtube|amazon prime|assinatura") to TransactionCategory.SUBSCRIPTIONS,
            Regex("fatura|cartao") to TransactionCategory.CREDIT_CARD,
            Regex("financiamento|emprestimo|parcela") to TransactionCategory.DEBT_PAYMENT,
            Regex("corretora|investimento|tesouro|cdb") to TransactionCategory.INVESTMENTS,
            Regex("imposto|ipva|iptu|tributo") to TransactionCategory.TAXES,
            Regex("pix enviado|pix recebido|transferencia|ted") to TransactionCategory.TRANSFER,
            Regex("cinema|ingresso|jogo|lazer") to TransactionCategory.LEISURE,
        )

    fun categorize(
        description: String,
        merchant: String?,
    ): TransactionCategory {
        val text = normalize(listOfNotNull(description, merchant).joinToString(" "))
        return rules.firstOrNull { (pattern, _) -> pattern.containsMatchIn(text) }?.second
            ?: TransactionCategory.OTHER
    }

    private fun normalize(value: String): String =
        Normalizer
            .normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
}
