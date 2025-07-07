package com.example.internetbt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object HttpFetcher {
    fun fetchHtml(url: String): String {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection

            // Configurar la conexión
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000 // 10 segundos
            connection.readTimeout = 15000 // 15 segundos
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) InternetBT/1.0")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "es-ES,es;q=0.8,en;q=0.6")
            connection.setRequestProperty("Accept-Encoding", "identity") // No compresión para simplificar
            connection.setRequestProperty("Connection", "close")

            // Conectar
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line).append("\n")
                }

                reader.close()
                connection.disconnect()

                response.toString()
            } else {
                connection.disconnect()
                createErrorHtml("Error HTTP $responseCode", "El servidor respondió con código $responseCode para la URL: $url")
            }

        } catch (e: java.net.UnknownHostException) {
            createErrorHtml("Host no encontrado", "No se pudo encontrar el sitio web: $url")
        } catch (e: java.net.SocketTimeoutException) {
            createErrorHtml("Tiempo de espera agotado", "La conexión tardó demasiado tiempo en responder: $url")
        } catch (e: java.net.ConnectException) {
            createErrorHtml("Error de conexión", "No se pudo conectar al sitio web: $url")
        } catch (e: Exception) {
            createErrorHtml("Error al obtener el sitio", "Error: ${e.message}\nURL: $url")
        }
    }

    private fun createErrorHtml(title: String, message: String): String {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Error - InternetBT</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        max-width: 800px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f5f5f5;
                    }
                    .error-container {
                        background: white;
                        padding: 30px;
                        border-radius: 8px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                        text-align: center;
                    }
                    .error-icon {
                        font-size: 64px;
                        color: #ff6b6b;
                        margin-bottom: 20px;
                    }
                    .error-title {
                        color: #333;
                        font-size: 24px;
                        margin-bottom: 15px;
                    }
                    .error-message {
                        color: #666;
                        line-height: 1.6;
                        margin-bottom: 20px;
                    }
                    .error-footer {
                        color: #999;
                        font-size: 14px;
                        border-top: 1px solid #eee;
                        padding-top: 20px;
                        margin-top: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="error-container">
                    <div class="error-icon">⚠️</div>
                    <h1 class="error-title">$title</h1>
                    <p class="error-message">$message</p>
                    <div class="error-footer">
                        <p>Conexión vía InternetBT</p>
                        <p>Verifica que el servidor tenga conexión a internet</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}