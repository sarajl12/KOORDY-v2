package com.koordy.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.koordy.app.MainActivity
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentCalendarBinding
import com.koordy.app.models.EvenementAvecStatut
import com.koordy.app.models.RsvpRequest
import com.koordy.app.ui.association.CalendarEventsAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private var allEvents: List<EvenementAvecStatut> = emptyList()
    private var currentYear  = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedDay  = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private var isAdmin      = false

    private lateinit var calendarAdapter: CalendarDayAdapter
    private lateinit var eventsAdapter: CalendarEventsAdapter

    private val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.FRENCH)

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCalendarGrid()
        setupEventsRecycler()
        setupMonthNavigation()
        setupFab()

        val session  = (activity as MainActivity).session
        val idMembre = session.idMembre
        val idAsso   = session.idAssociation

        checkAdminAndLoadEvents(idAsso, idMembre)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Initialisation des vues ───────────────────────────────────────────────

    private fun setupCalendarGrid() {
        calendarAdapter = CalendarDayAdapter(
            days = emptyList(),
            selectedDay = selectedDay,
            onDayClick = { day ->
                selectedDay = day
                calendarAdapter.setSelectedDay(day)
                updateSelectedDayLabel()
                updateEventsForDay()
            }
        )
        binding.recyclerCalendar.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
        }
    }

    private fun setupEventsRecycler() {
        eventsAdapter = CalendarEventsAdapter(emptyList()) { idEvenement, statut ->
            handleRsvp(idEvenement, statut)
        }
        binding.recyclerEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener { navigateMonth(-1) }
        binding.btnNextMonth.setOnClickListener { navigateMonth(+1) }
    }

    private fun setupFab() {
        binding.fabCreateEvent.setOnClickListener {
            val sheet = CreateEventBottomSheet()
            sheet.onEventCreated = {
                val session = (activity as MainActivity).session
                loadEvents(session.idMembre)
            }
            sheet.show(parentFragmentManager, CreateEventBottomSheet.TAG)
        }

        binding.llPendingBanner.setOnClickListener { navigateToFirstPending() }
    }

    private fun navigateToFirstPending() {
        val firstPending = allEvents
            .filter { it.statut == "En attente" }
            .minByOrNull { it.dateDebutEvent }
            ?: return

        try {
            val parsed = sdfIn.parse(firstPending.dateDebutEvent) ?: return
            val cal = Calendar.getInstance().apply { time = parsed }
            currentYear  = cal.get(Calendar.YEAR)
            currentMonth = cal.get(Calendar.MONTH)
            selectedDay  = cal.get(Calendar.DAY_OF_MONTH)
            refreshCalendar()
        } catch (_: Exception) {}
    }

    // ── Chargement : vérifie admin puis charge les événements ────────────────

    private fun checkAdminAndLoadEvents(idAsso: Int, idMembre: Int) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // 1) Vérifier si admin
            try {
                val adminRes = RetrofitClient.api.isAdmin(idAsso, idMembre)
                if (adminRes.isSuccessful) {
                    isAdmin = adminRes.body()?.isAdmin ?: false
                    binding.fabCreateEvent.visibility =
                        if (isAdmin) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                // Pas bloquant, le FAB reste caché
            }

            // 2) Charger les événements
            loadEvents(idMembre)
        }
    }

    private fun loadEvents(idMembre: Int) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getMembreEvenements(idMembre)
                if (response.isSuccessful) {
                    allEvents = response.body() ?: emptyList()
                    updatePendingBanner()
                } else {
                    Toast.makeText(requireContext(), "Impossible de charger les événements.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur de connexion.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                refreshCalendar()
            }
        }
    }

    // ── Bannière invitations en attente ───────────────────────────────────────

    private fun updatePendingBanner() {
        val pending = allEvents.count { it.statut == "En attente" }
        if (pending > 0) {
            binding.llPendingBanner.visibility = View.VISIBLE
            binding.tvPendingText.text = when (pending) {
                1 -> "📬  1 invitation en attente de réponse"
                else -> "📬  $pending invitations en attente de réponse"
            }
        } else {
            binding.llPendingBanner.visibility = View.GONE
        }
    }

    // ── RSVP ─────────────────────────────────────────────────────────────────

    private fun handleRsvp(idEvenement: Int, statut: String) {
        val idMembre = (activity as MainActivity).session.idMembre

        val statutPrecedent = allEvents.find { it.idEvenement == idEvenement }?.statut
        allEvents = allEvents.map { ev ->
            if (ev.idEvenement == idEvenement) ev.copy(statut = statut) else ev
        }
        val msg = if (statut == "Accepté") "Participation confirmée !" else "Invitation déclinée."
        // Defer RecyclerView update to next frame to avoid IllegalStateException during click dispatch
        binding.root.post {
            if (isAdded) {
                updatePendingBanner()
                refreshCalendar()
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        // Appel API en arrière-plan — rollback silencieux si échec
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.respondRsvp(
                    idEvenement,
                    RsvpRequest(idMembre = idMembre, statut = statut)
                )
                if (!response.isSuccessful && statutPrecedent != null) {
                    allEvents = allEvents.map { ev ->
                        if (ev.idEvenement == idEvenement) ev.copy(statut = statutPrecedent) else ev
                    }
                    if (isAdded) {
                        binding.root.post {
                            if (isAdded) {
                                updatePendingBanner()
                                refreshCalendar()
                                Toast.makeText(requireContext(), "Réponse non enregistrée, réessaie.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // La requête a probablement abouti malgré l'exception — on garde l'état optimiste
            }
        }
    }

    // ── Logique du calendrier ─────────────────────────────────────────────────

    private fun navigateMonth(direction: Int) {
        currentMonth += direction
        if (currentMonth > 11) { currentMonth = 0; currentYear++ }
        if (currentMonth < 0)  { currentMonth = 11; currentYear-- }

        val today = Calendar.getInstance()
        selectedDay = if (currentYear == today.get(Calendar.YEAR)
            && currentMonth == today.get(Calendar.MONTH)) {
            today.get(Calendar.DAY_OF_MONTH)
        } else { 1 }

        refreshCalendar()
    }

    private fun refreshCalendar() {
        val days = buildCalendarDays()
        calendarAdapter.update(days, selectedDay)
        updateMonthLabel()
        updateSelectedDayLabel()
        updateEventsForDay()
    }

    private fun buildCalendarDays(): List<CalendarDay> {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val rawFirst    = cal.get(Calendar.DAY_OF_WEEK)
        val offset      = (rawFirst - Calendar.MONDAY + 7) % 7

        val today  = Calendar.getInstance()
        val todayY = today.get(Calendar.YEAR)
        val todayM = today.get(Calendar.MONTH)
        val todayD = today.get(Calendar.DAY_OF_MONTH)

        val result = mutableListOf<CalendarDay>()
        repeat(offset) { result.add(CalendarDay(0, false, false, emptyList())) }

        for (d in 1..daysInMonth) {
            val isToday = (currentYear == todayY && currentMonth == todayM && d == todayD)
            val statuts = eventsForDay(d).map { it.statut }
            result.add(CalendarDay(d, isCurrentMonth = true, isToday = isToday, eventTypes = statuts))
        }

        while (result.size % 7 != 0) {
            result.add(CalendarDay(0, false, false, emptyList()))
        }

        return result
    }

    // ── Événements du jour ────────────────────────────────────────────────────

    private fun eventsForDay(day: Int): List<EvenementAvecStatut> {
        return allEvents.filter { ev ->
            try {
                val parsed = sdfIn.parse(ev.dateDebutEvent) ?: return@filter false
                val c = Calendar.getInstance().apply { time = parsed }
                c.get(Calendar.YEAR)         == currentYear &&
                c.get(Calendar.MONTH)        == currentMonth &&
                c.get(Calendar.DAY_OF_MONTH) == day
            } catch (e: Exception) { false }
        }
    }

    private fun updateEventsForDay() {
        val events = eventsForDay(selectedDay)
        eventsAdapter.update(events)

        binding.tvEmpty.visibility =
            if (events.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerEvents.visibility =
            if (events.isEmpty()) View.GONE else View.VISIBLE
    }

    // ── Labels ────────────────────────────────────────────────────────────────

    private fun updateMonthLabel() {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.FRENCH)
        binding.tvMonthLabel.text = sdf.format(cal.time).replaceFirstChar { it.uppercase() }
    }

    private fun updateSelectedDayLabel() {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, selectedDay)
        val sdf = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
        binding.tvSelectedDay.text = sdf.format(cal.time).replaceFirstChar { it.uppercase() }
    }
}
