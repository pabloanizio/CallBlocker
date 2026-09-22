package com.pablogold.callblocker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
@JvmSuppressWildcards
interface TemporaryWhitelistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTemporaryWhitelist(item: TemporaryWhitelistEntity): Long

    @Query("SELECT * FROM temporary_whitelist WHERE phoneNumber = :phoneNumber AND expiresAt > :now")
    suspend fun isWhitelisted(phoneNumber: String, now: Long): TemporaryWhitelistEntity?

    @Query("DELETE FROM temporary_whitelist WHERE expiresAt <= :now")
    suspend fun purgeExpired(now: Long): Int

    @Query("DELETE FROM temporary_whitelist WHERE phoneNumber = :phoneNumber")
    suspend fun removeFromWhitelist(phoneNumber: String): Int
}
