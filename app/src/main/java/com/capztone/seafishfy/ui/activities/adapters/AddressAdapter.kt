package com.capztone.seafishfy.ui.activities.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.capztone.seafishfy.databinding.ItemSavedAddressBinding
import com.capztone.seafishfy.ui.activities.models.Address
import com.google.firebase.database.*

class AddressAdapter(private val userId: String) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    private val addresses = mutableListOf<Address>()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("Locations").child(userId)

    init {
        fetchData()
    }

    private fun fetchData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                addresses.clear()
                val addressString = snapshot.child("address").getValue(String::class.java)
                val defaultAddressString = snapshot.child("Default Address").getValue(String::class.java)

                if (addressString != null) {
                    addresses.add(Address(addressString))
                } else if (defaultAddressString != null) {
                    addresses.add(Address(defaultAddressString))
                }
                notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val binding = ItemSavedAddressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(addresses[position])
    }

    override fun getItemCount() = addresses.size

    inner class AddressViewHolder(private val binding: ItemSavedAddressBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(address: Address) {
            binding.address.setText(address.address)
            binding.Edit.setOnClickListener {
                showEditDialog(address.address)
            }
            binding.Delete.setOnClickListener {
                showDeleteConfirmationDialog()
            }
        }

        private fun showEditDialog(currentAddress: String) {
            val editTextAddress = EditText(binding.root.context)
            editTextAddress.setText(currentAddress)

            AlertDialog.Builder(binding.root.context)
                .setTitle("Edit Address")
                .setView(editTextAddress)
                .setPositiveButton("Save") { dialog, _ ->
                    val newAddress = editTextAddress.text.toString()
                    updateAddressInFirebase(newAddress)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        private fun updateAddressInFirebase(newAddress: String) {
            val addressMap = mapOf("address" to newAddress)
            database.updateChildren(addressMap).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Update successful
                } else {
                    // Handle the error
                }
            }
        }

        private fun showDeleteConfirmationDialog() {
            AlertDialog.Builder(binding.root.context)
                .setTitle("Delete Address")
                .setMessage("Are you sure you want to delete this address?")
                .setPositiveButton("Yes") { dialog, _ ->
                    deleteAddressFromFirebase()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        private fun deleteAddressFromFirebase() {
            database.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Delete successful
                } else {
                    // Handle the error
                }
            }
        }
    }
}