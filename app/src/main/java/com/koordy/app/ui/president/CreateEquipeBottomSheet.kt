package com.koordy.app.ui.president

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.BottomSheetCreateEquipeBinding
import com.koordy.app.databinding.ItemMembreSelectBinding
import com.koordy.app.models.ConseilMembre
import com.koordy.app.models.CreateEquipeRequest
import kotlinx.coroutines.launch

class CreateEquipeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateEquipeBinding? = null
    private val binding get() = _binding!!

    private var idAssociation: Int = 0
    private var membres: List<ConseilMembre> = emptyList()
    private val selectedIds = mutableSetOf<Int>()

    companion object {
        private const val ARG_ID_ASSO = "id_association"
        private const val ARG_MEMBRES = "membres_ids"
        private const val ARG_MEMBRES_NOMS = "membres_noms"
        private const val ARG_MEMBRES_PRENOMS = "membres_prenoms"

        fun newInstance(idAssociation: Int, membres: List<ConseilMembre>): CreateEquipeBottomSheet {
            return CreateEquipeBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID_ASSO, idAssociation)
                    putIntArray(ARG_MEMBRES, membres.map { it.idMembre }.toIntArray())
                    putStringArray(ARG_MEMBRES_NOMS, membres.map { it.nom }.toTypedArray())
                    putStringArray(ARG_MEMBRES_PRENOMS, membres.map { it.prenom }.toTypedArray())
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCreateEquipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        idAssociation = arguments?.getInt(ARG_ID_ASSO) ?: 0
        val ids = arguments?.getIntArray(ARG_MEMBRES) ?: intArrayOf()
        val noms = arguments?.getStringArray(ARG_MEMBRES_NOMS) ?: arrayOf()
        val prenoms = arguments?.getStringArray(ARG_MEMBRES_PRENOMS) ?: arrayOf()

        membres = ids.mapIndexed { i, id ->
            ConseilMembre(
                idMembre = id,
                nom = noms.getOrElse(i) { "" },
                prenom = prenoms.getOrElse(i) { "" }
            )
        }

        binding.rvMembresSelect.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMembresSelect.adapter = MembreSelectAdapter(membres, selectedIds)

        binding.btnCreate.setOnClickListener { createEquipe() }
    }

    private fun createEquipe() {
        val nom = binding.etNomEquipe.text.toString().trim()
        if (nom.isEmpty()) {
            binding.etNomEquipe.error = "Nom requis"
            return
        }

        val description = binding.etDescription.text.toString().trim()

        lifecycleScope.launch {
            try {
                binding.btnCreate.isEnabled = false
                binding.btnCreate.text = "Création…"

                val req = CreateEquipeRequest(
                    idAssociation = idAssociation,
                    nomEquipe = nom,
                    description = description,
                    membres = selectedIds.toList()
                )
                val res = RetrofitClient.api.createEquipe(req)
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Équipe \"$nom\" créée !", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.setFragmentResult("equipe_updated", Bundle())
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Erreur lors de la création.", Toast.LENGTH_SHORT).show()
                    binding.btnCreate.isEnabled = true
                    binding.btnCreate.text = "Créer l'équipe"
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau.", Toast.LENGTH_SHORT).show()
                binding.btnCreate.isEnabled = true
                binding.btnCreate.text = "Créer l'équipe"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Adapter sélection membres ─────────────────────────────────────────────

    inner class MembreSelectAdapter(
        private val items: List<ConseilMembre>,
        private val selected: MutableSet<Int>
    ) : RecyclerView.Adapter<MembreSelectAdapter.VH>() {

        inner class VH(val b: ItemMembreSelectBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemMembreSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = items[position]
            holder.b.tvInitiale.text = m.prenom.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            holder.b.tvNom.text = "${m.prenom} ${m.nom}"
            holder.b.cbSelect.isChecked = m.idMembre in selected

            holder.b.root.setOnClickListener {
                if (m.idMembre in selected) selected.remove(m.idMembre)
                else selected.add(m.idMembre)
                holder.b.cbSelect.isChecked = m.idMembre in selected
            }
        }

        override fun getItemCount() = items.size
    }
}
