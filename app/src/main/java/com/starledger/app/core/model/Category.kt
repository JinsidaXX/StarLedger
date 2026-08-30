package com.starledger.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 收支分类 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "📦",
    val color: Long = 0xFF86A8FF,
    val isExpense: Boolean = true,
    /** 医疗类支出不占用「可用支出」额度 */
    val isMedical: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
