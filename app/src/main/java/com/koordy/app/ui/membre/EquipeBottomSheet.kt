package com.koordy.app.ui.membre

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.BottomSheetEquipeBinding
import com.koordy.app.databinding.ItemMembreEquipeBinding
import com.koordy.app.models.ConseilMembre
import com.koordy.app.utils.Constants
import kotlinx.coroutines.launch

class EquipeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEquipeBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_ID_EQUIPE = "id_equipe"
        private const val ARG_NOM_EQUIPE = "nom_equipe"

        fun newInstance(idEquipe: Int, nomEquipe: String): EquipeBottomSheet {
            return EquipeBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID_EQUIPE, idEquipe)
                    putString(ARG_NOM_EQUIPE, nomEquipe)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetEquipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val idEquipe = arguments?.getInt(ARG_ID_EQUIPE) ?: 0
        val nomEquipe = arguments?.getString(ARG_NOM_EQUIPE) ?: ""

        binding.tvSheetEquipeNom.text = nomEquipe
        binding.rvSheetMembres.layoutManager = LinearLayoutManager(requireContext())

        if (idEquipe == 0) {
            binding.tvSheetEmpty.text = "Équipe introuvable. Redémarrez le serveur."
            showEmpty()
            return
        }

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getEquipeMembres(idEquipe)
                if (res.isSuccessful) {
                    val membres = res.body() ?: emptyList()
                    if (membres.isEmpty()) {
                        showEmpty()
                    } else {
                        binding.tvSheetMembresCount.text = "${membres.size} membre${if (membres.size > 1) "s" else ""}"
                        binding.rvSheetMembres.adapter = MembreEquipeAdapter(membres)
                        binding.rvSheetMembres.visibility = View.VISIBLE
                        binding.tvSheetEmpty.visibility = View.GONE
                    }
                } else {
                    showEmpty()
                }
            } catch (e: Exception) {
                showEmpty()
                Toast.makeText(requireContext(), "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmpty() {
        binding.tvSheetEmpty.visibility = View.VISIBLE
        binding.rvSheetMembres.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class MembreEquipeAdapter(
        private val items: List<ConseilMembre>
    ) : RecyclerView.Adapter<MembreEquipeAdapter.VH>() {

        inner class VH(val b: ItemMembreEquipeBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemMembreEquipeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = items[position]
            holder.b.tvNom.text = "${m.prenom} ${m.nom}"
            if (m.photoMembre.isNotEmpty()) {
                holder.b.ivAvatar.visibility = View.VISIBLE
                holder.b.tvInitiale.visibility = View.GONE
                Glide.with(holder.b.root.context)
                    .load("${Constants.BASE_URL.trimEnd('/')}${m.photoMembre}")
                    .transform(CircleCrop())
                    .into(holder.b.ivAvatar)
            } else {
                holder.b.ivAvatar.visibility = View.GONE
                holder.b.tvInitiale.visibility = View.VISIBLE
                holder.b.tvInitiale.text = m.prenom.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            }
        }

        override fun getItemCount() = items.size
    }
}
