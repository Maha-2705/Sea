package com.capztone.seafishfy.ui.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.seafishfy.databinding.FragmentAddressBinding
import com.capztone.seafishfy.ui.activities.ManualLocation
import com.capztone.seafishfy.ui.activities.ManualMap
import com.capztone.seafishfy.ui.activities.adapters.AddressAdapter
import com.google.firebase.auth.FirebaseAuth

class AddressFragment : Fragment() {

    private var _binding: FragmentAddressBinding? = null
    private val binding get() = _binding!!
    private lateinit var addressAdapter: AddressAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddressBinding.inflate(inflater, container, false)

        binding.newAddress.setOnClickListener {
            val intent = Intent(context, ManualMap::class.java)
            startActivity(intent)
        }


        binding.detailGoToBackImageButton.setOnClickListener {
            // Navigate back to the previous destination
            findNavController().popBackStack()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            addressAdapter = AddressAdapter(userId)
            binding.addressrecycler.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = addressAdapter
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
