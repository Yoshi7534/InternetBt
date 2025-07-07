package com.example.internetbt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log // Import Log for debugging
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

class BluetoothClient(private val statusCallback: (String) -> Unit) {

    private val TAG = "BT_DEBUG_CLIENT" // Tag for Logcat
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private var bluetoothSocket: BluetoothSocket? = null // Keep the socket open

    // Delimitador para indicar el final de una respuesta HTML (debe coincidir con el del servidor)
    private val END_OF_HTML_DELIMITER = "<END_OF_HTML>"

    /**
     * Establishes a Bluetooth connection to a specific device.
     * @param device The Bluetooth device to connect to (the server).
     * @return true if connection is successful, false otherwise.
     */
    fun connect(device: BluetoothDevice): Boolean {
        Log.d(TAG, "connect: Attempting to connect to ${device.name ?: "Unknown Device"}")
        statusCallback("Intentando conectar a ${device.name ?: "Dispositivo Desconocido"}...")
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            statusCallback("Conectado a ${device.name ?: "Dispositivo Desconocido"}.")
            Log.d(TAG, "connect: Successfully connected to ${device.name ?: "Unknown Device"}")
            return true
        } catch (e: IOException) {
            statusCallback("Error de conexión con ${device.name ?: "Dispositivo Desconocido"}: ${e.message}")
            Log.e(TAG, "connect: IOException during connection to ${device.name ?: "Unknown Device"}: ${e.message}", e)
            closeSocket() // Ensure socket is closed on failure
            return false
        } catch (e: SecurityException) {
            statusCallback("Error de seguridad (permisos) con ${device.name ?: "Dispositivo Desconocido"}: ${e.message}")
            Log.e(TAG, "connect: SecurityException during connection to ${device.name ?: "Unknown Device"}: ${e.message}", e)
            closeSocket()
            return false
        } catch (e: Exception) {
            statusCallback("Error inesperado con ${device.name ?: "Dispositivo Desconocido"}: ${e.message}")
            Log.e(TAG, "connect: Unexpected Exception during connection to ${device.name ?: "Unknown Device"}: ${e.message}", e)
            closeSocket()
            return false
        }
    }

    /**
     * Sends a URL to the connected server and receives HTML response.
     * Requires an active connection.
     * @param url The URL to request from the server.
     * @return The HTML content received from the server, or null if an error occurs or not connected.
     */
    fun sendAndReceive(url: String): String? {
        if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
            statusCallback("Error: No hay conexión Bluetooth activa.")
            Log.e(TAG, "sendAndReceive: No active Bluetooth connection.")
            return null
        }

        Log.d(TAG, "sendAndReceive: Sending URL: $url")
        statusCallback("Enviando URL: $url. Esperando respuesta...")
        try {
            val writer = BufferedWriter(OutputStreamWriter(bluetoothSocket!!.outputStream))
            val reader = BufferedReader(InputStreamReader(bluetoothSocket!!.inputStream))

            // Enviar URL al servidor
            writer.write("$url\n")
            writer.flush()
            Log.d(TAG, "sendAndReceive: URL sent. Waiting for response.")

            // Leer respuesta del servidor hasta encontrar el delimitador
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line == END_OF_HTML_DELIMITER) {
                    Log.d(TAG, "sendAndReceive: Delimitador recibido. Fin del HTML.")
                    break // Salir del bucle cuando se encuentra el delimitador
                }
                response.append(line).append("\n")
            }

            statusCallback("Respuesta recibida.")
            Log.d(TAG, "sendAndReceive: Response received. Length: ${response.length}")
            return response.toString()

        } catch (e: IOException) {
            statusCallback("Error de comunicación: ${e.message}")
            Log.e(TAG, "sendAndReceive: IOException during send/receive: ${e.message}. Socket connected: ${bluetoothSocket?.isConnected}", e) // Más detalles
            // La conexión podría haberse roto, intentar cerrar y anular
            closeSocket()
            return null
        } catch (e: Exception) {
            statusCallback("Error inesperado durante la comunicación: ${e.message}")
            Log.e(TAG, "sendAndReceive: Unexpected Exception during send/receive: ${e.message}", e)
            closeSocket()
            return null
        }
    }

    /**
     * Disconnects the Bluetooth socket.
     */
    fun disconnect() {
        Log.d(TAG, "disconnect: Disconnecting Bluetooth socket.")
        statusCallback("Desconectando Bluetooth.")
        closeSocket()
    }

    private fun closeSocket() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null // Nullify to indicate no active connection
            statusCallback("Conexión Bluetooth cerrada.")
            Log.d(TAG, "closeSocket: Bluetooth socket closed.")
        } catch (e: IOException) {
            statusCallback("Error al cerrar el socket: ${e.message}")
            Log.e(TAG, "closeSocket: Error closing socket: ${e.message}", e)
        }
    }

    // Add a check for connection status
    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }
}