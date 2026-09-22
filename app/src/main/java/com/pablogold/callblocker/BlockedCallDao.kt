package com.pablogold.callblocker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface BlockedCallDao {
    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC")
    fun getAllBlockedCalls(): Flow<List<BlockedCallEntity>>

    @Insert
    suspend fun insert(call: BlockedCallEntity): Long

    @Insert
    suspend fun insertBlockedCall(call: BlockedCallEntity): Long

    @Query("SELECT COUNT(*) FROM blocked_calls WHERE phoneNumber = :phoneNumber AND timestamp >= :sinceTimestamp")
    suspend fun countRecentAttempts(phoneNumber: String, sinceTimestamp: Long): Int

    @Query("DELETE FROM blocked_calls")
    suspend fun clearHistory(): Int
}
