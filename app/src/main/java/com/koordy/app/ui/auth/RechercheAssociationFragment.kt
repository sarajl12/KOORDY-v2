package com.koordy.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.koordy.app.MainActivity
import com.koordy.app.R
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentRechercheAssociationBinding
import com.koordy.app.models.Association
import com.koordy.app.ui.association.AssociationSearchAdapter
import kotlinx.coroutines.launch

class RechercheAssociationFragment : Fragment() {

    private var _binding: FragmentRechercheAssociationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRechercheAssociationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = AssociationSearchAdapter { asso ->
            val session = (activity as MainActivity).session
            val idMembre = session.idMembre
            binding.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.rejoindreAssociation(
                        asso.idAssociation,
                        com.koordy.app.models.JoinAssociationRequest(idMembre)
                    )
                    if (response.isSuccessful) {
                        session.idAssociation = asso.idAssociation
                        findNavController().navigate(R.id.action_rechercheAssociation_to_homeAssociation)
                    } else {
                        Toast.makeText(requireContext(), "Impossible de rejoindre l'association.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Erreur réseau.", Toast.LENGTH_SHORT).show()
                } finally {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        binding.recyclerResults.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerResults.adapter = adapter

        binding.btnRechercher.setOnClickListener {
            val nom = binding.etSearch.text.toString().trim()
            if (nom.isEmpty()) {
                Toast.makeText(requireContext(), "Saisis un nom d'association.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            doSearch(nom, adapter)
        }
    }

    private fun doSearch(nom: String, adapter: AssociationSearchAdapter) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.searchAssociation(nom)
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    adapter.submitList(list)
                    if (list.isEmpty()) {
                        Toast.makeText(requireContext(), "Aucune association trouvée.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
