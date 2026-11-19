package com.sweak.qralarm.core.storage.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-photographed item. Its photos live in files keyed by [objectId]. */
@Entity(tableName = "custom_object")
data class CustomObjectEntity(
    @PrimaryKey(autoGenerate = true)
    val objectId: Long = 0,
    val name: String,
    val createdAtMillis: Long,
    val referenceCount: Int,
    /** Unused since items are recognised by their label text; kept to avoid a table rebuild. */
    val embeddingThreshold: Float,
    /** Label keywords, one per line. Empty for items that need new photos. */
    @ColumnInfo(defaultValue = "") val keywords: String
)
