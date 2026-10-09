
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

    // Datos recibidos del onboarding
    private var objetivo: String = ""
    private var sexo: String = ""
    private var edad: Int = 0
    private var fechaNacimiento: String = ""
    private var altura: Int = 0
    private var peso: Double = 0.0
    private var actividad: String = ""

    companion object {

        const val TAG = "EmailDialog"

        private const val EXTRA_ES_LOGIN = "EXTRA_ES_LOGIN"
        private const val EXTRA_OBJETIVO = "EXTRA_OBJETIVO"
        private const val EXTRA_SEXO = "EXTRA_SEXO"
        private const val EXTRA_EDAD = "EXTRA_EDAD"
        private const val EXTRA_FECHA_NACIMIENTO = "EXTRA_FECHA_NACIMIENTO"
        private const val EXTRA_ALTURA = "EXTRA_ALTURA"
        private const val EXTRA_PESO = "EXTRA_PESO"
        private const val EXTRA_ACTIVIDAD = "EXTRA_ACTIVIDAD"

        // Registro de una nueva cuenta
        fun newInstance(
            objetivo: String,
            sexo: String,
            edad: Int,
            altura: Int,
            peso: Double,
            actividad: String = "",
            fechaNacimiento: String = ""
        ): EmailDialog {

            return EmailDialog().apply {
                arguments = Bundle().apply {
                    putBoolean(EXTRA_ES_LOGIN, false)
                    putString(EXTRA_OBJETIVO, objetivo)
                    putString(EXTRA_SEXO, sexo)
                    putInt(EXTRA_EDAD, edad)
                    putInt(EXTRA_ALTURA, altura)
                    putDouble(EXTRA_PESO, peso)
                    putString(EXTRA_ACTIVIDAD, actividad)
                    putString(EXTRA_FECHA_NACIMIENTO, fechaNacimiento)
                }
            }
        }

        // Inicio de sesión con una cuenta existente
        fun newInstanceLogin(): EmailDialog {

            return EmailDialog().apply {

                arguments = Bundle().apply {
                    putBoolean(EXTRA_ES_LOGIN, true)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        arguments?.let { args ->

            esLogin = args.getBoolean(EXTRA_ES_LOGIN, false)

            objetivo = args.getString(EXTRA_OBJETIVO, "")
            sexo = args.getString(EXTRA_SEXO, "")
            edad = args.getInt(EXTRA_EDAD, 0)
            fechaNacimiento = args.getString(EXTRA_FECHA_NACIMIENTO, "")
            altura = args.getInt(EXTRA_ALTURA, 0)
            peso = args.getDouble(EXTRA_PESO, 0.0)
            actividad = args.getString(EXTRA_ACTIVIDAD, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.activity_email_dialog,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        val etEmail =
            view.findViewById<TextInputEditText>(R.id.etEmail)

        val etPassword =
            view.findViewById<TextInputEditText>(R.id.etPassword)

        val tilEmail =
            view.findViewById<TextInputLayout>(R.id.tilEmail)

        val tilPassword =
            view.findViewById<TextInputLayout>(R.id.tilPassword)

        val btnCancelar =
            view.findViewById<MaterialButton>(R.id.btnCancelar)

        val btnRegistrar =
            view.findViewById<MaterialButton>(R.id.btnRegistrar)

        // Configurar el texto según el modo
        if (esLogin) {
            btnRegistrar.setText(R.string.btn_iniciar_sesion)
        } else {
            btnRegistrar.setText(R.string.btn_registrarse)
        }

        btnCancelar.setOnClickListener {
            dismiss()
        }

        btnRegistrar.setOnClickListener {

            val email = etEmail.text
                ?.toString()
                ?.trim()
                .orEmpty()
            val password = etPassword.text
                ?.toString()
                .orEmpty()

            tilEmail.error = null
            tilPassword.error = null

            // Validar correo
            if (email.isEmpty()) {

                tilEmail.error =
                    getString(R.string.error_correo_requerido)

                return@setOnClickListener
            }

            // Validar contraseña obligatoria
            if (password.isEmpty()) {

                tilPassword.error =
                    getString(R.string.error_password_requerida)

                return@setOnClickListener
            }

            // Firebase requiere al menos 6 caracteres
            // para crear una contraseña.
            if (!esLogin && password.length < 6) {

                tilPassword.error = getString(
                    R.string.error_password_longitud,
                    6
                )

                return@setOnClickListener
            }

            // Evitar solicitudes duplicadas
            btnRegistrar.isEnabled = false

            if (esLogin) {

                // INICIAR SESIÓN
                auth.signInWithEmailAndPassword(
                    email,
                    password
                ).addOnCompleteListener(requireActivity()) { task ->

                    if (!isAdded) {
                        return@addOnCompleteListener
                    }

                    if (task.isSuccessful) {

                        val user = auth.currentUser

                        if (user != null) {
                            BaseDatosInicializador.iniciar(
                                requireContext().applicationContext,
                                user.uid
                            )
                        }

                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.login_exito_formato,
                                user?.email.orEmpty()
                            ),
                            Toast.LENGTH_SHORT
                        ).show()

                        dismiss()

                    } else {

                        btnRegistrar.isEnabled = true

                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.error_login_formato,
                                task.exception?.localizedMessage.orEmpty()
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } else {

                // CREAR CUENTA NUEVA
                auth.createUserWithEmailAndPassword(
                    email,
                    password
                ).addOnCompleteListener(requireActivity()) { task ->

                    if (!isAdded) {
                        return@addOnCompleteListener
                    }


                    if (task.isSuccessful) {

                        val usuario = task.result?.user
                        val contexto = context ?: return@addOnCompleteListener

                        if (usuario == null) {
                            btnRegistrar.isEnabled = true

                            Toast.makeText(
                                contexto,
                                R.string.error_usuario_firebase,
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnCompleteListener
                        }

                        // Obtener los datos del formulario.
                        val datosPerfil = DatosRegistroPerfil(
                            objetivo = objetivo,
                            sexo = sexo,
                            edad = edad,
                            fechaNacimiento = fechaNacimiento,
                            altura = altura,
                            peso = peso,
                            actividad = actividad
                        )

                        // Calcular las metas y guardar el perfil.
                        RegistroPerfilService.guardarSiNoExiste(
                            context = contexto.applicationContext,
                            firebaseUid = usuario.uid,
                            datos = datosPerfil
                        ) { resultado ->

                            if (!isAdded || this@EmailDialog.view == null) {
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
                                        requireContext(),
                                        mensaje,
                                        Toast.LENGTH_LONG
                                    ).show()

                                    dismiss()
                                },

                                onFailure = {

                                    btnRegistrar.isEnabled = true

                                    Toast.makeText(
                                        requireContext(),
                                        R.string.error_guardar_perfil,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    }
                    else {

                        btnRegistrar.isEnabled = true

                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.registro_error_formato,
                                task.exception?.localizedMessage.orEmpty()
                            ),
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
