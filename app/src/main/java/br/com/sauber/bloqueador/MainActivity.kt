package br.com.sauber.bloqueador

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/**
 * Tela única: ativar o app como "identificador de chamadas", pedir contatos,
 * gerenciar números liberados, horário de bloqueio e ver o histórico.
 */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var statusText: TextView
    private lateinit var roleButton: Button
    private lateinit var contactsButton: Button
    private lateinit var logText: TextView
    private lateinit var totalText: TextView
    private lateinit var allowedList: LinearLayout
    private lateinit var startButton: Button
    private lateinit var endButton: Button

    private val dp: Float get() = resources.displayMetrics.density

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
        val pad = (16 * dp).toInt()

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
            setPadding(0, pad / 2, 0, pad / 2)
            setOnCheckedChangeListener { _, checked -> prefs.blockHidden = checked }
        })

        // ----- Horário -----
        root.addView(sectionTitle("Horário do bloqueio"))

        root.addView(Switch(this).apply {
            text = "Bloquear só em certo horário"
            isChecked = prefs.scheduleEnabled
            setPadding(0, pad / 4, 0, pad / 4)
            setOnCheckedChangeListener { _, checked ->
                prefs.scheduleEnabled = checked
                refresh()
            }
        })

        val timeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        startButton = Button(this).apply {
            setOnClickListener {
                pickTime(prefs.scheduleStart) { prefs.scheduleStart = it; refresh() }
            }
        }
        endButton = Button(this).apply {
            setOnClickListener {
                pickTime(prefs.scheduleEnd) { prefs.scheduleEnd = it; refresh() }
            }
        }
        val half = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        timeRow.addView(startButton, half)
        timeRow.addView(endButton, LinearLayout.LayoutParams(half))
        root.addView(timeRow)

        // ----- Números liberados -----
        root.addView(sectionTitle("Números liberados"))
        root.addView(TextView(this).apply {
            text = "Esses números sempre tocam, mesmo fora dos contatos (entregador, banco, escola...)."
            textSize = 13f
        })

        val addRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val numberInput = EditText(this).apply {
            hint = "Ex.: (32) 99999-0000"
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine()
        }
        val addButton = Button(this).apply {
            text = "Adicionar"
            setOnClickListener {
                val text = numberInput.text.toString()
                when {
                    prefs.addAllowed(text) -> {
                        numberInput.setText("")
                        refresh()
                    }
                    Prefs.onlyDigits(text).length < 8 ->
                        Toast.makeText(this@MainActivity, "Digite o número com DDD.", Toast.LENGTH_SHORT).show()
                    else ->
                        Toast.makeText(this@MainActivity, "Esse número já está na lista.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        addRow.addView(numberInput, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addRow.addView(addButton)
        root.addView(addRow)

        allowedList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(allowedList)

        // ----- Histórico -----
        root.addView(sectionTitle("Histórico"))

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

        val scroll = ScrollView(this).apply {
            addView(root)
            // Android 15+ desenha o app sob as barras do sistema: empurra o conteúdo para dentro.
            setOnApplyWindowInsetsListener { v, insets ->
                val top: Int
                val bottom: Int
                if (Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
                    top = bars.top
                    bottom = bars.bottom
                } else {
                    @Suppress("DEPRECATION")
                    top = insets.systemWindowInsetTop
                    @Suppress("DEPRECATION")
                    bottom = insets.systemWindowInsetBottom
                }
                v.setPadding(0, top, 0, bottom)
                insets
            }
        }
        setContentView(scroll)
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 17f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, (20 * dp).toInt(), 0, (4 * dp).toInt())
    }

    private fun pickTime(current: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(this, { _, h, m -> onPicked(h * 60 + m) }, current / 60, current % 60, true).show()
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

        // Horário
        startButton.text = "Início: ${Prefs.formatMinutes(prefs.scheduleStart)}"
        endButton.text = "Fim: ${Prefs.formatMinutes(prefs.scheduleEnd)}"
        startButton.isEnabled = prefs.scheduleEnabled
        endButton.isEnabled = prefs.scheduleEnabled

        // Números liberados
        allowedList.removeAllViews()
        val allowed = prefs.allowedNumbers()
        if (allowed.isEmpty()) {
            allowedList.addView(TextView(this).apply {
                text = "Nenhum número liberado."
                textSize = 14f
                setPadding(0, (8 * dp).toInt(), 0, 0)
            })
        }
        for (number in allowed) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(
                TextView(this).apply { text = number; textSize = 16f },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            row.addView(Button(this).apply {
                text = "Remover"
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("Remover $number da lista de liberados?")
                        .setPositiveButton("Remover") { _, _ -> prefs.removeAllowed(number); refresh() }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            })
            allowedList.addView(row)
        }

        // Histórico
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
