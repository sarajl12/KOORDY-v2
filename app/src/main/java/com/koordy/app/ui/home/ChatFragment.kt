package com.koordy.app.ui.home

import android.app.AlertDialog
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
import com.koordy.app.models.ConversationRequest
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

        binding.btnNewChat.setOnClickListener { showNewConversationDialog() }

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

    private fun showNewConversationDialog() {
        val idAssociation = session.idAssociation ?: return
        val idMembre = session.idMembre ?: return

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getMembres(idAssociation)
                if (!resp.isSuccessful) return@launch
                val membres = (resp.body() ?: emptyList())
                    .filter { it.idMembre != idMembre }

                if (membres.isEmpty()) {
                    Toast.makeText(requireContext(), "Aucun autre membre dans l'association", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val names = membres.map { "${it.prenom} ${it.nom}" }.toTypedArray()

                AlertDialog.Builder(requireContext())
                    .setTitle("Nouvelle conversation")
                    .setItems(names) { _, index ->
                        val selected = membres[index]
                        startDirectConversation(idAssociation, idMembre, selected.idMembre, "${selected.prenom} ${selected.nom}")
                    }
                    .setNegativeButton("Annuler", null)
                    .show()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur de connexion", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startDirectConversation(idAssociation: Int, idInitiateur: Int, idDestinataire: Int, name: String) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.createConversation(
                    ConversationRequest(
                        idAssociation = idAssociation,
                        idInitiateur = idInitiateur,
                        type = "direct",
                        idDestinataire = idDestinataire
                    )
                )
                if (resp.isSuccessful) {
                    val idConv = resp.body()?.idConversation ?: return@launch
                    val bundle = Bundle().apply {
                        putInt("conversationId", idConv)
                        putString("conversationName", name)
                        putString("conversationType", "direct")
                    }
                    findNavController().navigate(R.id.action_chat_to_conversation, bundle)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur de connexion", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
