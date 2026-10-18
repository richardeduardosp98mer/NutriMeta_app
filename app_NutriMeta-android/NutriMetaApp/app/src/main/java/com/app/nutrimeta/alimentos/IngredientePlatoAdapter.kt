package com.app.nutrimeta.alimentos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.nutrimeta.R
import com.app.nutrimeta.data.local.IngredientePlato

// Lista de ingredientes del plato. Tocar la fila edita la cantidad;
// la X quita el ingrediente.
class IngredientePlatoAdapter(
    private val alEditar: (Int) -> Unit,
    private val alQuitar: (Int) -> Unit
) : RecyclerView.Adapter<IngredientePlatoAdapter.Vista>() {

    private var lista: List<IngredientePlato> = emptyList()

    class Vista(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreIngrediente)
        val tvDetalle: TextView = itemView.findViewById(R.id.tvDetalleIngrediente)
        val btnQuitar: View = itemView.findViewById(R.id.btnQuitarIngrediente)
    }

    fun actualizar(nueva: List<IngredientePlato>) {
        lista = ArrayList(nueva)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vista {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingrediente_plato, parent, false)

        return Vista(vista)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: Vista, position: Int) {

        val ingrediente = lista[position]
        val contexto = holder.itemView.context

        holder.tvNombre.text = ingrediente.alimento.nombre

        holder.tvDetalle.text = contexto.getString(
            R.string.ali_ingrediente_detalle,
            FormatoCantidad.texto(ingrediente.cantidad),
            ingrediente.alimento.unidad,
            ingrediente.energiaKcal
        )

        holder.itemView.setOnClickListener {
            val posicion = holder.bindingAdapterPosition
            if (posicion != RecyclerView.NO_POSITION) alEditar(posicion)
        }

        holder.btnQuitar.setOnClickListener {
            val posicion = holder.bindingAdapterPosition
            if (posicion != RecyclerView.NO_POSITION) alQuitar(posicion)
        }
    }
}
