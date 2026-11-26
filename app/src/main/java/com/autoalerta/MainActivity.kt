package com.autoalerta

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.IOException
import java.util.UUID

class MainActivity : Activity() {
    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val mainHandler = Handler(Looper.getMainLooper())
    private var socket: BluetoothSocket? = null
    private var readerThread: Thread? = null
    private var connected = false
    private var tone: ToneGenerator? = null
    private lateinit var connectionLabel: TextView
    private lateinit var alertLabel: TextView
    private lateinit var connectButton: Button
    private lateinit var activateButton: Button
    private lateinit var deactivateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tone = ToneGenerator(AudioManager.STREAM_ALARM, 90)
        buildScreen()
        if (bluetoothAdapter == null) {
            setConnectionState("Bluetooth no disponible", false)
            connectButton.isEnabled = false
        }
    }

    private fun buildScreen() {
        val bg = Color.rgb(15, 18, 22)
        val panel = Color.rgb(26, 31, 37)
        val text = Color.rgb(239, 242, 245)
        val muted = Color.rgb(156, 166, 176)
        val accent = Color.rgb(125, 190, 163)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(18))
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = TextView(this).apply {
            this.text = "AUTO\nALERTA"
            textSize = 22f
            setTextColor(text)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = .08f
        }
        val badge = TextView(this).apply {
            this.text = "  SISTEMA DE SEGURIDAD  "
            textSize = 10f
            setTextColor(accent)
            background = rounded(Color.rgb(34, 55, 48), 18)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(badge)
        root.addView(header)
        root.addView(label("SUPERVISIÓN DEL VEHÍCULO", 12f, muted, true).apply { setPadding(0, dp(26), 0, dp(12)) })

        val connectionCard = card(panel)
        connectionCard.addView(label("Conexión Bluetooth", 17f, text, true))
        connectionLabel = label("●  Por conectar", 14f, muted, false).apply { setPadding(0, dp(8), 0, dp(14)) }
        connectionCard.addView(connectionLabel)
        connectButton = button("Conectar HC-05", accent, Color.rgb(18, 27, 24)) { connectOrChoose() }
        connectionCard.addView(connectButton, matchWrap())
        root.addView(connectionCard, matchWrap().apply { bottomMargin = dp(14) })

        root.addView(label("CONTROL DE PRUEBA", 11f, muted, true).apply { setPadding(0, dp(8), 0, dp(10)) })
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        activateButton = button("ACTIVAR\nComando: 1", Color.rgb(36, 76, 61), text) { sendCommand("1") }
        deactivateButton = button("DESACTIVAR\nComando: 0", Color.rgb(57, 63, 70), text) { sendCommand("0") }
        activateButton.isEnabled = false
        deactivateButton.isEnabled = false
        controls.addView(activateButton, LinearLayout.LayoutParams(0, dp(88), 1f).apply { rightMargin = dp(7) })
        controls.addView(deactivateButton, LinearLayout.LayoutParams(0, dp(88), 1f).apply { leftMargin = dp(7) })
        root.addView(controls)

        val alertCard = card(panel).apply { setPadding(dp(16), dp(15), dp(16), dp(15)) }
        alertCard.addView(label("ALARMA SONORA", 11f, muted, true))
        alertLabel = label("Sin alertas recibidas", 15f, text, true).apply { setPadding(0, dp(8), 0, 0) }
        alertCard.addView(alertLabel)
        root.addView(alertCard, matchWrap().apply { topMargin = dp(18) })

        val spacer = View(this)
        root.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))
        val emergency = button("EMERGENCIAS 911  →", Color.rgb(43, 48, 54), text) { openEmergencyDialer() }
        root.addView(emergency, matchWrap())
        root.addView(label("Abre el marcador telefónico; la llamada requiere confirmación.", 11f, muted, false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(6))
        })
        root.addView(label("AutoAlerta · Arduino UNO + HC-05", 10f, muted, false).apply { gravity = Gravity.CENTER; alpha = .7f })
        setContentView(root)
    }

    private fun card(color: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(color, 18)
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private fun button(value: String, color: Int, foreground: Int, action: () -> Unit) = Button(this).apply {
        text = value
        textSize = 13f
        setTextColor(foreground)
        isAllCaps = false
        background = rounded(color, 14)
        setOnClickListener { action() }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun hasConnectPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun connectOrChoose() {
        if (!hasConnectPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 10)
            return
        }
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) {
            Toast.makeText(this, "Activa Bluetooth en los ajustes del teléfono", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val devices = adapter.bondedDevices.toList()
            if (devices.isEmpty()) {
                Toast.makeText(this, "Primero empareja el HC-05 en los ajustes Bluetooth", Toast.LENGTH_LONG).show()
                return
            }
            val choices = devices.map { "${it.name ?: "Dispositivo"} · ${it.address}" }.toTypedArray()
            android.app.AlertDialog.Builder(this)
                .setTitle("Selecciona el módulo HC-05")
                .setItems(choices) { _, index -> connectTo(devices[index]) }
                .setNegativeButton("Cancelar", null)
                .show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Se necesita permiso para consultar Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectTo(device: BluetoothDevice) {
        setConnectionState("Conectando…", false)
        connectButton.isEnabled = false
        Thread {
            try {
                val newSocket = device.createRfcommSocketToServiceRecord(sppUuid)
                newSocket.connect()
                socket = newSocket
                connected = true
                mainHandler.post {
                    setConnectionState("Conectado · ${device.name ?: "HC-05"}", true)
                    connectButton.text = "Conectado"
                    activateButton.isEnabled = true
                    deactivateButton.isEnabled = true
                    Toast.makeText(this, "Conexión establecida", Toast.LENGTH_SHORT).show()
                }
                listenForAlerts(newSocket)
            } catch (e: Exception) {
                connected = false
                try { socket?.close() } catch (_: IOException) { }
                socket = null
                mainHandler.post {
                    setConnectionState("No se pudo conectar", false)
                    connectButton.isEnabled = true
                    connectButton.text = "Reintentar conexión"
                    Toast.makeText(this, "Verifica que el HC-05 esté encendido y emparejado", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun listenForAlerts(activeSocket: BluetoothSocket) {
        readerThread = Thread {
            val buffer = StringBuilder()
            try {
                val input = activeSocket.inputStream
                while (connected) {
                    val value = input.read()
                    if (value < 0) break
                    val char = value.toChar()
                    if (char == '\n' || char == '\r') {
                        if (buffer.toString().contains("ALERTA", ignoreCase = true)) showAlarm()
                        buffer.clear()
                    } else {
                        buffer.append(char)
                        if (buffer.length > 256) buffer.delete(0, buffer.length - 32)
                        if (buffer.toString().contains("ALERTA", ignoreCase = true)) {
                            showAlarm()
                            buffer.clear()
                        }
                    }
                }
            } catch (_: Exception) { }
            connected = false
            mainHandler.post {
                setConnectionState("Conexión interrumpida", false)
                connectButton.isEnabled = true
                connectButton.text = "Conectar HC-05"
                activateButton.isEnabled = false
                deactivateButton.isEnabled = false
            }
        }.apply { start() }
    }

    private fun showAlarm() {
        mainHandler.post {
            alertLabel.text = "⚠  ALERTA recibida · revisa tu vehículo"
            alertLabel.setTextColor(Color.rgb(255, 174, 111))
            tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900)
            Toast.makeText(this, "¡AutoAlerta detectó una señal!", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendCommand(command: String) {
        val activeSocket = socket
        if (!connected || activeSocket == null) {
            Toast.makeText(this, "Conecta el HC-05 antes de enviar comandos", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            try {
                activeSocket.outputStream.write((command + "\n").toByteArray(Charsets.UTF_8))
                activeSocket.outputStream.flush()
                mainHandler.post { Toast.makeText(this, "Comando $command enviado", Toast.LENGTH_SHORT).show() }
            } catch (_: IOException) {
                mainHandler.post { Toast.makeText(this, "No se pudo enviar el comando", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun setConnectionState(value: String, isConnected: Boolean) {
        connectionLabel.text = if (isConnected) "●  $value" else "○  $value"
        connectionLabel.setTextColor(if (isConnected) Color.rgb(125, 190, 163) else Color.rgb(190, 170, 143))
    }

    private fun openEmergencyDialer() {
        try {
            startActivity(Intent(Intent.ACTION_DIAL).apply { data = android.net.Uri.parse("tel:911") })
        } catch (_: Exception) {
            Toast.makeText(this, "No se pudo abrir el marcador telefónico", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) connectOrChoose()
        else if (requestCode == 10) Toast.makeText(this, "Permiso Bluetooth necesario para conectar el módulo", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        connected = false
        try { socket?.close() } catch (_: IOException) { }
        tone?.release()
        super.onDestroy()
    }
}
