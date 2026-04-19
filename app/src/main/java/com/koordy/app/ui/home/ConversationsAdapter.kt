package com.koordy.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.koordy.app.databinding.ItemConversationBinding
import com.koordy.app.models.Conversation
import java.text.SimpleDateFormat
import java.util.Locale

class ConversationsAdapter(
    private val onClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationsAdapter.VH>() {

    private val items = mutableListOf<Conversation>()

    fun setItems(list: List<Conversation>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val b: ItemConversationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(conv: Conversation) {
            val name = when {
                conv.type == "group" -> conv.nom ?: "Général"
                conv.otherNom != null -> "${conv.otherPrenom} ${conv.otherNom}"
                else -> "Conversation"
            }

            val initial = when {
                conv.type == "group" -> "#"
                conv.otherPrenom != null -> conv.otherPrenom.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                else -> "?"
            }

            b.tvAvatar.text = initial
            b.tvName.text = name

            val lastMsg = when {
                conv.lastMessageType == "invitation" -> "Invitation à un événement"
                conv.lastMessage != null -> {
                    val senderFirst = conv.lastSenderPrenom ?: ""
                    if (conv.type == "group" && senderFirst.isNotEmpty()) "$senderFirst : ${conv.lastMessage}"
                    else conv.lastMessage
                }
                else -> "Démarrer la conversation"
            }
            b.tvLastMessage.text = lastMsg

            b.tvTime.text = conv.lastMessageAt?.let { formatTime(it) } ?: ""

            b.root.setOnClickListener { onClick(conv) }
        }

        private fun formatTime(iso: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val date = sdf.parse(iso) ?: return ""
                val now = java.util.Calendar.getInstance()
                val cal = java.util.Calendar.getInstance().apply { time = date }
                if (cal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                } else {
                    SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) { "" }
        }
    }
}
