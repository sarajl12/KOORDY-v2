package com.koordy.app.ui.association

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.koordy.app.R
import com.koordy.app.utils.Constants
import com.koordy.app.databinding.ItemChecklistItemBinding
import com.koordy.app.databinding.ItemEquipeSelectBinding
import com.koordy.app.databinding.ItemEventBinding
import com.koordy.app.databinding.ItemEventCalendarBinding
import com.koordy.app.databinding.ItemNewsBinding
import com.koordy.app.databinding.ItemConseilBinding
import com.koordy.app.models.Actualite
import com.koordy.app.models.ChecklistItem
import com.koordy.app.models.ConseilMembre
import com.koordy.app.models.EquipeDetail
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

        // Parse date (UTC → heure locale de l'appareil)
        val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.FRENCH)
            .also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
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

        b.tvTitle.text   = n.titre
        b.tvContenu.text = n.contenu

        val isEvent = n.typeActualite.lowercase() == "evenement"
        b.tvBadge.text = if (isEvent) "Événement" else "Article"
        b.tvBadge.setTextColor(
            if (isEvent) Color.parseColor("#A8FF60") else Color.parseColor("#6CCFFF")
        )

        try {
            val sdf    = SimpleDateFormat("yyyy-MM-dd", Locale.FRENCH)
            val sdfOut = SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH)
            val d = sdf.parse(n.datePublication.take(10))
            b.tvDate.text = "Publié le ${sdfOut.format(d!!)}"
        } catch (_: Exception) { b.tvDate.text = "" }

        if (!n.imagePrincipale.isNullOrEmpty()) {
            b.ivNewsImage.visibility = View.VISIBLE
            Glide.with(b.root.context)
                .load("${Constants.BASE_URL.trimEnd('/')}${n.imagePrincipale}")
                .centerCrop()
                .into(b.ivNewsImage)
        } else {
            b.ivNewsImage.visibility = View.GONE
        }
    }
}

// ── CalendarEventsAdapter  (calendrier personnel + RSVP) ─────────────────────

class CalendarEventsAdapter(
    private var items: List<EvenementAvecStatut>,
    private val onRsvp: (idEvenement: Int, statut: String) -> Unit
) : RecyclerView.Adapter<CalendarEventsAdapter.VH>() {

    private val sdfIn    = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.FRENCH)
        .also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
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

        if (m.photoMembre.isNotEmpty()) {
            b.ivAvatar.visibility = View.VISIBLE
            b.tvAvatar.visibility = View.GONE
            Glide.with(b.root.context)
                .load("${Constants.BASE_URL.trimEnd('/')}${m.photoMembre}")
                .circleCrop()
                .into(b.ivAvatar)
        } else {
            b.ivAvatar.visibility = View.GONE
            b.tvAvatar.visibility = View.VISIBLE
            b.tvAvatar.text = "${m.prenom.firstOrNull() ?: ""}".uppercase()
        }
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

// ── EquipeSelectAdapter  (sélection équipes pour participants événement) ──────

