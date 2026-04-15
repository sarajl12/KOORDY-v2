package com.koordy.app.ui.association

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.koordy.app.R
import com.koordy.app.databinding.ItemEventBinding
import com.koordy.app.databinding.ItemEventCalendarBinding
import com.koordy.app.databinding.ItemNewsBinding
import com.koordy.app.databinding.ItemConseilBinding
import com.koordy.app.models.Actualite
import com.koordy.app.models.ConseilMembre
import com.koordy.app.models.Evenement
import com.koordy.app.models.EvenementAvecStatut
import java.text.SimpleDateFormat
import java.util.Locale

// ── Events ────────────────────────────────────────────────────────────────────

class EventsAdapter(private val items: List<Evenement>) :
    RecyclerView.Adapter<EventsAdapter.VH>() {

    inner class VH(val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ev = items[position]
        val b = holder.binding

        // Parse date
        val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.FRENCH)
        val sdfDay = SimpleDateFormat("dd", Locale.FRENCH)
        val sdfMonth = SimpleDateFormat("MMM", Locale.FRENCH)
        val sdfTime = SimpleDateFormat("HH:mm", Locale.FRENCH)

        try {
            val d = sdfIn.parse(ev.dateDebutEvent)
            b.tvDay.text = sdfDay.format(d!!)
            b.tvMonth.text = sdfMonth.format(d).uppercase()
            b.tvTime.text = sdfTime.format(d)
        } catch (e: Exception) {
            b.tvDay.text = "--"
            b.tvMonth.text = "---"
            b.tvTime.text = "--:--"
        }

        b.tvTitle.text = ev.titreEvenement
        b.tvLieu.text = ev.lieuEvent
        b.tvType.text = ev.typeEvenement
        b.tvDesc.text = ev.descriptionEvenement

        // Badge color by type
        val badgeColor = when (ev.typeEvenement.uppercase()) {
            "MATCH" -> android.graphics.Color.parseColor("#FF6B6B")
            "ENTRAINEMENT" -> android.graphics.Color.parseColor("#6CCFFF")
            "REUNION" -> android.graphics.Color.parseColor("#A8FF60")
            else -> android.graphics.Color.parseColor("#9AA3B2")
        }
        b.tvType.setTextColor(badgeColor)
    }
}

// ── News ──────────────────────────────────────────────────────────────────────

class NewsAdapter(private val items: List<Actualite>) :
    RecyclerView.Adapter<NewsAdapter.VH>() {

    inner class VH(val binding: ItemNewsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val n = items[position]
        val b = holder.binding

        b.tvTitle.text = n.titre
        b.tvContenu.text = n.contenu

        val isEvent = n.typeActualite.lowercase() == "evenement"
        b.tvBadge.text = if (isEvent) "Événement" else "Article"
        b.tvBadge.setTextColor(
            if (isEvent) android.graphics.Color.parseColor("#A8FF60")
            else android.graphics.Color.parseColor("#6CCFFF")
        )

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRENCH)
            val d = sdf.parse(n.datePublication.take(10))
            val sdfOut = SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH)
            b.tvDate.text = "Publié le ${sdfOut.format(d!!)}"
        } catch (e: Exception) {
            b.tvDate.text = ""
        }
    }
}

// ── CalendarEventsAdapter  (calendrier personnel + RSVP) ─────────────────────

