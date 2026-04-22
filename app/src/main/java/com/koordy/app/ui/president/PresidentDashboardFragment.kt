package com.koordy.app.ui.president

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.koordy.app.MainActivity
import com.koordy.app.R
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentPresidentDashboardBinding
import com.koordy.app.databinding.ItemEquipePresidentBinding
import com.koordy.app.databinding.ItemMembrePresidentBinding
import com.koordy.app.models.*
import com.koordy.app.utils.Constants
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class PresidentDashboardFragment : Fragment() {

    private var _binding: FragmentPresidentDashboardBinding? = null
    private val binding get() = _binding!!

    private var allMembres: List<ConseilMembre> = emptyList()
    private var idAssociation: Int = 0
    private var idMembre: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPresidentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = (activity as MainActivity).session
        idMembre = session.idMembre
        idAssociation = session.idAssociation

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnSendAnnonce.setOnClickListener {
            showAnnonceDialog(idAssociation, idMembre)
        }

        // Créer une équipe : toujours disponible, utilise allMembres au moment du clic
        binding.btnCreerEquipe.setOnClickListener {
            CreateEquipeBottomSheet.newInstance(idAssociation, allMembres)
                .show(childFragmentManager, "create_equipe")
        }

        // Rafraîchir la liste des équipes après création ou modification
        childFragmentManager.setFragmentResultListener("equipe_updated", viewLifecycleOwner) { _, _ ->
            loadEquipes(idAssociation)
        }

        // Créer une actualité : navigue vers la page de création
        binding.btnCreerActualite.setOnClickListener {
            findNavController().navigate(R.id.action_presidentDashboard_to_createActualite)
        }

        loadAssociation(idAssociation)
        loadMembres(idAssociation)
        loadEvents(idAssociation)
        loadEquipes(idAssociation)
    }

    // ── Chargement des données ────────────────────────────────────────────────

    private fun loadAssociation(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getAssociation(id)
                if (res.isSuccessful) binding.tvAssoName.text = res.body()?.nom ?: ""
            } catch (_: Exception) {}
        }
    }

    private fun loadMembres(idAssociation: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMembres(idAssociation)
                if (res.isSuccessful) {
                    allMembres = res.body() ?: emptyList()
                    binding.tvStatMembres.text = allMembres.size.toString()
                    binding.tvMembresCount.text = "${allMembres.size} membres"
                    setupMembresAdapter(idAssociation)
                } else {
                    binding.tvNoMembres.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Erreur chargement membres.", Toast.LENGTH_SHORT).show()
                binding.tvNoMembres.visibility = View.VISIBLE
            }
        }
    }

    private fun loadEvents(idAssociation: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getEvents(idAssociation)
                if (res.isSuccessful) {
                    val events = res.body() ?: emptyList()
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val now = Calendar.getInstance()

                    val monthCount = events.count { ev ->
                        try {
                            val d = sdf.parse(ev.dateDebutEvent) ?: return@count false
                            val c = Calendar.getInstance().apply { time = d }
                            c.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                            c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        } catch (_: Exception) { false }
                    }
                    binding.tvStatEvents.text = monthCount.toString()

                    // KPI "À venir" : événements avec date > maintenant
                    val upcomingCount = events.count { ev ->
                        try {
                            val d = sdf.parse(ev.dateDebutEvent) ?: return@count false
                            d.after(now.time)
                        } catch (_: Exception) { false }
                    }
                    binding.tvStatUpcoming.text = upcomingCount.toString()
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadEquipes(idAssociation: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getAssociationEquipes(idAssociation)
                if (res.isSuccessful) {
                    val equipes = res.body() ?: emptyList()
                    binding.tvEquipesCount.text = "${equipes.size} équipe${if (equipes.size > 1) "s" else ""}"
                    if (equipes.isEmpty()) {
                        binding.tvNoEquipes.visibility = View.VISIBLE
                        binding.rvEquipes.visibility = View.GONE
                    } else {
                        binding.tvNoEquipes.visibility = View.GONE
                        binding.rvEquipes.visibility = View.VISIBLE
                        setupEquipesAdapter(equipes)
                    }
                } else {
                    binding.tvNoEquipes.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                binding.tvNoEquipes.visibility = View.VISIBLE
            }
        }
    }

    // ── Adapters ──────────────────────────────────────────────────────────────

    private fun setupMembresAdapter(idAssociation: Int) {
        if (allMembres.isEmpty()) {
            binding.tvNoMembres.visibility = View.VISIBLE
            binding.rvMembres.visibility = View.GONE
            return
        }
        binding.tvNoMembres.visibility = View.GONE
        binding.rvMembres.visibility = View.VISIBLE
        binding.rvMembres.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMembres.adapter = MembrePresidentAdapter(
            items = allMembres,
            onItemClick = { membre -> showMembreDetail(membre) },
            onRoleClick = { membre -> showRoleChangeDialog(membre, idAssociation) }
        )
    }

    private fun showMembreDetail(membre: ConseilMembre) {
        MembreDetailBottomSheet.newInstance(
            nom = membre.nom,
            prenom = membre.prenom,
            role = membre.role.ifEmpty { "Membre" },
            email = membre.email,
            age = membre.age,
            dateAdhesion = membre.dateAdhesion
        ).show(childFragmentManager, "membre_detail")
    }

    private fun setupEquipesAdapter(equipes: List<EquipeDetail>) {
        binding.rvEquipes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEquipes.isNestedScrollingEnabled = false
        binding.rvEquipes.adapter = EquipePresidentAdapter(equipes) { equipe ->
            EditEquipeBottomSheet.newInstance(
                equipe.idEquipe,
                equipe.nomEquipe,
                idAssociation,
                allMembres
            ).show(childFragmentManager, "edit_equipe")
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun showRoleChangeDialog(membre: ConseilMembre, idAssociation: Int) {
        val roles = arrayOf("Président", "Trésorier", "Secrétaire", "Adjoint", "Membre")
        var selected = roles.indexOfFirst { it.equals(membre.role, ignoreCase = true) }.coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setIcon(R.drawable.logo)
            .setTitle("Rôle de ${membre.prenom} ${membre.nom}")
            .setSingleChoiceItems(roles, selected) { _, which -> selected = which }
            .setPositiveButton("Confirmer") { _, _ ->
                changeRole(membre.idMembre, idAssociation, roles[selected])
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun changeRole(idMembreTarget: Int, idAssociation: Int, role: String) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.updateMembreRole(idMembreTarget, UpdateRoleRequest(role, idAssociation))
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Rôle mis à jour.", Toast.LENGTH_SHORT).show()
                    loadMembres(idAssociation)
                } else {
                    Toast.makeText(requireContext(), "Erreur mise à jour du rôle.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAnnonceDialog(idAssociation: Int, idMembre: Int) {
        val etMessage = EditText(requireContext()).apply {
            hint = "Votre annonce…"
            minLines = 3
            maxLines = 6
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 16, 56, 0)
            addView(etMessage)
        }

        AlertDialog.Builder(requireContext())
            .setIcon(R.drawable.logo)
            .setTitle("Envoyer une annonce")
            .setMessage("L'annonce sera envoyée à tous les membres de l'association.")
            .setView(container)
            .setPositiveButton("Envoyer") { _, _ ->
                val msg = etMessage.text.toString().trim()
                if (msg.isNotEmpty()) sendAnnonce(idAssociation, idMembre, msg)
                else Toast.makeText(requireContext(), "Message vide.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun sendAnnonce(idAssociation: Int, idMembre: Int, message: String) {
        lifecycleScope.launch {
            try {
                val participantIds = allMembres.map { it.idMembre }
                val convReq = ConversationRequest(
                    idAssociation = idAssociation,
                    idInitiateur = idMembre,
                    type = "group",
                    nom = "Annonce",
                    participants = participantIds
                )
                val convRes = RetrofitClient.api.createConversation(convReq)
                if (convRes.isSuccessful) {
                    val idConv = convRes.body()!!.idConversation
                    RetrofitClient.api.sendMessage(idConv, MessageRequest(idAuteur = idMembre, contenu = message))
                    Toast.makeText(requireContext(), "Annonce envoyée à tous les membres !", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Erreur lors de l'envoi.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Adapter Membres ───────────────────────────────────────────────────────

    inner class MembrePresidentAdapter(
        private val items: List<ConseilMembre>,
        private val onItemClick: (ConseilMembre) -> Unit,
        private val onRoleClick: (ConseilMembre) -> Unit
    ) : RecyclerView.Adapter<MembrePresidentAdapter.VH>() {

        inner class VH(val b: ItemMembrePresidentBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemMembrePresidentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = items[position]
            holder.b.tvNom.text = "${m.prenom} ${m.nom}"
            holder.b.tvRole.text = m.role.ifEmpty { "Membre" }
            holder.b.root.setOnClickListener { onItemClick(m) }
            holder.b.btnChangeRole.setOnClickListener { onRoleClick(m) }
            if (m.photoMembre.isNotEmpty()) {
                holder.b.ivAvatar.visibility = View.VISIBLE
                holder.b.tvInitiale.visibility = View.GONE
                Glide.with(holder.b.root.context)
                    .load("${Constants.BASE_URL.trimEnd('/')}${m.photoMembre}")
                    .transform(CircleCrop())
                    .into(holder.b.ivAvatar)
            } else {
                holder.b.ivAvatar.visibility = View.GONE
                holder.b.tvInitiale.visibility = View.VISIBLE
                holder.b.tvInitiale.text = m.prenom.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            }
        }

        override fun getItemCount() = items.size
    }

    // ── Adapter Équipes ───────────────────────────────────────────────────────

    inner class EquipePresidentAdapter(
        private val items: List<EquipeDetail>,
        private val onModifierClick: (EquipeDetail) -> Unit
    ) : RecyclerView.Adapter<EquipePresidentAdapter.VH>() {

        inner class VH(val b: ItemEquipePresidentBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemEquipePresidentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = items[position]
            holder.b.tvEquipeNom.text = e.nomEquipe
            holder.b.tvEquipeMembres.text = "${e.nbMembres} membre${if (e.nbMembres > 1) "s" else ""}"
            holder.b.btnModifierEquipe.setOnClickListener { onModifierClick(e) }
        }

        override fun getItemCount() = items.size
    }
}
