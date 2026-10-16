package br.com.sauber.bloqueador

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Configurações simples + histórico das últimas ligações bloqueadas. */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("bloqueador", Context.MODE_PRIVATE)

    /** Liga/desliga o bloqueio sem precisar desinstalar o app. */
    var enabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, true)
        set(value) = sp.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Bloquear também chamadas com número oculto/privado. */
    var blockHidden: Boolean
        get() = sp.getBoolean(KEY_BLOCK_HIDDEN, true)
        set(value) = sp.edit().putBoolean(KEY_BLOCK_HIDDEN, value).apply()

    val totalBlocked: Int
        get() = sp.getInt(KEY_TOTAL, 0)

    fun recentBlocked(): List<String> =
        (sp.getString(KEY_LOG, "") ?: "")
            .split("\n")
            .filter { it.isNotBlank() }

    fun logBlocked(number: String?) {
        val time = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date())
        val entry = "$time  ${number ?: "Número oculto"}"
        val updated = (listOf(entry) + recentBlocked()).take(MAX_LOG)
        sp.edit()
            .putString(KEY_LOG, updated.joinToString("\n"))
            .putInt(KEY_TOTAL, totalBlocked + 1)
            .apply()
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BLOCK_HIDDEN = "block_hidden"
        private const val KEY_TOTAL = "total_blocked"
        private const val KEY_LOG = "log"
        private const val MAX_LOG = 50
    }
}
