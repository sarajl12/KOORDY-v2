package com.koordy.app.ui.president

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.koordy.app.databinding.BottomSheetMembreDetailBinding

class MembreDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMembreDetailBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_NOM = "nom"
        private const val ARG_PRENOM = "prenom"
        private const val ARG_ROLE = "role"
        private const val ARG_EMAIL = "email"
        private const val ARG_AGE = "age"
        private const val ARG_DATE = "date_adhesion"

        fun newInstance(
            nom: String,
            prenom: String,
            role: String,
            email: String,
            age: Int,
            dateAdhesion: String
        ): MembreDetailBottomSheet {
            return MembreDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_NOM, nom)
                    putString(ARG_PRENOM, prenom)
                    putString(ARG_ROLE, role)
                    putString(ARG_EMAIL, email)
                    putInt(ARG_AGE, age)
                    putString(ARG_DATE, dateAdhesion)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMembreDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nom = arguments?.getString(ARG_NOM) ?: ""
        val prenom = arguments?.getString(ARG_PRENOM) ?: ""
        val role = arguments?.getString(ARG_ROLE) ?: "Membre"
        val email = arguments?.getString(ARG_EMAIL) ?: ""
        val age = arguments?.getInt(ARG_AGE) ?: 0
        val dateAdhesion = arguments?.getString(ARG_DATE) ?: ""

        binding.tvDetailInitiale.text = prenom.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        binding.tvDetailNom.text = "$prenom $nom"
        binding.tvDetailRole.text = role.ifEmpty { "Membre" }
        binding.tvDetailEmail.text = email.ifEmpty { "—" }
        binding.tvDetailAge.text = if (age > 0) "$age ans" else "—"
        binding.tvDetailDate.text = formatDate(dateAdhesion)
    }

    private fun formatDate(raw: String): String {
        if (raw.isBlank()) return "—"
        return try {
            // Format BDD: "2026-04-15" → "15 avril 2026"
            val parts = raw.take(10).split("-")
            if (parts.size < 3) return raw.take(10)
            val months = listOf(
                "", "janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre"
            )
            val month = parts[1].toIntOrNull() ?: 0
            "${parts[2]} ${months.getOrElse(month) { parts[1] }} ${parts[0]}"
        } catch (_: Exception) { raw.take(10) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
