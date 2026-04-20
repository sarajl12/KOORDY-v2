package com.koordy.app.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.koordy.app.databinding.ItemMessageInvitationBinding
import com.koordy.app.databinding.ItemMessageReceivedBinding
import com.koordy.app.databinding.ItemMessageSentBinding
import com.koordy.app.models.Message
import java.text.SimpleDateFormat
import java.util.Locale

class MessagesAdapter(
    private val currentUserId: Int,
    private val onRsvp: (idEvenement: Int, statut: String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SENT = 0
        private const val TYPE_RECEIVED = 1
        private const val TYPE_INVITATION = 2
    }

    private val items = mutableListOf<Message>()

    fun setItems(list: List<Message>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun addItem(msg: Message) {
        items.add(msg)
        notifyItemInserted(items.size - 1)
    }

    override fun getItemViewType(position: Int): Int {
        val msg = items[position]
        return when {
            msg.typeMessage == "invitation" -> TYPE_INVITATION
            msg.idAuteur == currentUserId -> TYPE_SENT
            else -> TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SENT -> SentVH(ItemMessageSentBinding.inflate(inflater, parent, false))
            TYPE_RECEIVED -> ReceivedVH(ItemMessageReceivedBinding.inflate(inflater, parent, false))
            else -> InvitationVH(ItemMessageInvitationBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = items[position]
        when (holder) {
            is SentVH -> holder.bind(msg)
            is ReceivedVH -> holder.bind(msg)
            is InvitationVH -> holder.bind(msg)
        }
    }

    override fun getItemCount() = items.size

    // ── Sent message ──────────────────────────────────────────────────────────

    inner class SentVH(private val b: ItemMessageSentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: Message) {
            b.tvBubble.text = msg.contenu
            b.tvTime.text = formatTime(msg.createdAt)
        }
    }

    // ── Received message ──────────────────────────────────────────────────────

    inner class ReceivedVH(private val b: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: Message) {
            b.tvBubble.text = msg.contenu
            b.tvTime.text = formatTime(msg.createdAt)
            b.tvAvatar.text = msg.prenomAuteur.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            b.tvSenderName.text = "${msg.prenomAuteur} ${msg.nomAuteur}"
        }
    }

    // ── Invitation card ───────────────────────────────────────────────────────

    inner class InvitationVH(private val b: ItemMessageInvitationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: Message) {
            b.tvTitre.text = msg.titreEvenement ?: msg.contenu
            b.tvType.text = msg.typeEvenement ?: "Événement"
            b.tvDate.text = msg.dateDebutEvent?.let { formatDate(it) } ?: ""

            if (!msg.lieuEvent.isNullOrBlank()) {
                b.tvLieu.text = "📍 ${msg.lieuEvent}"
                b.tvLieu.visibility = View.VISIBLE
            } else {
                b.tvLieu.visibility = View.GONE
            }

            // Couleur de la barre selon le type
            val color = when (msg.typeEvenement?.uppercase()) {
                "MATCH" -> Color.parseColor("#EF4444")
                "ENTRAINEMENT" -> Color.parseColor("#22C55E")
                "REUNION" -> Color.parseColor("#F59E0B")
                else -> Color.parseColor("#2D9CF0")
            }
            b.vTypeBar.setBackgroundColor(color)

            val statut = msg.statutRsvp ?: "En attente"
            val idEvenement = msg.idEvenement

            when {
                statut == "En attente" && idEvenement != null -> {
                    b.layoutBoutons.visibility = View.VISIBLE
                    b.tvStatut.visibility = View.GONE
                    b.btnAccepter.setOnClickListener {
                        onRsvp(idEvenement, "Accepté")
                        updateStatut("Accepté")
                    }
                    b.btnRefuser.setOnClickListener {
                        onRsvp(idEvenement, "Refusé")
                        updateStatut("Refusé")
                    }
                }
                statut == "Envoyée" -> {
                    b.layoutBoutons.visibility = View.GONE
                    b.tvStatut.visibility = View.VISIBLE
                    b.tvStatut.text = "En attente de réponse…"
                    b.tvStatut.setTextColor(Color.parseColor("#6B7280"))
                }
                else -> {
                    b.layoutBoutons.visibility = View.GONE
                    b.tvStatut.visibility = View.VISIBLE
                    val (label, colorRes) = when (statut) {
                        "Accepté" -> "✓ Accepté" to Color.parseColor("#22C55E")
                        "Refusé" -> "✗ Décliné" to Color.parseColor("#EF4444")
                        else -> statut to Color.parseColor("#6B7280")
                    }
                    b.tvStatut.text = label
                    b.tvStatut.setTextColor(colorRes)
                }
            }
        }

        private fun updateStatut(statut: String) {
            b.layoutBoutons.visibility = View.GONE
            b.tvStatut.visibility = View.VISIBLE
            val (label, color) = when (statut) {
                "Accepté" -> "✓ Accepté" to Color.parseColor("#22C55E")
                else -> "✗ Décliné" to Color.parseColor("#EF4444")
            }
            b.tvStatut.text = label
            b.tvStatut.setTextColor(color)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun formatTime(iso: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val date = sdf.parse(iso) ?: return ""
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        } catch (e: Exception) { "" }
    }

    private fun formatDate(iso: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val date = sdf.parse(iso) ?: return iso
            SimpleDateFormat("EEE dd MMM • HH:mm", Locale.FRENCH).format(date)
        } catch (e: Exception) { iso }
    }
}
