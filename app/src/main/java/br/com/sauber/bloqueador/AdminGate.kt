package br.com.sauber.bloqueador

import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Senha de administrador das versões de TESTE (APK compartilhado fora da loja).
 *
 * A senha em si NUNCA fica no código: o build (GitHub) lê o segredo ADMIN_PASSWORD,
 * calcula um hash PBKDF2 e embute só o hash. Sem o segredo, ou no pacote da Play Store
 * (ADMIN_GATE=0), a trava fica desligada.
 */
object AdminGate {

    /** Deve bater com o valor usado em app/build.gradle.kts. */
    private const val ITERATIONS = 120_000

    /** True se este build exige a senha de administrador. */
    val required: Boolean
        get() = BuildConfig.ADMIN_GATE && BuildConfig.ADMIN_HASH.isNotEmpty()

    fun verify(password: String): Boolean {
        if (password.isEmpty()) return false
        return try {
            val spec = PBEKeySpec(password.toCharArray(), fromHex(BuildConfig.ADMIN_SALT), ITERATIONS, 256)
            val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            MessageDigest.isEqual(hash, fromHex(BuildConfig.ADMIN_HASH))
        } catch (e: Exception) {
            false
        }
    }

    private fun fromHex(s: String): ByteArray =
        ByteArray(s.length / 2) { i -> s.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}
