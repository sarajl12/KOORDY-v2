package com.koordy.app.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.koordy.app.MainActivity
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.BottomSheetCreateEventBinding
import com.koordy.app.models.EvenementRequest
import com.koordy.app.ui.association.MemberSelectAdapter
import kotlinx.coroutines.launch
import java.util.Calendar

class CreateEventBottomSheet : BottomSheetDialogFragment() {

    private var _b: BottomSheetCreateEventBinding? = null
    private val b get() = _b!!

    private val selectedMemberIds = mutableSetOf<Int>()
    private lateinit var memberAdapter: MemberSelectAdapter

    private var selectedYear  = 0
    private var selectedMonth = 0
    private var selectedDay   = 0
    private var selectedHour  = 0
    private var selectedMin   = 0
    private var dateSet       = false
    private var timeSet       = false

    var onEventCreated: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = BottomSheetCreateEventBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onStart() {
        super.onStart()
        // Forcer le BottomSheet à prendre 90% de la hauteur de l'écran
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        val screenHeight = resources.displayMetrics.heightPixels
        bottomSheet.layoutParams.height = (screenHeight * 0.90).toInt()
        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDateTimePickers()
        setupMemberList()
        setupSelectAll()
        setupCreateButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    // ── Date & Heure ─────────────────────────────────────────────────────────

    private fun setupDateTimePickers() {
        b.tvDatePicker.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedYear  = year
                    selectedMonth = month
                    selectedDay   = day
                    dateSet = true
                    b.tvDatePicker.text = "%02d/%02d/%d".format(day, month + 1, year)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        b.tvTimePicker.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hour, min ->
                    selectedHour = hour
                    selectedMin  = min
                    timeSet = true
                    b.tvTimePicker.text = "%02d:%02d".format(hour, min)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    // ── Liste des membres ─────────────────────────────────────────────────────

    private fun setupMemberList() {
        memberAdapter = MemberSelectAdapter(emptyList(), selectedMemberIds)
        b.recyclerMembers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memberAdapter
            isNestedScrollingEnabled = false
        }

        val session = (activity as MainActivity).session
        val idAsso  = session.idAssociation

        b.progressMembers.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMembres(idAsso)
                if (res.isSuccessful) {
                    val membres = res.body() ?: emptyList()
                    memberAdapter.update(membres)
                    updateSelectedCount()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur chargement membres.", Toast.LENGTH_SHORT).show()
            } finally {
                b.progressMembers.visibility = View.GONE
            }
        }
    }

    // ── Tout sélectionner ─────────────────────────────────────────────────────

    private fun setupSelectAll() {
        b.tvSelectAll.setOnClickListener {
            if (memberAdapter.allSelected()) {
                memberAdapter.deselectAll()
                b.tvSelectAll.text = "Tout sélectionner"
            } else {
                memberAdapter.selectAll()
                b.tvSelectAll.text = "Tout désélectionner"
            }
            updateSelectedCount()
        }
    }

    private fun updateSelectedCount() {
        val count = selectedMemberIds.size
        b.tvSelectedCount.text = when {
            count == 0 -> "Tous les membres invités par défaut"
            count == 1 -> "1 participant sélectionné"
            else       -> "$count participants sélectionnés"
        }
    }

    // ── Créer l'événement ─────────────────────────────────────────────────────

    private fun setupCreateButton() {
        b.btnCreate.setOnClickListener { submitEvent() }
    }

    private fun submitEvent() {
        val titre = b.etTitre.text.toString().trim()
        val lieu  = b.etLieu.text.toString().trim()
        val desc  = b.etDescription.text.toString().trim()

        // Validation
        if (titre.isBlank()) {
            b.etTitre.error = "Le titre est obligatoire"
            return
        }
        if (!dateSet) {
            Toast.makeText(requireContext(), "Veuillez choisir une date.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!timeSet) {
            Toast.makeText(requireContext(), "Veuillez choisir une heure.", Toast.LENGTH_SHORT).show()
            return
        }

        // Type sélectionné
        val type = when (b.chipGroupType.checkedChipId) {
            b.chipMatch.id         -> "Match"
            b.chipEntrainement.id  -> "Entraînement"
            b.chipReunion.id       -> "Réunion"
            b.chipAutre.id         -> "Autre"
            else -> {
                Toast.makeText(requireContext(), "Veuillez choisir un type.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Format ISO 8601
        val dateISO = "%04d-%02d-%02dT%02d:%02d:00.000Z".format(
            selectedYear, selectedMonth + 1, selectedDay, selectedHour, selectedMin
        )

        val session    = (activity as MainActivity).session
        val idAsso     = session.idAssociation
        val idMembre   = session.idMembre
        val participants = if (selectedMemberIds.isEmpty()) null else selectedMemberIds.toList()

        b.btnCreate.isEnabled = false
        b.btnCreate.text = "Création en cours…"

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.createEvenement(
                    EvenementRequest(
                        idAssociation        = idAsso,
                        idAuteur             = idMembre,
                        titreEvenement       = titre,
                        typeEvenement        = type,
                        lieuEvent            = lieu,
                        descriptionEvenement = desc,
                        dateDebutEvent       = dateISO,
                        dateFinEvent         = null,
                        participants         = participants
                    )
                )

                if (res.isSuccessful) {
                    val nb = participants?.size
                    val msg = if (nb == null)
                        "✅ Événement créé ! Tous les membres ont été invités."
                    else
                        "✅ Événement créé ! $nb invitation(s) envoyée(s)."
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    onEventCreated?.invoke()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "❌ Erreur lors de la création.", Toast.LENGTH_SHORT).show()
                    b.btnCreate.isEnabled = true
                    b.btnCreate.text = "Créer l'événement"
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur de connexion.", Toast.LENGTH_SHORT).show()
                b.btnCreate.isEnabled = true
                b.btnCreate.text = "Créer l'événement"
            }
        }
    }

    companion object {
        const val TAG = "CreateEventBottomSheet"
    }
}
