package br.com.sauber.bloqueador

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract.PhoneLookup
import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Chamado pelo Android a cada ligação recebida (inclusive de operadoras como Claro, Vivo, TIM).
 * Se o número não estiver nos contatos nem na lista de liberados, a ligação é rejeitada antes de tocar.
 *
 * Regra de segurança: em qualquer dúvida ou erro, a ligação é PERMITIDA
 * (melhor tocar uma vez a mais do que perder uma ligação importante).
 * Chamadas de emergência nunca passam por este serviço.
 */
class ContactCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        try {
            if (shouldBlock(callDetails)) {
                val number = callDetails.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }
                Prefs(this).logBlocked(number)
                respondToCall(
                    callDetails,
                    CallResponse.Builder()
                        .setDisallowCall(true)      // impede a chamada de tocar
                        .setRejectCall(true)        // rejeita (quem liga cai na caixa postal/ocupado)
                        .setSkipNotification(true)  // sem notificação de chamada perdida
                        .setSkipCallLog(false)      // mantém no histórico para você conferir depois
                        .build()
                )
            } else {
                allow(callDetails)
            }
        } catch (e: Exception) {
            allow(callDetails)
        }
    }

    private fun shouldBlock(callDetails: Call.Details): Boolean {
        // Só triagem de chamadas recebidas.
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return false

        val prefs = Prefs(this)
        if (!prefs.enabled) return false

        // Versão de teste ainda não liberada com a senha de administrador: não bloqueia nada.
        if (AdminGate.required && !prefs.unlocked) return false

        // Fora do horário configurado, tudo passa.
        if (!prefs.isBlockingNow()) return false

        // Sem permissão de contatos não dá para saber quem é conhecido: não bloqueia nada.
        val hasContactsPermission =
            checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!hasContactsPermission) return false

        val number: String? = callDetails.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }

        // Número oculto/privado
        if (number == null) return prefs.blockHidden

        // Lista de liberados e contatos sempre passam.
        if (prefs.isAllowed(number)) return false
        return !isInContacts(number)
    }

    private fun allow(callDetails: Call.Details) {
        respondToCall(callDetails, CallResponse.Builder().build())
    }

    /** Usa a busca de telefone do próprio Android, que já normaliza +55, DDD, 9º dígito etc. */
    private fun isInContacts(number: String): Boolean {
        return try {
            val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            contentResolver.query(uri, arrayOf(PhoneLookup._ID), null, null, null)
                ?.use { it.count > 0 }
                ?: true // se a consulta falhar, assume que é contato (não bloqueia)
        } catch (e: Exception) {
            true
        }
    }
}
