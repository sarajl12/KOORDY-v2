package com.koordy.app.ui.home

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.koordy.app.MainActivity
import com.koordy.app.R
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentHomeAssociationBinding
import com.koordy.app.ui.association.EventsAdapter
import com.koordy.app.ui.association.NewsAdapter
import com.koordy.app.ui.association.ConseilAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HomeAssociationFragment : Fragment() {

    private var _binding: FragmentHomeAssociationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeAssociationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = (activity as MainActivity).session
        val idAsso = session.idAssociation

        if (idAsso == -1) {
            // Pas d'association → page de recherche
            findNavController().navigate(R.id.action_homeAssociation_to_rechercheAssociation)
            return
        }

        setupRecyclers()
        loadData(idAsso)
        checkUnreadMessages(session.idMembre, session.lastOpenedChat)

        binding.btnMembre.setOnClickListener {
            findNavController().navigate(R.id.action_homeAssociation_to_membre)
        }
    }

    private fun setupRecyclers() {
        binding.recyclerConseil.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNews.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadData(idAsso: Int) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Charge tout en parallèle
                val assoDeferred = RetrofitClient.api.getAssociation(idAsso)
                val conseilDeferred = RetrofitClient.api.getConseil(idAsso)
                val eventsDeferred = RetrofitClient.api.getEvents(idAsso)
                val newsDeferred = RetrofitClient.api.getNews(idAsso)

                // Association hero
                if (assoDeferred.isSuccessful) {
                    val asso = assoDeferred.body()!!
                    binding.tvAssoName.text = asso.nom
                    binding.tvAssoSubtitle.text = "${asso.ville} · ${asso.sport}"
                    binding.tvDescription.text = asso.description.ifEmpty { "Aucune description disponible." }
                    binding.tvAdresse.text = asso.adresse
                    binding.tvVille.text = "${asso.codePostal} ${asso.ville}"
                    binding.tvPays.text = asso.pays
                    binding.tvTelephone.text = asso.telephone.ifEmpty { "—" }

                    if (asso.image.isNotEmpty()) {
                        Glide.with(this@HomeAssociationFragment)
                            .load("${com.koordy.app.utils.Constants.BASE_URL}${asso.image}")
                            .placeholder(R.drawable.bg_card)
                            .into(binding.ivAssoAvatar)
                    }
                }

                // Conseil
                if (conseilDeferred.isSuccessful) {
                    val conseil = conseilDeferred.body() ?: emptyList()
                    binding.recyclerConseil.adapter = ConseilAdapter(conseil)
                    binding.tvConseilEmpty.visibility =
                        if (conseil.isEmpty()) View.VISIBLE else View.GONE
                }

                // Events
                if (eventsDeferred.isSuccessful) {
                    val events = eventsDeferred.body() ?: emptyList()
                    binding.recyclerEvents.adapter = EventsAdapter(events)
                    binding.tvEventsEmpty.visibility =
                        if (events.isEmpty()) View.VISIBLE else View.GONE
                }

                // News
                if (newsDeferred.isSuccessful) {
                    val news = newsDeferred.body() ?: emptyList()
                    binding.recyclerNews.adapter = NewsAdapter(news)
                    binding.tvNewsEmpty.visibility =
                        if (news.isEmpty()) View.VISIBLE else View.GONE
                }

            } catch (e: Exception) {
                // chargement silencieux
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun checkUnreadMessages(idMembre: Int, lastOpenedChat: Long) {
        if (idMembre == -1) return
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getConversations(idMembre)
                if (!resp.isSuccessful) return@launch
                val conversations = resp.body() ?: return@launch

                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")

                val unreadCount = conversations.count { conv ->
                    val msgAt = conv.lastMessageAt ?: return@count false
                    val msgTime = try { sdf.parse(msgAt)?.time ?: 0L } catch (e: Exception) { 0L }
                    conv.lastMessage != null && msgTime > lastOpenedChat
                }

                if (unreadCount > 0) {
                    showUnreadDialog(unreadCount)
                }
            } catch (_: Exception) {}
        }
    }

    private fun showUnreadDialog(unreadCount: Int) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_unread_messages)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val badgeText = if (unreadCount == 1) "1 NOUVEAU MESSAGE" else "$unreadCount NOUVEAUX MESSAGES"
        dialog.findViewById<android.widget.TextView>(R.id.tvBadge).text = badgeText

        val title = if (unreadCount == 1) "Tu as un nouveau message !" else "Tu as $unreadCount nouveaux messages !"
        dialog.findViewById<android.widget.TextView>(R.id.tvTitle).text = title

        dialog.findViewById<android.widget.TextView>(R.id.btnDismiss).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<android.widget.TextView>(R.id.btnGoToChat).setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.chatFragment)
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
