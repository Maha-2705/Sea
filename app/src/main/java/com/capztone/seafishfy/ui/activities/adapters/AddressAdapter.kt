package com.capztone.seafishfy.ui.activities.adapters

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.capztone.seafishfy.databinding.ItemSavedAddressBinding
import com.capztone.seafishfy.ui.activities.models.Address
import com.capztone.seafishfy.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AddressAdapter(private val onItemClick: (String) -> Unit) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    val addresses = mutableListOf<Address>()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val database: DatabaseReference? =
        userId?.let { FirebaseDatabase.getInstance().getReference("Locations").child(it) }

    private var selectedPosition = 0

    init {
        fetchData()
    }

    private fun fetchData() {
        val paths = listOf("HOME", "WORK", "OTHER")
        paths.forEach { path ->
            fetchAddressesFromPath(path)
        }
    }

    private fun fetchAddressesFromPath(path: String) {
        database?.child(path)?.child("address")?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("AddressAdapter", "Data fetched for path: $path")
                val address = snapshot.getValue(String::class.java)
                if (address != null) {
                    Log.d("AddressAdapter", "Address found: $address")
                    val existingAddress = addresses.find { it.addressType == path }
                    if (existingAddress != null) {
                        existingAddress.address = address
                    } else {
                        addresses.add(Address(path, address))
                    }
                    notifyDataSetChanged()
                } else {
                    Log.d("AddressAdapter", "No address found for path: $path")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AddressAdapter", "Error fetching data for path: $path", error.toException())
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

        fun getSelectedAddress(): Address? {
            return if (selectedPosition != RecyclerView.NO_POSITION) {
                addresses[selectedPosition]
            } else {
                null
            }
        }
        init {
            binding.selectaddress.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    selectedPosition = adapterPosition
                    notifyDataSetChanged()
                    onItemClick(addresses[selectedPosition].address)
                }
            }
        }

        fun bind(address: Address) {
            binding.address.setText(address.address)

            // Set icon based on address type
            when (address.addressType) {
                "HOME" -> {
                    binding.Home.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(binding.root.context, R.drawable.home)?.apply {
                            setTintList(ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.navy)))
                        },
                        null,
                        null,
                        null
                    )
                }                "WORK" -> binding.Home.setCompoundDrawablesWithIntrinsicBounds(R.drawable.work, 0, 0, 0)
                "OTHER" -> binding.Home.setCompoundDrawablesWithIntrinsicBounds(R.drawable.loco, 0, 0, 0)
                else -> binding.Home.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0) // Default case
            }

            binding.Home.text = address.addressType
            binding.selectaddress.isChecked = adapterPosition == selectedPosition

            binding.selectaddress.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    selectedPosition = pos
                    notifyDataSetChanged()
                    onItemClick(addresses[selectedPosition].address)
                }
            }

            binding.Edit.setOnClickListener {
                showEditDialog(address.addressType, address.address)
            }

            binding.Delete.setOnClickListener {
                showDeleteConfirmationDialog(address.addressType)
            }
        }


        private fun showEditDialog(addressType: String, currentAddress: String) {
            val context = binding.root.context
            val layoutInflater = LayoutInflater.from(context)
            val customLayout = layoutInflater.inflate(R.layout.dialog_edit_address, null)

            val editTextAddress = customLayout.findViewById<EditText>(R.id.edit_text_address)
            val buttonSave = customLayout.findViewById<AppCompatButton>(R.id.button_save)
            val buttonCancel = customLayout.findViewById<AppCompatButton>(R.id.button_cancel)

            editTextAddress.setText(currentAddress)

            val dialog = AlertDialog.Builder(context)
                .setView(customLayout)
                .create()

            buttonSave.setOnClickListener {
                val newAddress = editTextAddress.text.toString()
                updateAddressInFirebase(addressType, newAddress)
                updateAddressInFirebase1(addressType, newAddress)
                dialog.dismiss()
            }

            buttonCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        private fun updateAddressInFirebase(addressType: String, newAddress: String) {
            database?.child(addressType)?.child("address")?.setValue(newAddress)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        addresses.find { it.addressType == addressType }?.address = newAddress
                        notifyDataSetChanged()
                    } else {
                        Log.e("AddressAdapter", "Error updating address for path: $addressType", task.exception)
                    }
                }
        }

        private fun updateAddressInFirebase1(addressType: String, newAddress: String) {
            database?.child("PayoutAddress")?.setValue(newAddress)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AddressAdapter", "Address stored under PayoutAddress: $newAddress")
                        // Optionally, notify any listeners or update UI
                    } else {
                        Log.e("AddressAdapter", "Error storing address under PayoutAddress", task.exception)
                    }
                }
        }


        private fun showDeleteConfirmationDialog(addressType: String) {
            AlertDialog.Builder(binding.root.context)
                .setTitle("Delete Address")
                .setMessage("Are you sure you want to delete this address?")
                .setPositiveButton("Yes") { dialog, _ ->
                    deleteAddressFromFirebase(addressType)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        private fun deleteAddressFromFirebase(addressType: String) {
            database?.child(addressType)?.removeValue()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        addresses.removeIf { it.addressType == addressType }
                        notifyDataSetChanged()
                    } else {
                        Log.e("AddressAdapter", "Error deleting address for path: $addressType", task.exception)
                    }
                }
        }
    }
}