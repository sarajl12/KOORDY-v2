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
import com.koordy.app.databinding.BottomSheetEditEquipeBinding
import com.koordy.app.databinding.ItemMembreSelectBinding
import com.koordy.app.models.ConseilMembre
import com.koordy.app.models.UpdateEquipeMembresRequest
import kotlinx.coroutines.launch

class EditEquipeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditEquipeBinding? = null
    private val binding get() = _binding!!

    private var idEquipe: Int = 0
    private var idAssociation: Int = 0
    private var allMembres: List<ConseilMembre> = emptyList()
    private val selectedIds = mutableSetOf<Int>()

    companion object {
        private const val ARG_ID_EQUIPE = "id_equipe"
        private const val ARG_NOM_EQUIPE = "nom_equipe"
        private const val ARG_ID_ASSO = "id_association"
        private const val ARG_MEMBRES = "membres_ids"
        private const val ARG_MEMBRES_NOMS = "membres_noms"
        private const val ARG_MEMBRES_PRENOMS = "membres_prenoms"

        fun newInstance(
            idEquipe: Int,
            nomEquipe: String,
            idAssociation: Int,
            membres: List<ConseilMembre>
        ): EditEquipeBottomSheet {
            return EditEquipeBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID_EQUIPE, idEquipe)
                    putString(ARG_NOM_EQUIPE, nomEquipe)
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
        _binding = BottomSheetEditEquipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        idEquipe = arguments?.getInt(ARG_ID_EQUIPE) ?: 0
        idAssociation = arguments?.getInt(ARG_ID_ASSO) ?: 0
        val nomEquipe = arguments?.getString(ARG_NOM_EQUIPE) ?: "Équipe"
        val ids = arguments?.getIntArray(ARG_MEMBRES) ?: intArrayOf()
        val noms = arguments?.getStringArray(ARG_MEMBRES_NOMS) ?: arrayOf()
        val prenoms = arguments?.getStringArray(ARG_MEMBRES_PRENOMS) ?: arrayOf()

        allMembres = ids.mapIndexed { i, id ->
            ConseilMembre(
                idMembre = id,
                nom = noms.getOrElse(i) { "" },
                prenom = prenoms.getOrElse(i) { "" }
            )
        }

        binding.tvTitreEquipe.text = "Modifier : $nomEquipe"
        binding.btnSauvegarder.setOnClickListener { saveChanges() }

        loadCurrentMembres()
    }

    private fun loadCurrentMembres() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getEquipeMembres(idEquipe)
                if (res.isSuccessful) {
                    val currentMembres = res.body() ?: emptyList()
                    selectedIds.clear()
                    selectedIds.addAll(currentMembres.map { it.idMembre })
                } else {
                    // Si l'endpoint n'existe pas encore, on commence avec liste vide
                    selectedIds.clear()
                }
            } catch (_: Exception) {
                selectedIds.clear()
            } finally {
                showMembresUI()
            }
        }
    }

    private fun showMembresUI() {
        binding.progressLoading.visibility = View.GONE
        binding.rvMembresEquipe.visibility = View.VISIBLE
        binding.rvMembresEquipe.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMembresEquipe.adapter = MembreSelectAdapter(allMembres, selectedIds)
    }

    private fun saveChanges() {
        lifecycleScope.launch {
            try {
                binding.btnSauvegarder.isEnabled = false
                binding.btnSauvegarder.text = "Sauvegarde…"

                val req = UpdateEquipeMembresRequest(
                    idAssociation = idAssociation,
                    membres = selectedIds.toList()
                )
                val res = RetrofitClient.api.updateEquipeMembres(idEquipe, req)
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Équipe mise à jour !", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.setFragmentResult("equipe_updated", Bundle())
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Erreur lors de la sauvegarde.", Toast.LENGTH_SHORT).show()
                    binding.btnSauvegarder.isEnabled = true
                    binding.btnSauvegarder.text = "Sauvegarder"
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau.", Toast.LENGTH_SHORT).show()
                binding.btnSauvegarder.isEnabled = true
                binding.btnSauvegarder.text = "Sauvegarder"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

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
