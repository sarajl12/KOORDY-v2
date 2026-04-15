package com.koordy.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.koordy.app.R
import com.koordy.app.databinding.FragmentSuccessAssociationBinding

class SuccessAssociationFragment : Fragment() {

    private var _binding: FragmentSuccessAssociationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSuccessAssociationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnGoToSpace.setOnClickListener {
            findNavController().navigate(R.id.action_successAssociation_to_homeAssociation)
        }
        binding.btnGoHome.setOnClickListener {
            findNavController().navigate(R.id.action_successAssociation_to_homeAssociation)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
