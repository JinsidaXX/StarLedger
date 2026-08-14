package com.starledger.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 已购物品：从大额消费计划转换而来的长期物品记录 */
@Entity(tableName = "owned_items")
data class OwnedItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val purchasePrice: Long,
    val purchaseDate: Long = System.currentTimeMillis(),
    val note: String = "",
    val planId: Long? = null,
    val transactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
