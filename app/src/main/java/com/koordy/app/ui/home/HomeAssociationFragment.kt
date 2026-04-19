package com.koordy.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
