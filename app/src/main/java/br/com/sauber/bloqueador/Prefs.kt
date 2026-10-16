package br.com.sauber.bloqueador

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Configurações simples + lista de números liberados + histórico das últimas ligações bloqueadas. */
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

    // ---------- Horário ----------

    /** Se ligado, só bloqueia dentro da janela [scheduleStart, scheduleEnd]. */
    var scheduleEnabled: Boolean
        get() = sp.getBoolean(KEY_SCHEDULE_ON, false)
        set(value) = sp.edit().putBoolean(KEY_SCHEDULE_ON, value).apply()

    /** Minutos desde 00:00. Padrão: 22:00. */
    var scheduleStart: Int
        get() = sp.getInt(KEY_SCHEDULE_START, 22 * 60)
        set(value) = sp.edit().putInt(KEY_SCHEDULE_START, value).apply()

    /** Minutos desde 00:00. Padrão: 07:00. */
    var scheduleEnd: Int
        get() = sp.getInt(KEY_SCHEDULE_END, 7 * 60)
        set(value) = sp.edit().putInt(KEY_SCHEDULE_END, value).apply()

    /** True se o bloqueio deve valer agora (sem horário configurado = sempre). */
    fun isBlockingNow(): Boolean {
        if (!scheduleEnabled) return true
        val cal = Calendar.getInstance()
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = scheduleStart
        val end = scheduleEnd
        return when {
            start == end -> true                       // janela de 24h
            start < end -> now in start until end      // ex.: 08:00–18:00
            else -> now >= start || now < end          // vira a meia-noite: 22:00–07:00
        }
    }

    // ---------- Números liberados ----------

    fun allowedNumbers(): List<String> =
        (sp.getString(KEY_ALLOWED, "") ?: "")
            .split("\n")
            .filter { it.isNotBlank() }

    /** Retorna false se o número for inválido (menos de 8 dígitos) ou já estiver na lista. */
    fun addAllowed(raw: String): Boolean {
        val digits = onlyDigits(raw)
        if (digits.length < 8) return false
        if (allowedNumbers().any { sameNumber(it, digits) }) return false
        sp.edit().putString(KEY_ALLOWED, (allowedNumbers() + digits).joinToString("\n")).apply()
        return true
    }

    fun removeAllowed(number: String) {
        sp.edit()
            .putString(KEY_ALLOWED, allowedNumbers().filter { it != number }.joinToString("\n"))
            .apply()
    }

    fun isAllowed(number: String): Boolean {
        val digits = onlyDigits(number)
        if (digits.isEmpty()) return false
        return allowedNumbers().any { sameNumber(it, digits) }
    }

    // ---------- Histórico ----------

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
        private const val KEY_SCHEDULE_ON = "schedule_on"
        private const val KEY_SCHEDULE_START = "schedule_start"
        private const val KEY_SCHEDULE_END = "schedule_end"
        private const val KEY_ALLOWED = "allowed"
        private const val KEY_TOTAL = "total_blocked"
        private const val KEY_LOG = "log"
        private const val MAX_LOG = 50

        fun onlyDigits(s: String) = s.filter { it.isDigit() }

        /** Compara pelos últimos 8 dígitos: ignora +55, DDD e o 9º dígito. */
        fun sameNumber(a: String, b: String): Boolean {
            val x = onlyDigits(a)
            val y = onlyDigits(b)
            return if (x.length >= 8 && y.length >= 8) x.takeLast(8) == y.takeLast(8) else x == y
        }

        fun formatMinutes(total: Int) = "%02d:%02d".format(total / 60, total % 60)
    }
}
