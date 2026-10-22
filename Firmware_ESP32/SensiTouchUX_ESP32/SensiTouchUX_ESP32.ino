// ==========================================
// SENSITOUCH UX - ESP32 (version 4)
// Votos sin contacto + boton accesible con voz
// + pantalla OLED + WiFi para la app Android (Kotlin)
//
// Novedades de la version 4 (para la app):
//  - /estado responde en JSON: votos, votos por hora, ultimos 20 votos,
//    configuracion y estado de los componentes.
//  - /config permite cambiar desde la app la voz, el LED, el boton
//    accesible y el mensaje de la pantalla.
//  - Hora de internet (NTP, hora de Peru) para saber a que hora llego
//    cada voto y asi la app detecta quejas seguidas.
//  - La IP se muestra en la OLED para vincular la app facilmente.
//  - Los contadores se reinician solos a la medianoche.
//
// Sirve para Wokwi y para el prototipo real (cambiando SIMULACION).
// ==========================================

#include <WiFi.h>
#include <WebServer.h>
#include <ESPmDNS.h>
#include <Wire.h>
#include <time.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <DFRobotDFPlayerMini.h>

// ---------- MODO DE TRABAJO ----------
// true  = Wokwi (la voz se reemplaza por pitidos del buzzer y texto en el monitor)
// false = prototipo real (la voz sale del DFPlayer Mini con el parlante)
const bool SIMULACION = true;

// ---------- WIFI ----------
// En Wokwi: "Wokwi-GUEST" sin clave.
// En el prototipo real: la red del local o el punto de acceso del celular.
const char* redWifi = "Wokwi-GUEST";
const char* claveWifi = "";

WebServer servidor(80);

// ---------- HORA DE INTERNET (Peru = UTC-5) ----------
const long ZONA_HORARIA = -5 * 3600;
bool horaLista = false;
int diaActual = -1;

// ---------- SENSORES INFRARROJOS (FC-51 / HW-488) ----------
// Dan LOW cuando la mano pasa por el tunel. En Wokwi son botones.
const int sensorMalo = 32;
const int sensorRegular = 33;
const int sensorBueno = 25;

// ---------- BOTON ACCESIBLE (pulsador de 22 mm con tapa grande) ----------
const int botonAccesible = 27;

// ---------- LEDS ----------
const int ledMalo = 26;
const int ledRegular = 14;
const int ledBueno = 13;
const int ledAccesible = 4;

// ---------- BUZZER (solo en la simulacion) ----------
const int buzzer = 18;

// ---------- DFPLAYER MINI (solo en el prototipo real) ----------
// DFPlayer TX -> GPIO 16 (RX2 del ESP32)
// DFPlayer RX -> GPIO 17 (TX2 del ESP32) con resistencia de 1 kOhm
DFRobotDFPlayerMini reproductor;
bool dfplayerListo = false;

// Audios que van en la microSD, carpeta "mp3":
const int AUDIO_BIENVENIDA = 1;   // 0001.mp3 "Pasa tu mano por el tunel de tu opinion"
const int AUDIO_GRACIAS = 2;      // 0002.mp3 "Gracias por tu opinion"
const int AUDIO_INSTRUCCION = 3;  // 0003.mp3 "Escucha las opciones y presiona de nuevo el boton cuando escuches la tuya"
const int AUDIO_MALO = 4;         // 0004.mp3 "Malo"
const int AUDIO_REGULAR = 5;      // 0005.mp3 "Regular"
const int AUDIO_BUENO = 6;        // 0006.mp3 "Bueno"
const int AUDIO_CANCELADO = 7;    // 0007.mp3 "No elegiste ninguna opcion"

// ---------- PANTALLA OLED 0.96" (I2C: SDA 21, SCL 22) ----------
Adafruit_SSD1306 pantalla(128, 64, &Wire, -1);
bool oledLista = false;

// ---------- CONFIGURACION QUE SE CAMBIA DESDE LA APP ----------
bool voz = true;               // bienvenida y "gracias" en voz alta
bool ledConfirmacion = true;   // prender el LED del voto por un momento
bool modoAccesible = true;     // boton accesible activado
String mensajeOled = "Selecciona tu opinion";

