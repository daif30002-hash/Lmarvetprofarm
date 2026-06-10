package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val batchId: Int,
    val date: Long, // timestamp for day of the record
    val ageInDays: Int, // Calculated age
    val mortalityCount: Int, // عدد النافق
    val feedConsumptionKg: Double, // استهلاك العلف بالكجم
    val waterConsumptionLiters: Double, // استهلاك الماء باللتر
    val temperatureCelsius: Double, // درجة الحرارة بالعنبر
    val humidityPercent: Double, // الرطوبة النسبية
    val ammoniaLevelPpm: Double, // مستوى الأمونيا
    val ventilationRate: Double, // معدل التهوية / تبادل الهواء
    val averageWeightGrams: Double // متوسط الوزن بالجرام
)
