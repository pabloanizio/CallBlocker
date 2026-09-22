package com.pablogold.callblocker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "temporary_whitelist")
data class TemporaryWhitelistEntity(
    @PrimaryKey
    val phoneNumber: String,
    val expiresAt: Long
)
