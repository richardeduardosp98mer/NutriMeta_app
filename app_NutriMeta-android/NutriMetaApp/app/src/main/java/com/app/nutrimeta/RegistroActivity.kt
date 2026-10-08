package com.app.nutrimeta

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
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

    private var objetivo: String = ""
    private var sexo: String = ""
    private var edad: Int = 0
    private var altura: Int = 0
    private var peso: Double = 0.0

    // Receptor del resultado de la ventana emergente de Google
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                // Autenticar la cuenta obtenida dentro de Firebase Auth
                autenticarConFirebaseGoogle(account.idToken)
            } catch (e: ApiException) {
                Toast.makeText(this, "Fallo al autenticar: código ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Inicio de sesión cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Inicializar Firebase Authentication
        auth = FirebaseAuth.getInstance()

        objetivo = intent.getStringExtra("EXTRA_OBJETIVO") ?: ""
        sexo = intent.getStringExtra("EXTRA_SEXO") ?: ""
        edad = intent.getIntExtra("EXTRA_EDAD", 0)
        altura = intent.getIntExtra("EXTRA_ALTURA", 0)
        peso = intent.getDoubleExtra("EXTRA_PESO", 0.0)

        val btnGoogle = findViewById<MaterialButton>(R.id.btnGoogle)
        val btnEmail = findViewById<MaterialButton>(R.id.btnEmail)
        val tvTerminos = findViewById<TextView>(R.id.tvTerminos)

        tvTerminos.text = HtmlCompat.fromHtml(
            getString(R.string.auth_terminos),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        // Solicitar el idToken web que genera google-services.json
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogle.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        // Abre el diálogo para ingresar correo y clave
        btnEmail.setOnClickListener {
            EmailDialog.newInstance(objetivo, sexo, edad, altura, peso)
                .show(supportFragmentManager, "EmailDialog.TAG")
        }
    }

    // --- REGISTRO CON GOOGLE EN FIREBASE ---
    private fun autenticarConFirebaseGoogle(idToken: String?) {
        if (idToken == null) {
            Toast.makeText(this, "Error: Token de Google nulo", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(
                        this,
                        "¡Bienvenido ${user?.displayName}!\nEmail: ${user?.email}\nPeso: $peso kg | Meta: $objetivo",
                        Toast.LENGTH_LONG
                    ).show()

                    // TODO: Enviar métricas físicas y UID a tu backend
                } else {
                    Toast.makeText(this, "Error Firebase: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}