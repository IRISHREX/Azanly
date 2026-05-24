package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AzanDao {

    // --- Notices ---
    @Query("SELECT * FROM notices ORDER BY isPinned DESC, timestamp DESC")
    fun getAllNotices(): Flow<List<NoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: NoticeEntity)

    @Delete
    suspend fun deleteNotice(notice: NoticeEntity)

    @Query("DELETE FROM notices WHERE id = :noticeId")
    suspend fun deleteNoticeById(noticeId: Long)

    // --- Iqamah Timings ---
    @Query("SELECT * FROM iqamah_timings")
    fun getAllIqamahTimings(): Flow<List<IqamahTimingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIqamahTiming(timing: IqamahTimingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIqamahTimings(timings: List<IqamahTimingEntity>)

    // --- Donations ---
    @Query("SELECT * FROM donations ORDER BY timestamp DESC")
    fun getAllDonations(): Flow<List<DonationRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonation(donation: DonationRecordEntity)
}