class EquipeSelectAdapter(
    private val selectedIds: MutableSet<Int>,
    private val onFetchMembers: (EquipeDetail, (List<ConseilMembre>) -> Unit) -> Unit,
    private val onCountChanged: () -> Unit
) : RecyclerView.Adapter<EquipeSelectAdapter.VH>() {

    private var items: List<EquipeDetail> = emptyList()
    private val equipeMembers = mutableMapOf<Int, List<ConseilMembre>>()
    private val checkedEquipes = mutableSetOf<Int>()

    inner class VH(val b: ItemEquipeSelectBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemEquipeSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.b.tvEquipeName.text = e.nomEquipe
        holder.b.tvEquipeCount.text = "${e.nbMembres} membre${if (e.nbMembres > 1) "s" else ""}"
        holder.b.cbEquipeSelected.isChecked = checkedEquipes.contains(e.idEquipe)

        holder.b.root.setOnClickListener {
            if (checkedEquipes.contains(e.idEquipe)) {
                checkedEquipes.remove(e.idEquipe)
                equipeMembers[e.idEquipe]?.forEach { selectedIds.remove(it.idMembre) }
                holder.b.cbEquipeSelected.isChecked = false
                onCountChanged()
            } else {
                val cached = equipeMembers[e.idEquipe]
                if (cached != null) {
                    checkedEquipes.add(e.idEquipe)
                    cached.forEach { selectedIds.add(it.idMembre) }
                    holder.b.cbEquipeSelected.isChecked = true
                    onCountChanged()
                } else {
                    onFetchMembers(e) { membres ->
                        equipeMembers[e.idEquipe] = membres
                        checkedEquipes.add(e.idEquipe)
                        membres.forEach { selectedIds.add(it.idMembre) }
                        notifyItemChanged(position)
                        onCountChanged()
                    }
                }
            }
        }
    }

    fun update(newItems: List<EquipeDetail>) {
        items = newItems
        notifyDataSetChanged()
    }
}

// ── ChecklistAdapter ──────────────────────────────────────────────────────────

class ChecklistAdapter(
    private var items: List<ChecklistItem>,
    private val onToggle: (ChecklistItem) -> Unit
) : RecyclerView.Adapter<ChecklistAdapter.VH>() {

    private val sdfIn  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.FRENCH)
        .also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
    private val sdfOut = SimpleDateFormat("dd/MM à HH:mm", Locale.FRENCH)

    inner class VH(val b: ItemChecklistItemBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemChecklistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val b = holder.b

        b.cbItem.isChecked = item.isChecked

        if (item.isChecked) {
            b.tvItemName.paintFlags = b.tvItemName.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            b.tvItemName.alpha = 0.45f
        } else {
            b.tvItemName.paintFlags = b.tvItemName.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            b.tvItemName.alpha = 1f
        }
        b.tvItemName.text = item.nomItem

        val timeStr = try { sdfOut.format(sdfIn.parse(item.createdAt.take(19))!!) } catch (_: Exception) { "" }
        b.tvAuthor.text = if (timeStr.isNotBlank()) "Ajouté par ${item.nomAuteur} · $timeStr"
                          else "Ajouté par ${item.nomAuteur}"

        if (item.commentaire.isNotBlank()) {
            b.tvComment.text = "💬 ${item.commentaire}"
            b.tvComment.visibility = View.VISIBLE
        } else {
            b.tvComment.visibility = View.GONE
        }

        if (item.isChecked && !item.checkedByNom.isNullOrBlank()) {
            val checkedTime = try {
                item.checkedAt?.let { sdfOut.format(sdfIn.parse(it.take(19))!!) } ?: ""
            } catch (_: Exception) { "" }
            b.tvCheckedBy.text = if (checkedTime.isNotBlank()) "✓ Coché par ${item.checkedByNom} · $checkedTime"
                                 else "✓ Coché par ${item.checkedByNom}"
            b.tvCheckedBy.visibility = View.VISIBLE
        } else {
            b.tvCheckedBy.visibility = View.GONE
        }

        b.cbItem.setOnClickListener { onToggle(item) }
        b.root.setOnClickListener { onToggle(item) }
    }

    fun update(newItems: List<ChecklistItem>) {
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
        val b = holder.binding
        b.tvName.text = "${m.prenom} ${m.nom}"
        b.tvRole.text = m.role
        if (m.photoMembre.isNotEmpty()) {
            b.ivAvatar.visibility = View.VISIBLE
            b.tvAvatarInitial.visibility = View.GONE
            Glide.with(b.root.context)
                .load("${Constants.BASE_URL.trimEnd('/')}${m.photoMembre}")
                .circleCrop()
                .into(b.ivAvatar)
        } else {
            b.ivAvatar.visibility = View.GONE
            b.tvAvatarInitial.visibility = View.VISIBLE
            b.tvAvatarInitial.text = m.prenom.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        }
    }
}
