package com.jaedhc.lume.data.db

import androidx.room.*

@Entity(
    tableName = "categories"
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val displayOrder: Int = 0
)

@Entity(
    tableName = "account_types"
)
data class AccountTypeEntity(
    @PrimaryKey val id: String, // "DEBIT", "CREDIT", "SAVINGS", "INVESTMENT", "CASH"
    val name: String,
    val icon: String
)

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = AccountTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountTypeId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("accountTypeId")]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val bankName: String,
    val last4: String,
    val label: String? = null,
    val accountTypeId: String,      // Linking to AccountTypeEntity
    val balance: Double = 0.0,       // Current liquid balance
    
    // TDC Specific
    val closingDay: Int? = null,
    val dueDay: Int? = null,
    val creditLimit: Double? = null,
    
    // Savings Specific
    val targetAmount: Double? = null,
    val yieldRate: Double? = null,
    
    // Investment Specific
    val initialInvestment: Double? = null,
    val currentValue: Double? = null,
    val assetType: String? = null,   // "Acciones", "Cripto", etc.
    
    val color: String? = null        // Personalization
)

@Entity(
    tableName = "deferred_plans"
)
data class DeferredPlanEntity(
    @PrimaryKey val id: String,
    val transactionId: String, // Original transaction
    val totalAmount: Double,
    val totalInstallments: Int,
    val monthlyPayment: Double, // Principal + Interest
    val interestRate: Double? = null,
    val startDateIso: String,
    val status: String = "ACTIVE" // "ACTIVE", "COMPLETED", "CANCELLED"
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("accountId")]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val currency: String,
    val dateIso: String,
    val merchant: String?,
    val concept: String?,
    val categoryId: String,
    val accountId: String?,
    val type: String,           // "ingreso" or "egreso"
    val isSubscription: Boolean,
    val note: String?,
    /**
     * Only meaningful for CREDIT card transactions imported from a statement.
     * true  = transaction belongs to a previous billing cycle (already paid, not current debt)
     * false = current cycle debt, or any non-credit transaction (debit/cash/savings/investment)
     */
    val isPaid: Boolean = false,
    val deferredPlanId: String? = null,
    val usedAi: Boolean = false,
    val msiInstallment: Int? = null,
    val msiTotal: Int? = null,
    val isFuturePayment: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)
