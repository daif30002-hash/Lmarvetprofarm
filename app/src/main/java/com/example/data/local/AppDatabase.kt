package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Batch
import com.example.data.model.DailyRecord
import com.example.data.model.DiagnosisRecord
import com.example.data.model.MedsRecord

@Database(
    entities = [Batch::class, DailyRecord::class, DiagnosisRecord::class, MedsRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun farmDao(): FarmDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lmar_vet_profarm_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
