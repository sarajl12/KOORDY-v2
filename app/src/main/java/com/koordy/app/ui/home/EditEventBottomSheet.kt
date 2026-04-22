package com.koordy.app.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.BottomSheetEditEventBinding
import com.koordy.app.models.EvenementAvecStatut
import com.koordy.app.models.EvenementUpdateRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class EditEventBottomSheet : BottomSheetDialogFragment() {

    private var _b: BottomSheetEditEventBinding? = null
    private val b get() = _b!!

    var onEventUpdated: (() -> Unit)? = null

    private var selectedYear  = 0
    private var selectedMonth = 0
    private var selectedDay   = 0
    private var selectedHour  = 0
    private var selectedMin   = 0

    companion object {
        const val TAG = "edit_event"
        private const val ARG_EVENT = "event"

        fun newInstance(ev: EvenementAvecStatut): EditEventBottomSheet {
            return EditEventBottomSheet().apply {
                arguments = Bundle().apply { putSerializable(ARG_EVENT, ev) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getEvent(): EvenementAvecStatut =
        requireArguments().getSerializable(ARG_EVENT) as EvenementAvecStatut

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = BottomSheetEditEventBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        sheet.layoutParams.height = (resources.displayMetrics.heightPixels * 0.90).toInt()
        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ev = getEvent()
        prefill(ev)
        setupDatePicker()
        setupTimePicker()
        b.btnSaveEdit.setOnClickListener { submitUpdate(ev.idEvenement) }
    }

    private fun prefill(ev: EvenementAvecStatut) {
        b.etEditTitre.setText(ev.titreEvenement)
        b.etEditLieu.setText(ev.lieuEvent)
        b.etEditDescription.setText(ev.descriptionEvenement)

        when (ev.typeEvenement) {
            "Match"         -> b.chipGroupEditType.check(b.chipEditMatch.id)
            "Entraînement"  -> b.chipGroupEditType.check(b.chipEditEntrainement.id)
            "Réunion"       -> b.chipGroupEditType.check(b.chipEditReunion.id)
            else            -> b.chipGroupEditType.check(b.chipEditAutre.id)
        }

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.FRENCH)
                .also { it.timeZone = TimeZone.getTimeZone("UTC") }
            val cal = Calendar.getInstance().also { it.time = sdf.parse(ev.dateDebutEvent)!! }
            selectedYear  = cal.get(Calendar.YEAR)
            selectedMonth = cal.get(Calendar.MONTH)
            selectedDay   = cal.get(Calendar.DAY_OF_MONTH)
            selectedHour  = cal.get(Calendar.HOUR_OF_DAY)
            selectedMin   = cal.get(Calendar.MINUTE)
            b.tvEditDate.text  = "%02d/%02d/%d".format(selectedDay, selectedMonth + 1, selectedYear)
            b.tvEditDate.setTextColor(requireContext().getColor(android.R.color.black))
            b.tvEditHeure.text = "%02d:%02d".format(selectedHour, selectedMin)
            b.tvEditHeure.setTextColor(requireContext().getColor(android.R.color.black))
        } catch (_: Exception) {}
    }

    private fun setupDatePicker() {
        b.tvEditDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedYear = year; selectedMonth = month; selectedDay = day
                    b.tvEditDate.text = "%02d/%02d/%d".format(day, month + 1, year)
                    b.tvEditDate.setTextColor(requireContext().getColor(android.R.color.black))
                },
                selectedYear, selectedMonth, selectedDay
            ).show()
        }
    }

    private fun setupTimePicker() {
        b.tvEditHeure.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, min ->
                    selectedHour = hour; selectedMin = min
                    b.tvEditHeure.text = "%02d:%02d".format(hour, min)
                    b.tvEditHeure.setTextColor(requireContext().getColor(android.R.color.black))
                },
                selectedHour, selectedMin, true
            ).show()
        }
    }

    private fun submitUpdate(idEvenement: Int) {
        val titre = b.etEditTitre.text.toString().trim()
        if (titre.isBlank()) { b.etEditTitre.error = "Titre requis"; return }

        val type = when (b.chipGroupEditType.checkedChipId) {
            b.chipEditMatch.id        -> "Match"
            b.chipEditEntrainement.id -> "Entraînement"
            b.chipEditReunion.id      -> "Réunion"
            b.chipEditAutre.id        -> "Autre"
            else -> { Toast.makeText(requireContext(), "Choisissez un type.", Toast.LENGTH_SHORT).show(); return }
        }

        val dateISO = "%04d-%02d-%02dT%02d:%02d:00.000Z".format(
            selectedYear, selectedMonth + 1, selectedDay, selectedHour, selectedMin
        )

        b.btnSaveEdit.isEnabled = false
        b.btnSaveEdit.text = "Enregistrement…"

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.updateEvenement(
                    idEvenement,
                    EvenementUpdateRequest(
                        titreEvenement       = titre,
                        typeEvenement        = type,
                        lieuEvent            = b.etEditLieu.text.toString().trim(),
                        descriptionEvenement = b.etEditDescription.text.toString().trim(),
                        dateDebutEvent       = dateISO,
                        dateFinEvent         = null
                    )
                )
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Événement modifié ✓", Toast.LENGTH_SHORT).show()
                    onEventUpdated?.invoke()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Erreur lors de la modification.", Toast.LENGTH_SHORT).show()
                    b.btnSaveEdit.isEnabled = true
                    b.btnSaveEdit.text = "Enregistrer les modifications"
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau.", Toast.LENGTH_SHORT).show()
                b.btnSaveEdit.isEnabled = true
                b.btnSaveEdit.text = "Enregistrer les modifications"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
