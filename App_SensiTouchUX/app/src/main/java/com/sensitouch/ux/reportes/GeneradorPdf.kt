package com.sensitouch.ux.reportes

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.sensitouch.ux.ui.componentes.BarraApilada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Datos que van en el reporte (los arma la pantalla de reportes). */
data class DatosReporte(
    val negocio: String,
    val periodo: String,
    val malo: Int,
    val regular: Int,
    val bueno: Int,
    val barras: List<BarraApilada>,
    val hallazgos: List<String>,
    val alertas: List<String>,
) {
    val total: Int get() = malo + regular + bueno
}

/** Arma un PDF A4 con el resumen, el grafico y los hallazgos, y lo comparte. */
object GeneradorPdf {

    private const val ANCHO = 595
    private const val ALTO = 842
    private const val MARGEN = 40f

    private val AZUL = Color.parseColor("#1F3864")
    private val AZUL_CLARO = Color.parseColor("#2E75B6")
    private val GRIS = Color.parseColor("#5B6B82")
    private val MALO = Color.parseColor("#E5484D")
    private val REGULAR = Color.parseColor("#F2B705")
    private val BUENO = Color.parseColor("#2E9E4F")

    suspend fun crear(context: Context, datos: DatosReporte): File = withContext(Dispatchers.IO) {
        val documento = PdfDocument()
        val pagina = documento.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, 1).create())
        dibujar(pagina.canvas, datos)
        documento.finishPage(pagina)

        val carpeta = File(context.cacheDir, "reportes").apply { mkdirs() }
        val nombre = "SensiTouchUX_Reporte_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))}.pdf"
        val archivo = File(carpeta, nombre)
        archivo.outputStream().use { documento.writeTo(it) }
        documento.close()
        archivo
    }

    fun compartir(context: Context, archivo: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte SensiTouch UX")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun dibujar(c: Canvas, d: DatosReporte) {
        val texto = Paint(Paint.ANTI_ALIAS_FLAG)
        val relleno = Paint(Paint.ANTI_ALIAS_FLAG)

        // Encabezado
        relleno.color = AZUL
        c.drawRect(0f, 0f, ANCHO.toFloat(), 110f, relleno)
        texto.color = Color.WHITE
        texto.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        texto.textSize = 24f
        c.drawText("SensiTouch UX", MARGEN, 50f, texto)
        texto.typeface = Typeface.DEFAULT
        texto.textSize = 13f
        c.drawText("Reporte de opiniones · ${d.negocio}", MARGEN, 75f, texto)
        c.drawText(d.periodo, MARGEN, 95f, texto)

        var y = 150f

        // Totales
        val tarjetas = listOf(
            Triple("Total", d.total, AZUL_CLARO),
            Triple("Bueno", d.bueno, BUENO),
            Triple("Regular", d.regular, REGULAR),
            Triple("Malo", d.malo, MALO),
        )
        val anchoTarjeta = (ANCHO - 2 * MARGEN - 3 * 12f) / 4
        tarjetas.forEachIndexed { i, (nombre, valor, color) ->
            val x = MARGEN + i * (anchoTarjeta + 12f)
            relleno.color = color
            relleno.alpha = 30
            c.drawRoundRect(RectF(x, y - 30f, x + anchoTarjeta, y + 35f), 10f, 10f, relleno)
            relleno.alpha = 255
            texto.color = color
            texto.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            texto.textSize = 22f
            c.drawText("$valor", x + 12f, y + 2f, texto)
            texto.typeface = Typeface.DEFAULT
            texto.textSize = 11f
            texto.color = GRIS
            val pct = if (d.total == 0 || nombre == "Total") "" else " (${valor * 100 / d.total} %)"
            c.drawText("$nombre$pct", x + 12f, y + 22f, texto)
        }
        y += 80f

        // Grafico
        texto.color = AZUL
        texto.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        texto.textSize = 15f
        c.drawText("Votos en el periodo", MARGEN, y, texto)
        y += 15f
        val altoGrafico = 200f
        val anchoGrafico = ANCHO - 2 * MARGEN
        val maximo = maxOf(1, d.barras.maxOfOrNull { it.total } ?: 1)
        val ancho = if (d.barras.isEmpty()) 0f else anchoGrafico / d.barras.size
        relleno.color = Color.parseColor("#DDE4EE")
        for (i in 0..4) {
            val gy = y + altoGrafico * i / 4f
            c.drawLine(MARGEN, gy, MARGEN + anchoGrafico, gy, relleno)
        }
        texto.typeface = Typeface.DEFAULT
        texto.textSize = 8f
        texto.color = GRIS
        val paso = if (d.barras.size > 20) 3 else 1
        d.barras.forEachIndexed { i, b ->
            val x = MARGEN + i * ancho + ancho * 0.2f
            val w = ancho * 0.6f
            var base = y + altoGrafico
            listOf(b.bueno to BUENO, b.regular to REGULAR, b.malo to MALO).forEach { (valor, color) ->
                if (valor > 0) {
                    val h = altoGrafico * valor / maximo
                    relleno.color = color
                    c.drawRect(x, base - h, x + w, base, relleno)
                    base -= h
                }
            }
            if (i % paso == 0) c.drawText(b.etiqueta, x, y + altoGrafico + 12f, texto)
        }
        y += altoGrafico + 40f

        // Hallazgos
        y = seccion(c, "Lo que dicen los datos", d.hallazgos.ifEmpty { listOf("Aún no hay suficientes votos.") }, y, texto)
        y += 10f
        seccion(c, "Alertas del periodo", d.alertas.ifEmpty { listOf("No hubo alertas.") }.take(8), y, texto)

        // Pie
        texto.color = GRIS
        texto.textSize = 9f
        texto.typeface = Typeface.DEFAULT
        val fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        c.drawText("Generado por la app SensiTouch UX el $fecha · Grupo 8 · TECSUP", MARGEN, ALTO - 25f, texto)
    }

    private fun seccion(c: Canvas, titulo: String, lineas: List<String>, inicio: Float, texto: Paint): Float {
        var y = inicio
        texto.color = AZUL
        texto.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        texto.textSize = 15f
        c.drawText(titulo, MARGEN, y, texto)
        y += 20f
        texto.typeface = Typeface.DEFAULT
        texto.textSize = 11f
        texto.color = Color.parseColor("#14213D")
        lineas.forEach { linea ->
            partir("•  $linea", texto, ANCHO - 2 * MARGEN).forEach { parte ->
                c.drawText(parte, MARGEN, y, texto)
                y += 16f
            }
            y += 2f
        }
        return y
    }

    /** Parte una linea larga para que entre en el ancho de la hoja. */
    private fun partir(linea: String, texto: Paint, ancho: Float): List<String> {
        val resultado = mutableListOf<String>()
        var actual = ""
        linea.split(" ").forEach { palabra ->
            val prueba = if (actual.isEmpty()) palabra else "$actual $palabra"
            if (texto.measureText(prueba) > ancho && actual.isNotEmpty()) {
                resultado += actual
                actual = "   $palabra"
            } else {
                actual = prueba
            }
        }
        if (actual.isNotEmpty()) resultado += actual
        return resultado
    }
}