// ---------- CONTADORES DE VOTOS (del dia) ----------
int votosMalo = 0;
int votosRegular = 0;
int votosBueno = 0;
int votosAccesible = 0;
int porHora[24][3];            // [hora][0 = malo, 1 = regular, 2 = bueno]

// ---------- ULTIMOS VOTOS (los ve la app en tiempo real) ----------
struct VotoGuardado {
  int id;
  int opinion;     // 1 malo, 2 regular, 3 bueno
  char hora[6];    // "HH:MM"
  bool porBoton;
  long tiempo;     // segundos desde 1970 (0 si no hay hora de internet)
};
const int MAX_VOTOS = 20;
VotoGuardado ultimos[MAX_VOTOS];
int cantidadGuardados = 0;
int siguienteId = 1;

// ---------- MENU DEL BOTON ACCESIBLE ----------
// 1) Se presiona: la voz da la instruccion.
// 2) Cada 5 segundos la voz dice una opcion: Malo, Regular, Bueno.
// 3) Si se presiona mientras suena una opcion, se vota esa opcion.
// 4) Si pasan las 3 opciones sin presionar, se cancela.
bool menuActivo = false;
int opcionActual = 0;
unsigned long tiempoOpcion = 0;
const unsigned long ESPERA = 5000;

// ---------- ESTADOS ANTERIORES (para detectar una sola pulsacion) ----------
bool anteriorMalo = HIGH;
bool anteriorRegular = HIGH;
bool anteriorBueno = HIGH;
bool anteriorAccesible = HIGH;

unsigned long ultimaRevisionDia = 0;

// Prototipos (Arduino a veces no los crea solo cuando hay structs o referencias)
bool leerHora(struct tm &info);
void guardarVoto(int opinion, bool porBoton);
void hablar(int audio, bool forzar);
void pantallaEspera();
String textoJson(String texto);


void setup() {

  Serial.begin(115200);

  pinMode(sensorMalo, INPUT_PULLUP);
  pinMode(sensorRegular, INPUT_PULLUP);
  pinMode(sensorBueno, INPUT_PULLUP);
  pinMode(botonAccesible, INPUT_PULLUP);

  pinMode(ledMalo, OUTPUT);
  pinMode(ledRegular, OUTPUT);
  pinMode(ledBueno, OUTPUT);
  pinMode(ledAccesible, OUTPUT);
  pinMode(buzzer, OUTPUT);

  apagarOpiniones();
  digitalWrite(ledAccesible, LOW);
  reiniciarContadores();

  // Pantalla OLED
  oledLista = pantalla.begin(SSD1306_SWITCHCAPVCC, 0x3C);
  mostrarMensaje("Iniciando", "...");

  // DFPlayer (solo en el prototipo real)
  if (!SIMULACION) {
    Serial2.begin(9600, SERIAL_8N1, 16, 17);
    if (reproductor.begin(Serial2)) {
      reproductor.volume(25);   // volumen de 0 a 30
      dfplayerListo = true;
      Serial.println("DFPlayer listo");
    } else {
      Serial.println("No se encontro el DFPlayer (revisar cables y microSD)");
    }
  } else {
    dfplayerListo = true;   // en Wokwi la "voz" es el buzzer
  }

  conectarWifi();

  // Ordenes que puede mandar la app
  servidor.on("/estado", enviarEstado);
  servidor.on("/config", cambiarConfig);
  servidor.on("/reiniciar", reiniciarVotos);
  servidor.on("/votos", enviarVotos);           // version 3 (MIT App Inventor)
  servidor.on("/accesible", cambiarAccesible);  // version 3 (MIT App Inventor)
  servidor.onNotFound([]() { servidor.send(404, "text/plain", "Ruta no encontrada"); });
  servidor.begin();

  digitalWrite(ledAccesible, modoAccesible ? HIGH : LOW);
  pantallaEspera();
  hablar(AUDIO_BIENVENIDA, false);
  Serial.println("SensiTouch UX listo");
}


