package com.koordy.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.koordy.app.R
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentChatBinding
import com.koordy.app.models.Conversation
import com.koordy.app.utils.SessionManager
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private lateinit var adapter: ConversationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        adapter = ConversationsAdapter(
            onClick = { conv -> openConversation(conv) },
            onLongClick = { conv -> showOptions(conv) }
        )

        binding.rvConversations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvConversations.adapter = adapter

        binding.btnNewChat.setOnClickListener { showNewConversationBottomSheet() }

        loadConversations()
    }

    // ── Navigation vers la conversation ──────────────────────────────────────

    private fun openConversation(conv: Conversation) {
        val name = when {
            conv.type == "group" -> conv.nom ?: "Groupe"
            conv.otherNom != null -> "${conv.otherPrenom} ${conv.otherNom}"
            else -> "Conversation"
        }
        val bundle = Bundle().apply {
            putInt("conversationId", conv.idConversation)
            putString("conversationName", name)
            putString("conversationType", conv.type)
            putString("conversationPhoto", if (conv.type == "direct") conv.otherPhotoMembre else null)
        }
        findNavController().navigate(R.id.action_chat_to_conversation, bundle)
    }

    // ── Bottom sheet options (long press) ─────────────────────────────────────

    private fun showOptions(conv: Conversation) {
        val sheet = ConversationOptionsBottomSheet.newInstance(conv)
        sheet.onPinToggled = { _, _ ->
            adapter.updateStates(session.getPinnedConversations(), session.getMutedConversations())
        }
        sheet.onMuteToggled = { _, _ ->
            adapter.updateStates(session.getPinnedConversations(), session.getMutedConversations())
        }
        sheet.onDeleted = { _ ->
            adapter.updateStates(session.getPinnedConversations(), session.getMutedConversations())
            loadConversations()
            Toast.makeText(requireContext(), "Conversation supprimée", Toast.LENGTH_SHORT).show()
        }
        sheet.show(childFragmentManager, ConversationOptionsBottomSheet.TAG)
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private fun loadConversations() {
        val idMembre = session.idMembre
        if (idMembre == -1) return
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getConversations(idMembre)
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    adapter.setData(
                        list,
                        session.getPinnedConversations(),
                        session.getMutedConversations()
                    )
                    binding.layoutEmpty.visibility  = if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvConversations.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Erreur de connexion", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    // ── Nouvelle conversation ──────────────────────────────────────────────────

    private fun showNewConversationBottomSheet() {
        NewConversationBottomSheet().apply {
            onConversationStarted = { idConv, name, type ->
                val bundle = Bundle().apply {
                    putInt("conversationId", idConv)
                    putString("conversationName", name)
                    putString("conversationType", type)
                }
                findNavController().navigate(R.id.action_chat_to_conversation, bundle)
            }
        }.show(childFragmentManager, NewConversationBottomSheet.TAG)
    }

    override fun onResume() {
        super.onResume()
        val now = System.currentTimeMillis()
        session.lastOpenedChat = now
        session.lastShownUnreadDialog = now
        loadConversations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
