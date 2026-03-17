package com.jaedhc.lume.data.mappers

import com.jaedhc.lume.data.db.AccountWithType
import com.jaedhc.lume.data.db.TransactionEntity

object AccountMappers {
    /**
     * Updates the balance of each account in the list to reflect its current debt (for CREDIT) 
     * or liquid balance (for others) based on unpaid transactions.
     */
    fun List<AccountWithType>.mapWithDynamicBalances(
        transactions: List<TransactionEntity>
    ): List<AccountWithType> {
        return this.map { accWithType ->
            if (accWithType.type?.id == "CREDIT") {
                val pendingDebt = transactions
                    .filter { it.accountId == accWithType.account.id && it.type == "egreso" && !it.isPaid }
                    .sumOf { it.amount }
                val credits = transactions
                    .filter { it.accountId == accWithType.account.id && it.type == "ingreso" && !it.isPaid }
                    .sumOf { it.amount }
                
                // For a credit card, "balance" represents total unpaid debt
                val netPending = (pendingDebt - credits).coerceAtLeast(0.0)
                accWithType.copy(account = accWithType.account.copy(balance = netPending))
            } else {
                // For other accounts, we keep the DB balance (which is updated via TransactionDao)
                // because calculating liquid balance from all historical transactions would be expensive 
                // and potentially inaccurate if the user didn't start from 0.
                accWithType
            }
        }
    }
}
