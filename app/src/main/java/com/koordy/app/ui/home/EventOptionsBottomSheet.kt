package com.koordy.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.koordy.app.MainActivity
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.BottomSheetEventOptionsBinding
import com.koordy.app.databinding.ItemMemberSelectBinding
import com.koordy.app.models.ConseilMembre
import com.koordy.app.models.ConversationRequest
import com.koordy.app.models.EvenementAvecStatut
import com.koordy.app.models.MessageRequest
import com.koordy.app.utils.Constants
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class EventOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _b: BottomSheetEventOptionsBinding? = null
    private val b get() = _b!!

    var onEventUpdated: (() -> Unit)? = null

    companion object {
        const val TAG = "event_options"
        private const val ARG_EVENT = "event"

        fun newInstance(ev: EvenementAvecStatut): EventOptionsBottomSheet {
            return EventOptionsBottomSheet().apply {
                arguments = Bundle().apply { putSerializable(ARG_EVENT, ev) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getEvent(): EvenementAvecStatut =
        requireArguments().getSerializable(ARG_EVENT) as EvenementAvecStatut

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = BottomSheetEventOptionsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ev = getEvent()

        bindHeader(ev)

        b.btnModifier.setOnClickListener {
            dismiss()
            EditEventBottomSheet.newInstance(ev).also { sheet ->
                sheet.onEventUpdated = { onEventUpdated?.invoke() }
            }.show(parentFragmentManager, EditEventBottomSheet.TAG)
        }

        b.btnEnvoyerChat.setOnClickListener {
            if (b.sectionMembres.visibility == View.VISIBLE) {
                b.sectionMembres.visibility = View.GONE
            } else {
                b.sectionMembres.visibility = View.VISIBLE
                loadMembres(ev)
            }
        }
    }

    private fun bindHeader(ev: EvenementAvecStatut) {
        b.tvOptsTitle.text = ev.titreEvenement
        b.tvOptsType.text = ev.typeEvenement

        val sdfIn  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.FRENCH)
            .also { it.timeZone = TimeZone.getTimeZone("UTC") }
        val sdfOut = SimpleDateFormat("dd MMM · HH:mm", Locale.FRENCH)
        b.tvOptsDate.text = try { sdfOut.format(sdfIn.parse(ev.dateDebutEvent)!!) } catch (_: Exception) { "" }

        if (ev.lieuEvent.isNotBlank()) {
            b.tvOptsLieu.text = "📍 ${ev.lieuEvent}"
            b.tvOptsLieu.visibility = View.VISIBLE
        } else {
            b.tvOptsLieu.visibility = View.GONE
        }
    }

    private fun loadMembres(ev: EvenementAvecStatut) {
        val session = (activity as MainActivity).session
        val idAsso = session.idAssociation
        val idMe = session.idMembre

        b.progressMembres.visibility = View.VISIBLE
        b.rvMembresSend.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMembres(idAsso)
                if (res.isSuccessful) {
                    val membres = (res.body() ?: emptyList()).filter { it.idMembre != idMe }
                    if (membres.isEmpty()) {
                        b.tvMembresEmpty.visibility = View.VISIBLE
                    } else {
                        b.rvMembresSend.adapter = SendAdapter(membres, ev, idMe)
                    }
                } else {
                    b.tvMembresEmpty.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                b.tvMembresEmpty.visibility = View.VISIBLE
            } finally {
                b.progressMembres.visibility = View.GONE
            }
        }
    }

    private fun sendInvitation(membre: ConseilMembre, ev: EvenementAvecStatut, idMe: Int) {
        val session = (activity as MainActivity).session
        val idAsso = session.idAssociation

        lifecycleScope.launch {
            try {
                val convRes = RetrofitClient.api.createConversation(
                    ConversationRequest(
                        idAssociation = idAsso,
                        idInitiateur = idMe,
                        type = "direct",
                        idDestinataire = membre.idMembre
                    )
                )
                if (!convRes.isSuccessful) {
                    Toast.makeText(requireContext(), "Erreur de conversation.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val idConv = convRes.body()?.idConversation ?: return@launch
                RetrofitClient.api.sendMessage(
                    idConv,
                    MessageRequest(
                        idAuteur = idMe,
                        contenu = ev.titreEvenement,
                        typeMessage = "invitation",
                        idEvenement = ev.idEvenement
                    )
                )
                Toast.makeText(requireContext(), "Invitation envoyée à ${membre.prenom} !", Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    inner class SendAdapter(
        private val items: List<ConseilMembre>,
        private val ev: EvenementAvecStatut,
        private val idMe: Int
    ) : RecyclerView.Adapter<SendAdapter.VH>() {

        inner class VH(val b: ItemMemberSelectBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemMemberSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = items[position]
            holder.b.tvMemberName.text = "${m.prenom} ${m.nom}"
            if (m.photoMembre.isNotEmpty()) {
                holder.b.ivAvatar.visibility = View.VISIBLE
                holder.b.tvAvatar.visibility = View.GONE
                Glide.with(holder.b.root.context)
                    .load("${Constants.BASE_URL.trimEnd('/')}${m.photoMembre}")
                    .transform(CircleCrop())
                    .into(holder.b.ivAvatar)
            } else {
                holder.b.ivAvatar.visibility = View.GONE
                holder.b.tvAvatar.visibility = View.VISIBLE
                holder.b.tvAvatar.text = m.prenom.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            }
            holder.b.cbSelected.visibility = View.GONE
            holder.b.root.setOnClickListener { sendInvitation(m, ev, idMe) }
        }

        override fun getItemCount() = items.size
    }
}
