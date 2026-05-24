package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notices")
data class NoticeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "General", "Jama'at", "Khutbah", "Funeral", "Donation"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(tableName = "iqamah_timings")
data class IqamahTimingEntity(
    @PrimaryKey val prayerName: String, // "Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"
    val adhanTime: String,              // e.g. "04:45"
    val iqamahTime: String,             // e.g. "05:15"
    val minutesOffset: Int = 15         // countdown helper variable
)

@Entity(tableName = "donations")
data class DonationRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contributorName: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String? = null
)
