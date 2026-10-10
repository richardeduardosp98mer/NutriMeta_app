package com.app.nutrimeta.auth

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.app.nutrimeta.auth.EmailDialog
import com.app.nutrimeta.Navegacion
import com.app.nutrimeta.ui.dialogs.PadreDialog
import com.app.nutrimeta.R
import com.app.nutrimeta.data.local.BaseDatosInicializador
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginDialog : PadreDialog() {

    override val tituloResId: Int = R.string.titulo_iniciar_sesion
    override val layoutContenidoResId: Int = R.layout.activity_login_dialog

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Launcher para recibir el resultado del selector de cuentas de Google
    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val cuenta: GoogleSignInAccount = task.getResult(ApiException::class.java)
                val credencial = GoogleAuthProvider.getCredential(cuenta.idToken, null)

                auth.signInWithCredential(credencial)
                    .addOnCompleteListener(requireActivity()) { authTask ->

                        if (authTask.isSuccessful) {

                            val usuario = authTask.result?.user
                            val contexto = context ?: return@addOnCompleteListener

                            if (usuario != null) {

                                // Abrir y preparar SQLite del usuario autenticado
                                BaseDatosInicializador.iniciar(
                                    contexto.applicationContext,
                                    usuario.uid
                                )

                                // Mostrar mensaje de bienvenida
                                Toast.makeText(
                                    contexto,
                                    contexto.getString(
                                        R.string.login_exito_formato,
                                        usuario.displayName
                                            ?: usuario.email.orEmpty()
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()

                                //navegacion
                                Navegacion.abrirDashboard(requireActivity())
                            }

                        } else {

                            val contexto = context ?: return@addOnCompleteListener

                            Toast.makeText(
                                contexto,
                                contexto.getString(
                                    R.string.error_login_formato,
                                    authTask.exception?.localizedMessage.orEmpty()
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: ApiException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_google_formato, e.statusCode),
                        Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // Configuración de Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun configurarContenido(viewHijo: View) {
        val btnGoogle = viewHijo.findViewById<MaterialButton>(R.id.btnGoogle)
        val btnCorreo = viewHijo.findViewById<MaterialButton>(R.id.btnCorreo)

        // Boton Google: Cierra sesion previa para forzar a que siempre muestre el selector de cuentas
        btnGoogle.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                googleLauncher.launch(googleSignInClient.signInIntent)
            }
        }

        // Boton Correo: Cierra LoginDialog y abre tu EmailDialog en modo login
        btnCorreo.setOnClickListener {
            dismiss()
            EmailDialog.Companion.newInstanceLogin().show(parentFragmentManager, "EmailDialog")
        }
    }

    companion object {
        const val TAG = "LoginDialog"
        fun newInstance(): LoginDialog = LoginDialog()
    }
}