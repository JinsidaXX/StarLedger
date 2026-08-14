package com.starledger.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.starledger.app.core.database.dao.AccountDao
import com.starledger.app.core.database.dao.CategoryDao
import com.starledger.app.core.database.dao.CycleDao
import com.starledger.app.core.database.dao.EnvelopeDao
import com.starledger.app.core.database.dao.OwnedItemDao
import com.starledger.app.core.database.dao.PurchaseDao
import com.starledger.app.core.database.dao.RuleDao
import com.starledger.app.core.database.dao.StarDao
import com.starledger.app.core.database.dao.TemplateDao
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.model.Account
import com.starledger.app.core.model.AllocationRule
import com.starledger.app.core.model.AllocationTemplate
import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.Category
import com.starledger.app.core.model.MonthlyStar
import com.starledger.app.core.model.OwnedItem
import com.starledger.app.core.model.PlannedPurchase
import com.starledger.app.core.model.Transaction

@Database(
    entities = [
        Account::class,
        Category::class,
        Transaction::class,
        BudgetCycle::class,
        AllocationTemplate::class,
        AllocationRule::class,
        BudgetEnvelope::class,
        PlannedPurchase::class,
        MonthlyStar::class,
        OwnedItem::class,
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun cycleDao(): CycleDao
    abstract fun templateDao(): TemplateDao
    abstract fun ruleDao(): RuleDao
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun starDao(): StarDao
    abstract fun ownedItemDao(): OwnedItemDao

    companion object {
        const val NAME = "starledger.db"
    }
}
