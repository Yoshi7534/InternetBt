package com.example.internetbt

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log // Importar la clase Log
import androidx.annotation.RequiresPermission
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

class BluetoothServer(
    private val statusCallback: (String) -> Unit,
    private val htmlCallback: (String) -> Unit // Nuevo callback para el HTML
) : Thread() {
    private val TAG = "BT_DEBUG_SERVER" // Etiqueta para Logcat
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private var serverSocket: BluetoothServerSocket? = null
    @Volatile private var isRunning = true

    // Delimitador para indicar el final de una respuesta HTML
    private val END_OF_HTML_DELIMITER = "<END_OF_HTML>"

    override fun run() {
        Log.d(TAG, "BluetoothServer: Hilo de servidor iniciado.")
        try {
            if (bluetoothAdapter == null) {
                statusCallback("Error: Adaptador Bluetooth no disponible.")
                Log.e(TAG, "BluetoothServer: Adaptador Bluetooth no disponible.")
                return
            }
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BTServer", uuid)
            statusCallback("Servidor escuchando en puerto RFCOMM...")
            Log.d(TAG, "BluetoothServer: Servidor escuchando con UUID: $uuid")

            while (isRunning && !isInterrupted) {
                try {
                    statusCallback("Esperando conexiones...")
                    Log.d(TAG, "BluetoothServer: Esperando conexiones...")
                    val socket: BluetoothSocket = serverSocket!!.accept()
                    statusCallback("Cliente conectado: ${socket.remoteDevice.name ?: "Desconocido"}")
                    Log.d(TAG, "BluetoothServer: Cliente conectado: ${socket.remoteDevice.name ?: "Desconocido"} - ${socket.remoteDevice.address}")

                    Thread {
                        handleClientConnection(socket)
                    }.start()

                } catch (e: IOException) {
                    if (isRunning) {
                        statusCallback("Error en conexión: ${e.message}")
                        Log.e(TAG, "BluetoothServer: Error en accept() o conexión: ${e.message}", e)
                    } else {
                        Log.d(TAG, "BluetoothServer: serverSocket cerrado, saliendo del bucle accept().")
                    }
                    break
                }
            }
        } catch (e: SecurityException) {
            statusCallback("Error de permisos: ${e.message}")
            Log.e(TAG, "BluetoothServer: Error de permisos al iniciar servidor: ${e.message}", e)
        } catch (e: Exception) {
            statusCallback("Error del servidor: ${e.message}")
            Log.e(TAG, "BluetoothServer: Error inesperado en el hilo del servidor: ${e.message}", e)
        } finally {
            try {
                serverSocket?.close()
                Log.d(TAG, "BluetoothServer: serverSocket cerrado en finally.")
            } catch (e: IOException) {
                Log.e(TAG, "BluetoothServer: Error al cerrar serverSocket en finally: ${e.message}", e)
            }
            statusCallback("Servidor detenido.")
            Log.d(TAG, "BluetoothServer: Hilo de servidor detenido.")
        }
    }

    private fun handleClientConnection(socket: BluetoothSocket) {
        Log.d(TAG, "handleClientConnection: Manejando conexión con ${socket.remoteDevice.name ?: "Desconocido"}")
        var clientConnected = true
        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))

            // Bucle para manejar múltiples solicitudes del mismo cliente
            while (clientConnected) {
                val url = reader.readLine() // Espera la URL del cliente
                if (url != null) {
                    statusCallback("Solicitando: $url")
                    Log.d(TAG, "handleClientConnection: URL recibida: $url")

                    val html = HttpFetcher.fetchHtml(url)
                    Log.d(TAG, "handleClientConnection: HTML obtenido (longitud: ${html.length})")

                    // Enviar el HTML al callback para que la UI del servidor lo muestre
                    htmlCallback(html)

                    writer.write(html)
                    writer.write(END_OF_HTML_DELIMITER + "\n") // Enviar delimitador
                    writer.flush()
                    Log.d(TAG, "handleClientConnection: HTML enviado al cliente. Bytes enviados: ${html.toByteArray().size}. Delimitador enviado.")

                    statusCallback("Respuesta enviada para: $url")
                } else {
                    // Si readLine() devuelve null, el cliente ha cerrado su lado del socket
                    Log.d(TAG, "handleClientConnection: Cliente desconectado (readLine() returned null).")
                    clientConnected = false
                }
            }
        } catch (e: IOException) {
            statusCallback("Error procesando cliente: ${e.message}")
            Log.e(TAG, "handleClientConnection: Error de E/S al procesar cliente: ${e.message}", e)
            clientConnected = false // Terminar el bucle en caso de error de E/S
        } finally {
            try {
                socket.close() // Cerrar el socket del cliente cuando el bucle termina o hay un error
                statusCallback("Cliente desconectado")
                Log.d(TAG, "handleClientConnection: Socket de cliente cerrado.")
            } catch (e: IOException) {
                statusCallback("Error cerrando conexión: ${e.message}")
                Log.e(TAG, "handleClientConnection: Error al cerrar socket de cliente: ${e.message}", e)
            }
        }
    }

    fun stopServer() {
        Log.d(TAG, "stopServer: Solicitando detención del servidor.")
        isRunning = false
        try {
            serverSocket?.close()
            Log.d(TAG, "stopServer: serverSocket cerrado.")
        } catch (e: IOException) {
            Log.e(TAG, "stopServer: Error al cerrar serverSocket: ${e.message}", e)
        }
    }

    override fun interrupt() {
        Log.d(TAG, "interrupt: Hilo de servidor interrumpido externamente.")
        super.interrupt()
    }
}
