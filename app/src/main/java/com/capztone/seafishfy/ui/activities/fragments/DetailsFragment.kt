package com.capztone.seafishfy.ui.activities.fragments

import android.content.ContentValues.TAG
import android.graphics.text.LineBreaker
import android.net.Uri
import android.os.Build
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import com.bumptech.glide.Glide
import com.capztone.seafishfy.R
import com.capztone.seafishfy.databinding.FragmentDetailsBinding
import com.capztone.seafishfy.ui.activities.models.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DetailsFragment : Fragment() {

    private lateinit var binding: FragmentDetailsBinding
    private var foodName: String? = null
    private var productQuantity: String? = null
    private var foodPrice: String? = null
    private var foodDescription: String? = null
    private var discount: String? = null
    private var foodImage: String? = null
    private var foodNames: String? = null
    private var foodPrices: String? = null
    private var foodDescriptions: String? = null
    private var discounts: String? = null
    private var foodImages: String? = null
    private var quantity: Int = 1
    private lateinit var auth: FirebaseAuth
    private var isFavorited: Boolean = false

    private var lastClickTime: Long = 0
    private val debounceDuration: Long = 1000 // 1 second debounce duration

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.detailsShortDescriptionTextView.justificationMode =
                LineBreaker.JUSTIFICATION_MODE_INTER_WORD
        }

        auth = FirebaseAuth.getInstance()
        arguments?.let { bundle ->
            if (bundle.containsKey("MenuItemName")) {
                foodName = bundle.getString("MenuItemName")
                foodPrice = bundle.getString("MenuItemPrice")
                foodDescription = bundle.getString("MenuItemDescription")
                foodImage = bundle.getString("MenuItemImage")
                productQuantity = bundle.getString("MenuQuantity")
            } else if (bundle.containsKey("DiscountItemName")) {
                foodNames = bundle.getString("DiscountItemName")
                foodPrices = bundle.getString("DiscountItemPrice")
                foodDescriptions = bundle.getString("DiscountItemDescription")
                foodImages = bundle.getString("DiscountItemImage")
                productQuantity = bundle.getString("DiscountQuantity")
                discount = bundle.getString("discounts")
            }
        }

        with(binding) {
            if (arguments?.containsKey("MenuItemName") == true) {
                val foodName = arguments?.getString("MenuItemName")
                val foodNameParts = foodName?.split("/") ?: listOf(
                    "",
                    ""
                ) // Split by '/' if exists, or default to empty strings

                detailFoodNameTextView.text = foodNameParts.getOrNull(0) ?: ""
                detailFoodNameTextView1.text = foodNameParts.getOrNull(1)
                    ?: "" // Assign second part to detailFoodNameTextView1
                detailsShortDescriptionTextView.text = arguments?.getString("MenuItemDescription")
                textView22.text = arguments?.getString("MenuQuantity")
                val price = arguments?.getString("MenuItemPrice")
                textView21.text = "Price : ₹$price"
                Glide.with(this@DetailsFragment)
                    .load(Uri.parse(arguments?.getString("MenuItemImage")))
                    .into(detailImageView)

                foodName?.let {
                    val description = arguments?.getString("MenuItemDescription")
                    if (description != null) {
                        fetchShopLocations { shopLocations ->
                            fetchItemPath(it, description, shopLocations) { path ->
                                shopname.text = path ?: "Item not found in any path"
                            }
                        }
                    }
                }
            } else if (arguments?.containsKey("DiscountItemName") == true) {
                val foodName = arguments?.getString("DiscountItemName")
                val foodNameParts = foodName?.split("/") ?: listOf(
                    "",
                    ""
                ) // Split by '/' if exists, or default to empty strings

                detailFoodNameTextView.text = foodNameParts.getOrNull(0) ?: ""
                detailFoodNameTextView1.text = foodNameParts.getOrNull(1)
                    ?: "" // Assign second part to detailFoodNameTextView1
                detailsShortDescriptionTextView.text =
                    arguments?.getString("DiscountItemDescription")
                textView22.text = arguments?.getString("DiscountQuantity")

                val price = arguments?.getString("DiscountItemPrice")
                textView21.text = "Price : ₹$price"
                Glide.with(this@DetailsFragment)
                    .load(Uri.parse(arguments?.getString("DiscountItemImage")))
                    .into(detailImageView)


                foodName?.let {
                    val description = arguments?.getString("DiscountItemDescription")
                    if (description != null) {
                        fetchShopLocations { shopLocations ->
                            fetchItemPath1(it, description, shopLocations) { path ->
                                shopname.text = path ?: "Item not found in any path"
                            }
                        }
                    }
                }
            } else {
                // Handle the case where neither MenuItemName nor DiscountItemName is provided
            }
        }

        initQuantityFromFirebase()

        binding.detailGoToBackImageButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.plusImageButton.setOnClickListener {
            quantity++
            updateQuantityText()
        }
        binding.minusImageButton.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateQuantityText()
            }
        }

        binding.detailAddToCartButton.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > debounceDuration) {
                lastClickTime = currentTime
                addItemToCart()
            }
        }

        observeFoodQuantityChanges()
    }
    private fun initQuantityFromFirebase() {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference
        val cartItemsRef = database.child("user").child(userId).child("cartItems")

        cartItemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val foodName = arguments?.getString("MenuItemName") ?: arguments?.getString("DiscountItemName")
                foodName?.let { name ->
                    val cartQuery = cartItemsRef.orderByChild("foodName").equalTo(name)
                    cartQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (cartSnapshot in snapshot.children) {
                                val quantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java) ?: 1
                                updateQuantityText(quantity)
                                return
                            }
                            // If foodQuantity not found, default to 1
                            updateQuantityText(1)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle onCancelled
                        }
                    })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle onCancelled
            }
        })
    }

    private fun observeFoodQuantityChanges() {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference
        val cartItemsRef = database.child("user").child(userId).child("cartItems")

        cartItemsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle when a new item is added
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle when foodQuantity changes
                val foodName = arguments?.getString("MenuItemName") ?: arguments?.getString("DiscountItemName")
                foodName?.let { name ->
                    if (snapshot.child("foodName").getValue(String::class.java) == name) {
                        val quantity = snapshot.child("foodQuantity").getValue(Int::class.java) ?: 1
                        updateQuantityText(quantity)
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle when an item is removed (if necessary)
                val foodName = arguments?.getString("MenuItemName") ?: arguments?.getString("DiscountItemName")
                foodName?.let { name ->
                    if (snapshot.child("foodName").getValue(String::class.java) == name) {
                        updateQuantityText(1) // Default to 1 if removed
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle when a child node changes position (if necessary)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled
            }
        })
    }

    private fun updateQuantityText(quantity: Int) {
        binding.quantityText.text = quantity.toString()
        this.quantity = quantity // Update local quantity variable
    }

    private fun fetchShopLocations(onComplete: (List<String>) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference
        val shopLocationsRef = database.child("ShopLocations")

        shopLocationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val shopLocations = dataSnapshot.children.mapNotNull { it.key }
                onComplete(shopLocations)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
                onComplete(emptyList())
            }
        })
    }

    private fun fetchItemPath(
        itemName: String,
        itemDescription: String,
        paths: List<String>,
        onComplete: (String?) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance()
        for (shopPath in paths) {
            val shopReference = database.reference.child(shopPath)
            val childPaths = listOf("menu", "menu1", "menu2","menu3","menu4","menu5")

            val userId = FirebaseAuth.getInstance().currentUser?.uid

            userId?.let { uid ->
                for (childPath in childPaths) {
                    val childReference = shopReference.child(childPath)
                    childReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.children.forEach { shopSnapshot ->
                                if (
                                    shopSnapshot.child("foodDescription").value == itemDescription
                                ) {
                                    onComplete("$shopPath")
                                    return
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle error
                        }
                    })
                }
            }
        }
        onComplete(null)
    }

    private fun fetchItemPath1(
        itemName: String,
        itemDescription: String,
        paths: List<String>,
        onComplete: (String?) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let { uid ->
            paths.forEach { path ->
                val shopReference = database.reference.child(path)
                val childReference = shopReference.child("discount")

                childReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.children.forEach { shopSnapshot ->
                            if (
                                shopSnapshot.child("foodDescriptions").value == itemDescription
                            ) {
                                onComplete(path)
                                return
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        onComplete(null)
                    }
                })
            }
        }
    }

    private fun updateQuantityText() {
        binding.quantityText.text = quantity.toString()
    }

    private fun addItemToCart() {
        val database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid ?: ""
        val currentShopName = binding.shopname.text.toString()
        val cartItemsRef = database.child("user").child(userId).child("cartItems")

        cartItemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var differentShopFound = false

                for (itemSnapshot in dataSnapshot.children) {
                    val shopName = itemSnapshot.child("path").value as String
                    if (shopName != currentShopName) {
                        differentShopFound = true
                        break
                    }
                }

                if (differentShopFound) {
                    val context = binding.root.context
                    val layoutInflater = LayoutInflater.from(context)
                    val customLayout = layoutInflater.inflate(R.layout.shop_dialog, null)

                    val dialog = AlertDialog.Builder(context, R.style.CustomDialogg)
                        .setView(customLayout)
                        .create()

                    // Set background to transparent
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                    val positiveButton = customLayout.findViewById<AppCompatButton>(R.id.dialog_positive_button)
                    val negativeButton = customLayout.findViewById<AppCompatButton>(R.id.dialog_negative_button)

                    positiveButton.setOnClickListener {
                        cartItemsRef.removeValue().addOnSuccessListener {
                            addItemToCartWithoutCheck()
                            dialog.dismiss()
                        }
                    }

                    negativeButton.setOnClickListener {
                        dialog.dismiss()
                    }

                    dialog.show()
                } else {
                    addItemToCartWithoutCheck()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }
    private fun addItemToCartWithoutCheck() {
        val database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid ?: ""
        val shopname = binding.shopname.text

        if (shopname != null) {
            if (foodName != null && foodPrice != null && foodDescription != null && foodImage != null) {
                val cartQuery = database.child("user").child(userId).child("cartItems")
                    .orderByChild("foodName")
                    .equalTo(foodName)

                cartQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Item already in cart, update quantity
                            for (cartSnapshot in dataSnapshot.children) {

                                cartSnapshot.ref.child("foodQuantity").setValue(quantity)
                                    .addOnSuccessListener {
                                        updateHomeCartQuantity(
                                            foodName!!,
                                            quantity
                                        ) // Ensure new quantity is used here
                                    }.addOnFailureListener {
                                    }
                            }
                        } else {
                            // Item not in cart, add new item
                            val cartItem = CartItems(
                                shopname.toString(),
                                foodName!!,
                                foodPrice!!,
                                foodDescription!!,
                                foodImage!!,
                                quantity
                            )
                            database.child("user").child(userId).child("cartItems").push()
                                .setValue(cartItem)
                                .addOnSuccessListener {
                                    updateHomeCartQuantity(foodName!!, quantity)
                                }.addOnFailureListener {
                                }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
            }

            if (foodNames != null && foodPrices != null && foodDescriptions != null && foodImages != null) {
                val cartItemQuery = database.child("user").child(userId).child("cartItems")
                    .orderByChild("foodName").equalTo(foodNames)

                cartItemQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // Discount item already in cart, update quantity
                            for (cartSnapshot in snapshot.children) {

                                cartSnapshot.ref.child("foodQuantity").setValue(quantity)
                                    .addOnSuccessListener {
                                        foodName?.let { it1 ->
                                            updateHomeCartQuantity(
                                                it1,
                                                quantity
                                            )
                                        }

                                    }.addOnFailureListener {

                                    }
                            }
                        } else {
                            // Discount item not in cart, add new item
                            val cartItem = CartItems(
                                shopname.toString(),
                                foodNames!!,
                                foodPrices!!,
                                foodDescriptions!!,
                                foodImages!!,
                                quantity
                            )

                            database.child("user").child(userId).child("cartItems").push()
                                .setValue(cartItem)
                                .addOnSuccessListener {
                                    foodName?.let { it1 -> updateHomeCartQuantity(it1, quantity) }

                                }.addOnFailureListener {

                                }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle onCancelled
                    }
                })
            }
        }
    }

    private fun updateHomeCartQuantity(foodName: String, quantity: Int) {
        val userId = auth.currentUser?.uid ?: return
        val homeCartRef =
            FirebaseDatabase.getInstance().reference.child("user").child(userId).child("cartItems")

        homeCartRef.orderByChild("foodName").equalTo(foodName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (cartSnapshot in dataSnapshot.children) {
                        cartSnapshot.ref.child("foodQuantity").setValue(quantity)
                            .addOnSuccessListener {
                                // Handle success (optional)
                            }.addOnFailureListener { e ->
                                Log.e(TAG, "Failed to update foodQuantity: ${e.message}")
                                // Handle failure (optional)
                            }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Database error: ${databaseError.message}")
                    // Handle onCancelled (optional)
                }
            })
    }
}