package com.koordy.app.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.koordy.app.MainActivity
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.BottomSheetNewConversationBinding
import com.koordy.app.databinding.ItemEquipeSelectBinding
import com.koordy.app.databinding.ItemMemberSelectBinding
import com.koordy.app.models.ConseilMembre
import com.koordy.app.models.ConversationRequest
import com.koordy.app.models.EquipeDetail
import com.koordy.app.utils.Constants
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class NewConversationBottomSheet : BottomSheetDialogFragment() {

    private var _b: BottomSheetNewConversationBinding? = null
    private val b get() = _b!!

    var onConversationStarted: ((idConv: Int, name: String, type: String) -> Unit)? = null

    private var allMembres: List<ConseilMembre> = emptyList()
    private lateinit var membreAdapter: MembreConvAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = BottomSheetNewConversationBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val h = resources.displayMetrics.heightPixels
        sheet.layoutParams.height = (h * 0.88).toInt()
        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMembresRv()
        setupSearch()
        loadData()
    }

    private fun setupMembresRv() {
        membreAdapter = MembreConvAdapter(emptyList()) { membre ->
            startDirectConversation(membre)
        }
        b.rvMembres.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = membreAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSearch() {
        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim() ?: ""
                val filtered = if (q.isEmpty()) allMembres
                else allMembres.filter { "${it.prenom} ${it.nom}".contains(q, ignoreCase = true) }
                membreAdapter.update(filtered)
                b.tvNoMembres.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
        })
    }

    private fun loadData() {
        val session = (activity as MainActivity).session
        val idAsso = session.idAssociation
        val idMembre = session.idMembre

        b.progressEquipes.visibility = View.VISIBLE
        b.progressMembres.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val equipesDeferred = async { RetrofitClient.api.getAssociationEquipes(idAsso) }
                val membresDeferred = async { RetrofitClient.api.getMembres(idAsso) }

                val equipesRes = equipesDeferred.await()
                val membresRes = membresDeferred.await()

                if (equipesRes.isSuccessful) {
                    val equipes = equipesRes.body() ?: emptyList()
                    setupEquipes(equipes)
                }
                b.progressEquipes.visibility = View.GONE

                if (membresRes.isSuccessful) {
                    allMembres = (membresRes.body() ?: emptyList()).filter { it.idMembre != idMembre }
                    membreAdapter.update(allMembres)
                    b.tvNoMembres.visibility = if (allMembres.isEmpty()) View.VISIBLE else View.GONE
                }
                b.progressMembres.visibility = View.GONE

            } catch (_: Exception) {
                b.progressEquipes.visibility = View.GONE
                b.progressMembres.visibility = View.GONE
                Toast.makeText(requireContext(), "Erreur de connexion", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupEquipes(equipes: List<EquipeDetail>) {
        if (equipes.isEmpty()) {
            b.sectionEquipes.visibility = View.GONE
            return
        }
        b.tvNoEquipes.visibility = View.GONE
        b.rvEquipes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = EquipeConvAdapter(equipes) { equipe -> startEquipeConversation(equipe) }
            isNestedScrollingEnabled = false
        }
    }

    private fun startEquipeConversation(equipe: EquipeDetail) {
        val session = (activity as MainActivity).session
        lifecycleScope.launch {
            try {
                val membresRes = RetrofitClient.api.getEquipeMembres(equipe.idEquipe)
                if (!membresRes.isSuccessful) {
                    Toast.makeText(requireContext(), "Erreur chargement équipe", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val membres = membresRes.body() ?: emptyList()
                if (membres.isEmpty()) {
                    Toast.makeText(requireContext(), "Cette équipe n'a aucun membre", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val convRes = RetrofitClient.api.createConversation(
                    ConversationRequest(
                        idAssociation = session.idAssociation,
                        idInitiateur = session.idMembre,
                        type = "group",
                        nom = equipe.nomEquipe,
                        participants = membres.map { it.idMembre }
                    )
                )
                if (convRes.isSuccessful) {
                    val idConv = convRes.body()?.idConversation ?: return@launch
                    onConversationStarted?.invoke(idConv, equipe.nomEquipe, "group")
                    dismiss()
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Erreur de connexion", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startDirectConversation(membre: ConseilMembre) {
        val session = (activity as MainActivity).session
        lifecycleScope.launch {
            try {
                val convRes = RetrofitClient.api.createConversation(
                    ConversationRequest(
                        idAssociation = session.idAssociation,
                        idInitiateur = session.idMembre,
                        type = "direct",
                        idDestinataire = membre.idMembre
                    )
                )
                if (convRes.isSuccessful) {
                    val idConv = convRes.body()?.idConversation ?: return@launch
                    onConversationStarted?.invoke(idConv, "${membre.prenom} ${membre.nom}", "direct")
                    dismiss()
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Erreur de connexion", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    // ── Adapter Équipes ───────────────────────────────────────────────────────

    inner class EquipeConvAdapter(
        private val items: List<EquipeDetail>,
        private val onClick: (EquipeDetail) -> Unit
    ) : RecyclerView.Adapter<EquipeConvAdapter.VH>() {

        inner class VH(val b: ItemEquipeSelectBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemEquipeSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = items[position]
            holder.b.tvEquipeName.text = e.nomEquipe
            holder.b.tvEquipeCount.text = "${e.nbMembres} membre${if (e.nbMembres > 1) "s" else ""}"
            holder.b.cbEquipeSelected.visibility = View.GONE
            holder.b.root.setOnClickListener { onClick(e) }
        }
    }

    // ── Adapter Membres ───────────────────────────────────────────────────────

    inner class MembreConvAdapter(
        private var items: List<ConseilMembre>,
        private val onClick: (ConseilMembre) -> Unit
    ) : RecyclerView.Adapter<MembreConvAdapter.VH>() {

        inner class VH(val b: ItemMemberSelectBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemMemberSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = items[position]
            val b = holder.b
            if (m.photoMembre.isNotEmpty()) {
                b.ivAvatar.visibility = View.VISIBLE
                b.tvAvatar.visibility = View.GONE
                Glide.with(b.root.context)
                    .load("${Constants.BASE_URL.trimEnd('/')}${m.photoMembre}")
                    .circleCrop()
                    .into(b.ivAvatar)
            } else {
                b.ivAvatar.visibility = View.GONE
                b.tvAvatar.visibility = View.VISIBLE
                b.tvAvatar.text = m.prenom.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            }
            b.tvMemberName.text = "${m.prenom} ${m.nom}"
            b.tvMemberRole.text = m.role.ifBlank { "Membre" }
            b.cbSelected.visibility = View.GONE
            b.root.setOnClickListener { onClick(m) }
        }

        fun update(newItems: List<ConseilMembre>) {
            items = newItems
            notifyDataSetChanged()
        }
    }

    companion object {
        const val TAG = "NewConversationBottomSheet"
    }
}