class CalendarEventsAdapter(
    private var items: List<EvenementAvecStatut>,
    private val onRsvp: (idEvenement: Int, statut: String) -> Unit
) : RecyclerView.Adapter<CalendarEventsAdapter.VH>() {

    private val sdfIn    = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.FRENCH)
    private val sdfDay   = SimpleDateFormat("dd",    Locale.FRENCH)
    private val sdfMonth = SimpleDateFormat("MMM",   Locale.FRENCH)
    private val sdfTime  = SimpleDateFormat("HH:mm", Locale.FRENCH)

    inner class VH(val b: ItemEventCalendarBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemEventCalendarBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ev  = items[position]
        val b   = holder.b
        val ctx = b.root.context

        // ── Date ──────────────────────────────────────────────────────────────
        try {
            val d = sdfIn.parse(ev.dateDebutEvent)!!
            b.tvDay.text   = sdfDay.format(d)
            b.tvMonth.text = sdfMonth.format(d).uppercase()
            b.tvTime.text  = sdfTime.format(d)
        } catch (e: Exception) {
            b.tvDay.text   = "--"
            b.tvMonth.text = "---"
            b.tvTime.text  = "--:--"
        }

        // ── Contenu ───────────────────────────────────────────────────────────
        b.tvTitle.text = ev.titreEvenement
        b.tvType.text  = ev.typeEvenement
        b.tvType.setTextColor(typeColor(ev.typeEvenement))

        if (ev.lieuEvent.isNotBlank()) {
            b.tvLieu.text       = "📍 ${ev.lieuEvent}"
            b.tvLieu.visibility = View.VISIBLE
        } else {
            b.tvLieu.visibility = View.GONE
        }

        if (ev.descriptionEvenement.isNotBlank()) {
            b.tvDesc.text       = ev.descriptionEvenement
            b.tvDesc.visibility = View.VISIBLE
        } else {
            b.tvDesc.visibility = View.GONE
        }

        // ── Barre colorée statut ──────────────────────────────────────────────
        b.viewStatusBar.setBackgroundColor(rsvpColor(ev.statut))

        // ── Section RSVP ──────────────────────────────────────────────────────
        when (ev.statut) {
            "En attente" -> {
                b.viewRsvpSep.visibility    = View.VISIBLE
                b.llRsvpButtons.visibility  = View.VISIBLE
                b.llStatut.visibility       = View.GONE

                b.btnAccept.setOnClickListener { onRsvp(ev.idEvenement, "Accepté") }
                b.btnRefuse.setOnClickListener { onRsvp(ev.idEvenement, "Refusé") }
            }
            "Accepté", "Refusé" -> {
                b.viewRsvpSep.visibility    = View.VISIBLE
                b.llStatut.visibility       = View.VISIBLE
                b.llRsvpButtons.visibility  = View.GONE

                val color = rsvpColor(ev.statut)
                b.tvStatut.text      = ev.statut
                b.tvStatut.setTextColor(color)

                // Dot coloré
                val dot = GradientDrawable()
                dot.shape = GradientDrawable.OVAL
                dot.setColor(color)
                b.viewStatutDot.background = dot
            }
            else -> {
                b.viewRsvpSep.visibility   = View.GONE
                b.llStatut.visibility      = View.GONE
                b.llRsvpButtons.visibility = View.GONE
            }
        }
    }

    fun update(newItems: List<EvenementAvecStatut>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun typeColor(type: String): Int = when (type.uppercase()) {
        "MATCH"         -> Color.parseColor("#FF6B6B")
        "ENTRAINEMENT"  -> Color.parseColor("#6CCFFF")
        "REUNION"       -> Color.parseColor("#A8FF60")
        else            -> Color.parseColor("#9AA3B2")
    }

    private fun rsvpColor(statut: String): Int = when (statut) {
        "Accepté"    -> Color.parseColor("#22C55E")
        "Refusé"     -> Color.parseColor("#EF4444")
        else         -> Color.parseColor("#2D9CF0") // En attente
    }
}

// ── MemberSelectAdapter  (sélection participants pour création d'événement) ───

class MemberSelectAdapter(
    private var items: List<ConseilMembre>,
    private val selectedIds: MutableSet<Int>
) : RecyclerView.Adapter<MemberSelectAdapter.VH>() {

    inner class VH(val b: com.koordy.app.databinding.ItemMemberSelectBinding)
        : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(com.koordy.app.databinding.ItemMemberSelectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        val b = holder.b

        val initiale = "${m.prenom.firstOrNull() ?: ""}".uppercase()
        b.tvAvatar.text = initiale
        b.tvMemberName.text = "${m.prenom} ${m.nom}"
        b.tvMemberRole.text = m.role.ifBlank { "Membre" }
        b.cbSelected.isChecked = selectedIds.contains(m.idMembre)

        b.root.setOnClickListener {
            if (selectedIds.contains(m.idMembre)) {
                selectedIds.remove(m.idMembre)
                b.cbSelected.isChecked = false
            } else {
                selectedIds.add(m.idMembre)
                b.cbSelected.isChecked = true
            }
        }
    }

    fun selectAll() {
        selectedIds.clear()
        selectedIds.addAll(items.map { it.idMembre })
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectedIds.clear()
        notifyDataSetChanged()
    }

    fun allSelected() = items.isNotEmpty() && selectedIds.size == items.size

    fun update(newItems: List<ConseilMembre>) {
        items = newItems
        notifyDataSetChanged()
    }
}

// ── Conseil ───────────────────────────────────────────────────────────────────

class ConseilAdapter(private val items: List<ConseilMembre>) :
    RecyclerView.Adapter<ConseilAdapter.VH>() {

    inner class VH(val binding: ItemConseilBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemConseilBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.binding.tvName.text = "${m.prenom} ${m.nom}"
        holder.binding.tvRole.text = m.role
    }
}
