package com.example.data.local

import androidx.room.*
import com.example.data.model.Batch
import com.example.data.model.DailyRecord
import com.example.data.model.DiagnosisRecord
import com.example.data.model.MedsRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {
    // --- Batches ---
    @Query("SELECT * FROM batches ORDER BY startDate DESC")
    fun getAllBatches(): Flow<List<Batch>>

    @Query("SELECT * FROM batches WHERE id = :id LIMIT 1")
    suspend fun getBatchById(id: Int): Batch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: Batch): Long

    @Update
    suspend fun updateBatch(batch: Batch)

    @Delete
    suspend fun deleteBatch(batch: Batch)

    // --- Daily Records ---
    @Query("SELECT * FROM daily_records WHERE batchId = :batchId ORDER BY date ASC")
    fun getDailyRecordsForBatch(batchId: Int): Flow<List<DailyRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyRecord(record: DailyRecord): Long

    @Update
    suspend fun updateDailyRecord(record: DailyRecord)

    @Query("DELETE FROM daily_records WHERE id = :recordId")
    suspend fun deleteDailyRecord(recordId: Int)

    // --- Diagnosis Records ---
    @Query("SELECT * FROM diagnosis_records WHERE batchId = :batchId ORDER BY date DESC")
    fun getDiagnosisRecordsForBatch(batchId: Int): Flow<List<DiagnosisRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosisRecord(record: DiagnosisRecord): Long

    // --- Vaccination & Medication Records ---
    @Query("SELECT * FROM meds_records WHERE batchId = :batchId ORDER BY date ASC")
    fun getMedsRecordsForBatch(batchId: Int): Flow<List<MedsRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedsRecord(record: MedsRecord): Long

    @Update
    suspend fun updateMedsRecord(record: MedsRecord)

    @Query("DELETE FROM meds_records WHERE id = :id")
    suspend fun deleteMedsRecord(id: Int)
}
