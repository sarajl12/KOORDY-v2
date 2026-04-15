package com.koordy.app.ui.association

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koordy.app.databinding.ItemAssociationSearchBinding
import com.koordy.app.models.Association

class AssociationSearchAdapter(
    private val onClick: (Association) -> Unit
) : ListAdapter<Association, AssociationSearchAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemAssociationSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(asso: Association) {
            binding.tvNom.text = asso.nom
            binding.tvDetails.text = "${asso.typeStructure} — ${asso.sport} — ${asso.ville}"
            binding.root.setOnClickListener { onClick(asso) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAssociationSearchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Association>() {
            override fun areItemsTheSame(a: Association, b: Association) =
                a.idAssociation == b.idAssociation
            override fun areContentsTheSame(a: Association, b: Association) = a == b
        }
    }
}
