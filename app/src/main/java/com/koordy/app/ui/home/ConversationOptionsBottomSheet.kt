package com.koordy.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.BottomSheetConversationOptionsBinding
import com.koordy.app.models.Conversation
import com.koordy.app.utils.Constants
import com.koordy.app.utils.SessionManager
import kotlinx.coroutines.launch

class ConversationOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _b: BottomSheetConversationOptionsBinding? = null
    private val b get() = _b!!

    private lateinit var conversation: Conversation
    private lateinit var session: SessionManager

    var onPinToggled: ((idConv: Int, pinned: Boolean) -> Unit)? = null
    var onMuteToggled: ((idConv: Int, muted: Boolean) -> Unit)? = null
    var onDeleted: ((idConv: Int) -> Unit)? = null

    companion object {
        const val TAG = "ConversationOptionsSheet"

        fun newInstance(conv: Conversation) = ConversationOptionsBottomSheet().apply {
            conversation = conv
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = BottomSheetConversationOptionsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onStart() {
        super.onStart()
        val sheet = (dialog as? BottomSheetDialog)
            ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        BottomSheetBehavior.from(sheet).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        bindHeader()
        bindStates()
        setupClicks()
    }

    private fun bindHeader() {
        val name = when {
            conversation.type == "group" -> conversation.nom ?: "Groupe"
            conversation.otherNom != null -> "${conversation.otherPrenom} ${conversation.otherNom}"
            else -> "Conversation"
        }
        val otherPrenom = conversation.otherPrenom
        val initial = when {
            conversation.type == "group" -> "#"
            otherPrenom != null -> otherPrenom.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            else -> "?"
        }
        val photo = if (conversation.type == "direct") conversation.otherPhotoMembre else null

        b.tvHeaderName.text = name
        b.tvHeaderType.text = if (conversation.type == "group") "Groupe" else "Message privé"

        if (!photo.isNullOrEmpty()) {
            b.ivHeaderAvatar.visibility = View.VISIBLE
            b.tvHeaderAvatar.visibility = View.GONE
            Glide.with(this)
                .load("${Constants.BASE_URL.trimEnd('/')}$photo")
                .circleCrop()
                .into(b.ivHeaderAvatar)
        } else {
            b.tvHeaderAvatar.text = initial
        }
    }

    private fun bindStates() {
        val pinned = session.getPinnedConversations().contains(conversation.idConversation)
        val muted  = session.getMutedConversations().contains(conversation.idConversation)

        b.tvPinLabel.text = if (pinned) "Désépingler" else "Épingler"
        b.tvPinSub.text   = if (pinned) "Retirer de la tête de liste" else "Apparaît toujours en haut"
        b.tvPinActive.visibility = if (pinned) View.VISIBLE else View.GONE

        b.tvMuteLabel.text = if (muted) "Réactiver les notifications" else "Mettre en silencieux"
        b.tvMuteSub.text   = if (muted) "Recevoir à nouveau les notifications" else "Désactiver les notifications"
        b.tvMuteActive.visibility = if (muted) View.VISIBLE else View.GONE
    }

    private fun setupClicks() {
        b.optionPin.setOnClickListener {
            val pinned = session.getPinnedConversations().contains(conversation.idConversation)
            val updated = session.getPinnedConversations().toMutableSet()
            val nowPinned = if (pinned) { updated.remove(conversation.idConversation); false }
                            else        { updated.add(conversation.idConversation);    true  }
            session.setPinnedConversations(updated)
            onPinToggled?.invoke(conversation.idConversation, nowPinned)
            dismiss()
        }

        b.optionMute.setOnClickListener {
            val muted = session.getMutedConversations().contains(conversation.idConversation)
            val updated = session.getMutedConversations().toMutableSet()
            val nowMuted = if (muted) { updated.remove(conversation.idConversation); false }
                           else       { updated.add(conversation.idConversation);    true  }
            session.setMutedConversations(updated)
            onMuteToggled?.invoke(conversation.idConversation, nowMuted)
            dismiss()
        }

        b.optionDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setIcon(com.koordy.app.R.drawable.logo)
                .setTitle("Supprimer la conversation")
                .setMessage("Cette conversation et tous ses messages seront définitivement supprimés.")
                .setPositiveButton("Supprimer") { _, _ -> deleteConversation() }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }

    private fun deleteConversation() {
        lifecycleScope.launch {
            try {
                RetrofitClient.api.deleteConversation(conversation.idConversation)
                // Aussi retirer des listes pin/mute locales
                val pins = session.getPinnedConversations().toMutableSet()
                    .also { it.remove(conversation.idConversation) }
                session.setPinnedConversations(pins)
                val mutes = session.getMutedConversations().toMutableSet()
                    .also { it.remove(conversation.idConversation) }
                session.setMutedConversations(mutes)

                onDeleted?.invoke(conversation.idConversation)
                dismiss()
            } catch (_: Exception) {
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
