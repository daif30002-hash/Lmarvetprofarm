package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnosis_records")
data class DiagnosisRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val batchId: Int,
    val date: Long,
    val diseaseName: String,
    val probability: Double, // percentage e.g., 85.0
    val selectedSymptoms: String, // comma-separated symptoms
    val notes: String
)