void loop() {

  servidor.handleClient();

  revisarSensor(sensorMalo, anteriorMalo, 1);
  revisarSensor(sensorRegular, anteriorRegular, 2);
  revisarSensor(sensorBueno, anteriorBueno, 3);

  revisarBotonAccesible();
  avanzarMenuAccesible();

  // Cada 30 segundos revisa si ya cambio el dia para empezar en cero
  if (millis() - ultimaRevisionDia > 30000) {
    ultimaRevisionDia = millis();
    revisarCambioDeDia();
  }

  delay(20);
}


// ==========================================
// WIFI Y HORA
// ==========================================

void conectarWifi() {
  mostrarMensaje("Conectando", "WiFi...");
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.begin(redWifi, claveWifi);
  Serial.print("Conectando a WiFi");

  // Espera hasta 20 segundos. Si no hay WiFi igual se puede votar.
  unsigned long inicio = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - inicio < 20000) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();

  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("Sin WiFi: los votos se cuentan igual, pero la app no los vera");
    mostrarMensaje("Sin WiFi", "Votos OK");
    delay(2000);
    return;
  }

  String ip = WiFi.localIP().toString();
  Serial.print("Conectado. IP del ESP32 (escribela en la app): ");
  Serial.println(ip);

  // Tambien responde como http://sensitouch.local
  if (MDNS.begin("sensitouch")) {
    MDNS.addService("http", "tcp", 80);
  }

  // Hora de internet
  configTime(ZONA_HORARIA, 0, "pool.ntp.org", "time.google.com");
  struct tm info;
  horaLista = getLocalTime(&info, 5000);
  if (horaLista) {
    diaActual = info.tm_yday;
    Serial.printf("Hora: %02d:%02d\n", info.tm_hour, info.tm_min);
  } else {
    Serial.println("No se pudo obtener la hora de internet");
  }

  // Muestra la IP grande para vincular la app
  if (oledLista) {
    pantalla.clearDisplay();
    pantalla.setTextColor(SSD1306_WHITE);
    pantalla.setTextSize(2);
    pantalla.setCursor(0, 4);
    pantalla.println("Conectado");
    pantalla.setTextSize(1);
    pantalla.setCursor(0, 32);
    pantalla.println("IP para la app:");
    pantalla.setCursor(0, 46);
    pantalla.println(ip);
    pantalla.display();
  }
  delay(4000);
}

// Lee la hora actual. Devuelve false si todavia no hay hora de internet.
bool leerHora(struct tm &info) {
  if (!horaLista) {
    horaLista = getLocalTime(&info, 10);
    return horaLista;
  }
  return getLocalTime(&info, 10);
}

void revisarCambioDeDia() {
  struct tm info;
  if (!leerHora(info)) return;
  if (diaActual == -1) {
    diaActual = info.tm_yday;
    return;
  }
  if (info.tm_yday != diaActual) {
    diaActual = info.tm_yday;
    Serial.println("Nuevo dia: los contadores empiezan en cero");
    reiniciarContadores();
  }
}


// ==========================================
// SENSORES DE LOS TUNELES
// ==========================================

void revisarSensor(int pin, bool &anterior, int opinion) {

  bool estado = digitalRead(pin);

  // Solo cuenta cuando pasa de HIGH a LOW (la mano entra al tunel)
  if (estado == LOW && anterior == HIGH) {
    menuActivo = false;   // si alguien vota con la mano, se cancela el menu accesible
    registrarVoto(opinion, false);
  }

  anterior = estado;
}


// ==========================================
// BOTON ACCESIBLE
// ==========================================

void revisarBotonAccesible() {

  bool estado = digitalRead(botonAccesible);

  if (estado == LOW && anteriorAccesible == HIGH && modoAccesible) {

    if (!menuActivo) {
      // Primera pulsacion: empieza el menu con la instruccion
      menuActivo = true;
      opcionActual = 0;
      tiempoOpcion = millis();
      Serial.println("Boton accesible: inicia el menu de voz");
      mostrarMensaje("Escucha y", "presiona");
      hablar(AUDIO_INSTRUCCION, true);

    } else if (opcionActual >= 1) {
      // Segunda pulsacion: se elige la opcion que esta sonando
      menuActivo = false;
      registrarVoto(opcionActual, true);
    }
  }

  anteriorAccesible = estado;
}

