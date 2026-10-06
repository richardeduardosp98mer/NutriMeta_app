package com.app.nutrimeta

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton

class LoginDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_iniciar_sesion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCerrar = view.findViewById<ImageView>(R.id.btnCerrarDialog)
        val btnGoogle = view.findViewById<MaterialButton>(R.id.btnGoogle)
        val btnFacebook = view.findViewById<MaterialButton>(R.id.btnFacebook)
        val btnCorreo = view.findViewById<MaterialButton>(R.id.btnCorreo)

        // Cierra el dialogo de iniciar sesion
        btnCerrar.setOnClickListener {
            dismiss()
        }

        // Acciones preparadas
        btnGoogle.setOnClickListener {
            // Lógica Credential Manager / Google
        }

        btnFacebook.setOnClickListener {
            // Lógica Meta Login SDK
        }

        btnCorreo.setOnClickListener {
            // Abrir pantalla de correo y contraseña
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
    }
}