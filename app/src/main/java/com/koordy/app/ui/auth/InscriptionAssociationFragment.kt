package com.koordy.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.koordy.app.R
import com.koordy.app.databinding.FragmentInscriptionAssociationBinding

class InscriptionAssociationFragment : Fragment() {

    private var _binding: FragmentInscriptionAssociationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInscriptionAssociationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Créer une nouvelle association
        binding.cardCreer.setOnClickListener {
            findNavController().navigate(R.id.action_inscriptionAssociation_to_formAssociation)
        }

        // Rejoindre une association existante
        binding.cardRechercher.setOnClickListener {
            findNavController().navigate(R.id.action_inscriptionAssociation_to_rechercheAssociation)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