// Cada 5 segundos pasa a la siguiente opcion y la dice en voz alta
void avanzarMenuAccesible() {

  if (!menuActivo) {
    return;
  }

  if (millis() - tiempoOpcion >= ESPERA) {

    opcionActual = opcionActual + 1;
    tiempoOpcion = millis();

    if (opcionActual > 3) {
      // Ya pasaron las 3 opciones y nadie presiono: se cancela
      menuActivo = false;
      apagarOpiniones();
      Serial.println("Menu accesible cancelado (no se eligio opcion)");
      mostrarMensaje("Sin", "eleccion");
      hablar(AUDIO_CANCELADO, true);
      delay(1500);
      pantallaEspera();
      return;
    }

    // Prende solo el LED de la opcion que se esta diciendo
    apagarOpiniones();
    if (opcionActual == 1) digitalWrite(ledMalo, HIGH);
    if (opcionActual == 2) digitalWrite(ledRegular, HIGH);
    if (opcionActual == 3) digitalWrite(ledBueno, HIGH);

    Serial.print("Voz: ");
    Serial.println(nombreOpinion(opcionActual));
    mostrarMensaje("Opcion:", nombreOpinion(opcionActual));
    hablar(AUDIO_MALO + opcionActual - 1, true);   // 4 = Malo, 5 = Regular, 6 = Bueno
  }
}


// ==========================================
// REGISTRAR UN VOTO
// ==========================================

void registrarVoto(int opinion, bool porBoton) {

  apagarOpiniones();

  if (opinion == 1) votosMalo++;
  if (opinion == 2) votosRegular++;
  if (opinion == 3) votosBueno++;
  if (porBoton) votosAccesible++;

  if (ledConfirmacion) {
    if (opinion == 1) digitalWrite(ledMalo, HIGH);
    if (opinion == 2) digitalWrite(ledRegular, HIGH);
    if (opinion == 3) digitalWrite(ledBueno, HIGH);
  }

  guardarVoto(opinion, porBoton);

  Serial.print("Voto: ");
  Serial.print(nombreOpinion(opinion));
  if (porBoton) {
    Serial.print(" (boton accesible)");
  }
  Serial.print("  | Malo: ");
  Serial.print(votosMalo);
  Serial.print("  Regular: ");
  Serial.print(votosRegular);
  Serial.print("  Bueno: ");
  Serial.println(votosBueno);

  mostrarMensaje("Gracias!", nombreOpinion(opinion));
  hablar(AUDIO_GRACIAS, porBoton);   // con el boton accesible siempre se confirma con voz
  delay(1500);

  apagarOpiniones();
  pantallaEspera();
}

// Guarda el voto en la lista de ultimos y en la hora que corresponde
void guardarVoto(int opinion, bool porBoton) {

  VotoGuardado v;
  v.id = siguienteId++;
  v.opinion = opinion;
  v.porBoton = porBoton;
  v.tiempo = 0;
  strcpy(v.hora, "--:--");

  struct tm info;
  if (leerHora(info)) {
    snprintf(v.hora, sizeof(v.hora), "%02d:%02d", info.tm_hour, info.tm_min);
    v.tiempo = (long) time(nullptr);
    porHora[info.tm_hour][opinion - 1]++;
  }

  // Lista circular: si ya hay 20, se corre una posicion y se pierde el mas viejo
  if (cantidadGuardados < MAX_VOTOS) {
    ultimos[cantidadGuardados] = v;
    cantidadGuardados++;
  } else {
    for (int i = 1; i < MAX_VOTOS; i++) {
      ultimos[i - 1] = ultimos[i];
    }
    ultimos[MAX_VOTOS - 1] = v;
  }
}


// ==========================================
// VOZ
// ==========================================

// forzar = true: suena aunque la voz este apagada (menu accesible).
// En Wokwi no existe el DFPlayer: suena un pitido y se escribe en el monitor.
void hablar(int audio, bool forzar) {

  if (!voz && !forzar) {
    return;
  }

  if (!SIMULACION) {
    if (dfplayerListo) reproductor.play(audio);
    return;
  }

  Serial.print("  [voz] audio ");
  Serial.println(audio);

  if (audio == AUDIO_BIENVENIDA) tone(buzzer, 880, 150);
  if (audio == AUDIO_GRACIAS) tone(buzzer, 1300, 250);
  if (audio == AUDIO_INSTRUCCION) tone(buzzer, 1000, 400);
  if (audio == AUDIO_MALO) tone(buzzer, 300, 400);
  if (audio == AUDIO_REGULAR) tone(buzzer, 600, 400);
  if (audio == AUDIO_BUENO) tone(buzzer, 1200, 400);
  if (audio == AUDIO_CANCELADO) tone(buzzer, 200, 500);
}


