package com.example.data.disease

import com.example.data.model.Batch
import com.example.data.model.DailyRecord

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class PredictionResult(
    val disease: Disease,
    val probability: Double, // 0.0 to 100.0
    val riskLevel: RiskLevel,
    val riskFactors: List<String>
)

object DiseasePredictor {

    fun predictFutureRisks(
        batch: Batch,
        records: List<DailyRecord>
    ): List<PredictionResult> {
        if (records.isEmpty()) return emptyList()

        val lastRecord = records.last()
        val currentAge = lastRecord.ageInDays
        val results = mutableListOf<PredictionResult>()

        // Get recent history (up to last 5 records) to look at environmental trends
        val recentRecords = records.takeLast(5)

        // 1. Temperature Fluctuation and Max Temperature
        val maxTemp = recentRecords.map { it.temperatureCelsius }.maxOrNull() ?: lastRecord.temperatureCelsius
        val minTemp = recentRecords.map { it.temperatureCelsius }.minOrNull() ?: lastRecord.temperatureCelsius
        val tempFluctuation = maxTemp - minTemp

        // 2. Ammonia Average and Trend
        val avgAmmonia = recentRecords.map { it.ammoniaLevelPpm }.average()

        // 3. Humidity Average
        val avgHumidity = recentRecords.map { it.humidityPercent }.average()
        val lastHumidity = lastRecord.humidityPercent

        // 4. Feed & Water Intake Drops
        var feedDropPercent = 0.0
        var waterDropPercent = 0.0
        if (recentRecords.size >= 3) {
            val firstRecord = recentRecords.first()
            if (firstRecord.feedConsumptionKg > 0) {
                feedDropPercent = ((firstRecord.feedConsumptionKg - lastRecord.feedConsumptionKg) / firstRecord.feedConsumptionKg * 100.0).coerceAtLeast(0.0)
            }
            if (firstRecord.waterConsumptionLiters > 0) {
                waterDropPercent = ((firstRecord.waterConsumptionLiters - lastRecord.waterConsumptionLiters) / firstRecord.waterConsumptionLiters * 100.0).coerceAtLeast(0.0)
            }
        }

        // 5. Cumulative batch mortality percentage
        val totalMortality = records.sumOf { it.mortalityCount }
        val cumulativeMortalityRate = (totalMortality.toDouble() / batch.initialChickCount.toDouble()) * 100.0

        for (disease in DiseaseDb.diseases) {
            var score = 0.0
            val riskFactors = mutableListOf<String>()

            // Factor A: Age Profile Alignment
            if (currentAge in disease.minAgeDays..disease.maxAgeDays) {
                score += 35.0 // optimal target age for this disease
            } else if (currentAge in (disease.minAgeDays - 4)..(disease.maxAgeDays + 4)) {
                score += 15.0 // adjacent risk age
            }

            // Factor B: Breed Specificities (Ross 308 fast musculoskeletal load, Cobb 500 cardiac weight load)
            val breedLower = batch.breed.lowercase()
            if (breedLower.contains("cobb") && disease.id == "heat_stress") {
                score += 5.0
                riskFactors.add("معدل زيادة لحم صدر سلالة كوب (Cobb 500) السريعة يضاعف الجهد الحراري")
            }
            if (breedLower.contains("ross") && disease.id == "colibacillosis") {
                score += 3.0
            }

            // Factor C: Housing System Vulnerabilities (Closed ventilation pressure vs Open wild vector paths)
            if (batch.systemType == "CLOSED") {
                if (disease.id == "colibacillosis" || disease.id == "infectious_bronchitis") {
                    if (avgAmmonia > 12.0) {
                        score += 20.0
                        riskFactors.add("تراكم غاز الأمونيا بالتسجيلات الأخيرة (${String.format("%.1f", avgAmmonia)} ppm) يدمر أهداب القصبة التنفسية")
                    }
                }
                if (disease.id == "aspergillosis" && avgHumidity > 65.0) {
                    score += 15.0
                    riskFactors.add("متوسط رطوبة مغلقة مرتفع (${String.format("%.1f", avgHumidity)}%) يحفز نمو أبواغ الفطريات بالفرشة")
                }
            } else { // Open type systems
                if (disease.id == "newcastle") {
                    score += 15.0
                    riskFactors.add("عدم وجود حائل فيزيائي تام بالحظيرة المفتوحة يرفع التعرض للطيور البرية الناقلة للنيوكاسل")
                }
                if (disease.id == "salmonella") {
                    score += 10.0
                    riskFactors.add("صعوبة ضبط التعقيم الشامل في العنابر العادية المفتوحة تسند بكتيريا السالمونيلا")
                }
            }

            // Factor D: Micro-climate deviations & Trend alarms
            when (disease.id) {
                "salmonella" -> {
                    if (currentAge <= 8) {
                        if (minTemp < 29.0) {
                            score += 20.0
                            riskFactors.add("تدني درجة حرارة استقبال الكتاكيت لـ (${String.format("%.1f", minTemp)}°م) يثبت مناعتهم الأولية")
                        }
                        if (feedDropPercent > 8.0) {
                            score += 18.0
                            riskFactors.add("سقوط استهلاك العلف في مرحلة التحضين بنسبة (${String.format("%.1f", feedDropPercent)}%)")
                        }
                    }
                }
                "aspergillosis" -> {
                    if (currentAge <= 7) {
                        if (lastHumidity > 72.0) {
                            score += 25.0
                            riskFactors.add("رطوبة عالية للجو المعزول (${String.format("%.1f", lastHumidity)}%) تسرع عفونة التربة الأساسية")
                        }
                    }
                }
                "infectious_bronchitis" -> {
                    if (tempFluctuation > 4.5) {
                        score += 25.0
                        riskFactors.add("التذبذب العشوائي الحاد لدرجات الحرارة مؤخراً (${String.format("%.1f", tempFluctuation)}°م) يؤهب لنزلات الشعب المعدية")
                    }
                    if (lastRecord.ventilationRate < 50.0) {
                        score += 15.0
                        riskFactors.add("انخفاض تيار تغذية الهواء المتجدد (${String.format("%.1f", lastRecord.ventilationRate)}%) يسبب خمولاً رئوياً")
                    }
                }
                "coccidiosis" -> {
                    if (avgHumidity > 68.0) {
                        score += 25.0
                        riskFactors.add("توفر رطوبة تراكمية (${String.format("%.1f", avgHumidity)}%) يحفز تبرعم طفيليات الأووسيت بالأرضية")
                    }
                    if (feedDropPercent > 6.0) {
                        score += 15.0
                        riskFactors.add("تراجع العلف في السجلات الأخيرة (${String.format("%.1f", feedDropPercent)}%) كعارض معوي خفي")
                    }
                }
                "colibacillosis" -> {
                    if (avgAmmonia > 15.0) {
                        score += 30.0
                        riskFactors.add("ارتفاع معدل غاز الأمونيا الخانق (${String.format("%.1f", avgAmmonia)} ppm) يجرح الأمعاء والقصبات ويسرع تسمم الرئات")
                    }
                    if (waterDropPercent > 10.0) {
                        score += 15.0
                        riskFactors.add("هبوط حاد بطلب مياه الشرب (${String.format("%.1f", waterDropPercent)}%) يسرع تلف وظائف كبد الطائر")
                    }
                }
                "newcastle" -> {
                    if (cumulativeMortalityRate > 2.5) {
                        score += 15.0
                        riskFactors.add("ارتفاع تراكم النافق بالدفعة لـ (${String.format("%.2f", cumulativeMortalityRate)}%) يحفز الخطر الوبائي الشديد")
                    }
                    if (waterDropPercent > 12.0) {
                        score += 20.0
                        riskFactors.add("سقوط استهلاك الخزان الكلي للماء بنسبة (${String.format("%.1f", waterDropPercent)}%) في الأيام الأخيرة")
                    }
                }
                "heat_stress" -> {
                    if (maxTemp > 31.5) {
                        val heatDelta = maxTemp - 31.5
                        score += (heatDelta * 18.0).coerceAtMost(55.0)
                        riskFactors.add("ارتفاع ذروة درجة حرارة العنبر لـ (${String.format("%.1f", maxTemp)}°م) وهو أعلى بكثير من الحد المسموح به للدجاج اللحم")
                    }
                    if (lastRecord.ventilationRate < 60.0 && maxTemp > 30.0) {
                        score += 15.0
                        riskFactors.add("قصور سرعة دفق الهواء وتجديده بـ (${String.format("%.1f", lastRecord.ventilationRate)}%) يمنع تبريد القطيع إشعاعياً")
                    }
                }
            }

            // Cap risk probability safely
            var finalProb = score
            if (finalProb < 5.0) finalProb = 5.0
            if (finalProb > 97.0) finalProb = 97.0

            val rating = when {
                finalProb >= 70.0 -> RiskLevel.HIGH
                finalProb >= 35.0 -> RiskLevel.MEDIUM
                else -> RiskLevel.LOW
            }

            // Return prediction if we have any risk indicators
            if (finalProb >= 12.0 || riskFactors.isNotEmpty()) {
                results.add(
                    PredictionResult(
                        disease = disease,
                        probability = Math.round(finalProb * 10.0) / 10.0,
                        riskLevel = rating,
                        riskFactors = riskFactors
                    )
                )
            }
        }

        return results.sortedByDescending { it.probability }
    }
}
