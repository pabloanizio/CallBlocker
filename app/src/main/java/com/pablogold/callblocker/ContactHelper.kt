package com.pablogold.callblocker // Use o seu pacote real

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract.PhoneLookup

object ContactHelper {

    fun isContactInAgenda(context: Context, phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false

        // URI de busca otimizada do Android para números de telefone
        val uri = Uri.withAppendedPath(
            PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        // Projetamos apenas o ID para ser extremamente rápido (milissegundos)
        val projection = arrayOf(PhoneLookup._ID)

        return try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                // Se o cursor tiver pelo menos 1 linha, o contato existe
                cursor.count > 0
            } ?: false
        } catch (e: SecurityException) {
            // Caso a permissão READ_CONTACTS tenha sido revogada pelo usuário
            false
        }
    }
}