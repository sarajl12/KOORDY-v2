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
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentAllNewsBinding
import com.koordy.app.ui.association.NewsAdapter
import kotlinx.coroutines.launch

class AllNewsFragment : Fragment() {

    private var _binding: FragmentAllNewsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val idAsso = arguments?.getInt("id_association", -1) ?: -1

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        if (idAsso == -1) {
            findNavController().popBackStack()
            return
        }

        binding.recyclerAllNews.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            try {
                binding.loadingOverlay.visibility = View.VISIBLE
                val resp = RetrofitClient.api.getNews(idAsso)
                if (resp.isSuccessful) {
                    val news = (resp.body() ?: emptyList())
                        .filter { it.typeActualite.lowercase() != "evenement" }
                    binding.recyclerAllNews.adapter = NewsAdapter(news)
                    binding.tvEmpty.visibility =
                        if (news.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Toast.makeText(requireContext(), "Erreur de chargement.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.loadingOverlay.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
