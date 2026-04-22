package com.koordy.app.ui.home

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.TextView
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
import com.koordy.app.databinding.ItemChecklistItemBinding
import com.koordy.app.models.*
import com.koordy.app.ui.association.ConseilAdapter
import com.koordy.app.utils.Constants
import com.koordy.app.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HomeAssociationFragment : Fragment() {

    companion object {
        private var hasShownUnreadThisSession = false
    }

    private var _binding: FragmentHomeAssociationBinding? = null
    private val binding get() = _binding!!

    private var idAsso = -1
    private lateinit var session: SessionManager

    // Checklist state
    private var allChecklists: List<Checklist> = emptyList()
    private var checklistIndex: Int = 0
    private val currentChecklist get() = allChecklists.getOrNull(checklistIndex)

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

        session = (activity as MainActivity).session
        idAsso  = session.idAssociation

        if (idAsso == -1) {
            findNavController().navigate(R.id.action_homeAssociation_to_rechercheAssociation)
            return
        }

        // RecyclerViews
        binding.recyclerConseil.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Click listeners
        binding.btnVoirTout.setOnClickListener { navigateToAllNews(idAsso) }
        binding.cardLastNews.setOnClickListener { navigateToAllNews(idAsso) }
        binding.btnEditAsso.setOnClickListener {
            findNavController().navigate(
                R.id.action_homeAssociation_to_editAssociation,
                bundleOf("id_association" to idAsso)
            )
        }
        binding.btnNewChecklist.setOnClickListener { showCreateChecklistDialog() }
        binding.btnAddItem.setOnClickListener {
            currentChecklist?.let { showAddItemDialog(it.idChecklist) }
        }
        binding.btnPrevChecklist.setOnClickListener {
            if (checklistIndex > 0) { checklistIndex--; updateChecklistDisplay() }
        }
        binding.btnNextChecklist.setOnClickListener {
            if (checklistIndex < allChecklists.size - 1) { checklistIndex++; updateChecklistDisplay() }
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

    // ── Chargement principal ──────────────────────────────────────────────────

    private fun loadData(idAsso: Int, idMembre: Int) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val assoResp    = runCatching { RetrofitClient.api.getAssociation(idAsso) }.getOrNull()
            val conseilResp = runCatching { RetrofitClient.api.getConseil(idAsso) }.getOrNull()
            val membresResp = runCatching { RetrofitClient.api.getMembres(idAsso) }.getOrNull()
            val eventsResp  = runCatching { RetrofitClient.api.getEvents(idAsso) }.getOrNull()
            val newsResp    = runCatching { RetrofitClient.api.getNews(idAsso) }.getOrNull()
            val adminResp   = if (idMembre != -1)
                runCatching { RetrofitClient.api.isAdmin(idAsso, idMembre) }.getOrNull()
            else null

            // ── Association ───────────────────────────────────────────────
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

            val isPresident = adminResp?.takeIf { it.isSuccessful }?.body()?.isAdmin == true
            binding.btnEditAsso.visibility = if (isPresident) View.VISIBLE else View.GONE

            val membres = membresResp?.takeIf { it.isSuccessful }?.body() ?: emptyList()
            binding.tvStatMembres.text = membres.size.toString()

            val conseil = conseilResp?.takeIf { it.isSuccessful }?.body() ?: emptyList()
            binding.recyclerConseil.adapter = ConseilAdapter(conseil)
            binding.tvConseilEmpty.visibility = if (conseil.isEmpty()) View.VISIBLE else View.GONE

            val events = eventsResp?.takeIf { it.isSuccessful }?.body() ?: emptyList()
            val now = System.currentTimeMillis()
            val upcoming = events.filter { ev ->
                runCatching { (sdfEvent.parse(ev.dateDebutEvent)?.time ?: 0L) >= now }.getOrDefault(false)
            }
            binding.tvStatEvents.text = upcoming.size.toString()
            bindNextEvent(upcoming.firstOrNull())

            val news = (newsResp?.takeIf { it.isSuccessful }?.body() ?: emptyList())
                .filter { it.typeActualite.lowercase() != "evenement" }
            binding.tvStatNews.text = news.size.toString()
            bindLastNews(news.firstOrNull())

            // ── Checklists ────────────────────────────────────────────────
            loadChecklists()

            binding.progressBar.visibility = View.GONE
        }
    }

    // ── Checklist ─────────────────────────────────────────────────────────────

    private suspend fun loadChecklists() {
        val resp = runCatching { RetrofitClient.api.getChecklists(idAsso) }.getOrNull()
        val checklists = resp?.takeIf { it.isSuccessful }?.body() ?: emptyList()
        val prevId = currentChecklist?.idChecklist
        allChecklists = checklists
        checklistIndex = if (prevId != null)
            checklists.indexOfFirst { it.idChecklist == prevId }.takeIf { it >= 0 } ?: 0
        else 0
        updateChecklistDisplay()
    }

    private fun updateChecklistDisplay() {
        val checklist = currentChecklist
        if (checklist == null) {
            binding.cardChecklist.visibility    = View.GONE
            binding.tvChecklistEmpty.visibility = View.VISIBLE
            binding.llChecklistNav.visibility   = View.GONE
            return
        }
        binding.cardChecklist.visibility    = View.VISIBLE
        binding.tvChecklistEmpty.visibility = View.GONE

        // Event info
        binding.tvChecklistEventName.text = checklist.nomEvenement
        binding.tvChecklistDate.text  = if (!checklist.dateEvenement.isNullOrBlank()) "📅 ${checklist.dateEvenement}" else ""
        binding.tvChecklistLieu.text  = if (!checklist.lieuEvenement.isNullOrBlank()) "📍 ${checklist.lieuEvenement}" else ""

        val total = checklist.items.size
        val done  = checklist.items.count { it.isChecked }
        binding.tvChecklistProgress.text = when {
            total == 0    -> "Aucun élément · Ajoutez du matériel à apporter"
            done == total -> "✓ Tout est prêt ! ($total/$total)"
            else          -> "$done/$total éléments cochés"
        }

        renderChecklistItems(checklist.items)

        // Navigation
        if (allChecklists.size > 1) {
            binding.llChecklistNav.visibility = View.VISIBLE
            binding.tvChecklistCount.text = "Checklist ${checklistIndex + 1} / ${allChecklists.size}"
            binding.btnPrevChecklist.alpha = if (checklistIndex > 0) 1f else 0.3f
            binding.btnNextChecklist.alpha = if (checklistIndex < allChecklists.size - 1) 1f else 0.3f
        } else {
            binding.llChecklistNav.visibility = View.GONE
        }
    }

    private fun renderChecklistItems(items: List<ChecklistItem>) {
        val container = binding.llChecklistItems
        container.removeAllViews()
        if (items.isEmpty()) return

        val inflater = LayoutInflater.from(requireContext())
        val sdfIn  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.FRENCH)
            .also { it.timeZone = TimeZone.getTimeZone("UTC") }
        val sdfOut = SimpleDateFormat("dd/MM à HH:mm", Locale.FRENCH)

        items.forEachIndexed { index, item ->
            val b = ItemChecklistItemBinding.inflate(inflater, container, false)

            b.cbItem.isChecked = item.isChecked
            if (item.isChecked) {
                b.tvItemName.paintFlags = b.tvItemName.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                b.tvItemName.alpha = 0.45f
            } else {
                b.tvItemName.paintFlags = b.tvItemName.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                b.tvItemName.alpha = 1f
            }
            b.tvItemName.text = item.nomItem

            val timeStr = try { sdfOut.format(sdfIn.parse(item.createdAt.take(19))!!) } catch (_: Exception) { "" }
            b.tvAuthor.text = if (timeStr.isNotBlank()) "Ajouté par ${item.nomAuteur} · $timeStr"
                              else "Ajouté par ${item.nomAuteur}"

            if (item.commentaire.isNotBlank()) {
                b.tvComment.text = "💬 ${item.commentaire}"
                b.tvComment.visibility = View.VISIBLE
            } else {
                b.tvComment.visibility = View.GONE
            }

            if (item.isChecked && !item.checkedByNom.isNullOrBlank()) {
                val checkedTime = try {
                    item.checkedAt?.let { sdfOut.format(sdfIn.parse(it.take(19))!!) } ?: ""
                } catch (_: Exception) { "" }
                b.tvCheckedBy.text = if (checkedTime.isNotBlank())
                    "✓ Coché par ${item.checkedByNom} · $checkedTime"
                else
                    "✓ Coché par ${item.checkedByNom}"
                b.tvCheckedBy.visibility = View.VISIBLE
            } else {
                b.tvCheckedBy.visibility = View.GONE
            }

            b.cbItem.setOnClickListener { toggleItem(item) }
            b.root.setOnClickListener  { toggleItem(item) }
            container.addView(b.root)

            // Séparateur entre items
            if (index < items.size - 1) {
                val sep = View(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(Color.parseColor("#F0F4F8"))
                }
                container.addView(sep)
            }
        }
    }

    private fun toggleItem(item: ChecklistItem) {
        val cl = currentChecklist ?: return
        lifecycleScope.launch {
            runCatching {
                RetrofitClient.api.toggleChecklistItem(
                    cl.idChecklist, item.idItem,
                    ToggleItemRequest(
                        idMembre  = session.idMembre,
                        nomMembre = "${session.prenomMembre} ${session.nomMembre}".trim()
                    )
                )
            }
            loadChecklists()
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private fun showCreateChecklistDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_create_checklist)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etNom       = dialog.findViewById<EditText>(R.id.et_nom_evenement)
        val etLieu      = dialog.findViewById<EditText>(R.id.et_lieu_evenement)
        val tvDate      = dialog.findViewById<TextView>(R.id.btn_select_date)
        var selectedCal: Calendar? = null

        val sdfDisplay = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH)
        val sdfIso     = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        tvDate.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                TimePickerDialog(requireContext(), { _, h, min ->
                    selectedCal = Calendar.getInstance().apply { set(y, m, d, h, min, 0) }
                    tvDate.text = sdfDisplay.format(selectedCal!!.time)
                    tvDate.setTextColor(Color.parseColor("#1A1A2E"))
                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialog.findViewById<TextView>(R.id.btn_cancel).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<TextView>(R.id.btn_create).setOnClickListener {
            val nom = etNom.text.toString().trim()
            if (nom.isEmpty()) { etNom.error = "Requis"; return@setOnClickListener }
            dialog.dismiss()
            lifecycleScope.launch {
                val resp = runCatching {
                    RetrofitClient.api.createChecklist(idAsso, ChecklistRequest(
                        nomEvenement    = nom,
                        dateEvenement   = selectedCal?.let { sdfDisplay.format(it.time) },
                        dateEvenementTs = selectedCal?.let { sdfIso.format(it.time) },
                        lieuEvenement   = etLieu.text.toString().trim().takeIf { it.isNotBlank() }
                    ))
                }.getOrNull()
                if (resp?.isSuccessful == true) {
                    val newId = resp.body()?.idChecklist
                    val newResp = runCatching { RetrofitClient.api.getChecklists(idAsso) }.getOrNull()
                    val list = newResp?.takeIf { it.isSuccessful }?.body() ?: emptyList()
                    allChecklists = list
                    checklistIndex = list.indexOfFirst { it.idChecklist == newId }.takeIf { it >= 0 } ?: 0
                    updateChecklistDisplay()
                }
            }
        }
        dialog.show()
    }

    private fun showAddItemDialog(idChecklist: Int) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_checklist_item)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etNom     = dialog.findViewById<EditText>(R.id.et_nom_item)
        val etComment = dialog.findViewById<EditText>(R.id.et_commentaire)

        dialog.findViewById<TextView>(R.id.btn_cancel).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<TextView>(R.id.btn_add).setOnClickListener {
            val nom = etNom.text.toString().trim()
            if (nom.isEmpty()) { etNom.error = "Requis"; return@setOnClickListener }
            dialog.dismiss()
            lifecycleScope.launch {
                runCatching {
                    RetrofitClient.api.addChecklistItem(idChecklist, ChecklistItemRequest(
                        idAuteur    = session.idMembre,
                        nomAuteur   = "${session.prenomMembre} ${session.nomMembre}".trim(),
                        nomItem     = nom,
                        commentaire = etComment.text.toString().trim()
                    ))
                }
                // suspend → attend la fin avant d'afficher
                loadChecklists()
            }
        }
        dialog.show()
    }

    // ── Événement ─────────────────────────────────────────────────────────────

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

    // ── Actualité ─────────────────────────────────────────────────────────────

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

    // ── Messages non lus ──────────────────────────────────────────────────────

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

                if (unreadCount > 0 && !hasShownUnreadThisSession) {
                    hasShownUnreadThisSession = true
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

        dialog.findViewById<TextView>(R.id.tvBadge).text =
            if (unreadCount == 1) "1 NOUVEAU MESSAGE" else "$unreadCount NOUVEAUX MESSAGES"
        dialog.findViewById<TextView>(R.id.tvTitle).text =
            if (unreadCount == 1) "Tu as un nouveau message !" else "Tu as $unreadCount nouveaux messages !"

        dialog.findViewById<TextView>(R.id.btnDismiss).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<TextView>(R.id.btnGoToChat).setOnClickListener {
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
