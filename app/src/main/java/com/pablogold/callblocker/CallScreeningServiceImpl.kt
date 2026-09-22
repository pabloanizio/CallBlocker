package com.pablogold.callblocker

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class CallScreeningServiceImpl : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart ?: ""
        Log.d("CallBlocker", ">>> Interceptando chamada: '$rawNumber'")

        val settings = SettingsManager(applicationContext)
        val database = AppDatabase.getDatabase(applicationContext)

        val isEnabled = runBlocking { settings.isBlockingEnabled.first() }
        val actionMode = runBlocking { settings.actionMode.first() }
        val skipCallLog = runBlocking { settings.skipCallLog.first() }

        val isEmergencyEnabled = runBlocking { settings.isEmergencyBypassEnabled.first() }
        val requiredAttempts = runBlocking { settings.emergencyAttempts.first() }
        val windowMinutes = runBlocking { settings.emergencyWindowMinutes.first() }

        if (!isEnabled) {
            Log.d("CallBlocker", ">>> Bloqueador pausado nas configurações. Chamada permitida.")
            allowCall(callDetails)
            return
        }

        // 1. Identificação de contato nativo (apenas contactDisplayName)
        val contactName = callDetails.contactDisplayName
        if (!contactName.isNullOrBlank()) {
            Log.d("CallBlocker", "✅ APROVADA: Identificado na agenda pelo sistema ($contactName)")
            allowCall(callDetails)
            return
        }

        // 2. Consulta manual na agenda (ContactsContract)
        if (isSavedInContacts(applicationContext, rawNumber)) {
            Log.d("CallBlocker", "✅ APROVADA: Encontrado na agenda local")
            allowCall(callDetails)
            return
        }

        // 3. Regra de Emergência: Repetição recente
        if (isEmergencyEnabled && rawNumber.isNotBlank()) {
            val windowMillis = windowMinutes * 60 * 1000L
            val sinceTimestamp = System.currentTimeMillis() - windowMillis

            val recentAttempts = runBlocking {
                database.blockedCallDao().countRecentAttempts(rawNumber, sinceTimestamp)
            }

            Log.d("CallBlocker", "Tentativas recentes de '$rawNumber' nos últimos $windowMinutes min: $recentAttempts (Exige: $requiredAttempts)")

            if (recentAttempts >= requiredAttempts) {
                Log.d("CallBlocker", "🚨 APROVADA: Modo de emergência acionado por repetição!")
                allowCall(callDetails)
                return
            }
        }

        // 4. Bloqueia e salva no histórico
        Log.d("CallBlocker", "🚫 BLOQUEADA: Fora da agenda ('$rawNumber'). Ação: $actionMode")
        runBlocking {
            database.blockedCallDao().insert(
                BlockedCallEntity(
                    phoneNumber = rawNumber,
                    actionTaken = if (actionMode == "REJECT") "REJEITADA" else "SILENCIADA"
                )
            )
        }

        blockCall(callDetails, actionMode, skipCallLog)
    }

    private fun allowCall(callDetails: Call.Details) {
        val response = CallResponse.Builder().build()
        respondToCall(callDetails, response)
    }

    private fun blockCall(callDetails: Call.Details, actionMode: String, skipCallLog: Boolean) {
        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(actionMode == "REJECT")
            .setSilenceCall(true)
            .setSkipCallLog(skipCallLog)
            .setSkipNotification(true)
            .build()
        respondToCall(callDetails, response)
    }

    private fun isSavedInContacts(context: Context, rawNumber: String): Boolean {
        if (rawNumber.isBlank()) return false

        val digitsOnly = rawNumber.filter { it.isDigit() }
        if (digitsOnly.length < 8) return false
        val suffix = digitsOnly.takeLast(8)

        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

            // Campos que precisamos inspecionar
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.IN_VISIBLE_GROUP
            )

            // Filtro: deve coincidir com o final do número E fazer parte do grupo visível (agenda real)
            val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ? AND ${ContactsContract.CommonDataKinds.Phone.IN_VISIBLE_GROUP} = 1"
            val selectionArgs = arrayOf("%$suffix")

            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val displayName = if (nameIndex != -1) cursor.getString(nameIndex)?.trim() ?: "" else ""
                    val storedNumber = if (numberIndex != -1) cursor.getString(numberIndex)?.filter { it.isDigit() } ?: "" else ""
                    val cleanDisplayName = displayName.filter { it.isDigit() }

                    // Se o nome exibido for vazio OU for idêntico ao próprio número,
                    // significa que é um número do histórico/recente e NÃO um contato salvo de verdade
                    val isJustTheNumber = cleanDisplayName.isNotBlank() && cleanDisplayName == storedNumber

                    if (displayName.isNotBlank() && !isJustTheNumber) {
                        Log.d("CallBlocker", "✅ Contato legítimo salvo na agenda: '$displayName'")
                        return true
                    } else {
                        Log.d("CallBlocker", "⚠️ Número encontrado no cache, mas marcado como não salvo: '$displayName'")
                    }
                }
                false
            } ?: false
        } catch (e: Exception) {
            Log.e("CallBlocker", "Erro ao validar contato na agenda", e)
            false
        }
    }
}