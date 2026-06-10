package com.example.data.repository

import com.example.data.local.FarmDao
import com.example.data.model.Batch
import com.example.data.model.DailyRecord
import com.example.data.model.DiagnosisRecord
import com.example.data.model.MedsRecord
import kotlinx.coroutines.flow.Flow

class FarmRepository(private val farmDao: FarmDao) {

    // --- Batches ---
    val allBatches: Flow<List<Batch>> = farmDao.getAllBatches()

    suspend fun getBatchById(id: Int): Batch? {
        return farmDao.getBatchById(id)
    }

    suspend fun insertBatch(batch: Batch): Long {
        return farmDao.insertBatch(batch)
    }

    suspend fun updateBatch(batch: Batch) {
        farmDao.updateBatch(batch)
    }

    suspend fun deleteBatch(batch: Batch) {
        farmDao.deleteBatch(batch)
    }

    // --- Daily Records ---
    fun getDailyRecordsForBatch(batchId: Int): Flow<List<DailyRecord>> {
        return farmDao.getDailyRecordsForBatch(batchId)
    }

    suspend fun insertDailyRecord(record: DailyRecord): Long {
        return farmDao.insertDailyRecord(record)
    }

    suspend fun updateDailyRecord(record: DailyRecord) {
        farmDao.updateDailyRecord(record)
    }

    suspend fun deleteDailyRecord(recordId: Int) {
        farmDao.deleteDailyRecord(recordId)
    }

    // --- Diagnosis Records ---
    fun getDiagnosisRecordsForBatch(batchId: Int): Flow<List<DiagnosisRecord>> {
        return farmDao.getDiagnosisRecordsForBatch(batchId)
    }

    suspend fun insertDiagnosisRecord(record: DiagnosisRecord): Long {
        return farmDao.insertDiagnosisRecord(record)
    }

    // --- Meds Records ---
    fun getMedsRecordsForBatch(batchId: Int): Flow<List<MedsRecord>> {
        return farmDao.getMedsRecordsForBatch(batchId)
    }

    suspend fun insertMedsRecord(record: MedsRecord): Long {
        return farmDao.insertMedsRecord(record)
    }

    suspend fun updateMedsRecord(record: MedsRecord) {
        farmDao.updateMedsRecord(record)
    }

    suspend fun deleteMedsRecord(id: Int) {
        farmDao.deleteMedsRecord(id)
    }
}
