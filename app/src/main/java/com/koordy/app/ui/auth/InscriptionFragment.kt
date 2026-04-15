package com.koordy.app.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.koordy.app.databinding.FragmentInscriptionBinding
import com.koordy.app.models.InscriptionRequest
import kotlinx.coroutines.launch

class InscriptionFragment : Fragment() {

    private var _binding: FragmentInscriptionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Auto-format date JJ/MM/AAAA
        binding.etBirthday.addTextChangedListener(object : TextWatcher {
            private var previousLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s == null) return

                // Si l'utilisateur efface, on ne fait rien
                if (s.length < previousLength) return

                // Ajoute "/" après le jour (position 2)
                if (s.length == 2 && !s.contains('/')) {
                    s.append("/")
                }

                // Ajoute "/" après le mois (position 5)
                if (s.length == 5 && s.last() != '/') {
                    s.append("/")
                }
            }
        })

        binding.btnSuivant.setOnClickListener { doInscription() }
        binding.tvRetour.setOnClickListener { findNavController().popBackStack() }
    }

    private fun doInscription() {
        val nom = binding.etNom.text.toString().trim()
        val prenom = binding.etPrenom.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val passwordConfirm = binding.etPasswordConfirm.text.toString()
        val password = binding.etPassword.text.toString()
        val birthdayRaw = binding.etBirthday.text.toString().trim()

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || birthdayRaw.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Remplis tous les champs obligatoires.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != passwordConfirm) {
            Toast.makeText(requireContext(), "Les mots de passe ne correspondent pas.", Toast.LENGTH_SHORT).show()
            return
        }

        // Convertir vers AAAA-MM-JJ pour le backend PostgreSQL
        val birthday = when {
            // Format JJ/MM/AAAA → AAAA-MM-JJ
            birthdayRaw.length == 10 && birthdayRaw.contains("/") -> {
                val parts = birthdayRaw.split("/")
                "${parts[2]}-${parts[1]}-${parts[0]}"
            }
            // Format JJMMAAAA (sans slashes, 8 chiffres) → AAAA-MM-JJ
            birthdayRaw.length == 8 && !birthdayRaw.contains("/") -> {
                val dd = birthdayRaw.substring(0, 2)
                val mm = birthdayRaw.substring(2, 4)
                val aaaa = birthdayRaw.substring(4, 8)
                "$aaaa-$mm-$dd"
            }
            else -> birthdayRaw
        }

        binding.btnSuivant.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.inscription(
                    InscriptionRequest(nom, prenom, email, password, birthday)
                )
                if (response.isSuccessful) {
                    val data = response.body()!!
                    (activity as MainActivity).session.idMembre = data.id ?: -1
                    findNavController().navigate(R.id.action_inscription_to_inscriptionAssociation)
                } else {
                    val msg = if (response.code() == 409)
                        "Cet email est déjà utilisé."
                    else "Erreur lors de l'inscription."
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
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