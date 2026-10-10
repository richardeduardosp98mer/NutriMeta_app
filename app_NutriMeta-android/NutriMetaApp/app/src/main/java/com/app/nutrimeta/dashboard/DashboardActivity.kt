package com.app.nutrimeta.dashboard

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.app.nutrimeta.MainActivity
import com.app.nutrimeta.R
import com.app.nutrimeta.data.local.NutriMetaDatabaseHelper
import com.app.nutrimeta.profile.PerfilActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class DashboardActivity : AppCompatActivity() {

    private val fechaSeleccionada = Calendar.getInstance()
    private val localeEs = Locale.forLanguageTag("es-PE")

    private lateinit var uidUsuario: String
    private lateinit var contenedorSemana: LinearLayout

    private lateinit var tvFecha: TextView
    private lateinit var tvObjetivo: TextView
    private lateinit var tvCalorias: TextView
    private lateinit var tvProteinas: TextView
    private lateinit var tvGrasas: TextView
    private lateinit var tvCarbohidratos: TextView
    private lateinit var tvActividad: TextView
    private lateinit var tvEstado: TextView

    private lateinit var progresoCalorias: ProgressBar
    private lateinit var progresoProteinas: ProgressBar
    private lateinit var progresoGrasas: ProgressBar
    private lateinit var progresoCarbohidratos: ProgressBar

    private var numeroCarga = 0

    private data class Consumo(
        val kcal: Double = 0.0,
        val proteinas: Double = 0.0,
        val grasas: Double = 0.0,
        val carbohidratos: Double = 0.0
    ) {
        operator fun plus(otro: Consumo) = Consumo(
            kcal + otro.kcal,
            proteinas + otro.proteinas,
            grasas + otro.grasas,
            carbohidratos + otro.carbohidratos
        )
    }

    private data class PerfilMetas(
        val objetivo: String,
        val calorias: Double,
        val proteinas: Double,
        val grasas: Double,
        val carbohidratos: Double,
        val actividad: String,
        val factorActividad: Double
    )

    private data class DatosDashboard(
        val perfil: PerfilMetas?,
        val consumido: Consumo,
        val comidas: Map<String, Consumo>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        configurarBarrasSistema()
        vincularVistas()
        configurarBotones()

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            irABienvenida()
            return
        }

        uidUsuario = usuario.uid

        dibujarSemana()
    }

    private fun configurarBarrasSistema() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val raiz = findViewById<View>(R.id.rootDashboard)

        ViewCompat.setOnApplyWindowInsetsListener(raiz) {
                vista, insets ->

            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            vista.setPadding(
                0,
                barras.top,
                0,
                barras.bottom
            )

            insets
        }
    }

    private fun vincularVistas() {
        contenedorSemana = findViewById(
            R.id.contenedorSemana
        )

        tvFecha = findViewById(R.id.tvFechaDashboard)
        tvObjetivo = findViewById(R.id.tvObjetivoDashboard)
        tvCalorias = findViewById(R.id.tvCaloriasDashboard)
        tvProteinas = findViewById(R.id.tvProteinasDashboard)
        tvGrasas = findViewById(R.id.tvGrasasDashboard)
        tvCarbohidratos = findViewById(
            R.id.tvCarbohidratosDashboard
        )
        tvActividad = findViewById(R.id.tvActividadDashboard)
        tvEstado = findViewById(R.id.tvEstadoDashboard)

        progresoCalorias = findViewById(
            R.id.progresoCaloriasDashboard
        )
        progresoProteinas = findViewById(
            R.id.progresoProteinasDashboard
        )
        progresoGrasas = findViewById(
            R.id.progresoGrasasDashboard
        )
        progresoCarbohidratos = findViewById(
            R.id.progresoCarbohidratosDashboard
        )
    }

    private fun configurarBotones() {

        val usuarioActual = FirebaseAuth.getInstance().currentUser

        val nombreAvatar = usuarioActual?.displayName?.takeIf { it.isNotBlank() } ?: usuarioActual?.email.orEmpty()

        findViewById<TextView>(
            R.id.tvAvatarDashboard
        ).apply {

            text = nombreAvatar
                .firstOrNull()
                ?.uppercaseChar()
                ?.toString()
                ?: "?"

            setOnClickListener {
                startActivity(
                    Intent(
                        this@DashboardActivity,
                        PerfilActivity::class.java
                    )
                )
            }
        }

        findViewById<View>(
            R.id.btnSemanaAnterior
        ).setOnClickListener {
            fechaSeleccionada.add(
                Calendar.DAY_OF_MONTH,
                -7
            )
            cambiarFecha()
        }

        findViewById<View>(
            R.id.btnSemanaSiguiente
        ).setOnClickListener {
            fechaSeleccionada.add(
                Calendar.DAY_OF_MONTH,
                7
            )
            cambiarFecha()
        }

        prepararTarjeta(
            R.id.comidaDesayuno,
            R.string.nm_desayuno
        )

        prepararTarjeta(
            R.id.comidaAlmuerzo,
            R.string.nm_almuerzo
        )

        prepararTarjeta(
            R.id.comidaCena,
            R.string.nm_cena
        )

        prepararTarjeta(
            R.id.comidaSnack,
            R.string.nm_snack
        )
    }

    private fun prepararTarjeta(
        idTarjeta: Int,
        tituloRes: Int
    ) {

        val tarjeta = findViewById<View>(idTarjeta)

        tarjeta.findViewById<TextView>(
            R.id.tvTituloComida
        ).setText(tituloRes)

        tarjeta.findViewById<MaterialButton>(
            R.id.btnAgregarComida
        ).setOnClickListener {

            Toast.makeText(
                this,
                getString(
                    R.string.nm_registro_pendiente,
                    getString(tituloRes)
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun cambiarFecha() {
        dibujarSemana()
        cargarDatos()
    }

    private fun dibujarSemana() {

        val formato = SimpleDateFormat(
            "EEEE, d 'de' MMMM",
            localeEs
        )

        tvFecha.text = formato.format(
            fechaSeleccionada.time
        ).replaceFirstChar {
            it.titlecase(localeEs)
        }

        contenedorSemana.removeAllViews()

        val lunes = fechaSeleccionada.clone() as Calendar

        val desplazamiento =
            (lunes.get(Calendar.DAY_OF_WEEK) + 5) % 7

        lunes.add(
            Calendar.DAY_OF_MONTH,
            -desplazamiento
        )

        val etiquetas = resources.getStringArray(
            R.array.nm_dias_semana
        )

        for (i in 0..6) {

            val dia = lunes.clone() as Calendar

            dia.add(Calendar.DAY_OF_MONTH, i)

            val seleccionado =
                mismoDia(dia, fechaSeleccionada)

            val bloque = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )

                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(12)
                )

                if (seleccionado) {
                    background = GradientDrawable().apply {
                        setColor(
                            ContextCompat.getColor(
                                this@DashboardActivity,
                                R.color.nm_acento
                            )
                        )
                        cornerRadius = dp(16).toFloat()
                    }
                }
            }

            val colorTexto = ContextCompat.getColor(
                this,
                if (seleccionado) {
                    R.color.nm_fondo
                } else {
                    R.color.nm_texto
                }
            )

            val etiqueta = TextView(this).apply {
                text = etiquetas[i]
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(colorTexto)
            }

            val numero = TextView(this).apply {
                text = dia.get(
                    Calendar.DAY_OF_MONTH
                ).toString()

                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(colorTexto)
            }

            bloque.addView(etiqueta)
            bloque.addView(numero)

            bloque.setOnClickListener {
                fechaSeleccionada.timeInMillis =
                    dia.timeInMillis

                cambiarFecha()
            }

            contenedorSemana.addView(bloque)
        }
    }

    private fun mismoDia(
        primero: Calendar,
        segundo: Calendar
    ): Boolean {
        return primero.get(Calendar.YEAR) ==
                segundo.get(Calendar.YEAR) &&
                primero.get(Calendar.DAY_OF_YEAR) ==
                segundo.get(Calendar.DAY_OF_YEAR)
    }

    private fun cargarDatos() {

        val fechaSql = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(fechaSeleccionada.time)

        val cargaActual = ++numeroCarga

        tvEstado.visibility = View.VISIBLE
        tvEstado.setText(R.string.dashboard_cargando)

        Thread {

            val resultado = runCatching {
                consultarSQLite(
                    uidUsuario,
                    fechaSql
                )
            }

            runOnUiThread {

                if (
                    isFinishing ||
                    isDestroyed ||
                    cargaActual != numeroCarga ||
                    FirebaseAuth.getInstance()
                        .currentUser?.uid != uidUsuario
                ) {
                    return@runOnUiThread
                }

                resultado.fold(
                    onSuccess = { datos ->
                        mostrarDatos(datos)
                    },
                    onFailure = {
                        tvEstado.visibility = View.VISIBLE
                        tvEstado.setText(
                            R.string.dashboard_error_carga
                        )
                    }
                )
            }

        }.start()
    }

    private fun consultarSQLite(
        uid: String,
        fecha: String
    ): DatosDashboard {

        NutriMetaDatabaseHelper.Companion.forUser(
            applicationContext,
            uid
        ).use { helper ->

            val db = helper.readableDatabase

            // Perfil nutricional y factor de actividad.
            val perfil = db.rawQuery(
                """
                SELECT
                    p.objetivo,
                    p.meta_kcal,
                    p.meta_proteina_g,
                    p.meta_grasa_g,
                    p.meta_carbohidratos_g,
                    n.nombre,
                    n.factor
                FROM perfil p
                LEFT JOIN nivel_actividad n
                    ON n.id_nivel_actividad =
                       p.id_nivel_actividad
                WHERE p.id = 1
                """.trimIndent(),
                null
            ).use { cursor ->

                if (cursor.moveToFirst()) {

                    PerfilMetas(
                        objetivo = cursor.getString(0),
                        calorias = cursor.getDouble(1),
                        proteinas = cursor.getDouble(2),
                        grasas = cursor.getDouble(3),
                        carbohidratos = cursor.getDouble(4),
                        actividad = cursor.getString(5)
                            ?: "",
                        factorActividad = cursor.getDouble(6)
                    )
                } else {
                    null
                }
            }

            var total = Consumo()

            val comidas = mutableMapOf<String, Consumo>()

            // Sumar los alimentos realmente registrados.
            db.rawQuery(
                """
                SELECT
                    t.nombre,
                    COALESCE(SUM(d.energia_kcal), 0),
                    COALESCE(SUM(d.proteina_g), 0),
                    COALESCE(SUM(d.grasa_g), 0),
                    COALESCE(SUM(d.carbohidratos_g), 0)
                FROM registro_comida r
                JOIN tipo_comida t
                    ON t.id_tipo_comida = r.id_tipo_comida
                JOIN detalle_registro d
                    ON d.id_registro = r.id_registro
                WHERE r.eliminado = 0
                    AND SUBSTR(r.fecha, 1, 10) = ?
                GROUP BY t.id_tipo_comida, t.nombre
                """.trimIndent(),
                arrayOf(fecha)
            ).use { cursor ->

                while (cursor.moveToNext()) {

                    val nombre = cursor.getString(0)

                    val consumo = Consumo(
                        kcal = cursor.getDouble(1),
                        proteinas = cursor.getDouble(2),
                        grasas = cursor.getDouble(3),
                        carbohidratos = cursor.getDouble(4)
                    )

                    total += consumo

                    val clave = clasificarComida(nombre)

                    if (clave != null) {
                        comidas[clave] =
                            (comidas[clave] ?: Consumo()) +
                                    consumo
                    }
                }
            }

            return DatosDashboard(
                perfil = perfil,
                consumido = total,
                comidas = comidas
            )
        }
    }

    private fun clasificarComida(
        nombre: String
    ): String? {

        val valor = nombre.lowercase(Locale.ROOT)

        return when {
            "desay" in valor -> "desayuno"
            "almuer" in valor -> "almuerzo"
            "cena" in valor -> "cena"
            "snack" in valor ||
                    "meriend" in valor ||
                    "colaci" in valor -> "snack"
            else -> null
        }
    }

    private fun mostrarDatos(datos: DatosDashboard) {

        val perfil = datos.perfil

        if (perfil == null) {
            tvEstado.visibility = View.VISIBLE
            tvEstado.setText(R.string.dashboard_sin_perfil)
            return
        }

        tvEstado.visibility = View.GONE

        tvObjetivo.text = when (perfil.objetivo) {
            "BAJAR" -> getString(
                R.string.dashboard_objetivo_bajar
            )
            "MANTENER" -> getString(
                R.string.dashboard_objetivo_mantener
            )
            "SUBIR" -> getString(
                R.string.dashboard_objetivo_subir
            )
            else -> perfil.objetivo
        }

        tvCalorias.text = getString(
            R.string.nm_calorias_formato,
            datos.consumido.kcal,
            perfil.calorias
        )

        tvProteinas.text = getString(
            R.string.nm_macro_proteinas_formato,
            datos.consumido.proteinas,
            perfil.proteinas
        )

        tvGrasas.text = getString(
            R.string.nm_macro_grasas_formato,
            datos.consumido.grasas,
            perfil.grasas
        )

        tvCarbohidratos.text = getString(
            R.string.nm_macro_carbohidratos_formato,
            datos.consumido.carbohidratos,
            perfil.carbohidratos
        )

        actualizarProgreso(
            progresoCalorias,
            datos.consumido.kcal,
            perfil.calorias
        )

        actualizarProgreso(
            progresoProteinas,
            datos.consumido.proteinas,
            perfil.proteinas
        )

        actualizarProgreso(
            progresoGrasas,
            datos.consumido.grasas,
            perfil.grasas
        )

        actualizarProgreso(
            progresoCarbohidratos,
            datos.consumido.carbohidratos,
            perfil.carbohidratos
        )

        tvActividad.text = getString(
            R.string.nm_actividad_formato,
            perfil.actividad,
            perfil.factorActividad
        )

        mostrarComida(
            R.id.comidaDesayuno,
            datos.comidas["desayuno"] ?: Consumo()
        )

        mostrarComida(
            R.id.comidaAlmuerzo,
            datos.comidas["almuerzo"] ?: Consumo()
        )

        mostrarComida(
            R.id.comidaCena,
            datos.comidas["cena"] ?: Consumo()
        )

        mostrarComida(
            R.id.comidaSnack,
            datos.comidas["snack"] ?: Consumo()
        )
    }

    private fun mostrarComida(
        idTarjeta: Int,
        consumo: Consumo
    ) {

        val tarjeta = findViewById<View>(idTarjeta)

        tarjeta.findViewById<TextView>(
            R.id.tvResumenComida
        ).text = getString(
            R.string.nm_resumen_comida_formato,
            consumo.kcal,
            consumo.proteinas,
            consumo.carbohidratos,
            consumo.grasas
        )
    }

    private fun actualizarProgreso(
        barra: ProgressBar,
        consumido: Double,
        meta: Double
    ) {

        barra.progress = if (
            meta > 0.0 &&
            consumido.isFinite() &&
            meta.isFinite()
        ) {
            ((consumido / meta) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
        } else {
            0
        }
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density)
            .roundToInt()
    }

    private fun irABienvenida() {

        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()

        if (::uidUsuario.isInitialized) {
            cargarDatos()
        }
    }
}