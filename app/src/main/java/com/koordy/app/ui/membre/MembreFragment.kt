package com.koordy.app.ui.membre

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.koordy.app.MainActivity
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentMembreBinding
import com.koordy.app.models.MembreUpdateRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MembreFragment : Fragment() {

    private var _binding: FragmentMembreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = (activity as MainActivity).session
        val id = session.idMembre

        loadProfil(id)
        loadAssociation(id)
        loadEquipes(id)
        loadPresences(id)

        binding.btnEditProfile.setOnClickListener {
            binding.layoutEdit.visibility = View.VISIBLE
            binding.btnEditProfile.visibility = View.GONE
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile(id)
        }

        binding.btnCancelEdit.setOnClickListener {
            binding.layoutEdit.visibility = View.GONE
            binding.btnEditProfile.visibility = View.VISIBLE
        }

        binding.btnLogout.setOnClickListener {
            session.clear()
            requireActivity().finish()
            requireActivity().startActivity(requireActivity().intent)
        }
    }

    private fun loadProfil(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMembre(id)
                if (res.isSuccessful) {
                    val m = res.body()!!

                    // Format date
                    var formattedDate = "—"
                    var age = "—"
                    if (m.dateNaissance.isNotEmpty()) {
                        try {
                            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.FRENCH)
                            val d = sdfIn.parse(m.dateNaissance.take(10))
                            val sdfOut = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
                            formattedDate = sdfOut.format(d!!)
                            age = (java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) -
                                    sdfIn.parse(m.dateNaissance.take(10))!!.let {
                                        val cal = java.util.Calendar.getInstance()
                                        cal.time = it
                                        cal.get(java.util.Calendar.YEAR)
                                    }).toString()
                        } catch (e: Exception) { }
                    }

                    binding.tvNom.text = "Nom : ${m.nomMembre}"
                    binding.tvPrenom.text = "Prénom : ${m.prenomMembre}"
                    binding.tvEmail.text = "Email : ${m.mailMembre}"
                    binding.tvNaissance.text = "Naissance : $formattedDate"
                    binding.tvAge.text = "Âge : $age ans"
                    binding.tvRole.text = "Rôle : ${m.roleAsso.ifEmpty { "Membre" }}"
                    binding.tvAdhesion.text = if (m.dateAdhesion.isNotEmpty()) {
                        "Adhésion : ${m.dateAdhesion.take(10)}"
                    } else "Adhésion : —"

                    // Pré-remplir le formulaire d'édition
                    binding.etEditNom.setText(m.nomMembre)
                    binding.etEditPrenom.setText(m.prenomMembre)
                    binding.etEditEmail.setText(m.mailMembre)
                    binding.etEditBirth.setText(m.dateNaissance.take(10))
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur chargement profil.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAssociation(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMembreAssociation(id)
                if (res.isSuccessful) {
                    val a = res.body()
                    if (a != null && a.nom.isNotEmpty()) {
                        binding.tvAssoNom.text = "Nom : ${a.nom}"
                        binding.tvAssoSport.text = "Sport : ${a.sport}"
                        binding.tvAssoVille.text = "Ville : ${a.ville}"
                    } else {
                        binding.tvAssoNom.text = "Aucune association."
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun loadEquipes(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMembreEquipes(id)
                if (res.isSuccessful) {
                    val equipes = res.body() ?: emptyList()
                    if (equipes.isNotEmpty()) {
                        binding.tvEquipe.text = "Équipe : ${equipes[0].nomEquipe} — ${equipes[0].role}"
                    } else {
                        binding.tvEquipe.text = "Aucune équipe."
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun loadPresences(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMembrePresences(id)
                if (res.isSuccessful) {
                    val presences = res.body() ?: emptyList()
                    if (presences.isEmpty()) {
                        binding.tvPresences.text = "Aucune activité."
                        return@launch
                    }
                    val presents = presences.count { it.statut == "présent" }
                    val taux = (presents * 100) / presences.size
                    val sb = StringBuilder("Taux de présence : $taux%\n\n")
                    presences.forEach { p ->
                        sb.append("• ${p.nomActivite} — ${p.statut} — ${p.datePresence.take(10)}\n")
                    }
                    binding.tvPresences.text = sb.toString()
                }
            } catch (e: Exception) { }
        }
    }

    private fun saveProfile(id: Int) {
        val nom = binding.etEditNom.text.toString().trim()
        val prenom = binding.etEditPrenom.text.toString().trim()
        val email = binding.etEditEmail.text.toString().trim()
        val birthday = binding.etEditBirth.text.toString().trim()

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.updateMembre(
                    id, MembreUpdateRequest(nom, prenom, email, birthday)
                )
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "✔ Profil mis à jour !", Toast.LENGTH_SHORT).show()
                    binding.layoutEdit.visibility = View.GONE
                    binding.btnEditProfile.visibility = View.VISIBLE
                    loadProfil(id)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur mise à jour.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
