package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoticeEntity::class,
        IqamahTimingEntity::class,
        DonationRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AzanDatabase : RoomDatabase() {

    abstract fun azanDao(): AzanDao

    companion object {
        @Volatile
        private var INSTANCE: AzanDatabase? = null

        fun getDatabase(context: Context): AzanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AzanDatabase::class.java,
                    "azan_mahalla_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
