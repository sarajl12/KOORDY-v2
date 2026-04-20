package com.koordy.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentConversationBinding
import com.koordy.app.models.Message
import com.koordy.app.models.MessageRequest
import com.koordy.app.models.RsvpRequest
import com.koordy.app.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationFragment : Fragment() {

    private var _binding: FragmentConversationBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private lateinit var adapter: MessagesAdapter

    private var conversationId: Int = -1
    private var conversationName: String = ""
    private var conversationType: String = "direct"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        conversationId = arguments?.getInt("conversationId") ?: -1
        conversationName = arguments?.getString("conversationName") ?: "Conversation"
        conversationType = arguments?.getString("conversationType") ?: "direct"

        val idMembre = session.idMembre ?: return

        adapter = MessagesAdapter(
            currentUserId = idMembre,
            onRsvp = { idEvenement, statut -> respondRsvp(idEvenement, statut) }
        )

        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        // Header
        binding.tvConvName.text = conversationName
        binding.tvConvType.text = if (conversationType == "group") "Groupe" else "Message privé"
        binding.tvAvatarHeader.text = when {
            conversationType == "group" -> "#"
            conversationName.isNotEmpty() -> conversationName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            else -> "?"
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }

        if (conversationId != -1) loadMessages()
    }

    private fun loadMessages() {
        val idMembre = session.idMembre ?: return
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getMessages(conversationId, idMembre)
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    adapter.setItems(list)
                    scrollToBottom()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur de connexion", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return
        val idMembre = session.idMembre ?: return

        binding.etMessage.text.clear()

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.sendMessage(
                    conversationId,
                    MessageRequest(idAuteur = idMembre, contenu = text)
                )
                if (resp.isSuccessful) {
                    val response = resp.body()
                    val localMsg = Message(
                        idMessage = response?.idMessage ?: 0,
                        idConversation = conversationId,
                        idAuteur = idMembre,
                        contenu = text,
                        typeMessage = "text",
                        createdAt = response?.createdAt ?: nowIso(),
                        nomAuteur = session.nomMembre ?: "",
                        prenomAuteur = session.prenomMembre ?: ""
                    )
                    adapter.addItem(localMsg)
                    scrollToBottom()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur d'envoi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun respondRsvp(idEvenement: Int, statut: String) {
        val idMembre = session.idMembre ?: return
        // Confirmation immédiate (l'UI a déjà basculé dans MessagesAdapter)
        val label = if (statut == "Accepté") "Participation confirmée ✓" else "Invitation déclinée"
        if (isAdded) Toast.makeText(requireContext(), label, Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                RetrofitClient.api.respondRsvp(idEvenement, RsvpRequest(idMembre, statut))
            } catch (_: Exception) {
                // La requête a très probablement abouti — pas de rollback pour ne pas perturber l'UI
            }
        }
    }

    private fun scrollToBottom() {
        val count = adapter.itemCount
        if (count > 0) binding.rvMessages.scrollToPosition(count - 1)
    }

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
