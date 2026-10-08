package com.app.nutrimeta

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class EmailDialog : DialogFragment() {

    private lateinit var auth: FirebaseAuth

    private var esLogin: Boolean = false
    private var objetivo: String = ""
    private var sexo: String = ""
    private var edad: Int = 0
    private var altura: Int = 0
    private var peso: Double = 0.0

    companion object {
        const val TAG = "EmailDialog"
        private const val EXTRA_ES_LOGIN = "EXTRA_ES_LOGIN"
        private const val EXTRA_OBJETIVO = "EXTRA_OBJETIVO"
        private const val EXTRA_SEXO = "EXTRA_SEXO"
        private const val EXTRA_EDAD = "EXTRA_EDAD"
        private const val EXTRA_ALTURA = "EXTRA_ALTURA"
        private const val EXTRA_PESO = "EXTRA_PESO"

        // Para el flujo de REGISTRO (desde RegistroActivity)
        fun newInstance(objetivo: String, sexo: String, edad: Int, altura: Int, peso: Double): EmailDialog {
            val dialog = EmailDialog()
            val args = Bundle().apply {
                putBoolean(EXTRA_ES_LOGIN, false)
                putString(EXTRA_OBJETIVO, objetivo)
                putString(EXTRA_SEXO, sexo)
                putInt(EXTRA_EDAD, edad)
                putInt(EXTRA_ALTURA, altura)
                putDouble(EXTRA_PESO, peso)
            }
            dialog.arguments = args
            return dialog
        }

        // Para el flujo de INICIAR SESIÓN (desde LoginDialog)
        fun newInstanceLogin(): EmailDialog {
            val dialog = EmailDialog()
            val args = Bundle().apply {
                putBoolean(EXTRA_ES_LOGIN, true)
            }
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        arguments?.let {
            esLogin = it.getBoolean(EXTRA_ES_LOGIN, false)
            objetivo = it.getString(EXTRA_OBJETIVO, "")
            sexo = it.getString(EXTRA_SEXO, "")
            edad = it.getInt(EXTRA_EDAD, 0)
            altura = it.getInt(EXTRA_ALTURA, 0)
            peso = it.getDouble(EXTRA_PESO, 0.0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_email_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val tilEmail = view.findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = view.findViewById<TextInputLayout>(R.id.tilPassword)
        val btnCancelar = view.findViewById<MaterialButton>(R.id.btnCancelar)
        val btnRegistrar = view.findViewById<MaterialButton>(R.id.btnRegistrar)

        // Cambiar el texto del botón según el modo
        if (esLogin) {
            btnRegistrar.setText(R.string.btn_iniciar_sesion)
        } else {
            btnRegistrar.setText(R.string.btn_registrarse)
        }

        btnCancelar.setOnClickListener { dismiss() }

        btnRegistrar.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            tilEmail.error = null
            tilPassword.error = null

            if (email.isEmpty()) {
                tilEmail.error = getString(R.string.error_correo_requerido)
                return@setOnClickListener
            }

            if (password.length < 6) {
                tilPassword.error = getString(R.string.error_password_longitud, 6)
                return@setOnClickListener
            }

            if (esLogin) {
                // Modo Iniciar Sesión
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.login_exito_formato, user?.email.orEmpty()),
                                Toast.LENGTH_SHORT
                            ).show()
                            dismiss()
                            // TODO: Redirigir a HomeActivity
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.registro_error_formato, task.exception?.localizedMessage.orEmpty()),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            } else {
                // Modo Crear Cuenta
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.registro_exito_formato, user?.email.orEmpty(), peso, objetivo),
                                Toast.LENGTH_LONG
                            ).show()
                            dismiss()
                            // TODO: Redirigir a HomeActivity
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.registro_error_formato, task.exception?.localizedMessage.orEmpty()),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}