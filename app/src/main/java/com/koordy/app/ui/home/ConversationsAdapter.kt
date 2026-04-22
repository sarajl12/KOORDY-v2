package com.koordy.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.koordy.app.databinding.ItemConversationBinding
import com.koordy.app.models.Conversation
import com.koordy.app.utils.Constants
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ConversationsAdapter(
    private val onClick: (Conversation) -> Unit,
    private val onLongClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationsAdapter.VH>() {

    private var allItems = listOf<Conversation>()
    private var pinnedIds = setOf<Int>()
    private var mutedIds  = setOf<Int>()

    /** Met à jour la liste et les états pin/mute, puis trie épinglées en premier. */
    fun setData(
        list: List<Conversation>,
        pinned: Set<Int>,
        muted: Set<Int>
    ) {
        allItems  = list
        pinnedIds = pinned
        mutedIds  = muted
        notifyDataSetChanged()
    }

    /** Rafraîchit uniquement les états pin/mute sans recharger la liste réseau. */
    fun updateStates(pinned: Set<Int>, muted: Set<Int>) {
        pinnedIds = pinned
        mutedIds  = muted
        notifyDataSetChanged()
    }

    private fun sorted(): List<Conversation> =
        allItems.sortedWith(compareByDescending { pinnedIds.contains(it.idConversation) })

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(sorted()[position])

    override fun getItemCount() = allItems.size

    inner class VH(private val b: ItemConversationBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(conv: Conversation) {
            val name = when {
                conv.type == "group" -> conv.nom ?: "Groupe"
                conv.otherNom != null -> "${conv.otherPrenom} ${conv.otherNom}"
                else -> "Conversation"
            }
            val otherPrenom = conv.otherPrenom
            val initial = when {
                conv.type == "group" -> "#"
                otherPrenom != null -> otherPrenom.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                else -> "?"
            }

            // ── Avatar ────────────────────────────────────────────────────────
            val photo = if (conv.type == "direct") conv.otherPhotoMembre else null
            if (!photo.isNullOrEmpty()) {
                b.ivAvatar.visibility = View.VISIBLE
                b.tvAvatar.visibility = View.GONE
                Glide.with(b.root.context)
                    .load("${Constants.BASE_URL.trimEnd('/')}$photo")
                    .circleCrop()
                    .into(b.ivAvatar)
            } else {
                b.ivAvatar.visibility = View.GONE
                b.tvAvatar.visibility = View.VISIBLE
                b.tvAvatar.text = initial
            }

            b.tvName.text = name

            // ── Dernier message ───────────────────────────────────────────────
            val lastMsg = when {
                conv.lastMessageType == "invitation" -> "Invitation à un événement"
                conv.lastMessage != null -> {
                    val first = conv.lastSenderPrenom ?: ""
                    if (conv.type == "group" && first.isNotEmpty()) "$first : ${conv.lastMessage}"
                    else conv.lastMessage
                }
                else -> "Démarrer la conversation"
            }
            b.tvLastMessage.text = lastMsg

            // ── Heure ─────────────────────────────────────────────────────────
            b.tvTime.text = conv.lastMessageAt?.let { formatTime(it) } ?: ""

            // ── Badges pin / mute ─────────────────────────────────────────────
            val isPinned = pinnedIds.contains(conv.idConversation)
            val isMuted  = mutedIds.contains(conv.idConversation)

            b.ivPinBadge.visibility  = if (isPinned) View.VISIBLE else View.GONE
            b.muteBadge.visibility   = if (isMuted)  View.VISIBLE else View.GONE

            // ── Fond légèrement teinté pour les épinglées ─────────────────────
            b.root.setBackgroundColor(
                if (isPinned) android.graphics.Color.parseColor("#FFF8E7")
                else android.graphics.Color.parseColor("#FFFFFF")
            )

            // ── Clics ─────────────────────────────────────────────────────────
            b.root.setOnClickListener { onClick(conv) }
            b.root.setOnLongClickListener { onLongClick(conv); true }
        }

        private fun formatTime(iso: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    .also { it.timeZone = TimeZone.getTimeZone("UTC") }
                val date = sdf.parse(iso) ?: return ""
                val now = java.util.Calendar.getInstance()
                val cal = java.util.Calendar.getInstance().apply { time = date }
                if (cal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR))
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                else
                    SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
            } catch (_: Exception) { "" }
        }
    }
}
