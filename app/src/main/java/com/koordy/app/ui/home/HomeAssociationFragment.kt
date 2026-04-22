package com.koordy.app.ui.home

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.koordy.app.MainActivity
import com.koordy.app.R
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentHomeAssociationBinding
import com.koordy.app.models.Actualite
import com.koordy.app.models.Evenement
import com.koordy.app.ui.association.ConseilAdapter
import com.koordy.app.utils.Constants
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HomeAssociationFragment : Fragment() {

    private var _binding: FragmentHomeAssociationBinding? = null
    private val binding get() = _binding!!

    private val sdfEvent = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.FRENCH)
        .also { it.timeZone = TimeZone.getTimeZone("UTC") }

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
            findNavController().navigate(R.id.action_homeAssociation_to_rechercheAssociation)
            return
        }

        binding.recyclerConseil.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Click listeners posés ici, toujours actifs peu importe ce que fait le réseau
        binding.btnVoirTout.setOnClickListener { navigateToAllNews(idAsso) }
        binding.cardLastNews.setOnClickListener { navigateToAllNews(idAsso) }
        binding.btnEditAsso.setOnClickListener {
            findNavController().navigate(
                R.id.action_homeAssociation_to_editAssociation,
                bundleOf("id_association" to idAsso)
            )
        }

        loadData(idAsso, session.idMembre)
        checkUnreadMessages(session.idMembre, session.lastOpenedChat)
    }

    private fun navigateToAllNews(idAsso: Int) {
        findNavController().navigate(
            R.id.action_homeAssociation_to_allNews,
            bundleOf("id_association" to idAsso)
        )
    }

    private fun loadData(idAsso: Int, idMembre: Int) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Chaque appel est indépendant — une erreur réseau n'en bloque pas un autre
            val assoResp    = runCatching { RetrofitClient.api.getAssociation(idAsso) }.getOrNull()
            val conseilResp = runCatching { RetrofitClient.api.getConseil(idAsso) }.getOrNull()
            val membresResp = runCatching { RetrofitClient.api.getMembres(idAsso) }.getOrNull()
            val eventsResp  = runCatching { RetrofitClient.api.getEvents(idAsso) }.getOrNull()
            val newsResp    = runCatching { RetrofitClient.api.getNews(idAsso) }.getOrNull()
            val adminResp   = if (idMembre != -1)
                runCatching { RetrofitClient.api.isAdmin(idAsso, idMembre) }.getOrNull()
            else null

            // ── Association hero ──────────────────────────────────────────
            val asso = assoResp?.takeIf { it.isSuccessful }?.body()
            if (asso != null) {
                binding.tvAssoName.text     = asso.nom
                binding.tvAssoSubtitle.text = "${asso.ville} · ${asso.sport}"
                binding.tvDescription.text  = asso.description.ifEmpty { "Aucune description disponible." }
                binding.tvAdresse.text      = asso.adresse.ifEmpty { "—" }
                binding.tvVille.text        = "${asso.codePostal} ${asso.ville}".trim()
                binding.tvPays.text         = asso.pays.ifEmpty { "—" }
                binding.tvTelephone.text    = asso.telephone.ifEmpty { "—" }

                if (asso.image.isNotEmpty()) {
                    Glide.with(this@HomeAssociationFragment)
                        .load("${Constants.BASE_URL.trimEnd('/')}${asso.image}")
                        .circleCrop()
                        .placeholder(R.drawable.bg_avatar_round)
                        .into(binding.ivAssoAvatar)
                }
            }

            // ── Bouton modifier (Président uniquement) ────────────────────
            val isPresident = adminResp?.takeIf { it.isSuccessful }?.body()?.isAdmin == true
            binding.btnEditAsso.visibility = if (isPresident) View.VISIBLE else View.GONE

            // ── Stats ─────────────────────────────────────────────────────
            val membres = membresResp?.takeIf { it.isSuccessful }?.body() ?: emptyList()
            binding.tvStatMembres.text = membres.size.toString()

            // ── Conseil ───────────────────────────────────────────────────
            val conseil = conseilResp?.takeIf { it.isSuccessful }?.body() ?: emptyList()
            binding.recyclerConseil.adapter = ConseilAdapter(conseil)
            binding.tvConseilEmpty.visibility = if (conseil.isEmpty()) View.VISIBLE else View.GONE

            // ── Prochain événement ────────────────────────────────────────
            val events = eventsResp?.takeIf { it.isSuccessful }?.body() ?: emptyList()
            val now = System.currentTimeMillis()
            val upcoming = events.filter { ev ->
                runCatching { (sdfEvent.parse(ev.dateDebutEvent)?.time ?: 0L) >= now }.getOrDefault(false)
            }
            binding.tvStatEvents.text = upcoming.size.toString()
            bindNextEvent(upcoming.firstOrNull())

            // ── Dernière actualité ────────────────────────────────────────
            val news = newsResp?.takeIf { it.isSuccessful }?.body() ?: emptyList()
            binding.tvStatNews.text = news.size.toString()
            bindLastNews(news.firstOrNull())

            binding.progressBar.visibility = View.GONE
        }
    }

    private fun bindNextEvent(event: Evenement?) {
        if (event == null) {
            binding.cardNextEvent.visibility = View.GONE
            binding.tvEventsEmpty.visibility = View.VISIBLE
            return
        }
        binding.cardNextEvent.visibility = View.VISIBLE
        binding.tvEventsEmpty.visibility = View.GONE

        runCatching {
            val d = sdfEvent.parse(event.dateDebutEvent)!!
            binding.tvNextEventDay.text   = SimpleDateFormat("dd", Locale.FRENCH).format(d)
            binding.tvNextEventMonth.text = SimpleDateFormat("MMM", Locale.FRENCH).format(d)
            val heure = SimpleDateFormat("HH:mm", Locale.FRENCH).also {
                it.timeZone = TimeZone.getTimeZone("Europe/Paris")
            }.format(d)
            binding.tvNextEventTime.text = "🕐 $heure"
        }.onFailure {
            binding.tvNextEventDay.text   = "--"
            binding.tvNextEventMonth.text = "---"
            binding.tvNextEventTime.text  = ""
        }

        binding.tvNextEventTitle.text = event.titreEvenement
        binding.tvNextEventLieu.text  = if (event.lieuEvent.isNotBlank()) "📍 ${event.lieuEvent}" else ""
        binding.tvNextEventType.text  = event.typeEvenement
    }

    private fun bindLastNews(news: Actualite?) {
        if (news == null) {
            binding.cardLastNews.visibility = View.GONE
            binding.btnVoirTout.visibility  = View.GONE
            binding.tvNewsEmpty.visibility  = View.VISIBLE
            return
        }
        binding.cardLastNews.visibility = View.VISIBLE
        binding.btnVoirTout.visibility  = View.VISIBLE
        binding.tvNewsEmpty.visibility  = View.GONE

        val isEvent = news.typeActualite.lowercase() == "evenement"
        binding.tvNewsBadge.text = if (isEvent) "Événement" else "Article"
        binding.tvNewsBadge.setTextColor(
            if (isEvent) Color.parseColor("#A8FF60") else Color.parseColor("#6CCFFF")
        )

        runCatching {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.FRENCH).parse(news.datePublication.take(10))
            binding.tvNewsDate.text = "Publié le ${SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(d!!)}"
        }.onFailure { binding.tvNewsDate.text = "" }

        binding.tvNewsTitle.text   = news.titre
        binding.tvNewsExcerpt.text = news.contenu

        if (!news.imagePrincipale.isNullOrEmpty()) {
            binding.ivNewsImage.visibility = View.VISIBLE
            Glide.with(this)
                .load("${Constants.BASE_URL.trimEnd('/')}${news.imagePrincipale}")
                .centerCrop()
                .into(binding.ivNewsImage)
        } else {
            binding.ivNewsImage.visibility = View.GONE
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
                    .also { it.timeZone = TimeZone.getTimeZone("UTC") }

                val unreadCount = conversations.count { conv ->
                    val msgAt = conv.lastMessageAt ?: return@count false
                    val msgTime = runCatching { sdf.parse(msgAt)?.time ?: 0L }.getOrDefault(0L)
                    conv.lastMessage != null && msgTime > lastOpenedChat
                }

                if (unreadCount > 0) showUnreadDialog(unreadCount)
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

        dialog.findViewById<android.widget.TextView>(R.id.tvBadge).text =
            if (unreadCount == 1) "1 NOUVEAU MESSAGE" else "$unreadCount NOUVEAUX MESSAGES"
        dialog.findViewById<android.widget.TextView>(R.id.tvTitle).text =
            if (unreadCount == 1) "Tu as un nouveau message !" else "Tu as $unreadCount nouveaux messages !"

        dialog.findViewById<android.widget.TextView>(R.id.btnDismiss).setOnClickListener { dialog.dismiss() }
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
