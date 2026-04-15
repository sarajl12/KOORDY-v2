package com.koordy.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.koordy.app.MainActivity
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentEventsBinding
import com.koordy.app.databinding.FragmentNewsBinding
import com.koordy.app.ui.association.EventsAdapter
import com.koordy.app.ui.association.NewsAdapter
import kotlinx.coroutines.launch

// ── EventsFragment ─────────────────────────────────────────────────────────────

class EventsFragment : Fragment() {
    private var _b: FragmentEventsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentEventsBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val idAsso = (activity as MainActivity).session.idAssociation
        if (idAsso == -1) return

        b.recyclerEvents.layoutManager = LinearLayoutManager(requireContext())
        b.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getEvents(idAsso)
                if (res.isSuccessful) {
                    val events = res.body() ?: emptyList()
                    b.recyclerEvents.adapter = EventsAdapter(events)
                    b.tvEmpty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur chargement.", Toast.LENGTH_SHORT).show()
            } finally {
                b.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

// ── NewsFragment ───────────────────────────────────────────────────────────────

class NewsFragment : Fragment() {
    private var _b: FragmentNewsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentNewsBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val idAsso = (activity as MainActivity).session.idAssociation
        if (idAsso == -1) return

        b.recyclerNews.layoutManager = LinearLayoutManager(requireContext())
        b.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getNews(idAsso)
                if (res.isSuccessful) {
                    val news = res.body() ?: emptyList()
                    b.recyclerNews.adapter = NewsAdapter(news)
                    b.tvEmpty.visibility = if (news.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur chargement.", Toast.LENGTH_SHORT).show()
            } finally {
                b.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
