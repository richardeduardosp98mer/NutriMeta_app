package com.app.nutrimeta

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
                            val usuario = auth.currentUser
                            Toast.makeText(
                                requireContext(),
                                "Bienvenido: ${usuario?.displayName ?: usuario?.email}",
                                Toast.LENGTH_SHORT
                            ).show()
                            dismiss()
                            // TODO: Intent a HomeActivity
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Error: ${authTask.exception?.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Fallo Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
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

        // 1. Botón Google: Cierra sesión previa para FORZAR a que siempre muestre el selector de cuentas
        btnGoogle.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                googleLauncher.launch(googleSignInClient.signInIntent)
            }
        }

        // 2. Botón Correo: Cierra LoginDialog y abre tu EmailDialog en modo login
        btnCorreo.setOnClickListener {
            dismiss()
            EmailDialog.newInstanceLogin().show(parentFragmentManager, "EmailDialog")
        }
    }

    companion object {
        const val TAG = "LoginDialog"
        fun newInstance(): LoginDialog = LoginDialog()
    }
}