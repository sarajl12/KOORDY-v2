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
import com.koordy.app.models.Evenement
import com.koordy.app.ui.association.EventsAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    // État du calendrier
    private var allEvents: List<Evenement> = emptyList()
    private var currentYear  = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedDay  = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    private lateinit var calendarAdapter: CalendarDayAdapter
    private lateinit var eventsAdapter: EventsAdapter

    // Parseur de date ISO utilisé dans tout le projet
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

        val idAsso = (activity as MainActivity).session.idAssociation
        loadEvents(idAsso)
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
        eventsAdapter = EventsAdapter(emptyList())
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

    // ── Chargement des données ────────────────────────────────────────────────

    private fun loadEvents(idAsso: Int) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getEvents(idAsso)
                if (response.isSuccessful) {
                    allEvents = response.body() ?: emptyList()
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

    // ── Logique du calendrier ─────────────────────────────────────────────────

    private fun navigateMonth(direction: Int) {
        currentMonth += direction
        if (currentMonth > 11) { currentMonth = 0; currentYear++ }
        if (currentMonth < 0)  { currentMonth = 11; currentYear-- }

        // Sélectionner aujourd'hui si on revient au mois courant, sinon le 1er
        val today = Calendar.getInstance()
        selectedDay = if (currentYear == today.get(Calendar.YEAR)
            && currentMonth == today.get(Calendar.MONTH)) {
            today.get(Calendar.DAY_OF_MONTH)
        } else {
            1
        }

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

        // Décalage : Calendar.MONDAY = 2, on veut Lundi = 0
        val rawFirst = cal.get(Calendar.DAY_OF_WEEK)
        val offset   = (rawFirst - Calendar.MONDAY + 7) % 7

        val today  = Calendar.getInstance()
        val todayY = today.get(Calendar.YEAR)
        val todayM = today.get(Calendar.MONTH)
        val todayD = today.get(Calendar.DAY_OF_MONTH)

        val result = mutableListOf<CalendarDay>()

        // Cellules vides avant le 1er du mois
        repeat(offset) { result.add(CalendarDay(0, false, false, emptyList())) }

        // Jours du mois
        for (d in 1..daysInMonth) {
            val isToday = (currentYear == todayY && currentMonth == todayM && d == todayD)
            val types   = eventsForDay(d).map { it.typeEvenement }
            result.add(CalendarDay(d, isCurrentMonth = true, isToday = isToday, eventTypes = types))
        }

        // Compléter la dernière ligne (multiple de 7)
        while (result.size % 7 != 0) {
            result.add(CalendarDay(0, false, false, emptyList()))
        }

        return result
    }

    // ── Événements du jour sélectionné ───────────────────────────────────────

    private fun eventsForDay(day: Int): List<Evenement> {
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
        eventsAdapter = EventsAdapter(events)
        binding.recyclerEvents.adapter = eventsAdapter

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
        binding.tvMonthLabel.text = sdf.format(cal.time)
            .replaceFirstChar { it.uppercase() }
    }

    private fun updateSelectedDayLabel() {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, selectedDay)
        val sdf = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
        binding.tvSelectedDay.text = sdf.format(cal.time)
            .replaceFirstChar { it.uppercase() }
    }
}