// ==========================================
// ORDENES QUE LLEGAN DESDE LA APP
// ==========================================

// /estado -> todo en JSON para la app Android
void enviarEstado() {

  struct tm info;
  String hora = "";
  String fecha = "";
  if (leerHora(info)) {
    char texto[20];
    snprintf(texto, sizeof(texto), "%02d:%02d:%02d", info.tm_hour, info.tm_min, info.tm_sec);
    hora = texto;
    snprintf(texto, sizeof(texto), "%04d-%02d-%02d", info.tm_year + 1900, info.tm_mon + 1, info.tm_mday);
    fecha = texto;
  }

  String json = "{";
  json += "\"malo\":" + String(votosMalo);
  json += ",\"regular\":" + String(votosRegular);
  json += ",\"bueno\":" + String(votosBueno);
  json += ",\"accesible\":" + String(votosAccesible);
  json += ",\"modoAccesible\":" + String(modoAccesible ? "true" : "false");
  json += ",\"voz\":" + String(voz ? "true" : "false");
  json += ",\"led\":" + String(ledConfirmacion ? "true" : "false");
  json += ",\"mensaje\":\"" + textoJson(mensajeOled) + "\"";
  json += ",\"hora\":\"" + hora + "\"";
  json += ",\"fecha\":\"" + fecha + "\"";
  json += ",\"rssi\":" + String(WiFi.RSSI());
  json += ",\"ip\":\"" + WiFi.localIP().toString() + "\"";
  json += ",\"dfplayer\":" + String(dfplayerListo ? "true" : "false");
  json += ",\"oled\":" + String(oledLista ? "true" : "false");

  // votos por hora: [[malo,regular,bueno], ... 24 veces]
  json += ",\"porHora\":[";
  for (int h = 0; h < 24; h++) {
    if (h > 0) json += ",";
    json += "[" + String(porHora[h][0]) + "," + String(porHora[h][1]) + "," + String(porHora[h][2]) + "]";
  }
  json += "]";

  // ultimos votos, el mas nuevo primero
  json += ",\"ultimos\":[";
  for (int i = cantidadGuardados - 1; i >= 0; i--) {
    VotoGuardado &v = ultimos[i];
    if (i < cantidadGuardados - 1) json += ",";
    json += "{\"id\":" + String(v.id);
    json += ",\"op\":" + String(v.opinion);
    json += ",\"hora\":\"" + String(v.hora) + "\"";
    json += ",\"boton\":" + String(v.porBoton ? "true" : "false");
    json += ",\"t\":" + String(v.tiempo) + "}";
  }
  json += "]}";

  servidor.sendHeader("Access-Control-Allow-Origin", "*");
  servidor.send(200, "application/json", json);
}

// /config?voz=1&led=0&accesible=1&mensaje=Hola -> cambia la configuracion
void cambiarConfig() {

  if (servidor.hasArg("voz")) {
    voz = servidor.arg("voz") == "1";
    Serial.println(voz ? "Voz ACTIVADA desde la app" : "Voz APAGADA desde la app");
  }
  if (servidor.hasArg("led")) {
    ledConfirmacion = servidor.arg("led") == "1";
    Serial.println(ledConfirmacion ? "LED ACTIVADO desde la app" : "LED APAGADO desde la app");
  }
  if (servidor.hasArg("accesible")) {
    modoAccesible = servidor.arg("accesible") == "1";
    menuActivo = false;
    digitalWrite(ledAccesible, modoAccesible ? HIGH : LOW);
    Serial.println(modoAccesible ? "Boton accesible ACTIVADO desde la app" : "Boton accesible APAGADO desde la app");
  }
  if (servidor.hasArg("mensaje")) {
    mensajeOled = servidor.arg("mensaje");
    mensajeOled.trim();
    if (mensajeOled.length() == 0) mensajeOled = "Selecciona tu opinion";
    if (mensajeOled.length() > 20) mensajeOled = mensajeOled.substring(0, 20);
    Serial.print("Nuevo mensaje en la pantalla: ");
    Serial.println(mensajeOled);
  }

  if (!menuActivo) pantallaEspera();
  servidor.sendHeader("Access-Control-Allow-Origin", "*");
  servidor.send(200, "text/plain", "OK");
}

