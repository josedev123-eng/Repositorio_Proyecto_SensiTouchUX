package com.sensitouch.ux.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sensitouch.ux.SensiTouchApp
import com.sensitouch.ux.alertas.EvaluadorAlertas
import com.sensitouch.ux.alertas.Notificaciones
import com.sensitouch.ux.alertas.Programador
import com.sensitouch.ux.data.Alerta
import com.sensitouch.ux.data.ConfigAlertas
import com.sensitouch.ux.data.ConfigDispositivo
import com.sensitouch.ux.data.EstadoConexion
import com.sensitouch.ux.data.EstadoDispositivo
import com.sensitouch.ux.data.Negocio
import com.sensitouch.ux.data.Opinion
import com.sensitouch.ux.data.ResumenDia
import com.sensitouch.ux.data.TipoAlerta
import com.sensitouch.ux.data.limpiarIp
import com.sensitouch.ux.ui.navegacion.Rutas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Aviso grande que aparece al entrar a la app o cuando llega una alerta con la app abierta. */
data class DialogoAlerta(
    val titulo: String,
    val mensaje: String,
    val tipo: TipoAlerta,
)

data class UiEstado(
    val inicio: String? = null,
    val negocio: Negocio? = null,
    val dispositivo: ConfigDispositivo = ConfigDispositivo(),
    val estado: EstadoDispositivo = EstadoDispositivo(),
    val hayDatos: Boolean = false,
    val conexion: EstadoConexion = EstadoConexion.CONECTANDO,
    val config: ConfigAlertas = ConfigAlertas(),
    val alertas: List<Alerta> = emptyList(),
    val historial: List<ResumenDia> = emptyList(),
    val dialogo: DialogoAlerta? = null,
    val actualizado: Long = 0L,
    val actualizando: Boolean = false,
) {
    val superaLimite: Boolean get() = hayDatos && EvaluadorAlertas.superaLimite(estado, config)
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SensiTouchApp
    private val preferencias = app.preferencias
    private val repositorio = app.repositorio

    private val _ui = MutableStateFlow(UiEstado())
    val ui: StateFlow<UiEstado> = _ui.asStateFlow()

    /** Mensajes cortos para el Snackbar. */
    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensajes = _mensajes.asSharedFlow()

    /** Pantalla que se pidio abrir desde una notificacion. */
    private val _destino = MutableStateFlow<String?>(null)
    val destino: StateFlow<String?> = _destino.asStateFlow()

    private val enPrimerPlano = MutableStateFlow(false)
    private var revisarAlAbrir = true

    init {
        viewModelScope.launch {
            val sesion = preferencias.sesionActiva.first()
            val dispositivo = preferencias.dispositivo.first()
            val inicio = when {
                !sesion -> Rutas.LOGIN
                !dispositivo.vinculado -> Rutas.VINCULAR
                else -> Rutas.PANEL
            }
            _ui.update { it.copy(inicio = inicio) }
        }
        viewModelScope.launch { preferencias.negocio.collect { n -> _ui.update { it.copy(negocio = n) } } }
        viewModelScope.launch { preferencias.configAlertas.collect { c -> _ui.update { it.copy(config = c) } } }
        viewModelScope.launch { preferencias.alertas.collect { a -> _ui.update { it.copy(alertas = a) } } }
        viewModelScope.launch { preferencias.historial.collect { h -> _ui.update { it.copy(historial = h) } } }

        // Lee el ESP32 cada 3 segundos solo mientras la app esta en pantalla
        viewModelScope.launch {
            combine(preferencias.sesionActiva, preferencias.dispositivo, enPrimerPlano) { sesion, disp, visible ->
                Triple(sesion, disp, visible)
            }.distinctUntilChanged().collectLatest { (sesion, disp, visible) ->
                _ui.update { it.copy(dispositivo = disp) }
                if (sesion && disp.vinculado && visible) {
                    _ui.update { it.copy(conexion = EstadoConexion.CONECTANDO) }
                    while (true) {
                        actualizar(disp)
                        delay(INTERVALO_MS)
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    fun alEntrar() {
        revisarAlAbrir = true
        enPrimerPlano.value = true
    }

    fun alSalir() {
        enPrimerPlano.value = false
    }

    fun abrirDesdeNotificacion(destino: String?) {
        if (destino != null) _destino.value = destino
    }

    fun destinoAbierto() {
        _destino.value = null
    }

    // ------------------------------------------------------------------
    // Lectura del dispositivo y alertas
    // ------------------------------------------------------------------

    private suspend fun actualizar(disp: ConfigDispositivo) {
        try {
            val estado = repositorio.leerEstado(disp)
            val nuevas = repositorio.procesar(estado)
            _ui.update {
                it.copy(
                    estado = estado,
                    hayDatos = true,
                    conexion = if (disp.demo) EstadoConexion.DEMO else EstadoConexion.CONECTADO,
                    actualizado = System.currentTimeMillis(),
                )
            }
            if (revisarAlAbrir) {
                revisarAlAbrir = false
                avisoAlEntrar(estado)
            } else if (nuevas.isNotEmpty()) {
                // con la app abierta se muestra el aviso dentro de la app (no hace falta notificacion)
                val alerta = nuevas.first()
                _ui.update { it.copy(dialogo = DialogoAlerta(alerta.titulo, alerta.detalle, alerta.tipo)) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _ui.update { it.copy(conexion = EstadoConexion.SIN_CONEXION) }
            repositorio.revisarDesconexion()
        }
    }

    /**
     * Pedido de la profesora: al entrar a la app, si las opiniones negativas pasaron el limite,
     * el gerente ve un aviso grande aunque haya pasado en la manana y recien revise en la noche.
     */
    private suspend fun avisoAlEntrar(estado: EstadoDispositivo) {
        val visitaAnterior = preferencias.registrarVisita()
        val config = preferencias.configAlertas.first()
        val alertasDesde = if (visitaAnterior == 0L) emptyList()
        else preferencias.alertas.first().filter { it.fechaHora > visitaAnterior && it.tipo != TipoAlerta.PRUEBA }
        val supera = EvaluadorAlertas.superaLimite(estado, config)
        if (!supera && alertasDesde.isEmpty()) return

        val mensaje = buildString {
            if (supera) {
                append("Hoy el ${estado.porcentaje(Opinion.MALO)} % de las opiniones son \"Malo\" ")
                append("(${estado.malo} de ${estado.total}). Tu límite es ${config.umbralPorcentaje} %.")
                estado.horaConMasQuejas?.let { append("\n\nLa mayoría de quejas llegó entre las ${EvaluadorAlertas.rango(it)}.") }
            }
            if (alertasDesde.isNotEmpty()) {
                if (isNotEmpty()) append("\n\n")
                append(
                    if (alertasDesde.size == 1) "Mientras no estabas hubo 1 alerta: ${alertasDesde.first().titulo}."
                    else "Mientras no estabas hubo ${alertasDesde.size} alertas. La más reciente: ${alertasDesde.first().titulo}.",
                )
            }
        }
        _ui.update {
            it.copy(
                dialogo = DialogoAlerta(
                    titulo = if (supera) "Opiniones negativas sobre el límite" else "Tienes alertas nuevas",
                    mensaje = mensaje,
                    tipo = if (supera) TipoAlerta.PORCENTAJE else alertasDesde.first().tipo,
                ),
            )
        }
    }

    fun cerrarDialogo() {
        _ui.update { it.copy(dialogo = null) }
    }

    fun refrescar() {
        val disp = _ui.value.dispositivo
        if (!disp.vinculado) return
        viewModelScope.launch {
            _ui.update { it.copy(actualizando = true) }
            actualizar(disp)
            delay(400)
            _ui.update { it.copy(actualizando = false) }
        }
    }

    // ------------------------------------------------------------------
    // Cuenta
    // ------------------------------------------------------------------

    fun iniciarSesion(correo: String, clave: String, alError: (String) -> Unit, alEntrar: (String) -> Unit) {
        viewModelScope.launch {
            val error = preferencias.iniciarSesion(correo, clave)
            if (error != null) {
                alError(error)
            } else {
                revisarAlAbrir = true
                alEntrar(if (preferencias.dispositivo.first().vinculado) Rutas.PANEL else Rutas.VINCULAR)
            }
        }
    }

    fun registrar(nombre: String, correo: String, clave: String, alTerminar: () -> Unit) {
        viewModelScope.launch {
            preferencias.registrar(nombre, correo, clave)
            alTerminar()
        }
    }

    fun cerrarSesion(alTerminar: () -> Unit) {
        viewModelScope.launch {
            preferencias.cerrarSesion()
            _ui.update { it.copy(hayDatos = false, estado = EstadoDispositivo(), dialogo = null) }
            alTerminar()
        }
    }

    // ------------------------------------------------------------------
    // Vincular dispositivo
    // ------------------------------------------------------------------

    fun probarConexion(ip: String, alTerminar: (Result<EstadoDispositivo>) -> Unit) {
        viewModelScope.launch {
            val resultado = runCatching {
                repositorio.leerEstado(ConfigDispositivo(ip = limpiarIp(ip), demo = false, vinculado = true))
            }
            if (resultado.exceptionOrNull() is CancellationException) return@launch
            alTerminar(resultado)
        }
    }

    fun vincular(ip: String, demo: Boolean, alTerminar: () -> Unit) {
        viewModelScope.launch {
            preferencias.vincular(if (demo) "demo" else ip, demo)
            revisarAlAbrir = false
            alTerminar()
        }
    }

    fun cambiarDispositivo(alTerminar: () -> Unit) {
        viewModelScope.launch {
            preferencias.desvincular()
            _ui.update { it.copy(hayDatos = false, estado = EstadoDispositivo()) }
            alTerminar()
        }
    }

    // ------------------------------------------------------------------
    // Ajustes del dispositivo
    // ------------------------------------------------------------------

    fun configurar(voz: Boolean? = null, led: Boolean? = null, accesible: Boolean? = null, mensaje: String? = null) {
        val anterior = _ui.value.estado
        // se cambia en pantalla al instante y luego se manda al ESP32
        _ui.update {
            it.copy(
                estado = it.estado.copy(
                    voz = voz ?: it.estado.voz,
                    led = led ?: it.estado.led,
                    modoAccesible = accesible ?: it.estado.modoAccesible,
                    mensaje = mensaje ?: it.estado.mensaje,
                ),
            )
        }
        viewModelScope.launch {
            try {
                repositorio.configurar(_ui.value.dispositivo, voz, led, accesible, mensaje)
                if (mensaje != null) _mensajes.emit("Mensaje enviado a la pantalla")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _ui.update { it.copy(estado = anterior) }
                _mensajes.emit("No se pudo enviar al SensiTouch. Revisa la conexión.")
            }
        }
    }

    fun reiniciarVotos() {
        viewModelScope.launch {
            try {
                repositorio.reiniciar(_ui.value.dispositivo)
                _mensajes.emit("Votos reiniciados")
                actualizar(_ui.value.dispositivo)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _mensajes.emit("No se pudo reiniciar. Revisa la conexión.")
            }
        }
    }

    // ------------------------------------------------------------------
    // Ajustes de alertas
    // ------------------------------------------------------------------

    fun guardarConfig(nueva: ConfigAlertas) {
        val anterior = _ui.value.config
        _ui.update { it.copy(config = nueva) }
        viewModelScope.launch {
            preferencias.guardarConfigAlertas(nueva)
            if (anterior.resumenDiario != nueva.resumenDiario || anterior.horaResumen != nueva.horaResumen) {
                Programador.programarResumen(app, nueva.resumenDiario, nueva.horaResumen)
            }
        }
    }

    fun probarNotificacion() {
        if (!Notificaciones.activadasEnSistema(app)) {
            _mensajes.tryEmit("Activa las notificaciones de SensiTouch en tu celular")
            return
        }
        Notificaciones.mostrarAlerta(
            app,
            Alerta(
                id = "prueba-${System.currentTimeMillis()}",
                tipo = TipoAlerta.PRUEBA,
                titulo = "Así te llegarán las alertas",
                detalle = "Cuando las opiniones \"Malo\" pasen tu límite recibirás un aviso como este, " +
                    "aunque la app esté cerrada.",
                fechaHora = System.currentTimeMillis(),
            ),
        )
    }

    /** Solo en modo demo: mete varias opiniones malas seguidas para mostrar la alerta. */
    fun simularRachaMala() {
        repositorio.demo.simularRachaMala()
        refrescar()
    }

    fun borrarAlertas() {
        viewModelScope.launch {
            preferencias.borrarAlertas()
            _mensajes.emit("Historial de alertas borrado")
        }
    }

    companion object {
        private const val INTERVALO_MS = 3000L
    }
}
