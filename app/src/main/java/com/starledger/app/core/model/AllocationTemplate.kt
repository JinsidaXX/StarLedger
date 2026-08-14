package com.starledger.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 分配模板 */
@Entity(tableName = "allocation_templates")
data class AllocationTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
