package com.capztone.seafishfy.ui.activities.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.FragmentProfileBinding
import com.capztone.seafishfy.ui.activities.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient


    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = Firebase.auth
        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_Id)) // Replace with your web client ID
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
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
        binding.containerLogout.setOnClickListener {
            // Show confirmation dialog
            showLogoutConfirmationDialog()
        }
    }
    private fun showLogoutConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout_confirmation, null)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)

        val alertDialog = dialogBuilder.create()

        dialogView.findViewById<View>(R.id.btnDialogYes).setOnClickListener {
            // Perform logout action
            performLogout()
            alertDialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnDialogNo).setOnClickListener {
            // Dismiss the dialog
            alertDialog.dismiss()
        }

        alertDialog.show()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
    private fun performLogout() {
        // Firebase sign out
        auth.signOut()

        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(requireActivity()) {
            // Navigate to login activity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}