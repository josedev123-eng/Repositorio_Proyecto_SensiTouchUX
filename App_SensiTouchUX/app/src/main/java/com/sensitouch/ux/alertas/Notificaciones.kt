package com.sensitouch.ux.alertas

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sensitouch.ux.MainActivity
import com.sensitouch.ux.R
import com.sensitouch.ux.data.Alerta
import com.sensitouch.ux.data.TipoAlerta

/** Canales y avisos del sistema (notificaciones push locales). */
object Notificaciones {

    const val CANAL_ALERTAS = "alertas"
    const val CANAL_RESUMEN = "resumen"
    const val CANAL_DISPOSITIVO = "dispositivo"
    private const val GRUPO_ALERTAS = "com.sensitouch.ux.ALERTAS"
    private const val ID_RESUMEN = 2100

    /** Extra del Intent para abrir una pantalla al tocar la notificacion. */
    const val EXTRA_ABRIR = "abrir"

    fun crearCanales(context: Context) {
        val gestor = context.getSystemService(NotificationManager::class.java)
        val alertas = NotificationChannel(
            CANAL_ALERTAS,
            context.getString(R.string.canal_alertas),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.canal_alertas_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300)
        }
        val resumen = NotificationChannel(
            CANAL_RESUMEN,
            context.getString(R.string.canal_resumen),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.canal_resumen_desc) }
        val dispositivo = NotificationChannel(
            CANAL_DISPOSITIVO,
            context.getString(R.string.canal_dispositivo),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.canal_dispositivo_desc) }
        gestor.createNotificationChannels(listOf(alertas, resumen, dispositivo))
    }

    fun permisoConcedido(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun activadasEnSistema(context: Context): Boolean =
        permisoConcedido(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    @SuppressLint("MissingPermission")
    fun mostrarAlerta(context: Context, alerta: Alerta) {
        if (!activadasEnSistema(context)) return
        val canal = if (alerta.tipo == TipoAlerta.DESCONECTADO) CANAL_DISPOSITIVO else CANAL_ALERTAS
        val destino = if (alerta.tipo == TipoAlerta.DESCONECTADO) "ajustes" else "alertas"
        val notificacion = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(0xFFE5484D.toInt())
            .setContentTitle(alerta.titulo)
            .setContentText(alerta.detalle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alerta.detalle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setWhen(alerta.fechaHora)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setGroup(GRUPO_ALERTAS)
            .setContentIntent(abrirApp(context, destino, alerta.id.hashCode()))
            .addAction(0, "Ver votos", abrirApp(context, "votos", alerta.id.hashCode() + 1))
            .build()
        NotificationManagerCompat.from(context).notify(alerta.id.hashCode(), notificacion)
    }

    @SuppressLint("MissingPermission")
    fun mostrarResumen(context: Context, titulo: String, texto: String) {
        if (!activadasEnSistema(context)) return
        val notificacion = NotificationCompat.Builder(context, CANAL_RESUMEN)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(0xFF1F3864.toInt())
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setAutoCancel(true)
            .setContentIntent(abrirApp(context, "reportes", ID_RESUMEN))
            .build()
        NotificationManagerCompat.from(context).notify(ID_RESUMEN, notificacion)
    }

    private fun abrirApp(context: Context, destino: String, codigo: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ABRIR, destino)
        }
        return PendingIntent.getActivity(
            context,
            codigo,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
