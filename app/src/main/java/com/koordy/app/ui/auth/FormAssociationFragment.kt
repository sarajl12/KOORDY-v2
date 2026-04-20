package com.koordy.app.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.koordy.app.MainActivity
import com.koordy.app.R
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentFormAssociationBinding
import com.koordy.app.models.AssociationRequest
import kotlinx.coroutines.launch

class FormAssociationFragment : Fragment() {

    private var _binding: FragmentFormAssociationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormAssociationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialiser le spinner type de structure
        val types = resources.getStringArray(R.array.type_structure_array)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = spinnerAdapter

        binding.etDateCreation.addTextChangedListener(dateSlashWatcher())

        binding.btnSuivant.setOnClickListener { submitForm() }
        binding.tvRetour.setOnClickListener { findNavController().popBackStack() }
    }

    private fun submitForm() {
        val nom = binding.etNom.text.toString().trim()
        val typeStructure = binding.spinnerType.selectedItem.toString()
        val adresse = binding.etAdresse.text.toString().trim()
        val codePostal = binding.etCodePostal.text.toString().trim()
        val ville = binding.etVille.text.toString().trim()
        val pays = binding.etPays.text.toString().trim()
        val dateCreationRaw = binding.etDateCreation.text.toString().trim()
        val dateCreation = parseDate(dateCreationRaw)

        if (nom.isEmpty() || adresse.isEmpty() || codePostal.isEmpty()
            || ville.isEmpty() || pays.isEmpty() || dateCreation.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Remplis tous les champs obligatoires.", Toast.LENGTH_SHORT).show()
            return
        }

        val session = (activity as MainActivity).session
        val idMembre = session.idMembre
        if (idMembre == -1) {
            Toast.makeText(requireContext(), "Session expirée. Reconnecte-toi.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSuivant.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.createAssociation(
                    AssociationRequest(
                        idMembre = idMembre,
                        nom = nom,
                        typeStructure = typeStructure,
                        sport = "",
                        adresse = adresse,
                        adresse2 = binding.etAdresse2.text.toString().trim(),
                        description = binding.etDescription.text.toString().trim(),
                        dateCreation = dateCreation,
                        codePostal = codePostal,
                        ville = ville,
                        pays = pays
                    )
                )
                if (response.isSuccessful) {
                    val idAsso = response.body()?.idAssociation ?: -1
                    session.idAssociation = idAsso
                    findNavController().navigate(R.id.action_formAssociation_to_successAssociation)
                } else {
                    Toast.makeText(requireContext(), "Erreur : ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSuivant.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun dateSlashWatcher() = object : TextWatcher {
        private var previousLength = 0
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { previousLength = s?.length ?: 0 }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (s == null || s.length < previousLength) return
            if (s.length == 2 && !s.contains('/')) s.append("/")
            if (s.length == 5 && s.last() != '/') s.append("/")
        }
    }

    private fun parseDate(raw: String): String = when {
        raw.length == 10 && raw.contains("/") -> {
            val p = raw.split("/")
            "${p[2]}-${p[1]}-${p[0]}"
        }
        raw.length == 8 && !raw.contains("/") -> {
            "${raw.substring(4, 8)}-${raw.substring(2, 4)}-${raw.substring(0, 2)}"
        }
        else -> raw
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
