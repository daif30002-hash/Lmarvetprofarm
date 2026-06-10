package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Batch
import com.example.data.model.DailyRecord
import com.example.data.model.DiagnosisRecord
import com.example.data.model.MedsRecord
import com.example.data.repository.FarmRepository
import com.example.data.api.GeminiApi
import com.example.data.disease.Disease
import com.example.data.disease.DiseaseDb
import com.example.data.disease.DiseasePredictor
import com.example.data.disease.PredictionResult
import com.example.data.disease.RiskLevel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FarmRepository
    
    // --- States ---
    val allBatches: StateFlow<List<Batch>>
    
    private val _selectedBatch = MutableStateFlow<Batch?>(null)
    val selectedBatch: StateFlow<Batch?> = _selectedBatch.asStateFlow()

    private val _dailyRecords = MutableStateFlow<List<DailyRecord>>(emptyList())
    val dailyRecords: StateFlow<List<DailyRecord>> = _dailyRecords.asStateFlow()

    private val _diagnosisRecords = MutableStateFlow<List<DiagnosisRecord>>(emptyList())
    val diagnosisRecords: StateFlow<List<DiagnosisRecord>> = _diagnosisRecords.asStateFlow()

    private val _medsRecords = MutableStateFlow<List<MedsRecord>>(emptyList())
    val medsRecords: StateFlow<List<MedsRecord>> = _medsRecords.asStateFlow()

    // --- Diagnosis screen logic ---
    private val _selectedSymptoms = MutableStateFlow<List<String>>(emptyList())
    val selectedSymptoms: StateFlow<List<String>> = _selectedSymptoms.asStateFlow()

    private val _diagnosisResults = MutableStateFlow<List<Pair<Disease, Double>>>(emptyList())
    val diagnosisResults: StateFlow<List<Pair<Disease, Double>>> = _diagnosisResults.asStateFlow()

    // --- Gemini state ---
    private val _aiVetResponse = MutableStateFlow<String?>(null)
    val aiVetResponse: StateFlow<String?> = _aiVetResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // --- Advanced Prediction logic ---
    private val _predictionResults = MutableStateFlow<List<PredictionResult>>(emptyList())
    val predictionResults: StateFlow<List<PredictionResult>> = _predictionResults.asStateFlow()

    private val _aiPredictionReport = MutableStateFlow<String?>(null)
    val aiPredictionReport: StateFlow<String?> = _aiPredictionReport.asStateFlow()

    private val _isAiPredicting = MutableStateFlow(false)
    val isAiPredicting: StateFlow<Boolean> = _isAiPredicting.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FarmRepository(database.farmDao())
        
        allBatches = repository.allBatches.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun selectBatch(batch: Batch) {
        _selectedBatch.value = batch
        
        // Collect records of selected batch
        viewModelScope.launch {
            repository.getDailyRecordsForBatch(batch.id).collect {
                _dailyRecords.value = it
                runOfflinePredictions()
            }
        }
        viewModelScope.launch {
            repository.getDiagnosisRecordsForBatch(batch.id).collect {
                _diagnosisRecords.value = it
            }
        }
        viewModelScope.launch {
            repository.getMedsRecordsForBatch(batch.id).collect {
                _medsRecords.value = it
            }
        }
    }

    // --- Batch management operations ---
    fun createBatch(
        batchNumber: String,
        initialChickCount: Int,
        chickSource: String,
        breed: String,
        systemType: String,
        startDate: Long
    ) {
        viewModelScope.launch {
            val newBatch = Batch(
                batchNumber = batchNumber,
                initialChickCount = initialChickCount,
                chickSource = chickSource,
                breed = breed,
                systemType = systemType,
                startDate = startDate
            )
            val batchId = repository.insertBatch(newBatch).toInt()
            
            // Automatically insert recommended vaccination templates to save time for Dr. Daifallah!
            val dayMs = 24 * 60 * 60 * 1000L
            val recommendedMeds = listOf(
                MedsRecord(
                    batchId = batchId,
                    date = startDate + (1 * dayMs),
                    type = "VACCINE",
                    name = "تحصين نيوكاسل + شعب هوائية (رش أو قطرة)",
                    dosage = "H120 + HB1 - جرعة لكل طائر",
                    notes = "تحصين أساسي لحماية الكتاكيت من عمر يوم بالأنظمة المغلقة."
                ),
                MedsRecord(
                    batchId = batchId,
                    date = startDate + (7 * dayMs),
                    type = "VACCINE",
                    name = "تحصين نيوكاسل كلون 30 (تقطير عيني)",
                    dosage = "Clone 30 - نقطة بالعين",
                    notes = "حماية متوسطة للوقاية من النيوكاسل الوجيز."
                ),
                MedsRecord(
                    batchId = batchId,
                    date = startDate + (12 * dayMs),
                    type = "VACCINE",
                    name = "تحصين جمبورو معوي (بمياه الشرب)",
                    dosage = "Gumboro Intermediate plus",
                    notes = "تحصين معوي حماية من الجمبورو، يعطش القطيع قبل الإعطاء بساعتين."
                ),
                MedsRecord(
                    batchId = batchId,
                    date = startDate + (18 * dayMs),
                    type = "VACCINE",
                    name = "تحصين نيوكاسل لاسوتة (ماء شرب)",
                    dosage = "LaSota strain - مياه معزولة باللبن منزوع الدسم",
                    notes = "رفع المناعة الحقلية ضد الشوطة."
                ),
                MedsRecord(
                    batchId = batchId,
                    date = startDate + (28 * dayMs),
                    type = "VACCINE",
                    name = "تنشيط نيوكاسل + شعب (لقاح زيتي ثنائي)",
                    dosage = "0.2 مل حقناً بالفخذ أو تحت جلد الرقبة",
                    notes = "تحصين زيتي للوقاية الممتدة في الدورات الطويلة."
                ),
                MedsRecord(
                    batchId = batchId,
                    date = startDate + (3 * dayMs),
                    type = "MEDICINE",
                    name = "مضاد حيوي وقائي (أموكسيسيلين + كولستين)",
                    dosage = "1 جرام لكل لتر ماء شرب",
                    notes = "استقبال وقائي ضد السالمونيلا والإي كولاي لمدة 3 أيام."
                ),
                MedsRecord(
                    batchId = batchId,
                    date = startDate + (15 * dayMs),
                    type = "MEDICINE",
                    name = "وقاية ضد الكوكسيديا (أمبروليوم)",
                    dosage = "1 جرام لكل لتر ماء شرب لمدة 3 أيام",
                    notes = "جرعة استباقية للوقاية من نشاط الكوكسيديا الأرضية بالأنظمة المحكمة."
                )
            )

            for (med in recommendedMeds) {
                repository.insertMedsRecord(med)
            }
        }
    }

    fun closeBatch(batch: Batch) {
        viewModelScope.launch {
            val updated = batch.copy(isClosed = true)
            repository.updateBatch(updated)
            _selectedBatch.value = updated
        }
    }

    fun deleteBatch(batch: Batch) {
        viewModelScope.launch {
            repository.deleteBatch(batch)
            if (_selectedBatch.value?.id == batch.id) {
                _selectedBatch.value = null
                _dailyRecords.value = emptyList()
                _diagnosisRecords.value = emptyList()
                _medsRecords.value = emptyList()
            }
        }
    }

    // --- Daily record operations ---
    fun addDailyRecord(
        date: Long,
        ageInDays: Int,
        mortality: Int,
        feedKg: Double,
        waterL: Double,
        temp: Double,
        humidity: Double,
        ammonia: Double,
        ventilation: Double,
        avgWeight: Double
    ) {
        val currentBatch = _selectedBatch.value ?: return
        viewModelScope.launch {
            val record = DailyRecord(
                batchId = currentBatch.id,
                date = date,
                ageInDays = ageInDays,
                mortalityCount = mortality,
                feedConsumptionKg = feedKg,
                waterConsumptionLiters = waterL,
                temperatureCelsius = temp,
                humidityPercent = humidity,
                ammoniaLevelPpm = ammonia,
                ventilationRate = ventilation,
                averageWeightGrams = avgWeight
            )
            repository.insertDailyRecord(record)
        }
    }

    fun deleteDailyRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteDailyRecord(id)
        }
    }

    // --- Diagnosis operations ---
    fun toggleSymptom(symptom: String) {
        val current = _selectedSymptoms.value.toMutableList()
        if (current.contains(symptom)) {
            current.remove(symptom)
        } else {
            current.add(symptom)
        }
        _selectedSymptoms.value = current
        runOfflineDiagnosis()
    }

    fun clearSymptoms() {
        _selectedSymptoms.value = emptyList()
        _diagnosisResults.value = emptyList()
        _aiVetResponse.value = null
    }

    private fun runOfflineDiagnosis() {
        val symptoms = _selectedSymptoms.value
        val batch = _selectedBatch.value ?: return
        val currentAge = if (_dailyRecords.value.isNotEmpty()) {
            _dailyRecords.value.last().ageInDays
        } else {
            // estimate age based on current dates
            val diffMs = System.currentTimeMillis() - batch.startDate
            (diffMs / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
        }
        _diagnosisResults.value = DiseaseDb.diagnose(symptoms, currentAge)
    }

    fun saveDiagnosisResult(diseaseName: String, prob: Double) {
        val batch = _selectedBatch.value ?: return
        viewModelScope.launch {
            val record = DiagnosisRecord(
                batchId = batch.id,
                date = System.currentTimeMillis(),
                diseaseName = diseaseName,
                probability = prob,
                selectedSymptoms = _selectedSymptoms.value.joinToString("، "),
                notes = "التشخيص الذكي بالمنصة."
            )
            repository.insertDiagnosisRecord(record)
        }
    }

    // --- Gemini Smart AI Vet Consult ---
    fun consultAiVet(additionalQuery: String? = null) {
        val symptoms = _selectedSymptoms.value
        val batch = _selectedBatch.value ?: return
        val records = _dailyRecords.value
        val age = if (records.isNotEmpty()) records.last().ageInDays else 1
        
        val offlineDiagStr = if (_diagnosisResults.value.isNotEmpty()) {
            _diagnosisResults.value.joinToString(", ") { "${it.first.nameAr} (${it.second}%)" }
        } else {
            "لا يوجد تشريح دقيق بقاعدة البيانات لهذا الدمج العرضي"
        }

        _isAiLoading.value = true
        _aiVetResponse.value = null

        viewModelScope.launch {
            try {
                val advice = GeminiApi.consultVeterinarian(
                    ageInDays = age,
                    symptoms = symptoms,
                    offlineDiagnosis = offlineDiagStr,
                    additionalQuery = additionalQuery
                )
                _aiVetResponse.value = advice
            } catch (e: Exception) {
                _aiVetResponse.value = "حدث خطأ غير متوقع: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // --- Meds & Vaccinations management ---
    fun addMedsRecord(type: String, name: String, dosage: String, date: Long, notes: String) {
        val batch = _selectedBatch.value ?: return
        viewModelScope.launch {
            val record = MedsRecord(
                batchId = batch.id,
                date = date,
                type = type,
                name = name,
                dosage = dosage,
                notes = notes,
                isCompleted = false
            )
            repository.insertMedsRecord(record)
        }
    }

    fun toggleMedsCompleted(record: MedsRecord) {
        viewModelScope.launch {
            val updated = record.copy(
                isCompleted = !record.isCompleted,
                actualDate = if (!record.isCompleted) System.currentTimeMillis() else null
            )
            repository.updateMedsRecord(updated)
        }
    }

    fun deleteMedsRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteMedsRecord(id)
        }
    }

    // --- Technical calculations helper ---
    fun calculateKPIs(): KPIReport {
        val batch = _selectedBatch.value ?: return KPIReport()
        val records = _dailyRecords.value
        if (records.isEmpty()) return KPIReport(initialChicks = batch.initialChickCount)

        var totalMortality = 0
        var totalFeedKg = 0.0
        var totalWaterL = 0.0
        var currentAvgWeightGrams = 0.0
        var lastAge = 1

        for (r in records) {
            totalMortality += r.mortalityCount
            totalFeedKg += r.feedConsumptionKg
            totalWaterL += r.waterConsumptionLiters
            if (r.averageWeightGrams > 0) {
                currentAvgWeightGrams = r.averageWeightGrams
            }
            lastAge = maxOf(lastAge, r.ageInDays)
        }

        val remainingBirds = batch.initialChickCount - totalMortality
        val livabilityPercent = (remainingBirds.toDouble() / batch.initialChickCount.toDouble()) * 100.0
        val mortalityPercent = 100.0 - livabilityPercent

        // FCR calculation: FCR = Total Feed Consumed (kg) / Total Weight Gained (kg)
        // Weight Gain = (Average Weight in grams / 1000) * Number of remaining birds
        val weightGainedKg = (currentAvgWeightGrams / 1000.0) * remainingBirds
        val fcr = if (weightGainedKg > 0) totalFeedKg / weightGainedKg else 0.0

        // Avg Daily Gain (ADG)
        val adg = if (lastAge > 0) currentAvgWeightGrams / lastAge else 0.0

        // European Production Efficiency Factor (EPEF / EPI)
        // formula: EPI = [Livability (%) * Live Weight (kg) / (Age (days) * FCR)] * 100
        val avgWeightKg = currentAvgWeightGrams / 1000.0
        val epi = if (lastAge > 0 && fcr > 0) {
            (livabilityPercent * avgWeightKg) * 100.0 / (lastAge * fcr)
        } else {
            0.0
        }

        // Daily average consumption per bird on the latest recorded day
        val lastRecord = records.last()
        val currentRemainingOnLastDay = batch.initialChickCount - records.filter { it.date <= lastRecord.date }.sumOf { it.mortalityCount }
        val dailyFeedPerBirdGrams = if (currentRemainingOnLastDay > 0) {
            (lastRecord.feedConsumptionKg * 1000.0) / currentRemainingOnLastDay
        } else {
            0.0
        }
        val dailyWaterPerBirdMl = if (currentRemainingOnLastDay > 0) {
            (lastRecord.waterConsumptionLiters * 1000.0) / currentRemainingOnLastDay
        } else {
            0.0
        }

        // Stocking Density (birds per square meter on a fixed typical cage/floor surface e.g. 500m²)
        val stockingDensity = remainingBirds / 500.0

        return KPIReport(
            initialChicks = batch.initialChickCount,
            remainingBirds = remainingBirds,
            totalMortality = totalMortality,
            mortalityPercent = mortalityPercent,
            livabilityPercent = livabilityPercent,
            totalFeedKg = totalFeedKg,
            totalWaterL = totalWaterL,
            avgWeightGrams = currentAvgWeightGrams,
            fcr = fcr,
            adg = adg,
            epi = epi,
            ageInDays = lastAge,
            dailyFeedPerBirdGrams = dailyFeedPerBirdGrams,
            dailyWaterPerBirdMl = dailyWaterPerBirdMl,
            stockingDensity = stockingDensity,
            latestTemp = lastRecord.temperatureCelsius,
            latestHumidity = lastRecord.humidityPercent,
            latestAmmonia = lastRecord.ammoniaLevelPpm,
            latestVentilation = lastRecord.ventilationRate
        )
    }

    // --- Report generation & PDF Print (ACTION_SEND Share) ---
    fun generateAndSharePDFReport(context: Context) {
        val batch = _selectedBatch.value
        if (batch == null) {
            Toast.makeText(context, "لم يتم تحديد أي دفعة لتجهيز التقرير البيطري.", Toast.LENGTH_LONG).show()
            return
        }

        val records = _dailyRecords.value
        val kpis = calculateKPIs()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val generatedTime = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())

        val pdfDocument = PdfDocument()

        // Shared Paints
        val paintTitle = Paint().apply {
            color = Color.parseColor("#0F172A") // Slate 900
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintSubTitle = Paint().apply {
            color = Color.parseColor("#0D6E3F") // Emerald green
            textSize = 13f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintHeading = Paint().apply {
            color = Color.parseColor("#1E3A8A") // Deep Blue
            textSize = 11f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintText = Paint().apply {
            color = Color.parseColor("#1E293B") // Dark Slate
            textSize = 10f
            isAntiAlias = true
        }

        val paintTextBold = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintDivider = Paint().apply {
            color = Color.parseColor("#E2E8F0") // Slate 200
            strokeWidth = 1f
        }

        val paintBox = Paint().apply {
            color = Color.parseColor("#F8FAFC") // Slate 50
            style = Paint.Style.FILL
        }

        val paintBoxBorder = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val paintCardFill = Paint().apply {
            style = Paint.Style.FILL
        }

        val paintCardBorder = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Helper local function to paint header and footer on every single page
        fun drawHeaderAndFooter(canvas: Canvas, pageNum: Int, totalPages: Int) {
            // Header Top Band
            canvas.drawRect(30f, 20f, 565f, 85f, Paint().apply { color = Color.parseColor("#0F172A") })
            
            // Gold vertical accent strip
            canvas.drawRect(30f, 20f, 38f, 85f, Paint().apply { color = Color.parseColor("#D97706") })

            // Label Title inside the dark header
            val paintHeaderTitle = Paint().apply {
                color = Color.WHITE
                textSize = 15f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Lmar Vet ProFarm • مجمّع التقارير الفنية والطبية", 55f, 48f, paintHeaderTitle)

            val paintHeaderSub = Paint().apply {
                color = Color.parseColor("#10B981") // Success green
                textSize = 9f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("لوحة متابعة وبائية معتمدة لإنتاج الدواجن • إشراف المستشار د. ضيف الله الحسني", 55f, 68f, paintHeaderSub)

            // Header Bottom Gold Divider
            canvas.drawLine(30f, 85f, 565f, 85f, Paint().apply { color = Color.parseColor("#D97706"); strokeWidth = 2f })

            // Bottom Footer
            canvas.drawLine(30f, 800f, 565f, 800f, paintDivider)
            
            val paintFooterText = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 8f
                isAntiAlias = true
            }
            canvas.drawText("تم استخراج الوثيقة إلكترونياً لخدمة المزارع المباشرة: $generatedTime", 40f, 815f, paintFooterText)
            canvas.drawText("الصفحة $pageNum من $totalPages", 495f, 815f, paintFooterText)
        }

        // ============================================
        // PAGE 1: EXECUTIVE BRIEFING & KPI OVERVIEW
        // ============================================
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        drawHeaderAndFooter(canvas, 1, 3)

        var y = 110f

        // 1. Batch Demographics Section
        canvas.drawText("أولاً: بطاقة تعريف وبيانات الدفعة (Flock Base Information)", 40f, y, paintSubTitle)
        canvas.drawLine(40f, y + 5, 555f, y + 5, paintDivider)
        y += 20

        // Rounded Box for Metadata
        canvas.drawRoundRect(40f, y, 555f, y + 80f, 10f, 10f, paintBox)
        canvas.drawRoundRect(40f, y, 555f, y + 80f, 10f, 10f, paintBoxBorder)

        canvas.drawText("رقم تعريف الدفعة: ${batch.batchNumber}", 55f, y + 25, paintTextBold)
        canvas.drawText("سلالة الكتاكيت: ${batch.breed}", 300f, y + 25, paintText)
        
        canvas.drawText("العدد المستلم بالتحضين: ${batch.initialChickCount} طير", 55f, y + 48, paintText)
        canvas.drawText("نظام رعاية العنبر: ${if (batch.systemType == "CLOSED") "نظام مغلق ومحكم العزل" else "حظيرة عادية مفتوحة"}", 300f, y + 48, paintText)

        canvas.drawText("تاريخ بدء الدورة: ${dateFormat.format(Date(batch.startDate))}", 55f, y + 70, paintText)
        canvas.drawText("جهة وشركة التوريد: ${batch.chickSource}", 300f, y + 70, paintText)

        y += 105f

        // 2. Dashboard KPIs Section (6 Cards Grid)
        canvas.drawText("ثانياً: لوحة مؤشرات الكفاءة والإنتاج الكلية (Cumulative KPIs Dashboard)", 40f, y, paintSubTitle)
        canvas.drawLine(40f, y + 5, 555f, y + 5, paintDivider)
        y += 20

        // Grid parameters:
        val colW = 162f
        val cardH = 62f
        val gap = 12f

        // Card 1: FCR (Row 1, Col 1)
        var cLeft = 40f
        var cTop = y
        paintCardFill.color = Color.parseColor("#ECFDF5") // emerald light
        paintCardBorder.color = Color.parseColor("#A7F3D0")
        canvas.drawRoundRect(cLeft, cTop, cLeft + colW, cTop + cardH, 8f, 8f, paintCardFill)
        canvas.drawRoundRect(cLeft, cTop, cLeft + colW, cTop + cardH, 8f, 8f, paintCardBorder)
        canvas.drawText("معامل التحويل (FCR)", cLeft + 12f, cTop + 20f, paintText)
        canvas.drawText(String.format("%.2f", kpis.fcr), cLeft + 12f, cTop + 48f, Paint(paintTitle).apply { color = Color.parseColor("#047857"); textSize = 18f })

        // Card 2: EPI (Row 1, Col 2)
        cLeft = 40f + colW + gap
        paintCardFill.color = Color.parseColor("#EFF6FF") // blue light
        paintCardBorder.color = Color.parseColor("#BFDBFE")
        canvas.drawRoundRect(cLeft, cTop, cLeft + colW, cTop + cardH, 8f, 8f, paintCardFill)
        canvas.drawRoundRect(cLeft, cTop, cLeft + colW, cTop + cardH, 8f, 8f, paintCardBorder)
        canvas.drawText("مؤشر الكفاءة (EPI)", cLeft + 12f, cTop + 20f, paintText)
        canvas.drawText(String.format("%.1f", kpis.epi), cLeft + 12f, cTop + 48f, Paint(paintTitle).apply { color = Color.parseColor("#1D4ED8"); textSize = 18f })

        // Card 3: Mortality rate (Row 1, Col 3)
        cLeft = 40f + (colW + gap) * 2
        paintCardFill.color = Color.parseColor("#FEF2F2") // rose light
        paintCardBorder.color = Color.parseColor("#FCA5A5")
        canvas.drawRoundRect(cLeft, cTop, cLeft + colW, cTop + cardH, 8f, 8f, paintCardFill)
        canvas.drawRoundRect(cLeft, cTop, cLeft + colW, cTop + cardH, 8f, 8f, paintCardBorder)
        canvas.drawText("معدل وفيات القطيع", cLeft + 12f, cTop + 20f, paintText)
        canvas.drawText("${String.format("%.2f", kpis.mortalityPercent)}%", cLeft + 12f, cTop + 48f, Paint(paintTitle).apply { color = Color.parseColor("#B91C1C"); textSize = 16f })

        y += cardH + gap

        // Card 4: Avg Weight (Row 2, Col 1)
        cLeft = 40f
        cTop = y
        paintCardFill.color = Color.parseColor("#FFFBEB") // amber light
        paintCardBorder.color = Color.parseColor("#FDE68A")
        canvas.drawRoundRect(cLeft, cTop, cLeft + colW, cTop + cardH, 8f, 8f, paintCardFill)
        canvas.drawRoundRect(cLeft, cTop, cLeft + colW, cTop + cardH, 8f, 8f, paintCardBorder)
        canvas.drawText("متوسط وزن الطير", cLeft + 12f, cTop + 20f, paintText)
        canvas.drawText("${kpis.avgWeightGrams} جرام", cLeft + 12f, cTop + 48f, Paint(paintTitle).apply { color = Color.parseColor("#B45309"); textSize = 15f })

        // Card 5: ADG (Row 2, Col 2)
        cLeft = 40f + colW + gap
        paintCardFill.color = Color.parseColor("#FDF2F8") // pink light
        paintCardBorder.color = Color.parseColor("#FBCFE8")
        canvas.drawRoundRect(cLeft, cTop, cLeft + colW, cTop + cardH, 8f, 8f, paintCardFill)
        canvas.drawRoundRect(cLeft, cTop, cLeft + colW, cTop + cardH, 8f, 8f, paintCardBorder)
        canvas.drawText("النمو اليومي (ADG)", cLeft + 12f, cTop + 20f, paintText)
        canvas.drawText("${String.format("%.2f", kpis.adg)} جم/يوم", cLeft + 12f, cTop + 48f, Paint(paintTitle).apply { color = Color.parseColor("#BE185D"); textSize = 14f })

        // Card 6: Livability (Row 2, Col 3)
        cLeft = 40f + (colW + gap) * 2
        paintCardFill.color = Color.parseColor("#F0FDF4") // green light
        paintCardBorder.color = Color.parseColor("#BBF7D0")
        canvas.drawRoundRect(cLeft, cTop, cLeft + colW, cTop + cardH, 8f, 8f, paintCardFill)
        canvas.drawRoundRect(cLeft, cTop, cLeft + colW, cTop + cardH, 8f, 8f, paintCardBorder)
        canvas.drawText("نسبة متبقي الحيوية", cLeft + 12f, cTop + 20f, paintText)
        canvas.drawText("${String.format("%.2f", kpis.livabilityPercent)}%", cLeft + 12f, cTop + 48f, Paint(paintTitle).apply { color = Color.parseColor("#15803D"); textSize = 16f })

        y += cardH + gap + 15f

        // 3. Resource Summary Box
        canvas.drawText("ثالثاً: ملخص تدفق واستهلاك الموارد الحيوية والمحيط", 40f, y, paintSubTitle)
        canvas.drawLine(40f, y + 5, 555f, y + 5, paintDivider)
        y += 20

        canvas.drawRoundRect(40f, y, 555f, y + 70f, 10f, 10f, paintBox)
        canvas.drawRoundRect(40f, y, 555f, y + 70f, 10f, 10f, paintBoxBorder)

        canvas.drawText("إجمالي استهلاك العلف: ${kpis.totalFeedKg} كجم", 55f, y + 25, paintTextBold)
        canvas.drawText("إجمالي سحب مياه الشرب: ${kpis.totalWaterL} لتر", 300f, y + 25, paintText)

        canvas.drawText("استهلاك العلف اليومي لكل فرخ: ${String.format("%.1f", kpis.dailyFeedPerBirdGrams)} جرام/يوم", 55f, y + 48, paintText)
        canvas.drawText("معدل استهلاك المياه الفردي: ${String.format("%.1f", kpis.dailyWaterPerBirdMl)} مل/يوم", 300f, y + 48, paintText)

        y += 95f

        // 4. Biosecurity Guidelines Box (In Cooperation with Dr. Daifallah)
        canvas.drawText("رابعاً: مصفوفة الأمن الحيوي الاستباقية (بروتوكول د. ضيف الله الحسني)", 40f, y, paintSubTitle)
        canvas.drawLine(40f, y + 5, 555f, y + 5, paintDivider)
        y += 20

        val paintAdviceFill = Paint().apply { color = Color.parseColor("#FFFDF2"); style = Paint.Style.FILL }
        val paintAdviceBorder = Paint().apply { color = Color.parseColor("#F59E0B"); style = Paint.Style.STROKE; strokeWidth = 1f }

        canvas.drawRoundRect(40f, y, 555f, y + 80f, 10f, 10f, paintAdviceFill)
        canvas.drawRoundRect(40f, y, 555f, y + 80f, 10f, 10f, paintAdviceBorder)

        canvas.drawText("١. عزل تام للحظائر بالكامل ومنع عبور الطيور البرية والناقلة كحائط صد وبائي لمرض النيوكاسل.", 55f, y + 22, Paint(paintText).apply { textSize = 9f })
        canvas.drawText("٢. متابعة دقيقة لمراوح التهوية ومعدل استبدال هواء الحظيرة لتفادي صعود مستويات الأمونيا الضارة بالرئات.", 55f, y + 37, Paint(paintText).apply { textSize = 9f })
        canvas.drawText("٣. صيانة جفاف وتطهير فرشة الدجاج باستمرار لمنع تسييل الرطوبة التي تنشط بكتيريا الكوكسيديا المعوية.", 55f, y + 52, Paint(paintText).apply { textSize = 9f })
        canvas.drawText("٤. فحص تدرج معامل التحويل FCR يومياً لكونه الكاشف الفوري الخفي لأي هجوم معوي أو نقص مناعي صامت.", 55f, y + 67, Paint(paintText).apply { textSize = 9f })

        pdfDocument.finishPage(page)

        // ============================================
        // PAGE 2: COMPLETE PERFORMANCE LEDGER TABLE
        // ============================================
        pageInfo = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas

        drawHeaderAndFooter(canvas, 2, 3)

        y = 110f
        canvas.drawText("خامساً: السجل اليومي المتكامل لمتابعة الأداء الفني والبيئي (Standard Flock Ledger)", 40f, y, paintSubTitle)
        canvas.drawLine(40f, y + 5, 555f, y + 5, paintDivider)
        y += 20

        // Table Header
        canvas.drawRect(40f, y, 555f, y + 20f, Paint().apply { color = Color.parseColor("#0F172A") })

        val tableHeaderPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText("العمر", 45f, y + 14f, tableHeaderPaint)
        canvas.drawText("النافق", 85f, y + 14f, tableHeaderPaint)
        canvas.drawText("العلف (كجم)", 125f, y + 14f, tableHeaderPaint)
        canvas.drawText("الماء (لتر)", 195f, y + 14f, tableHeaderPaint)
        canvas.drawText("الوزن (جرام)", 260f, y + 14f, tableHeaderPaint)
        canvas.drawText("حرارة العنبر", 330f, y + 14f, tableHeaderPaint)
        canvas.drawText("رطوبة (%)", 395f, y + 14f, tableHeaderPaint)
        canvas.drawText("الأمونيا", 455f, y + 14f, tableHeaderPaint)
        canvas.drawText("التهوية", 510f, y + 14f, tableHeaderPaint)

        y += 20f

        var rowY = y
        val rowHeight = 16f
        var pageCounter = 2

        // Print ALL records with dynamic page splitting if they exceed the usable bounds of page 2
        for (i in records.indices) {
            val r = records[i]
            
            if (rowY > 770f) {
                // End current page 2 and start temporary sub-page or push remaining
                pdfDocument.finishPage(page)
                pageCounter++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageCounter).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                
                // Draw headers on the continuation page
                drawHeaderAndFooter(canvas, pageCounter, 3)
                
                canvas.drawText("تابع - خامساً: السجل اليومي المتكامل لمتابعة الأداء الفني والبيئي", 40f, 110f, paintSubTitle)
                canvas.drawLine(40f, 115f, 555f, 115f, paintDivider)
                
                canvas.drawRect(40f, 125f, 555f, 145f, Paint().apply { color = Color.parseColor("#0F172A") })
                canvas.drawText("العمر", 45f, 139f, tableHeaderPaint)
                canvas.drawText("النافق", 85f, 139f, tableHeaderPaint)
                canvas.drawText("العلف (كجم)", 125f, 139f, tableHeaderPaint)
                canvas.drawText("الماء (لتر)", 195f, 139f, tableHeaderPaint)
                canvas.drawText("الوزن (جرام)", 260f, 139f, tableHeaderPaint)
                canvas.drawText("حرارة العنبر", 330f, 139f, tableHeaderPaint)
                canvas.drawText("رطوبة (%)", 395f, 139f, tableHeaderPaint)
                canvas.drawText("الأمونيا", 455f, 139f, tableHeaderPaint)
                canvas.drawText("التهوية", 510f, 139f, tableHeaderPaint)
                
                rowY = 145f
            }

            // Zebra striping
            val rowPaint = Paint().apply {
                color = if (i % 2 == 0) Color.parseColor("#FFFFFF") else Color.parseColor("#F8FAFC")
            }
            canvas.drawRect(40f, rowY, 555f, rowY + rowHeight, rowPaint)

            canvas.drawText("${r.ageInDays} يوم", 45f, rowY + 12f, paintText)
            canvas.drawText("${r.mortalityCount}", 85f, rowY + 12f, if (r.mortalityCount > 0) Paint(paintTextBold).apply { color = Color.parseColor("#DC2626") } else paintText)
            canvas.drawText(String.format("%.1f", r.feedConsumptionKg), 125f, rowY + 12f, paintText)
            canvas.drawText(String.format("%.1f", r.waterConsumptionLiters), 195f, rowY + 12f, paintText)
            canvas.drawText("${r.averageWeightGrams.toInt()}ج", 260f, rowY + 12f, paintTextBold)
            canvas.drawText("${r.temperatureCelsius}°م", 330f, rowY + 12f, paintText)
            canvas.drawText("${r.humidityPercent}%", 395f, rowY + 12f, paintText)
            canvas.drawText("${r.ammoniaLevelPpm}ppm", 455f, rowY + 12f, if (r.ammoniaLevelPpm > 15.0) Paint(paintTextBold).apply { color = Color.parseColor("#D97706") } else paintText)
            canvas.drawText("${r.ventilationRate.toInt()}%", 510f, rowY + 12f, paintText)

            canvas.drawLine(40f, rowY + rowHeight, 555f, rowY + rowHeight, paintDivider)
            rowY += rowHeight
        }

        pdfDocument.finishPage(page)

        // ============================================
        // PAGE 3: MEDICAL LOG & CLINICAL DIAGNOSIS HISTORY & OFFICIAL ENDORSEMENT
        // ============================================
        pageInfo = PdfDocument.PageInfo.Builder(595, 842, 3).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas

        drawHeaderAndFooter(canvas, 3, 3)

        y = 110f

        // 1. Scheduled and Executed Immunizations
        canvas.drawText("سادساً: سجل بروتوكولات التحصين والبرامح العلاجية (Medication & Vaccine Log)", 40f, y, paintSubTitle)
        canvas.drawLine(40f, y + 5, 555f, y + 5, paintDivider)
        y += 20

        canvas.drawRect(40f, y, 555f, y + 18f, Paint().apply { color = Color.parseColor("#1E3A8A") })
        canvas.drawText("اسم اللقاح ومكوناته المستهدفة", 45f, y + 12f, tableHeaderPaint)
        canvas.drawText("الجرعة المقررة وطريقة الإعطاء", 230f, y + 12f, tableHeaderPaint)
        canvas.drawText("الحالة التنفيذية", 390f, y + 12f, tableHeaderPaint)
        canvas.drawText("التاريخ الفعلي", 475f, y + 12f, tableHeaderPaint)

        y += 18f

        val allMeds = medsRecords.value
        if (allMeds.isEmpty()) {
            canvas.drawText("عينة الرعاية لم تسجل تلقي لقاحات وقائية للمستند الحالي.", 55f, y + 18f, paintText)
            y += 30f
        } else {
            for (i in allMeds.indices) {
                if (y > 330f) break // Bound to prevent overlapping sections
                val med = allMeds[i]
                val zebraPaint = Paint().apply { color = if (i % 2 == 0) Color.WHITE else Color.parseColor("#F1F5F9") }
                canvas.drawRect(40f, y, 555f, y + 16f, zebraPaint)

                canvas.drawText("${med.name} (${if (med.type == "VACCINE") "تحصين" else "علاج"})", 45f, y + 12f, paintTextBold)
                canvas.drawText(med.dosage, 230f, y + 12f, paintText)
                
                if (med.isCompleted) {
                    canvas.drawText("تم الحقن والإكمال ✓", 390f, y + 12f, Paint(paintTextBold).apply { color = Color.parseColor("#10B981") })
                    val dateStr = if (med.actualDate != null) dateFormat.format(Date(med.actualDate)) else dateFormat.format(Date(med.date))
                    canvas.drawText(dateStr, 475f, y + 12f, paintText)
                } else {
                    canvas.drawText("مجدول وقيد الانتظار ⏳", 390f, y + 12f, Paint(paintText).apply { color = Color.parseColor("#D97706") })
                    canvas.drawText(dateFormat.format(Date(med.date)), 475f, y + 12f, paintText)
                }
                canvas.drawLine(40f, y + 16f, 555f, y + 16f, paintDivider)
                y += 16f
            }
        }

        y += 30f

        // 2. Clinical Diagnostics Registry
        canvas.drawText("سابعاً: الفحوصات الطبية والتشخيصات الحقلية المعتمدة (Clinical Diagnostic Records)", 40f, y, paintSubTitle)
        canvas.drawLine(40f, y + 5, 555f, y + 5, paintDivider)
        y += 20

        canvas.drawRect(40f, y, 555f, y + 18f, Paint().apply { color = Color.parseColor("#0F172A") })
        canvas.drawText("التشخيص الوبائي المتوقع", 45f, y + 12f, tableHeaderPaint)
        canvas.drawText("الاحتمالية", 195f, y + 12f, tableHeaderPaint)
        canvas.drawText("الأعراض والعلامات الحلقية المستكشفة", 255f, y + 12f, tableHeaderPaint)
        canvas.drawText("تاريخ الفحص", 475f, y + 12f, tableHeaderPaint)

        y += 18f

        val diagList = diagnosisRecords.value
        if (diagList.isEmpty()) {
            canvas.drawText("سجل دورة القطيع يخلو من الأوبة والاشتباهات الطبية ولله الحمد والمنة.", 55f, y + 18f, paintText)
            y += 30f
        } else {
            for (i in diagList.indices) {
                if (y > 540f) break
                val diag = diagList[i]
                val zebraPaint = Paint().apply { color = if (i % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC") }
                canvas.drawRect(40f, y, 555f, y + 22f, zebraPaint)

                canvas.drawText(diag.diseaseName, 45f, y + 14f, paintTextBold)
                canvas.drawText("${diag.probability.toInt()}%", 195f, y + 14f, Paint(paintTextBold).apply { color = Color.parseColor("#DC2626") })
                
                // Truncate selected symptoms string formatted nicely
                val sympStr = if (diag.selectedSymptoms.length > 35) diag.selectedSymptoms.take(32) + "..." else diag.selectedSymptoms
                canvas.drawText(sympStr, 255f, y + 14f, paintText)
                canvas.drawText(dateFormat.format(Date(diag.date)), 475f, y + 14f, paintText)

                canvas.drawLine(40f, y + 22f, 555f, y + 22f, paintDivider)
                y += 22f
            }
        }

        y += 50f

        // 3. Official Vet Doctor Endorsement / Verification Box (Dr. Daifallah Husseini Stamp)
        if (y < 610f) y = 610f

        val paintStampFill = Paint().apply { color = Color.parseColor("#FFFDF2"); style = Paint.Style.FILL }
        val paintStampBorder = Paint().apply { color = Color.parseColor("#D97706"); style = Paint.Style.STROKE; strokeWidth = 1.5f }

        canvas.drawRoundRect(40f, y, 555f, y + 130f, 12f, 12f, paintStampFill)
        canvas.drawRoundRect(40f, y, 555f, y + 130f, 12f, 12f, paintStampBorder)

        canvas.drawText("إقرار ومصادقة الأمن الحيوي الفنية • لومار فيت برو فارم", 180f, y + 25f, Paint(paintHeading).apply { color = Color.parseColor("#D97706"); textSize = 11f })
        
        val bodyAdvicePaint = Paint(paintText).apply { textSize = 9.5f; color = Color.parseColor("#334155") }
        canvas.drawText("بموجب الاطلاع الطوعي على المؤشرات الفنية والبيئية الواردة بهذا المستند لدفعة الدواجن الحالية ومقارنتها بمعايير دقة كفاية معامل", 55f, y + 50f, bodyAdvicePaint)
        canvas.drawText("التحويل والنمو، نوصي بشدة بصلابة تدابير التعقيم عند الأبواب وصيانة أهبة النيكل والتلقيحات، راجين لكم دورة مربحة خالية من العلل.", 55f, y + 68f, bodyAdvicePaint)
        
        canvas.drawText("المستشار الطبي المتابع للدورة: د. ضيف الله الحسني", 160f, y + 96f, Paint(paintTextBold).apply { color = Color.parseColor("#0D6E3F"); textSize = 11f })
        canvas.drawText("Lmar Vet ProFarm © المصدق الطبي الرقمي", 210f, y + 115f, Paint(paintText).apply { color = Color.parseColor("#94A3B8"); textSize = 8.5f })

        pdfDocument.finishPage(page)

        // ============================================
        // WRITE FILE AND SHARE
        // ============================================
        try {
            val cachePath = File(context.cacheDir, "shared_pdfs")
            cachePath.mkdirs()
            val pdfFile = File(cachePath, "ProFarm_Report_Batch_${batch.batchNumber}.pdf")
            val fileOutputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fileOutputStream)
            pdfDocument.close()
            fileOutputStream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.lmarvetprofarm.vptprm.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "تقرير الأداء الكلي والصحي للدفعة ${batch.batchNumber}")
                putExtra(Intent.EXTRA_TEXT, "يرجى التفضل بالاطلاع على التقرير الفني والطبي المطبوع للدفعة رقم ${batch.batchNumber} الصادر من تطبيق لومار فيت برو فارم.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير PDF"))

        } catch (e: Exception) {
            Toast.makeText(context, "فشل تصدير التقرير: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun runOfflinePredictions() {
        val batch = _selectedBatch.value ?: return
        val records = _dailyRecords.value
        if (records.isEmpty()) {
            _predictionResults.value = emptyList()
            return
        }
        _predictionResults.value = DiseasePredictor.predictFutureRisks(batch, records)
    }

    fun getAIPredictionPlan() {
        val batch = _selectedBatch.value ?: return
        val records = _dailyRecords.value
        if (records.isEmpty()) return

        val lastRecord = records.last()
        val age = lastRecord.ageInDays
        val breed = batch.breed
        val systemType = batch.systemType

        val recentRecords = records.takeLast(5)
        val maxTemp = recentRecords.map { it.temperatureCelsius }.maxOrNull() ?: lastRecord.temperatureCelsius
        val minTemp = recentRecords.map { it.temperatureCelsius }.minOrNull() ?: lastRecord.temperatureCelsius
        val avgAmmonia = recentRecords.map { it.ammoniaLevelPpm }.average()
        val avgHumidity = recentRecords.map { it.humidityPercent }.average()
        val lastHumidity = lastRecord.humidityPercent

        val summary = "أحدث قراءات الحرارة مؤخراً (${String.format("%.1f", minTemp)} - ${String.format("%.1f", maxTemp)}°م)، متوسط الأمونيا: ${String.format("%.1f", avgAmmonia)} ppm، الرطوبة الحالية: ${String.format("%.1f", lastHumidity)}%، متوسط رطوبة الفرشة المحيطة: ${String.format("%.1f", avgHumidity)}%."

        val risksList = _predictionResults.value
        val risksFoundText = if (risksList.isEmpty()) {
            "لا توجد مخاطر إحصائية مرتفعة مسجلة بذكاء الأرقام الميداني."
        } else {
            risksList.joinToString("\n") { result ->
                "- مرض ${result.disease.nameAr}: احتمالية الحدوث المتوقعة: ${result.probability}% [خطوة الخطر: ${result.riskLevel}]. العوامل المشخصة:\n  " +
                        result.riskFactors.joinToString("\n  ")
            }
        }

        _isAiPredicting.value = true
        _aiPredictionReport.value = null

        viewModelScope.launch {
            try {
                val report = GeminiApi.predictFutureDiseases(
                    ageInDays = age,
                    breed = breed,
                    systemType = systemType,
                    environmentalSummary = summary,
                    risksFoundText = risksFoundText
                )
                _aiPredictionReport.value = report
            } catch (e: Exception) {
                _aiPredictionReport.value = "فشل توليد التحليل التنبؤي من خادم الاستشارة: ${e.localizedMessage}"
            } finally {
                _isAiPredicting.value = false
            }
        }
    }
}

// Helper data class for technical KPI reports
data class KPIReport(
    val initialChicks: Int = 0,
    val remainingBirds: Int = 0,
    val totalMortality: Int = 0,
    val mortalityPercent: Double = 0.0,
    val livabilityPercent: Double = 100.0,
    val totalFeedKg: Double = 0.0,
    val totalWaterL: Double = 0.0,
    val avgWeightGrams: Double = 0.0,
    val fcr: Double = 0.0,
    val adg: Double = 0.0,
    val epi: Double = 0.0,
    val ageInDays: Int = 1,
    val dailyFeedPerBirdGrams: Double = 0.0,
    val dailyWaterPerBirdMl: Double = 0.0,
    val stockingDensity: Double = 0.0,
    val latestTemp: Double = 0.0,
    val latestHumidity: Double = 0.0,
    val latestAmmonia: Double = 0.0,
    val latestVentilation: Double = 0.0
)
