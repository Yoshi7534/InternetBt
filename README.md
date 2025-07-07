# InternetBT - Navegador Web vía Bluetooth

InternetBT es una aplicación Android innovadora que permite navegar por internet utilizando Bluetooth como medio de comunicación. Un dispositivo actúa como servidor (con conexión a internet) y otro como cliente (que puede navegar sin conexión directa a internet).

## Características

- **Navegación compartida**: Navega por internet desde un dispositivo sin conexión directa
- **Comunicación Bluetooth**: Utiliza RFCOMM para la transferencia de datos
- **Interfaz WebView**: Experiencia de navegación familiar con WebView nativo
- **Manejo de errores**: Gestión robusta de errores de conexión y comunicación
- **Arquitectura cliente-servidor**: Diseño modular y escalable

## Arquitectura

La aplicación está dividida en varios componentes principales:

### Componentes Principales

1. **MainActivity** - Pantalla principal con selección de modo
2. **ServerActivity** - Interfaz del servidor Bluetooth
3. **ClientActivity** - Interfaz del cliente con navegador
4. **BluetoothServer** - Servidor Bluetooth que maneja conexiones
5. **BluetoothClient** - Cliente Bluetooth para conectarse al servidor
6. **HttpFetcher** - Utilidad para obtener contenido web

### Flujo de Comunicación

```
Cliente → Bluetooth → Servidor → Internet → Servidor → Bluetooth → Cliente
```

## Funcionalidades por Componente

### MainActivity
- Selección entre modo servidor o cliente
- Verificación de permisos Bluetooth
- Punto de entrada principal de la aplicación

### ServerActivity  
- Inicia el servidor Bluetooth
- Muestra el estado de conexiones
- Visualiza el contenido web solicitado por los clientes
- Maneja múltiples conexiones de clientes

### ClientActivity
- Interfaz de navegación con WebView
- Campo de entrada de URL
- Conexión automática al servidor
- Manejo de navegación por enlaces

### BluetoothServer
- Escucha conexiones entrantes en puerto RFCOMM
- Procesa solicitudes de URL de clientes
- Obtiene contenido web usando HttpFetcher
- Envía HTML de respuesta con delimitadores

### BluetoothClient
- Establece conexión con el servidor
- Envía URLs y recibe contenido HTML
- Maneja reconexiones automáticas
- Gestión de estado de conexión

### HttpFetcher
- Realiza peticiones HTTP/HTTPS
- Maneja timeouts y errores de conexión
- Genera páginas de error personalizadas
- Configuración de User-Agent específico

## Configuración Técnica

### Permisos Requeridos
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

### UUID del Servicio
```kotlin
private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
```

### Protocolo de Comunicación
- **Delimiter**: `<END_OF_HTML>` marca el final de cada respuesta
- **Encoding**: UTF-8 para todo el contenido
- **Timeout**: 10s conexión, 15s lectura

## Instalación y Uso

### Requisitos Previos
- Android 6.0+ (API 23+)
- Bluetooth habilitado en ambos dispositivos
- Dispositivos emparejados previamente
- Conexión a internet en el dispositivo servidor

### Configuración del Servidor
1. Instalar la aplicación en el dispositivo con internet
2. Ejecutar la aplicación y seleccionar "Servidor"
3. Habilitar Bluetooth y visibilidad si se solicita
4. Esperar conexiones de clientes

### Configuración del Cliente
1. Instalar la aplicación en el dispositivo cliente
2. Emparejar con el dispositivo servidor
3. Ejecutar la aplicación y seleccionar "Cliente"
4. La aplicación se conectará automáticamente al servidor
5. Introducir URLs en el campo de texto o navegar por enlaces

## Búsqueda de Dispositivos

El cliente busca automáticamente dispositivos emparejados con los siguientes nombres (actualmente se tiene que modificar el código con el nombre del dispositivo Bluetooth que va a fungir como el servidor sino no lo encuentra al momento de buscar el servidor):
- "S24 de Uriel"
- "InternetBT"

Para usar otros nombres, modificar el método `findServerDevice()` en `ClientActivity.kt`.

## Logging y Depuración

La aplicación incluye logging extensivo con las siguientes etiquetas:
- `BT_DEBUG_SERVER` - Logs del servidor Bluetooth
- `BT_DEBUG_CLIENT` - Logs del cliente Bluetooth

Para ver los logs:
```bash
adb logcat -s BT_DEBUG_SERVER BT_DEBUG_CLIENT
```

## Limitaciones Conocidas

- **Velocidad**: La navegación es más lenta que una conexión directa
- **Contenido multimedia**: Videos y audio pueden no funcionar correctamente
- **JavaScript complejo**: Algunas funcionalidades web avanzadas pueden fallar
- **Seguridad**: No implementa HTTPS end-to-end entre dispositivos

## Personalización

### Modificar el User-Agent
En `HttpFetcher.kt`:
```kotlin
connection.setRequestProperty("User-Agent", "Tu-User-Agent-Personalizado")
```

### Cambiar Timeouts
En `HttpFetcher.kt`:
```kotlin
connection.connectTimeout = 10000 // milisegundos
connection.readTimeout = 15000 // milisegundos
```

### Personalizar Páginas de Error
Modificar el método `createErrorHtml()` en `HttpFetcher.kt`.

## Desarrollo

### Estructura del Proyecto
```
app/src/main/java/com/example/internetbt/
├── MainActivity.kt          # Actividad principal
├── ServerActivity.kt        # Interfaz del servidor
├── ClientActivity.kt        # Interfaz del cliente
├── BluetoothServer.kt       # Servidor Bluetooth
├── BluetoothClient.kt       # Cliente Bluetooth
└── HttpFetcher.kt          # Utilidad HTTP
```

### Agregar Nuevas Funcionalidades
1. **Historial de navegación**: Implementar en `ClientActivity`
2. **Favoritos**: Agregar base de datos local
3. **Múltiples pestañas**: Extender la interfaz WebView
4. **Compresión**: Implementar en `HttpFetcher` para mejor rendimiento

## Flujo de Datos

1. **Cliente** ingresa URL o hace clic en enlace
2. **Cliente** envía URL al **Servidor** vía Bluetooth
3. **Servidor** realiza petición HTTP a internet
4. **Servidor** envía HTML de respuesta al **Cliente**
5. **Cliente** muestra contenido en WebView
6. **Cliente** puede navegar por enlaces, repitiendo el proceso

## Manejo de Errores

- **Conexión perdida**: Reconexión automática del cliente
- **Timeout HTTP**: Páginas de error personalizadas
- **Permisos**: Verificación y solicitud automática
- **Bluetooth deshabilitado**: Solicitud de habilitación

## Licencia

Este proyecto está desarrollado para fines educativos y de demostración. Úsalo bajo tu propia responsabilidad.

## Contribuciones

Para contribuir al proyecto:
1. Fork el repositorio
2. Crear una rama para tu funcionalidad
3. Commit tus cambios
4. Push a la rama
5. Crear un Pull Request

## Soporte

Si encuentras problemas:
1. Verifica que los dispositivos estén emparejados
2. Revisa los logs usando las etiquetas mencionadas
3. Asegúrate de que los permisos estén concedidos
4. Verifica que el Bluetooth esté habilitado en ambos dispositivos

## Ejemplo de uso


https://github.com/user-attachments/assets/bb77029c-c323-4f90-9bf3-1049900cb078


