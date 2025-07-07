package com.example.internetbt

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent // Importar Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.concurrent.thread
import android.util.Log

class ClientActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var editTextUrl: EditText
    private lateinit var btnBuscar: Button
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var bluetoothClient: BluetoothClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        initializeViews()
        setupWebView()
        setupClickListeners()

        bluetoothClient = BluetoothClient { status ->
            runOnUiThread {
                Toast.makeText(this@ClientActivity, status, Toast.LENGTH_SHORT).show()
            }
        }

        attemptInitialConnection()
    }

    private fun initializeViews() {
        webView = findViewById(R.id.webView)
        editTextUrl = findViewById(R.id.editTextUrl)
        btnBuscar = findViewById(R.id.btnBuscar)
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() { // <--- WebViewClient personalizado
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString()
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    // Si es una URL HTTP/HTTPS, la enviamos por Bluetooth
                    sendUrlToServer(url)
                    return true // Indicamos que hemos manejado la URL y el WebView no debe cargarla
                }
                // Para otras URLs (ej. tel:, mailto:, o internas del WebView), dejamos que el WebView las maneje
                return false
            }

            // Para compatibilidad con versiones antiguas de Android (API < 24)
            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    sendUrlToServer(url)
                    return true
                }
                return false
            }
        }

        webView.loadDataWithBaseURL(
            null,
            "<html><body><h2>Cliente Bluetooth</h2><p>Ingresa una URL y presiona Buscar para navegar a través del servidor Bluetooth.</p><p>Intentando conectar al servidor...</p></body></html>",
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun setupClickListeners() {
        btnBuscar.setOnClickListener {
            val url = editTextUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    val formattedUrl = "https://$url"
                    sendUrlToServer(formattedUrl)
                } else {
                    sendUrlToServer(url)
                }
            } else {
                Toast.makeText(this, "Por favor ingresa una URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun attemptInitialConnection() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Por favor habilita el Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Se requieren permisos de Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        btnBuscar.isEnabled = false
        btnBuscar.text = "Conectando..."

        thread {
            val serverDevice = findServerDevice()
            if (serverDevice == null) {
                runOnUiThread {
                    Toast.makeText(this@ClientActivity, "Servidor 'BTServer' no encontrado. Asegúrate de que esté emparejado y visible.", Toast.LENGTH_LONG).show()
                    Toast.makeText(this@ClientActivity, "Por favor, inicia el servidor Bluetooth en el dispositivo emparejado.", Toast.LENGTH_LONG).show()
                    btnBuscar.isEnabled = true
                    btnBuscar.text = "Buscar"
                }
                return@thread
            }

            val connected = bluetoothClient.connect(serverDevice)
            runOnUiThread {
                if (connected) {
                    Toast.makeText(this@ClientActivity, "Conectado al servidor Bluetooth.", Toast.LENGTH_SHORT).show()
                    btnBuscar.isEnabled = true
                    btnBuscar.text = "Buscar"
                    sendUrlToServer("https://www.google.com")
                } else {
                    Toast.makeText(this@ClientActivity, "No se pudo conectar al servidor Bluetooth. Revisa los logs.", Toast.LENGTH_LONG).show()
                    btnBuscar.isEnabled = true
                    btnBuscar.text = "Buscar"
                }
            }
        }
    }

    private fun sendUrlToServer(url: String) {
        if (!bluetoothClient.isConnected()) {
            Toast.makeText(this, "No hay conexión activa con el servidor Bluetooth. Intentando reconectar...", Toast.LENGTH_LONG).show()
            attemptInitialConnection()
            return
        }

        btnBuscar.isEnabled = false
        btnBuscar.text = "Cargando..."

        thread {
            val htmlResponse = bluetoothClient.sendAndReceive(url)

            runOnUiThread {
                if (htmlResponse != null) {
                    webView.loadDataWithBaseURL(url, htmlResponse, "text/html", "UTF-8", null)
                    Toast.makeText(this@ClientActivity, "Página cargada", Toast.LENGTH_SHORT).show()
                } else {
                    webView.loadDataWithBaseURL(
                        null,
                        "<html><body><h2>Error de Conexión</h2><p>No se pudo obtener la página. Consulta los mensajes de estado.</p></body></html>",
                        "text/html",
                        "UTF-8",
                        null
                    )
                    Toast.makeText(this@ClientActivity, "Falló la comunicación. Asegúrate de que el servidor Bluetooth esté iniciado y escuchando.", Toast.LENGTH_LONG).show()
                }
                btnBuscar.isEnabled = true
                btnBuscar.text = "Buscar"
            }
        }
    }

    private fun findServerDevice(): BluetoothDevice? {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        return bluetoothAdapter.bondedDevices.find { device ->
            device.name?.contains("S24 de Uriel", ignoreCase = true) == true ||
                    device.name?.contains("InternetBT", ignoreCase = true) == true
        }
    }

    override fun onBackPressed() {
        // Regresar a MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) // Limpiar la pila de actividades
        startActivity(intent)
        finish() // Finalizar esta actividad
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BT_DEBUG_CLIENT", "onDestroy: Desconectando cliente Bluetooth.")
        bluetoothClient.disconnect()
    }
}