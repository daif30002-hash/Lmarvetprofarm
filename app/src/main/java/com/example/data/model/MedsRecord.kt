package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meds_records")
data class MedsRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val batchId: Int,
    val date: Long, // Planned date
    val type: String, // "VACCINE" or "MEDICINE"
    val name: String, // Vaccine or drug name
    val dosage: String, // dosage or method administration
    val notes: String,
    val isCompleted: Boolean = false,
    val actualDate: Long? = null // Timestamp of administration
)
