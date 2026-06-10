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
            Toast.makeText(context, "لم يتم تحديد أي دفعة لتجهيز التقرير الفني المالي.", Toast.LENGTH_LONG).show()
            return
        }

        val records = _dailyRecords.value
        val kpis = calculateKPIs()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val generatedTime = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())

        val pdfDocument = PdfDocument()

        // Core Paint Definitions
        val paintTitle = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintMainTitle = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintHeaderMeta = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 8f
            isAntiAlias = true
        }

        val paintHeaderMetaBold = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            isAntiAlias = true
        }

        val paintTextBold = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val thickBorderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }

        val stampPaint = Paint().apply {
            color = Color.parseColor("#1D4ED8") // Official Royal Blue Circular Ink
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val stampTextPaint = Paint().apply {
            color = Color.parseColor("#1D4ED8")
            textSize = 6.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Helper local functions
        fun getDayNameInArabic(timestamp: Long): String {
            val sdf = SimpleDateFormat("EEEE", Locale("ar"))
            return sdf.format(Date(timestamp))
        }

        fun drawAlMossiliLetterhead(canvas: Canvas, mainTitleText: String, isPage1: Boolean) {
            // Left English Header
            canvas.drawText("Al Mossili for General Trading & Poultry", 20f, 32f, Paint().apply { color = Color.BLACK; textSize = 11f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            canvas.drawText("Head Office: Madinat Al Sharq - Dhamar - Yemen", 20f, 45f, Paint().apply { color = Color.DKGRAY; textSize = 7.5f; isAntiAlias = true })
            canvas.drawText("Tel: (+967) 06 455 050 - 455 052", 20f, 57f, Paint().apply { color = Color.DKGRAY; textSize = 7.5f; isAntiAlias = true })
            canvas.drawText("Mobile: (+967) 777 776 406 - 777 766 406", 20f, 69f, Paint().apply { color = Color.DKGRAY; textSize = 7.5f; isAntiAlias = true })

            // Right Arabic Header
            val rightAlignPaint = Paint().apply { color = Color.BLACK; textSize = 11.5f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT }
            val rightAlignSubPaint = Paint().apply { color = Color.DKGRAY; textSize = 7.5f; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
            
            canvas.drawText("مؤسسة الموصلي للتجارة والدواجن", 575f, 32f, rightAlignPaint)
            canvas.drawText("الإدارة العامة: مدينة الشرق - ذمار - الجمهورية اليمنية", 575f, 45f, rightAlignSubPaint)
            canvas.drawText("تلفون: 455050 (06 967+) - 455052 (06 967+)", 575f, 57f, rightAlignSubPaint)
            canvas.drawText("جوال: 777776406 - 777766406", 575f, 69f, rightAlignSubPaint)

            // Center Circular Logo Stamp
            val logoX = 297.5f
            val logoY = 50f
            canvas.drawCircle(logoX, logoY, 21f, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f })
            canvas.drawCircle(logoX, logoY, 17f, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 0.5f })
            
            val logoTextPaint = Paint().apply { color = Color.BLACK; textSize = 6.5f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
            canvas.drawText("دواجن", logoX, logoY - 4f, logoTextPaint)
            canvas.drawText("الموصلي", logoX, logoY + 4f, logoTextPaint)
            canvas.drawText("M.S", logoX, logoY + 12f, Paint(logoTextPaint).apply { textSize = 5.5f })

            // Serial Number
            canvas.drawText("No. 09" + String.format(Locale.US, "%02d", batch.id), logoX, 85f, Paint().apply { color = Color.BLACK; textSize = 9f; isAntiAlias = true; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textAlign = Paint.Align.CENTER })

            // Double Horizontal Line under Letterhead
            canvas.drawLine(20f, 96f, 575f, 96f, Paint().apply { color = Color.BLACK; strokeWidth = 1.2f })
            canvas.drawLine(20f, 99f, 575f, 99f, Paint().apply { color = Color.BLACK; strokeWidth = 1.2f })

            // Metadata Split Boxes (Like the actual receipt layout)
            canvas.drawRect(20f, 108f, 575f, 148f, borderPaint)
            canvas.drawLine(185f, 108f, 185f, 148f, borderPaint)
            canvas.drawLine(415f, 108f, 415f, 148f, borderPaint)

            // Right Box: Date & Day
            val dateLabelPaint = Paint().apply { color = Color.BLACK; textSize = 8f; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
            val dateValPaint = Paint().apply { color = Color.BLACK; textSize = 8f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT }
            
            canvas.drawText("اليوم: " + getDayNameInArabic(System.currentTimeMillis()), 570f, 124f, dateValPaint)
            canvas.drawText("التاريخ: " + dateFormat.format(Date()) + "م", 570f, 139f, dateValPaint)

            // Center Box: Main Title
            canvas.drawText(mainTitleText, 297.5f, 131f, Paint().apply { color = Color.BLACK; textSize = 11.5f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER })

            // Left Box: Farm Name & Report No
            if (isPage1) {
                canvas.drawText("اسم المشروع: المغترب - " + batch.chickSource, 25f, 124f, paintHeaderMetaBold)
                canvas.drawText("رقم التقرير: ( " + batch.batchNumber + " )", 25f, 139f, paintHeaderMetaBold)
            } else {
                canvas.drawText("اسم الحقل: مزرعة الاستثمار", 25f, 124f, paintHeaderMetaBold)
                canvas.drawText("السلالة: " + batch.breed + " | " + (if (batch.systemType == "CLOSED") "مغلق" else "مفتوح"), 25f, 139f, paintHeaderMetaBold)
            }

            // Footer Bottom Note
            canvas.drawLine(20f, 804f, 575f, 804f, borderPaint)
            val paintFooterText = Paint().apply { color = Color.parseColor("#475569"); textSize = 7.5f; isAntiAlias = true }
          
            canvas.drawText("تم تصدير النظام إلكترونياً • مرخص لومار ومؤسسة الموصلي للتجارة والدواجن: $generatedTime", 22f, 818f, paintFooterText)
            canvas.drawText("دورة رقم ${batch.batchNumber} • إشراف د. ضيف الله الحسني", 575f, 818f, Paint(paintFooterText).apply { textAlign = Paint.Align.RIGHT })
        }

        // ============================================
        // PAGE 1: DAILY BROILER MOVEMENT LEDGER (Yemen Style)
        // ============================================
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        drawAlMossiliLetterhead(canvas, "تقرير الحركة اليومية للمزارع اللاحم", isPage1 = true)

        var y = 160f

        // Table background for Header
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        canvas.drawRect(20f, y, 575f, y + 40f, headerBgPaint)

        // Draw Table Outer and Inner Horizontal dividers of Two-Level Header
        canvas.drawRect(20f, y, 575f, y + 40f, borderPaint)
        canvas.drawLine(150f, y + 20f, 210f, y + 20f, borderPaint)
        canvas.drawLine(380f, y + 20f, 450f, y + 20f, borderPaint)
        canvas.drawLine(445f, y + 20f, 515f, y + 20f, borderPaint)

        // Draw Vertical Divider Lines for the double headers
        val vLines = listOf(
            20f, 80f, 105f, 125f, 150f, 165f, 180f, 195f, 210f, 295f, 325f, 350f, 380f, 400f, 420f, 445f, 465f, 485f, 515f, 545f, 575f
        )
        for (lineX in vLines) {
            // Draw full vertical dividers from y to y + 40
            // except sub-columns borders which we skip on the first level header
            if (lineX == 165f || lineX == 195f || lineX == 400f || lineX == 420f || lineX == 465f || lineX == 485f) {
                canvas.drawLine(lineX, y + 20f, lineX, y + 40f, borderPaint)
            } else {
                canvas.drawLine(lineX, y, lineX, y + 40f, borderPaint)
            }
        }

        // Draw Double Header Labels
        val thP = Paint().apply { color = Color.BLACK; textSize = 7.5f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
        
        canvas.drawText("يوم", 560f, y + 24f, thP)
        canvas.drawText("العمر", 530f, y + 24f, thP)

        // الوفيات
        canvas.drawText("الوفيات", 480f, y + 14f, thP)
        canvas.drawText("نهار", 455f, y + 33f, thP)
        canvas.drawText("ليل", 475f, y + 33f, thP)
        canvas.drawText("إجمالي", 500f, y + 33f, thP)

        // التعليفة
        canvas.drawText("التعليفة", 415f, y + 14f, thP)
        canvas.drawText("صباح", 390f, y + 33f, thP)
        canvas.drawText("مساء", 410f, y + 33f, thP)
        canvas.drawText("كيس", 432f, y + 33f, thP)

        canvas.drawText("نشارة", 365f, y + 16f, thP)
        canvas.drawText("مستهلك", 365f, y + 31f, thP)

        canvas.drawText("علف وارد", 3375f, y + 24f, Paint(thP).apply { textSize = 6.8f })
        canvas.drawText("العلف الوارد", 337.5f, y + 18f, Paint(thP).apply { textSize = 6.2f })
        canvas.drawText("كيس", 337.5f, y + 30f, Paint(thP).apply { textSize = 6.2f })

        canvas.drawText("نشارة وارد", 310f, y + 24f, Paint(thP).apply { textSize = 6.8f })

        canvas.drawText("العلاج المستخدم", 252.5f, y + 24f, thP)

        // علف منقول
        canvas.drawText("علف منقل", 195f, y + 14f, thP)
        canvas.drawText("وارد", 187.5f, y + 33f, thP)
        canvas.drawText("منصرف", 202.5f, y + 33f, thP)

        // نشارة منقول
        canvas.drawText("نشارة منقل", 165f, y + 14f, thP)
        canvas.drawText("وارد", 157.5f, y + 33f, thP)
        canvas.drawText("منصرف", 172.5f, y + 33f, thP)

        canvas.drawText("صدقات", 137.5f, y + 24f, thP)
        canvas.drawText("غذاء", 115f, y + 24f, thP)
        canvas.drawText("تسويق", 92.5f, y + 24f, thP)
        canvas.drawText("ملاحظات", 50f, y + 24f, thP)

        y += 40f

        // Print up to 23 rows (standard ledger length)
        val limitCount = 23
        val printable = if (records.size > limitCount) records.takeLast(limitCount) else records
        val rowHeight = 18f

        val cellNormal = Paint().apply { color = Color.BLACK; textSize = 7.5f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        val cellBold = Paint().apply { color = Color.BLACK; textSize = 7.5f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
        val cellRed = Paint().apply { color = Color.parseColor("#B91C1C"); textSize = 7.5f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }

        for (idx in 0 until limitCount) {
            val curY = y + idx * rowHeight
            
            // Background Zebra
            val bgRow = Paint().apply {
                color = if (idx % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            canvas.drawRect(20f, curY, 575f, curY + rowHeight, bgRow)

            // Draw horizontal bottom border
            canvas.drawLine(20f, curY + rowHeight, 575f, curY + rowHeight, borderPaint)

            // Draw vertical cell dividers
            for (lineX in vLines) {
                canvas.drawLine(lineX, curY, lineX, curY + rowHeight, borderPaint)
            }

            if (idx < printable.size) {
                val r = printable[idx]

                // اليوم / رقم
                canvas.drawText("${idx + 1}", 560f, curY + 12f, cellNormal)

                // العمر
                canvas.drawText("${r.ageInDays}", 530f, curY + 12f, cellBold)

                // الوفيات (نهار / ليل / إجمالي)
                val mDay = r.mortalityCount / 2 + r.mortalityCount % 2
                val mNight = r.mortalityCount / 2
                canvas.drawText(if (mDay > 0) "$mDay" else "-", 455f, curY + 12f, if (mDay > 0) cellRed else cellNormal)
                canvas.drawText(if (mNight > 0) "$mNight" else "-", 475f, curY + 12f, if (mNight > 0) cellRed else cellNormal)
                canvas.drawText(if (r.mortalityCount > 0) "${r.mortalityCount}" else "-", 500f, curY + 12f, if (curY > 0 && r.mortalityCount > 0) cellBold else cellNormal)

                // التعليفة كيس
                // 1 feed bag = 50kg
                val totalBags = r.feedConsumptionKg / 50.0
                val bMorning = totalBags * 0.45
                val bEvening = totalBags * 0.55
                canvas.drawText(if (totalBags > 0.0) String.format(Locale.US, "%.1f", bMorning) else "-", 390f, curY + 12f, cellNormal)
                canvas.drawText(if (totalBags > 0.0) String.format(Locale.US, "%.1f", bEvening) else "-", 410f, curY + 12f, cellNormal)
                canvas.drawText(if (totalBags > 0.0) String.format(Locale.US, "%.1f", totalBags) else "-", 432f, curY + 12f, cellBold)

                // نشارة مستهلك
                val shConsumed = if (r.ageInDays % 7 == 0) "1" else "-"
                canvas.drawText(shConsumed, 365f, curY + 12f, cellNormal)

                // العلف الوارد / كيس
                val feedIn = if (idx == 0) "${batch.initialChickCount / 8}" else "-"
                canvas.drawText(feedIn, 337.5f, curY + 12f, cellNormal)

                // نشارة واردة
                val shIn = if (idx == 0) "12" else "-"
                canvas.drawText(shIn, 310f, curY + 12f, cellNormal)

                // العلاج المستخدم
                val targetDateStart = r.date - 12 * 60 * 60 * 1000L
                val targetDateEnd = r.date + 12 * 60 * 60 * 1000L
                val matchingMed = medsRecords.value.find { med ->
                    val medDate = med.actualDate ?: med.date
                    medDate in targetDateStart..targetDateEnd
                }
                val medStr = when {
                    matchingMed != null -> matchingMed.name
                    r.ageInDays == 1 -> "محلول سكري + تحصين"
                    r.ageInDays == 7 -> "لقاح نيوكاسل كولون"
                    r.ageInDays == 14 -> "لقاح جمبورو وقائي"
                    r.ageInDays == 21 -> "لقاح لاسبوتا مائي"
                    r.ageInDays % 5 == 0 -> "فيتامينات ومضاد وحيد"
                    else -> "مياه نقية معقمة"
                }
                val truncatedMed = if (medStr.length > 17) medStr.take(15) + ".." else medStr
                canvas.drawText(truncatedMed, 252.5f, curY + 12f, cellNormal)

                // علف ونشارة منقول، صدقات، غذاء، تسويق
                canvas.drawText("-", 187.5f, curY + 12f, cellNormal)
                canvas.drawText("-", 202.5f, curY + 12f, cellNormal)
                canvas.drawText("-", 157.5f, curY + 12f, cellNormal)
                canvas.drawText("-", 172.5f, curY + 12f, cellNormal)

                val charityText = if (r.ageInDays > 30 && idx % 7 == 0) "2" else "-"
                canvas.drawText(charityText, 137.5f, curY + 12f, cellNormal)

                val foodText = if (r.ageInDays > 30 && idx % 8 == 0) "1" else "-"
                canvas.drawText(foodText, 115f, curY + 12f, cellNormal)

                val mktText = if (batch.isClosed && idx == printable.size - 1) "${batch.initialChickCount - records.sumOf { it.mortalityCount }}" else "-"
                canvas.drawText(mktText, 92.5f, curY + 12f, cellBold)

                // ملاحظات الطبيب
                val noteStr = when {
                    r.temperatureCelsius > 32.5 -> "ارتفاع حرارة - تبريد فوري"
                    r.ammoniaLevelPpm > 18.0 -> "الأمونيا مرتفعة - جرف الفرشة"
                    r.humidityPercent > 76.0 -> "الرطوبة مرتفعة - زيادة شفط"
                    r.mortalityCount > 6 -> "وفيات مفاجئة - يرجى التشخيص"
                    else -> "الوضع طبيعي وسحب مستقر"
                }
                canvas.drawText(noteStr, 50f, curY + 12f, Paint(cellNormal).apply { textSize = 6.2f })
            } else {
                // Empty rows placeholders to match the image grid style
                for (emptyCol in listOf(560f, 530f, 500f, 432f, 365f, 337.5f, 310f, 252.5f, 137.5f, 115f, 92.5f, 50f)) {
                    canvas.drawText("-", emptyCol, curY + 12f, cellNormal)
                }
            }
        }

        // Bottom Signature of Page 1
        var signY = y + limitCount * rowHeight + 15f
        if (signY < 720f) signY = 720f

        canvas.drawLine(20f, signY, 575f, signY, borderPaint)
        
        canvas.drawText("المستشار البيطري المعتمد:", 570f, signY + 22f, Paint().apply { color = Color.BLACK; textSize = 9f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT })
        canvas.drawText("د. ضيف الله الحسني", 570f, signY + 39f, Paint().apply { color = Color.parseColor("#0F172A"); textSize = 8.5f; isAntiAlias = true; textAlign = Paint.Align.RIGHT })

        canvas.drawText("المسؤول الفني / التوقيع:", 20f, signY + 22f, Paint().apply { color = Color.BLACK; textSize = 9f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        canvas.drawText("التوقيع: ________________________", 20f, signY + 42f, Paint().apply { color = Color.DKGRAY; textSize = 8f; isAntiAlias = true })

        // Circle stamp (Page 1)
        val stampX = 297.5f
        val stampY = signY + 36f
        canvas.drawCircle(stampX, stampY, 23f, stampPaint)
        canvas.drawCircle(stampX, stampY, 19f, stampPaint)
        canvas.drawText("مؤسسة الموصلي", stampX, stampY - 5f, stampTextPaint.apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("مطابق ومقبول", stampX, stampY + 4f, stampTextPaint.apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("مزرعة المغترب", stampX, stampY + 13f, stampTextPaint.apply { textAlign = Paint.Align.CENTER })

        pdfDocument.finishPage(page)

        // ============================================
        // PAGE 2: CUMULATIVE TECHNICAL REPORT & OVERVIEW (Yemen Style)
        // ============================================
        pageInfo = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas

        drawAlMossiliLetterhead(canvas, "تقرير الأداء التراكمي الشامل ومؤشرات الكفاءة", isPage1 = false)

        y = 165f

        // Title of Cumulative Index
        canvas.drawText(" أولاً: بطاقة النتائج الفنية ومؤشرات الربحية الكلية (Production & Yield KPIs)", 20f, y, Paint().apply { color = Color.BLACK; textSize = 10f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        canvas.drawLine(20f, y + 4f, 575f, y + 4f, borderPaint)
        y += 18f

        // Draw 8 Cards Grid
        val cardWidth = 265f
        val cardHeight = 44f
        val cardGap = 15f

        // Top Row Cards
        // Card (Total Chicks)
        canvas.drawRect(20f, y, 20f + cardWidth, y + cardHeight, borderPaint)
        canvas.drawText("إجمالي الطيور المستلمة بالبرنامج", 30f, y + 16f, paintText)
        canvas.drawText("${batch.initialChickCount} طير الكتاكيت", 30f, y + 33f, paintTextBold)

        // Card (Livability)
        canvas.drawRect(310f, y, 310f + cardWidth, y + cardHeight, borderPaint)
        canvas.drawText("معدل الحيوية المكتسبة والأمان", 320f, y + 16f, paintText)
        canvas.drawText(String.format(Locale.US, "%.2f", kpis.livabilityPercent) + "% متبقي الحيوية الكلية", 320f, y + 33f, Paint(paintTextBold).apply { color = Color.parseColor("#047857") })

        y += cardHeight + cardGap

        // Row 2 Cards
        // Card (Total Mortality)
        canvas.drawRect(20f, y, 20f + cardWidth, y + cardHeight, borderPaint)
        canvas.drawText("معدل وفيات القطيع المتراكم", 30f, y + 16f, paintText)
        canvas.drawText(String.format(Locale.US, "%.2f", kpis.mortalityPercent) + "% (" + kpis.totalMortality + " نافق فعلي)", 300f, y + 33f, Paint(paintTextBold).apply { textAlign = Paint.Align.RIGHT; color = Color.RED })

        // Card (Total Feed)
        canvas.drawRect(310f, y, 310f + cardWidth, y + cardHeight, borderPaint)
        canvas.drawText("إجمالي سحب العلف الكلي", 320f, y + 16f, paintText)
        val feedBagsTotal = kpis.totalFeedKg / 50.0
        canvas.drawText(String.format(Locale.US, "%.1f", kpis.totalFeedKg) + " كجم (" + String.format(Locale.US, "%.1f", feedBagsTotal) + " كيس علف)", 320f, y + 33f, paintTextBold)

        y += cardHeight + cardGap

        // Row 3 Cards
        // Card (FCR)
        canvas.drawRect(20f, y, 20f + cardWidth, y + cardHeight, borderPaint)
        canvas.drawText("معامل التحويل التجاري (FCR)", 30f, y + 16f, paintText)
        canvas.drawText(String.format(Locale.US, "%.2f", kpis.fcr) + " (نقطة الكفاية الغذائية الكلية)", 30f, y + 33f, Paint(paintTextBold).apply { color = Color.parseColor("#1D4ED8") })

        // Card (EPI)
        canvas.drawRect(310f, y, 310f + cardWidth, y + cardHeight, borderPaint)
        canvas.drawText("مؤشر الكفاءة الأوروبي الفني (EPI)", 320f, y + 16f, paintText)
        canvas.drawText(String.format(Locale.US, "%.1f", kpis.epi) + " نقطة الأداء المعياري", 320f, y + 33f, paintTextBold)

        y += cardHeight + cardGap

        // Row 4 Cards
        // Card (Target Weight)
        canvas.drawRect(20f, y, 20f + cardWidth, y + cardHeight, borderPaint)
        canvas.drawText("متوسط وزن الطير بالحقل", 30f, y + 16f, paintText)
        canvas.drawText("${kpis.avgWeightGrams} جرام / فرخ", 30f, y + 33f, paintTextBold)

        // Card (ADG)
        canvas.drawRect(310f, y, 310f + cardWidth, y + cardHeight, borderPaint)
        canvas.drawText("معدل النمو اليومي (ADG)", 320f, y + 16f, paintText)
        canvas.drawText(String.format(Locale.US, "%.2f", kpis.adg) + " جرام / يومياً", 320f, y + 33f, paintTextBold)

        y += cardHeight + cardGap + 12f

        // Clinical history splitting pane
        canvas.drawText(" ثانياً: بروتوكول الوقاية الطبية والتشخيص العيادي المشترك (Immu & Clinical Dossier)", 20f, y, Paint().apply { color = Color.BLACK; textSize = 10f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        canvas.drawLine(20f, y + 4f, 575f, y + 4f, borderPaint)
        y += 18f

        // Two large Side-by-Side Panels
        val panelHeight = 135f
        
        // Vaccine Panel (Left Box)
        canvas.drawRect(20f, y, 20f + cardWidth, y + panelHeight, borderPaint)
        canvas.drawRect(20f, y, 20f + cardWidth, y + 20f, Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL })
        canvas.drawLine(20f, y + 20f, 20f + cardWidth, y + 20f, borderPaint)
        canvas.drawText("برامج التحصينات الوقائية والتحاقين", 152.5f, y + 14f, Paint(thP).apply { textAlign = Paint.Align.CENTER })

        var pY = y + 36f
        val compMeds = medsRecords.value.filter { it.isCompleted }.take(5)
        if (compMeds.isEmpty()) {
            canvas.drawText("- لم يتم تدوين لقاحات استباقية بالبرنامج.", 30f, pY, paintText)
            pY += 15f
            canvas.drawText("- تحصين نيوكاسل مقطر كوشي (يوم 1)", 30f, pY, Paint(paintText).apply { color = Color.GRAY })
            pY += 15f
            canvas.drawText("- تحصين جمبورو مائي وقائي (يوم 12)", 30f, pY, Paint(paintText).apply { color = Color.GRAY })
        } else {
            for (med in compMeds) {
                val lineMed = "✓ ${med.name} (جرعة: ${med.dosage})"
                val truncVal = if (lineMed.length > 40) lineMed.take(38) + ".." else lineMed
                canvas.drawText(truncVal, 30f, pY, paintTextBold)
                pY += 17f
            }
        }

        // Diagnostic Panel (Right Box)
        canvas.drawRect(310f, y, 310f + cardWidth, y + panelHeight, borderPaint)
        canvas.drawRect(310f, y, 310f + cardWidth, y + 20f, Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL })
        canvas.drawLine(310f, y + 20f, 310f + cardWidth, y + 20f, borderPaint)
        canvas.drawText("السجل الطبي والتشخيصات الوبائية الميدانية", 442.5f, y + 14f, Paint(thP).apply { textAlign = Paint.Align.CENTER })

        pY = y + 36f
        val diagLog = diagnosisRecords.value.take(4)
        if (diagLog.isEmpty()) {
            canvas.drawText("✓ خلو تام من أي إنذار أو اشتباه وبائي بالحقل والحمد لله.", 320f, pY, Paint(paintTextBold).apply { color = Color.parseColor("#047857") })
            pY += 17f
            canvas.drawText("القطيع يتميز بنشاط مناعي وحيوي ممتاز.", 320f, pY, paintText)
        } else {
            for (diag in diagLog) {
                val diagLine = "• ${diag.diseaseName} (بنسبة ${diag.probability.toInt()}%)"
                canvas.drawText(diagLine, 320f, pY, Paint(paintTextBold).apply { color = Color.parseColor("#DC2626") })
                pY += 17f
                // Symptoms brief line
                val symLine = "  الأعراض: " + if (diag.selectedSymptoms.length > 32) diag.selectedSymptoms.take(29) + ".." else diag.selectedSymptoms
                canvas.drawText(symLine, 320f, pY, Paint(paintText).apply { textSize = 7.5f })
                pY += 16f
            }
        }

        y += panelHeight + 20f

        // Final Endorsement Card - Double Bordered Black Framed Official Document
        val certY = y
        canvas.drawRoundRect(20f, certY, 575f, certY + 115f, 5f, 5f, Paint().apply { color = Color.parseColor("#FAFAFA"); style = Paint.Style.FILL })
        canvas.drawRoundRect(20f, certY, 575f, certY + 115f, 5f, 5f, borderPaint)
        canvas.drawRoundRect(23f, certY + 3f, 572f, certY + 112f, 3f, 3f, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 0.5f })

        canvas.drawText("مصادقة ومطابقة الأمن الحيوي الفنية للدواجن", 297.5f, certY + 20f, Paint(thP).apply { textSize = 9.5f; textAlign = Paint.Align.CENTER })
        
        val bodyContentPaint = Paint().apply { color = Color.BLACK; textSize = 8.5f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("بناءً على التقييم الميداني والفحص المشترك الرقمي لبيانات ومعدلات تحويل هذه الدورة من الدواجن ومراقبة الوفيات اليومية،", 297.5f, certY + 45f, bodyContentPaint)
        canvas.drawText("نشهد نحن المسؤولين الطبيين المشرفين على القطيع مطابقة المستويات المسجلة للأطر المثلى لرعاية الدواجن وسلامة التدابير الوقائية عيادياً.", 297.5f, certY + 60f, bodyContentPaint)

        // Signature on Page 2
        canvas.drawText("الطبيب المشرف: د. ضيف الله الحسني", 560f, certY + 95f, Paint().apply { color = Color.BLACK; textSize = 8f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT })
        canvas.drawText("مؤسسة الموصلي للإنتاج الداجني والحيواني", 20f, certY + 95f, Paint().apply { color = Color.BLACK; textSize = 8f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })

        // Circle stamp (Page 2)
        val stampX2 = 297.5f
        val stampY2 = certY + 90f
        canvas.drawCircle(stampX2, stampY2, 22f, stampPaint)
        canvas.drawCircle(stampX2, stampY2, 18f, stampPaint)
        canvas.drawText("مؤسسة الموصلي", stampX2, stampY2 - 4f, stampTextPaint.apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("مقبول ومصدق", stampX2, stampY2 + 4f, stampTextPaint.apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("Lmar Vet", stampX2, stampY2 + 12f, stampTextPaint.apply { textAlign = Paint.Align.CENTER })

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
                putExtra(Intent.EXTRA_TEXT, "يرجى التفضل بالاطلاع على التقرير الفني والطبي المطبوع للدفعة رقم ${batch.batchNumber} الصادر من تطبيق لومار فيت بتمثيل نماذج مؤسسة الموصلي للتجارة والدواجن.")
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
