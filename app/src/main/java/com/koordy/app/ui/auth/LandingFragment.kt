package com.koordy.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.koordy.app.R
import com.koordy.app.databinding.FragmentLandingBinding

class LandingFragment : Fragment() {

    private var _binding: FragmentLandingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLandingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConnexion.setOnClickListener {
            findNavController().navigate(R.id.action_landing_to_login)
        }

        binding.btnInscription.setOnClickListener {
            findNavController().navigate(R.id.action_landing_to_inscription)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
