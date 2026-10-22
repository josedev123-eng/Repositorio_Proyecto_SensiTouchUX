# SensiTouch UX – Código del ESP32 (versión 4)

`SensiTouchUX_ESP32/SensiTouchUX_ESP32.ino` es el código del dispositivo. Funciona con la app Android (`../App_SensiTouchUX`).

## Novedades frente a la versión 3

- **`/estado` en JSON** para la app:
  - votos;
  - votos por hora;
  - últimos 20 votos con su hora y si llegaron por el botón accesible;
  - configuración y estado de la OLED y el DFPlayer.
- **`/config`**: la app cambia la voz, el LED de confirmación, el botón accesible y el mensaje de la pantalla.
- **Hora de internet (NTP, Perú UTC-5):** permite detectar quejas seguidas y armar el gráfico por hora.
- **La IP se muestra en la OLED** al encender, y también abajo en la pantalla normal, para vincular la app.
- **Reinicio automático a la medianoche**, para empezar cada día en cero.
- **Si no hay WiFi** se sigue votando igual.
- También responde en `http://sensitouch.local`.
- Se mantienen `/votos` y `/accesible` de la versión 3.

## Probar en Wokwi

1. Copia el contenido de `SensiTouchUX_ESP32.ino` en `sketch.ino`.
2. Usa `diagram.json` y `libraries.txt` de esta carpeta.
3. En la simulación, `SIMULACION = true` y la red `Wokwi-GUEST`.

## Prototipo real

- `SIMULACION = false`.
- Escribe tu red en `redWifi` y `claveWifi`.
- Graba los audios 0001.mp3 a 0007.mp3 en la carpeta `mp3` de la microSD.

## Conexiones

| Componente | Pin |
|---|---|
| Sensores IR Malo / Regular / Bueno | GPIO 32 / 33 / 25 (3.3 V) |
| Botón accesible | GPIO 27 (INPUT_PULLUP) |
| LEDs Malo / Regular / Bueno / Accesible | GPIO 26 / 14 / 13 / 4, con 220 Ω |
| OLED SSD1306 | SDA 21, SCL 22 |
| DFPlayer Mini | TX → GPIO 16, RX ← GPIO 17 (con 1 kΩ), 5 V |
| Buzzer (solo Wokwi) | GPIO 18 |
