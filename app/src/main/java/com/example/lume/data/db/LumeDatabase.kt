package com.example.lume.data.db

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, AccountEntity::class, DeferredPlanEntity::class, AccountTypeEntity::class],
    version = 8,
    exportSchema = false
)
abstract class LumeDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: LumeDatabase? = null

        fun getDatabase(context: Context): LumeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LumeDatabase::class.java,
                    "lume_database"
                )
                .addCallback(LumeDatabaseCallback(CoroutineScope(Dispatchers.IO)))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class LumeDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            seedDatabase(db)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            seedDatabase(db)
        }

        private fun seedDatabase(db: SupportSQLiteDatabase) {
            scope.launch {
                // 1. Seed Categories (using INSERT OR IGNORE)
                val categories = listOf(
                    "('comida', 'Comida', 'Restaurant', '#FACC15', 0)",
                    "('transporte', 'Transporte', 'DirectionsCar', '#60A5FA', 1)",
                    "('entretenimiento', 'Entretenimiento', 'ConfirmationNumber', '#A78BFA', 2)",
                    "('salud', 'Salud', 'MedicalServices', '#F87171', 3)",
                    "('finanzas', 'Finanzas', 'SwapHoriz', '#FFB800', 4)",
                    "('servicios', 'Servicios', 'Lightbulb', '#FB923C', 5)",
                    "('otros', 'Otros', 'Category', '#94A3B8', 6)"
                )
                categories.forEach { values ->
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, icon, color, displayOrder) VALUES $values")
                }

                // 2. Seed Account Types (using INSERT OR IGNORE)
                val accountTypes = listOf(
                    "('DEBIT', 'Débito / Nómina', 'AccountBalance')",
                    "('CREDIT', 'Tarjeta de Crédito', 'CreditCard')",
                    "('SAVINGS', 'Ahorro / Apartados', 'Savings')",
                    "('INVESTMENT', 'Inversión', 'ShowChart')",
                    "('CASH', 'Efectivo', 'Payments')"
                )
                accountTypes.forEach { values ->
                    db.execSQL("INSERT OR IGNORE INTO account_types (id, name, icon) VALUES $values")
                }
            }
        }
    }
}
