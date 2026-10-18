package com.app.nutrimeta.alimentos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.nutrimeta.R
import com.app.nutrimeta.data.local.AlimentoDb

class AlimentoAdapter(
    private val alAbrir: (AlimentoDb) -> Unit
) : ListAdapter<AlimentoDb, AlimentoAdapter.Holder>(ComparadorAlimento) {

    class Holder(vista: View) : RecyclerView.ViewHolder(vista) {
        val tvNombre: TextView = vista.findViewById(R.id.tvNombreAlimento)
        val tvDetalle: TextView = vista.findViewById(R.id.tvDetalleAlimento)
        val tvEnergia: TextView = vista.findViewById(R.id.tvEnergiaAlimento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alimento, parent, false)

        return Holder(vista)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val alimento = getItem(position)
        val contexto = holder.itemView.context

        holder.tvNombre.text = alimento.nombre

        holder.tvDetalle.text = contexto.getString(
            R.string.ali_item_detalle,
            alimento.grupo,
            alimento.unidad
        )

        holder.tvEnergia.text = contexto.getString(
            R.string.ali_kcal,
            alimento.energiaKcal
        )

        holder.itemView.setOnClickListener {
            alAbrir(alimento)
        }
    }
}

private object ComparadorAlimento : DiffUtil.ItemCallback<AlimentoDb>() {

    override fun areItemsTheSame(anterior: AlimentoDb, nuevo: AlimentoDb): Boolean {
        return anterior.id == nuevo.id
    }

    override fun areContentsTheSame(anterior: AlimentoDb, nuevo: AlimentoDb): Boolean {
        return anterior == nuevo
    }
}
