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

        adapter = ConversationsAdapter { conversation ->
            val bundle = Bundle().apply {
                putInt("conversationId", conversation.idConversation)
                putString("conversationName", getConversationDisplayName(conversation))
                putString("conversationType", conversation.type)
            }
            findNavController().navigate(R.id.action_chat_to_conversation, bundle)
        }

        binding.rvConversations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvConversations.adapter = adapter

        binding.btnNewChat.setOnClickListener { showNewConversationBottomSheet() }

        loadConversations()
    }

    private fun getConversationDisplayName(conversation: com.koordy.app.models.Conversation): String {
        return when {
            conversation.type == "group" -> conversation.nom ?: "Groupe"
            conversation.otherNom != null -> "${conversation.otherPrenom} ${conversation.otherNom}"
            else -> "Conversation"
        }
    }

    private fun loadConversations() {
        val idMembre = session.idMembre ?: return
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getConversations(idMembre)
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    adapter.setItems(list)
                    binding.layoutEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvConversations.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur de connexion", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

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
        session.lastOpenedChat = System.currentTimeMillis()
        loadConversations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
