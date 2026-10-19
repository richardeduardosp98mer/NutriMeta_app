package com.app.nutrimeta.alimentos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.nutrimeta.R
import com.app.nutrimeta.data.local.ResumenPlato

// Lista de platos guardados. Tocar la fila o "Editar" abre el plato;
// "Eliminar" pide confirmacion en la pantalla.
class PlatoHistorialAdapter(
    private val alEditar: (ResumenPlato) -> Unit,
    private val alEliminar: (ResumenPlato) -> Unit
) : RecyclerView.Adapter<PlatoHistorialAdapter.Vista>() {

    private var lista: List<ResumenPlato> = emptyList()

    class Vista(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloPlatoHistorial)
        val tvIngredientes: TextView = itemView.findViewById(R.id.tvIngredientesPlatoHistorial)
        val btnEditar: View = itemView.findViewById(R.id.btnEditarPlatoHistorial)
        val btnEliminar: View = itemView.findViewById(R.id.btnEliminarPlatoHistorial)
    }

    fun actualizar(nueva: List<ResumenPlato>) {
        lista = ArrayList(nueva)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vista {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plato_historial, parent, false)

        return Vista(vista)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: Vista, position: Int) {

        val plato = lista[position]
        val contexto = holder.itemView.context

        holder.tvTitulo.text = contexto.getString(
            R.string.hist_plato_titulo,
            position + 1,
            plato.kcal
        )

        holder.tvIngredientes.text = plato.nombresIngredientes.joinToString(", ")

        val editar = View.OnClickListener {
            val posicion = holder.bindingAdapterPosition
            if (posicion != RecyclerView.NO_POSITION) alEditar(lista[posicion])
        }

        holder.itemView.setOnClickListener(editar)
        holder.btnEditar.setOnClickListener(editar)

        holder.btnEliminar.setOnClickListener {
            val posicion = holder.bindingAdapterPosition
            if (posicion != RecyclerView.NO_POSITION) alEliminar(lista[posicion])
        }
    }
}