// /reiniciar -> pone los votos en cero
void reiniciarVotos() {
  reiniciarContadores();
  Serial.println("Votos reiniciados desde la app");
  mostrarMensaje("Votos", "en cero");
  delay(1000);
  pantallaEspera();
  servidor.sendHeader("Access-Control-Allow-Origin", "*");
  servidor.send(200, "text/plain", "OK");
}

// /votos -> (version 3) responde: malo,regular,bueno,accesible,modo
void enviarVotos() {
  String texto = String(votosMalo) + "," + String(votosRegular) + "," +
                 String(votosBueno) + "," + String(votosAccesible) + "," +
                 String(modoAccesible ? 1 : 0);
  servidor.send(200, "text/plain", texto);
}

// /accesible?estado=1 -> (version 3) activa o desactiva el boton accesible
void cambiarAccesible() {
  modoAccesible = servidor.arg("estado") == "1";
  menuActivo = false;
  digitalWrite(ledAccesible, modoAccesible ? HIGH : LOW);
  pantallaEspera();
  servidor.send(200, "text/plain", "OK");
}


// ==========================================
// FUNCIONES DE APOYO
// ==========================================

void reiniciarContadores() {
  votosMalo = 0;
  votosRegular = 0;
  votosBueno = 0;
  votosAccesible = 0;
  for (int h = 0; h < 24; h++) {
    porHora[h][0] = 0;
    porHora[h][1] = 0;
    porHora[h][2] = 0;
  }
  cantidadGuardados = 0;
  siguienteId = 1;
}

void apagarOpiniones() {
  digitalWrite(ledMalo, LOW);
  digitalWrite(ledRegular, LOW);
  digitalWrite(ledBueno, LOW);
}

String nombreOpinion(int opinion) {
  if (opinion == 1) return "MALO";
  if (opinion == 2) return "REGULAR";
  if (opinion == 3) return "BUENO";
  return "";
}

// Evita que comillas o barras rompan el JSON
String textoJson(String texto) {
  texto.replace("\\", "\\\\");
  texto.replace("\"", "\\\"");
  return texto;
}

// Muestra dos lineas de texto grande en la OLED
void mostrarMensaje(String linea1, String linea2) {
  if (!oledLista) return;
  pantalla.clearDisplay();
  pantalla.setTextColor(SSD1306_WHITE);
  pantalla.setTextSize(2);
  pantalla.setCursor(0, 10);
  pantalla.println(linea1);
  pantalla.setCursor(0, 38);
  pantalla.println(linea2);
  pantalla.display();
}

// Pantalla normal: el mensaje de la app en grande y la IP chiquita abajo
void pantallaEspera() {
  if (!oledLista) return;

  // Parte el mensaje en dos lineas de hasta 10 letras (lo que entra con letra grande)
  String linea1 = mensajeOled;
  String linea2 = "";
  if (mensajeOled.length() > 10) {
    int corte = mensajeOled.lastIndexOf(' ', 10);
    if (corte <= 0) corte = 10;
    linea1 = mensajeOled.substring(0, corte);
    linea2 = mensajeOled.substring(corte);
    linea2.trim();
    if (linea2.length() > 10) linea2 = linea2.substring(0, 10);
  }

  pantalla.clearDisplay();
  pantalla.setTextColor(SSD1306_WHITE);
  pantalla.setTextSize(2);
  pantalla.setCursor(0, 6);
  pantalla.println(linea1);
  pantalla.setCursor(0, 28);
  pantalla.println(linea2);
  pantalla.setTextSize(1);
  pantalla.setCursor(0, 55);
  if (WiFi.status() == WL_CONNECTED) {
    pantalla.print("IP ");
    pantalla.print(WiFi.localIP());
  } else {
    pantalla.print("Sin WiFi");
  }
  pantalla.display();
}
