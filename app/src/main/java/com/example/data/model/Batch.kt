package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "batches")
data class Batch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val batchNumber: String,
    val startDate: Long,
    val initialChickCount: Int,
    val chickSource: String,
    val breed: String,
    val systemType: String, // "CLOSED" or "OPEN"
    val isClosed: Boolean = false
) : Serializable
