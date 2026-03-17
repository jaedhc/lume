package com.jaedhc.lume.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountType(type: AccountTypeEntity)

    @Query("SELECT * FROM account_types")
    fun getAllAccountTypes(): Flow<List<AccountTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeferredPlan(plan: DeferredPlanEntity)

    @Update
    suspend fun updateDeferredPlan(plan: DeferredPlanEntity)

    @Query("SELECT * FROM deferred_plans WHERE status = 'ACTIVE'")
    fun getActiveDeferredPlans(): Flow<List<DeferredPlanEntity>>

    @Query("SELECT * FROM deferred_plans WHERE status = 'ACTIVE'")
    suspend fun getActiveDeferredPlansSnapshot(): List<DeferredPlanEntity>

    @Query("SELECT * FROM deferred_plans WHERE transactionId = :txId")
    suspend fun getDeferredPlanByTransactionId(txId: String): DeferredPlanEntity?

    @Query("SELECT * FROM transactions ORDER BY dateIso DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM categories ORDER BY displayOrder ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getCategoriesSnapshot(): List<CategoryEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String?): AccountEntity?

    @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: String, delta: Double)

    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Transaction
    @Query("SELECT * FROM accounts")
    fun getAllAccountsWithType(): Flow<List<AccountWithType>>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY dateIso DESC, createdAt DESC")
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY dateIso DESC, createdAt DESC")
    fun getAllTransactionsDetail(): Flow<List<TransactionDetail>>

    @Query("SELECT * FROM accounts")
    suspend fun getAccountsSnapshot(): List<AccountEntity>

    @Transaction
    suspend fun insertTransactionWithBalanceUpdate(transaction: TransactionEntity) {
        // 1. Insert the transaction
        insertTransaction(transaction)

        val accountId = transaction.accountId ?: return
        val account = getAccountById(accountId) ?: return
        val isCredit = account.accountTypeId == "CREDIT"

        // 2. Update account balance if it's NOT a future payment, 
        // OR if it IS a future payment but it belongs to a CREDIT account (MSI installments)
        if (transaction.isFuturePayment && !isCredit) return

        // Logic:
        // Egreso on Debit/Cash/Savings -> subtract from balance
        // Egreso on Credit -> add to balance (debt)
        // Ingreso on Debit/Cash/Savings -> add to balance
        // Ingreso on Credit -> subtract from balance (reduce debt)
        
        val isEgreso = transaction.type == "egreso"
        
        val delta = when {
            isEgreso && !isCredit -> -transaction.amount
            isEgreso && isCredit -> transaction.amount
            !isEgreso && !isCredit -> transaction.amount
            !isEgreso && isCredit -> -transaction.amount
            else -> 0.0
        }
        
        if (delta != 0.0) {
            updateAccountBalance(accountId, delta)
        }
    }

    @Transaction
    suspend fun deleteTransactionWithBalanceUpdate(transaction: TransactionEntity) {
        // 1. Delete the transaction
        deleteTransaction(transaction)

        val accountId = transaction.accountId ?: return
        val account = getAccountById(accountId) ?: return
        val isCredit = account.accountTypeId == "CREDIT"

        // 2. Reverse account balance if it's NOT a future payment,
        // OR it IS a future payment but it belongs to a CREDIT account.
        if (transaction.isFuturePayment && !isCredit) return

        // Logic: Reverse of the insert logic
        val isEgreso = transaction.type == "egreso"
        
        val delta = when {
            isEgreso && !isCredit -> transaction.amount // Reversed: add back
            isEgreso && isCredit -> -transaction.amount // Reversed: subtract debt
            !isEgreso && !isCredit -> -transaction.amount // Reversed: subtract from balance
            !isEgreso && isCredit -> transaction.amount // Reversed: add to debt
            else -> 0.0
        }
        
        if (delta != 0.0) {
            updateAccountBalance(accountId, delta)
        }
    }

    @Query("SELECT * FROM transactions WHERE accountId = :accountId")
    suspend fun getTransactionsByAccountSnapshot(accountId: String): List<TransactionEntity>

    @Transaction
    suspend fun recalculateAllBalances() {
        val accounts = getAccountsSnapshot()
        for (account in accounts) {
            val transactions = getTransactionsByAccountSnapshot(account.id)
            val isCredit = account.accountTypeId == "CREDIT"
            
            var newBalance = 0.0 // Starting balance for sync
            
            for (tx in transactions) {
                // Only consider non-future payments, 
                // OR future payments for CREDIT accounts (MSI)
                if (tx.isFuturePayment && !isCredit) continue

                // FOR CREDIT: Only unpaid transactions contribute to current debt
                if (isCredit && tx.isPaid) continue
                
                val isEgreso = tx.type == "egreso"
                val delta = when {
                    isEgreso && !isCredit -> -tx.amount
                    isEgreso && isCredit -> tx.amount
                    !isEgreso && !isCredit -> tx.amount
                    !isEgreso && isCredit -> -tx.amount
                    else -> 0.0
                }
                newBalance += delta
            }
            
            // Update the account balance explicitly
            setAccountBalance(account.id, newBalance)
        }
    }

    @Query("UPDATE accounts SET balance = :balance WHERE id = :accountId")
    suspend fun setAccountBalance(accountId: String, balance: Double)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Transaction
    suspend fun updateTransactionWithBalanceUpdate(oldTransaction: TransactionEntity, newTransaction: TransactionEntity) {
        // 1. Update the transaction
        updateTransaction(newTransaction)

        // 2. We handle balance changes by conceptually "deleting" the old one and "inserting" the new one
        if (oldTransaction.accountId != newTransaction.accountId) {
            // If the account changed, reverse from old account and add to new account
            oldTransaction.accountId?.let {
                reverseBalance(it, oldTransaction.amount, oldTransaction.type)
            }
            newTransaction.accountId?.let {
                applyBalance(it, newTransaction.amount, newTransaction.type)
            }
        } else {
            // Same account, just calculate the net difference
            // We reverse the old, add the new
            oldTransaction.accountId?.let {
                reverseBalance(it, oldTransaction.amount, oldTransaction.type)
                applyBalance(it, newTransaction.amount, newTransaction.type)
            }
        }
    }

    private suspend fun reverseBalance(accountId: String, amount: Double, type: String) {
        val account = getAccountById(accountId) ?: return
        val isEgreso = type == "egreso"
        val isCredit = account.accountTypeId == "CREDIT"
        
        val delta = when {
            isEgreso && !isCredit -> amount 
            isEgreso && isCredit -> -amount 
            !isEgreso && !isCredit -> -amount 
            !isEgreso && isCredit -> amount 
            else -> 0.0
        }
        if (delta != 0.0) updateAccountBalance(accountId, delta)
    }

    private suspend fun applyBalance(accountId: String, amount: Double, type: String) {
        val account = getAccountById(accountId) ?: return
        val isEgreso = type == "egreso"
        val isCredit = account.accountTypeId == "CREDIT"
        
        val delta = when {
            isEgreso && !isCredit -> -amount
            isEgreso && isCredit -> amount
            !isEgreso && !isCredit -> amount
            !isEgreso && isCredit -> -amount
            else -> 0.0
        }
        if (delta != 0.0) updateAccountBalance(accountId, delta)
    }

    @Delete
    suspend fun deleteDeferredPlan(plan: DeferredPlanEntity)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsByAccountId(accountId: String)

    /**
     * Sums unpaid egreso transactions for a specific account.
     * Use ONLY for CREDIT accounts — for all other types isPaid is always false
     * and this would return the full transaction sum.
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE accountId = :accountId AND type = 'egreso' AND isPaid = 0")
    suspend fun getPendingCreditDebt(accountId: String): Double

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccountById(accountId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    @Query("DELETE FROM deferred_plans")
    suspend fun deleteAllDeferredPlans()

    @Transaction
    suspend fun clearAllUserData() {
        deleteAllTransactions()
        deleteAllAccounts()
        deleteAllDeferredPlans()
    }

    @Transaction
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY dateIso DESC, createdAt DESC")
    fun getTransactionsByAccountFlow(accountId: String): kotlinx.coroutines.flow.Flow<List<TransactionWithCategory>>

    @Query("UPDATE transactions SET isPaid = 1 WHERE id IN (:txIds)")
    suspend fun markTransactionsAsPaid(txIds: List<String>)

    @Transaction
    suspend fun registerTdcPayment(
        sourceAccountId: String,
        tdcAccountId: String,
        amount: Double,
        txIdsToPay: List<String>,
        dateIso: String,
        note: String? = null
    ) {
        // 1. Mark target TDC transactions as paid
        markTransactionsAsPaid(txIdsToPay)

        // 2. Create the payment transaction (egreso from source account)
        val tdcAccount = getAccountById(tdcAccountId)
        val paymentTx = TransactionEntity(
            id = java.util.UUID.randomUUID().toString(),
            amount = amount,
            currency = "MXN",
            dateIso = dateIso,
            merchant = "Pago TDC ${tdcAccount?.bankName ?: ""}",
            concept = "Pago de Tarjeta de Crédito",
            categoryId = "finanzas", // Assuming 'finanzas' exists as a seed
            accountId = sourceAccountId,
            type = "egreso",
            isSubscription = false,
            note = note,
            isPaid = false // Non-TDC transaction
        )
        insertTransactionWithBalanceUpdate(paymentTx)

        // 3. Update the TDC balance (reduce debt)
        // Since it's a CREDIT account, reducing debt means subtracting from balance
        updateAccountBalance(tdcAccountId, -amount)
    }
}

data class AccountWithType(
    @Embedded val account: AccountEntity,
    @Relation(
        parentColumn = "accountTypeId",
        entityColumn = "id"
    )
    val type: AccountTypeEntity?
)

data class TransactionDetail(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?,
    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    val account: AccountEntity?
)
