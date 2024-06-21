package com.capztone.seafishfy.ui.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set OnClickListener on the profile container layout
        binding.containerProfile.setOnClickListener {
            // Navigate to the profile fragment
            findNavController().navigate(R.id.action_profileFragment_to_myProfileFragment)
        }
        binding.containerOrders.setOnClickListener {
            // Navigate to the profile fragment
            findNavController().navigate(R.id.action_profileFragment_to_historyFragment)
        }
        binding.containerLanguage.setOnClickListener {
            // Navigate to the profile fragment
            findNavController().navigate(R.id.action_profileFragment_to_language)
        }
        binding.containerAboutus.setOnClickListener {
            // Navigate to the profile fragment
            findNavController().navigate(R.id.action_profileFragment_to_about)
        }
        binding.containerAddress.setOnClickListener {
            // Navigate to the profile fragment
            findNavController().navigate(R.id.action_profileFragment_to_AddressFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}