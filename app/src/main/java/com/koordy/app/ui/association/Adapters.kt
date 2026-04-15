package com.koordy.app.ui.association

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.koordy.app.databinding.ItemEventBinding
import com.koordy.app.databinding.ItemNewsBinding
import com.koordy.app.databinding.ItemConseilBinding
import com.koordy.app.models.Actualite
import com.koordy.app.models.ConseilMembre
import com.koordy.app.models.Evenement
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
