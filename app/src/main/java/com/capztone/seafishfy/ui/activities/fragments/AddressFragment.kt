package com.capztone.seafishfy.ui.activities.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.seafishfy.databinding.FragmentAddressBinding
import com.capztone.seafishfy.ui.activities.ManualLocation
import com.capztone.seafishfy.ui.activities.adapters.AddressAdapter
import com.capztone.seafishfy.ui.activities.models.Address
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddressFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddressBinding? = null
    private val binding get() = _binding!!
    private lateinit var addressAdapter: AddressAdapter
    private var selectedAddress: String? = null
    interface AddressSelectionListener {
        fun onAddressSelected(address: String)
    }

    private var listener: AddressSelectionListener? = null

    fun setAddressSelectionListener(listener: AddressSelectionListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize AddressAdapter
        addressAdapter = AddressAdapter() { address ->
            selectedAddress = address.toString()
        }
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = android.graphics.Color.WHITE
            }
        }

        // Set up RecyclerView
        binding.addressrecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = addressAdapter
        }

        // Handle click on new_address RelativeLayout
        binding.newAddress.setOnClickListener {
            // Open ManualLocationActivity
            startActivity(Intent(context, ManualLocation::class.java))
        }
        // Handle click on Address_comfirm button
        // Handle click on Address_confirm button
        binding.AddressComfirm.setOnClickListener {
            // Check if selectedAddress is null or empty
            if (selectedAddress.isNullOrEmpty()) {
                // Retrieve the address at position 0 if available
                val firstAddress = addressAdapter.getFirstAddress()
                if (firstAddress != null) {
                    storeAddressInFirebase(firstAddress.address)
                } else {
                    Toast.makeText(context, "Please select an address", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Store the selected address in Firebase
                storeAddressInFirebase(selectedAddress!!)
            }
        }
    }
    // Helper function to get the address at position 0 from adapter
    private fun AddressAdapter.getFirstAddress(): Address? {
        return if (addresses.isNotEmpty()) {
            addresses[0]
        } else {
            null
        }
    }
    private fun storeAddressInFirebase(address: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val databaseReference = FirebaseDatabase.getInstance().getReference("PayoutAddress").child(uid).child("address")

            // Store the address directly
            databaseReference.setValue(address)
                .addOnSuccessListener {
                    Toast.makeText(context, "Address stored successfully", Toast.LENGTH_SHORT).show()
                    // Optionally, navigate to the next screen or perform other actions
                }
                .addOnFailureListener { e ->
                    Log.e("AddressFragment", "Error storing address", e)
                    Toast.makeText(context, "Failed to store address", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        dialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        dialog?.behavior?.skipCollapsed = true
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}