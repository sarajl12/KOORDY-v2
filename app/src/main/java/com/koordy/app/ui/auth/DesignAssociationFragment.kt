package com.koordy.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.koordy.app.MainActivity
import com.koordy.app.R
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentDesignAssociationBinding
import com.koordy.app.models.DesignRequest
import kotlinx.coroutines.launch

class DesignAssociationFragment : Fragment() {

    private var _binding: FragmentDesignAssociationBinding? = null
    private val binding get() = _binding!!

    // Couleurs sélectionnées
    private var couleur1 = "#6CCFFF"
    private var couleur2 = "#FFFFFF"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDesignAssociationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Affiche la couleur par défaut
        updateColorPreview()

        binding.etCouleur1.setText(couleur1)
        binding.etCouleur2.setText(couleur2)

        // Mise à jour en temps réel
        binding.etCouleur1.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                couleur1 = binding.etCouleur1.text.toString().trim()
                updateColorPreview()
            }
        }
        binding.etCouleur2.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                couleur2 = binding.etCouleur2.text.toString().trim()
                updateColorPreview()
            }
        }

        binding.btnSuivant.setOnClickListener { submitDesign() }
        binding.tvRetour.setOnClickListener { findNavController().popBackStack() }
    }

    private fun updateColorPreview() {
        try {
            binding.previewCouleur1.setBackgroundColor(
                android.graphics.Color.parseColor(couleur1)
            )
            binding.previewCouleur2.setBackgroundColor(
                android.graphics.Color.parseColor(couleur2)
            )
        } catch (e: Exception) { /* Couleur invalide, on ignore */ }
    }

    private fun submitDesign() {
        couleur1 = binding.etCouleur1.text.toString().trim()
        couleur2 = binding.etCouleur2.text.toString().trim()

        val session = (activity as MainActivity).session
        val idAsso = session.idAssociation
        if (idAsso == -1) {
            Toast.makeText(requireContext(), "Association introuvable.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSuivant.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.updateDesign(
                    idAsso,
                    DesignRequest(couleur1, couleur2, "")
                )
                if (response.isSuccessful) {
                    findNavController().navigate(R.id.action_designAssociation_to_successAssociation)
                } else {
                    Toast.makeText(requireContext(), "Erreur design.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSuivant.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
