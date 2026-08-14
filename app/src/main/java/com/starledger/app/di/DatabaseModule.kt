package com.starledger.app.di

import android.content.Context
import androidx.room.Room
import com.starledger.app.core.database.AppDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCycleDao(db: AppDatabase): CycleDao = db.cycleDao()

    @Provides
    fun provideTemplateDao(db: AppDatabase): TemplateDao = db.templateDao()

    @Provides
    fun provideRuleDao(db: AppDatabase): RuleDao = db.ruleDao()

    @Provides
    fun provideEnvelopeDao(db: AppDatabase): EnvelopeDao = db.envelopeDao()

    @Provides
    fun providePurchaseDao(db: AppDatabase): PurchaseDao = db.purchaseDao()

    @Provides
    fun provideStarDao(db: AppDatabase): StarDao = db.starDao()

    @Provides
    fun provideOwnedItemDao(db: AppDatabase): OwnedItemDao = db.ownedItemDao()
}
