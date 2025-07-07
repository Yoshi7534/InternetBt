package com.example.internetbt

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebView // Importar WebView
import android.webkit.WebViewClient // Importar WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat

class ServerActivity : ComponentActivity() {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var textStatus: TextView
    private lateinit var webViewServer: WebView // Declarar el WebView
    private var bluetoothServer: BluetoothServer? = null

       override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        textStatus = findViewById(R.id.textStatus)
        webViewServer = findViewById(R.id.webViewServer) // <--- Aquí se usa, después de la declaración.

        setupWebView()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ensureBluetoothEnabled()
    }


    private fun setupWebView() {
        webViewServer.settings.javaScriptEnabled = true
        webViewServer.settings.domStorageEnabled = true
        webViewServer.webViewClient = WebViewClient()

        // Cargar una página inicial en el WebView del servidor
        webViewServer.loadDataWithBaseURL(
            null,
            "<html><body><h2>Servidor Bluetooth</h2><p>Esperando solicitudes de URL del cliente...</p></body></html>",
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun ensureBluetoothEnabled() {
        if (!bluetoothAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, 1)
        } else {
            makeDiscoverable()
        }
    }

    private fun makeDiscoverable() {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(discoverableIntent, 2)
        } else {
            startServer()
        }
    }

    private fun startServer() {
        try {
            // Pasar ambos callbacks al constructor de BluetoothServer
            bluetoothServer = BluetoothServer(
                statusCallback = { status ->
                    runOnUiThread {
                        textStatus.text = status
                    }
                },
                htmlCallback = { html -> // Implementación del nuevo callback para el HTML
                    runOnUiThread {
                        webViewServer.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    }
                }
            )
            bluetoothServer?.start()
            textStatus.text = "Servidor iniciado. Esperando conexiones..."
            Toast.makeText(this, "Servidor Bluetooth iniciado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            textStatus.text = "Error al iniciar servidor: ${e.message}"
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            1 -> { // Bluetooth enable request
                if (resultCode == RESULT_OK) {
                    makeDiscoverable()
                } else {
                    Toast.makeText(this, "Bluetooth es necesario para continuar", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            2 -> { // Discoverable request
                startServer()
            }
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
        bluetoothServer?.stopServer()
    }
}