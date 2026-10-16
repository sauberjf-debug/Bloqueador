package br.com.sauber.bloqueador

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** Tela única: ativar o app como "identificador de chamadas", pedir contatos e ver o histórico. */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var statusText: TextView
    private lateinit var roleButton: Button
    private lateinit var contactsButton: Button
    private lateinit var logText: TextView
    private lateinit var totalText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        buildUi()

        // Configuração automática: a cada abertura nova do app, pede o que ainda falta,
        // na ordem certa (contatos primeiro, depois o papel de triagem de chamadas).
        if (savedInstanceState == null) {
            if (!hasContactsPermission()) {
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQ_CONTACTS)
            } else if (!hasScreeningRole()) {
                requestScreeningRole()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun buildUi() {
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "Bloqueador de Ligações"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        })

        statusText = TextView(this).apply {
            textSize = 15f
            setPadding(0, pad / 2, 0, pad / 2)
        }
        root.addView(statusText)

        // Passo 1: permissão de contatos
        contactsButton = Button(this).apply {
            text = "1) Permitir acesso aos contatos"
            setOnClickListener {
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQ_CONTACTS)
            }
        }
        root.addView(contactsButton)

        // Passo 2: virar o app de identificação de chamadas padrão
        roleButton = Button(this).apply {
            text = "2) Ativar como app de triagem de chamadas"
            setOnClickListener { requestScreeningRole() }
        }
        root.addView(roleButton)

        root.addView(Switch(this).apply {
            text = "Bloqueio ativo"
            isChecked = prefs.enabled
            setPadding(0, pad, 0, pad / 2)
            setOnCheckedChangeListener { _, checked -> prefs.enabled = checked }
        })

        root.addView(Switch(this).apply {
            text = "Bloquear também números ocultos"
            isChecked = prefs.blockHidden
            setPadding(0, pad / 2, 0, pad)
            setOnCheckedChangeListener { _, checked -> prefs.blockHidden = checked }
        })

        totalText = TextView(this).apply {
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(totalText)

        logText = TextView(this).apply {
            textSize = 14f
            setPadding(0, pad / 2, 0, 0)
        }
        root.addView(logText)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun hasContactsPermission() =
        checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    private fun hasScreeningRole(): Boolean {
        val rm = getSystemService(RoleManager::class.java)
        return rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestScreeningRole() {
        val rm = getSystemService(RoleManager::class.java)
        if (!rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, "Este aparelho não oferece triagem de chamadas.", Toast.LENGTH_LONG).show()
            return
        }
        @Suppress("DEPRECATION")
        startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), REQ_ROLE)
    }

    private fun refresh() {
        val contacts = hasContactsPermission()
        val role = hasScreeningRole()

        contactsButton.visibility = if (contacts) View.GONE else View.VISIBLE
        roleButton.visibility = if (role) View.GONE else View.VISIBLE

        statusText.text = when {
            contacts && role ->
                "✅ Tudo pronto. Ligações de números fora dos seus contatos são rejeitadas automaticamente."
            else ->
                "⚠️ Falta configurar: " +
                    listOfNotNull(
                        if (!contacts) "acesso aos contatos" else null,
                        if (!role) "ativar como app de triagem" else null
                    ).joinToString(" e ") + "."
        }

        totalText.text = "Bloqueadas até agora: ${prefs.totalBlocked}"
        val recent = prefs.recentBlocked()
        logText.text = if (recent.isEmpty()) "Nenhuma ligação bloqueada ainda." else recent.joinToString("\n")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refresh()
        // Contatos liberados: segue direto para a janela de filtro de chamadas.
        if (requestCode == REQ_CONTACTS && hasContactsPermission() && !hasScreeningRole()) {
            requestScreeningRole()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        refresh()
    }

    private companion object {
        const val REQ_CONTACTS = 1
        const val REQ_ROLE = 2
    }
}
