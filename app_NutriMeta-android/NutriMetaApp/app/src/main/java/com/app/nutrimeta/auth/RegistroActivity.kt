package com.app.nutrimeta.auth

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.app.nutrimeta.profile.DatosRegistroPerfil
import com.app.nutrimeta.Navegacion
import com.app.nutrimeta.R
import com.app.nutrimeta.profile.RegistroPerfilService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class RegistroActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    // Datos recibidos del onboarding
    private var objetivo: String = ""
    private var sexo: String = ""
    private var edad: Int = 0
    private var fechaNacimiento: String = ""
    private var altura: Int = 0
    private var peso: Double = 0.0
    private var actividad: String = ""

    // Resultado del selector de cuentas Google
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == RESULT_OK) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                val account: GoogleSignInAccount =
                    task.getResult(ApiException::class.java)

                autenticarConFirebaseGoogle(account.idToken)

            } catch (e: ApiException) {
                Toast.makeText(
                    this,
                    getString(
                        R.string.error_autenticacion_google,
                        e.statusCode
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }

        } else {
            Toast.makeText(
                this,
                getString(R.string.inicio_sesion_cancelado),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()

        // Recibir datos de SobreUsuarioActivity
        objetivo = intent.getStringExtra("EXTRA_OBJETIVO") ?: ""
        sexo = intent.getStringExtra("EXTRA_SEXO") ?: ""
        edad = intent.getIntExtra("EXTRA_EDAD", 0)
        fechaNacimiento = intent.getStringExtra("EXTRA_FECHA_NACIMIENTO").orEmpty()
        altura = intent.getIntExtra("EXTRA_ALTURA", 0)
        peso = intent.getDoubleExtra("EXTRA_PESO", 0.0)
        actividad = intent.getStringExtra("EXTRA_ACTIVIDAD") ?: ""

        val btnGoogle = findViewById<MaterialButton>(R.id.btnGoogle)
        val btnEmail = findViewById<MaterialButton>(R.id.btnEmail)
        val tvTerminos = findViewById<TextView>(R.id.tvTerminos)

        // Mostrar términos y condiciones
        tvTerminos.text = HtmlCompat.fromHtml(
            getString(R.string.auth_terminos),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        // Configuración de Google Sign-In
        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // REGISTRO CON GOOGLE
        btnGoogle.setOnClickListener {

            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(
                    googleSignInClient.signInIntent
                )
            }
        }

        btnEmail.setOnClickListener {

            EmailDialog.newInstance(
                objetivo,
                sexo,
                edad,
                altura,
                peso,
                actividad,
                fechaNacimiento
            ).show(
                supportFragmentManager,
                EmailDialog.TAG
            )
        }
    }

    // AUTENTICACIÓN CON GOOGLE EN FIREBASE

    private fun autenticarConFirebaseGoogle(idToken: String?) {

        if (idToken == null) {
            Toast.makeText(
                this,
                getString(R.string.error_token_google_nulo),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val credential = GoogleAuthProvider.getCredential(
            idToken,
            null
        )

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                if (!task.isSuccessful) {

                    Toast.makeText(
                        this,
                        getString(
                            R.string.error_firebase,
                            task.exception?.localizedMessage.orEmpty()
                        ),
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnCompleteListener
                }

                val usuario = task.result?.user

                if (usuario == null) {

                    Toast.makeText(
                        this,
                        R.string.error_usuario_firebase,
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnCompleteListener
                }

                // Datos recopilados en SobreUsuarioActivity.
                val datosPerfil = DatosRegistroPerfil(
                    objetivo = objetivo,
                    sexo = sexo,
                    edad = edad,
                    fechaNacimiento = fechaNacimiento,
                    altura = altura,
                    peso = peso,
                    actividad = actividad
                )

                // El servicio abre SQLite, calcula las
                // metas y guarda el perfil si no existe.
                RegistroPerfilService.guardarSiNoExiste(
                    context = applicationContext,
                    firebaseUid = usuario.uid,
                    datos = datosPerfil
                ) { resultado ->

                    if (isFinishing || isDestroyed) {
                        return@guardarSiNoExiste
                    }

                    resultado.fold(
                        onSuccess = { perfilCreado ->

                            val mensaje = if (perfilCreado) {
                                R.string.perfil_guardado_exito
                            } else {
                                R.string.perfil_ya_existia
                            }

                            Toast.makeText(
                                this,
                                mensaje,
                                Toast.LENGTH_LONG
                            ).show()
                            //navegacion
                            Navegacion.abrirDashboard(this@RegistroActivity)
                        },

                        onFailure = {

                            // Firebase pudo autenticar correctamente
                            // aunque el guardado local haya fallado.
                            Toast.makeText(
                                this,
                                R.string.error_guardar_perfil,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }
    }

}