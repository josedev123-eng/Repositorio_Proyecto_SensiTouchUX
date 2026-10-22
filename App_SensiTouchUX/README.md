# SensiTouch UX – App Android

App para el gerente del local. Muestra en tiempo real las opiniones que llegan al dispositivo **SensiTouch UX** (ESP32 con sensores infrarrojos y botón accesible) y **avisa al momento** cuando las opiniones negativas se disparan.

Grupo 8 · Investigación e Innovación Tecnológica · TECSUP

## Lo que hace

| Pantalla | Para qué sirve |
|---|---|
| **Inicio de sesión / Crear cuenta** | Cuenta del negocio guardada en el celular (contraseña con SHA-256). |
| **Vincular dispositivo** | Se escribe la IP que muestra la OLED del SensiTouch. La app revisa la conexión, la pantalla y el DFPlayer. También tiene **modo demo** para usarla sin el prototipo. |
| **Panel** | Votos de hoy, satisfacción, gráfico de dona, votos por hora, actividad reciente y estado del dispositivo. Para actualizar, se desliza hacia abajo. |
| **Votos** | Votos en vivo (cada 3 s), el último voto en grande y filtros por Malo, Regular, Bueno o Accesible. |
| **Reportes** | Día, semana y mes, con hallazgos automáticos (hora con más quejas, mejor día, tendencia) y **exportación a PDF**. |
| **Alertas** | Historial y la sección **"¿Dónde está el problema?"**, con las quejas por hora. |
| **Ajustes** | Voz, botón accesible, LED y mensaje de la OLED (se mandan al ESP32), y las reglas de alertas. |

### Cambios pedidos por la profesora

1. **Notificaciones.** Un trabajo en segundo plano (WorkManager) revisa el ESP32 **cada 15 minutos aunque la app esté cerrada**. Si encuentra un problema, manda una notificación. Así, si algo pasa en la mañana, el gerente se entera en ese momento y no recién en la noche.
2. **Aviso al entrar a la app.** Si al abrir la app las opiniones "Malo" superan el límite, aparece un aviso grande con:
   - el porcentaje;
   - la hora con más quejas;
   - cuántas alertas hubo desde la última visita.

   El botón **"Ver qué pasó"** lleva al análisis.

### Mejoras relacionadas que se agregaron

- **Límite configurable:** 10 a 80 % y un mínimo de votos, para evitar alertas falsas con 1 o 2 votos.
- **Quejas seguidas:** avisa si llegan, por ejemplo, 3 votos "Malo" en 10 minutos, aunque el porcentaje del día siga bien. Detecta problemas puntuales.
- **Aviso de desconexión:** avisa si el SensiTouch no responde por más de 30 minutos.
- **Resumen del día:** una notificación a la hora que elija el gerente (por defecto, 9 p. m.).
- **Sin avisos repetidos:** la misma alerta no se avisa dos veces. Si la situación empeora (30 % → 40 %), avisa de nuevo.
- **Botón "Probar notificación"** y botón **"Simular quejas seguidas"** (modo demo), para la exposición.

## Cómo abrir el proyecto

1. Instala **Android Studio** (Ladybug o más nuevo).
2. Ve a **File > Open** y elige la carpeta `App_SensiTouchUX`.
3. Espera a que termine **Gradle Sync**. La primera vez descarga las librerías.
4. Conecta el celular con la depuración USB activada, o usa un emulador, y presiona **Run ▶**.

Requisitos: Android 8.0 (API 26) o superior.

## Conectar con el ESP32

1. Carga en el ESP32 el código `../Firmware_ESP32/SensiTouchUX_ESP32/SensiTouchUX_ESP32.ino` (versión 4).
   - En el prototipo real cambia `SIMULACION` a `false` y pon el nombre y la clave del WiFi en `redWifi` y `claveWifi`.
   - Librerías: Adafruit SSD1306, Adafruit GFX Library y DFRobotDFPlayerMini.
2. El celular y el ESP32 deben estar en la **misma red WiFi**. También sirve el punto de acceso del celular.
3. Al encender, la OLED muestra **"IP para la app"**. Esa IP se escribe en la pantalla **Vincular dispositivo**.

### Comunicación (HTTP en la red local)

| Ruta | Respuesta |
|---|---|
| `GET /estado` | JSON con `malo`, `regular`, `bueno`, `accesible`, `modoAccesible`, `voz`, `led`, `mensaje`, `hora`, `fecha`, `rssi`, `ip`, `dfplayer`, `oled`, `porHora` (24 × [malo, regular, bueno]) y `ultimos` (los últimos 20 votos). |
| `GET /config?voz=1&led=0&accesible=1&mensaje=Hola` | Cambia la configuración del dispositivo. |
| `GET /reiniciar` | Pone los votos en cero. |

## Estructura

```
app/src/main/java/com/sensitouch/ux/
├── SensiTouchApp.kt        # Inicio: canales de notificación y trabajos en segundo plano
├── MainActivity.kt
├── data/                   # Modelos, conexión al ESP32, demo y datos guardados (DataStore)
├── alertas/                # Reglas de alertas, notificaciones y WorkManager
├── reportes/               # Reporte en PDF
└── ui/                     # Jetpack Compose + Material 3
    ├── theme/              # Colores del mockup de Figma
    ├── componentes/        # Tarjetas, gráficos y encabezado
    ├── navegacion/         # Rutas y barra inferior
    └── pantallas/          # Login, Vincular, Panel, Votos, Reportes, Alertas y Ajustes
```

**Tecnologías:**
- Kotlin, Jetpack Compose y Material 3.
- Navigation Compose, ViewModel y StateFlow.
- WorkManager y DataStore.
- Notificaciones con canales.
- PdfDocument y FileProvider.
