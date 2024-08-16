package com.capztone.fishfy.ui.activities.fragments

import android.content.ContentValues.TAG
import android.graphics.Color
import android.graphics.text.LineBreaker
import android.net.Uri
import android.os.Build
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import com.bumptech.glide.Glide
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.FragmentDetailsBinding
import com.capztone.fishfy.ui.activities.Utils.ToastHelper
import com.capztone.fishfy.ui.activities.models.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DetailsFragment : Fragment() {
    private val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var binding: FragmentDetailsBinding
    private var foodName: String? = null
    private var productQuantity: String? = null
    private var foodPrice: String? = null
    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference
    }


    private var foodDescription: String? = null
    private var discount: String? = null
    private var foodImage: String? = null
    private var foodNames: String? = null
    private var foodPrices: String? = null
    private var foodDescriptions: String? = null
    private var discounts: String? = null
    private var foodImages: String? = null
    private var quantity: Int = 0
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
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.WHITE
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color. WHITE
            }
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
                                shopname.text = path ?: ""
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
                                shopname.text = path ?: ""
                            }
                        }
                    }
                }
            } else {
                // Handle the case where neither MenuItemName nor DiscountItemName is provided
            }
        }
startMonitoringCart()
        initQuantityFromFirebase()

        binding.detailGoToBackImageButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.plusImageButton.setOnClickListener {
            quantity++
            addItemToCart()
            updateQuantityText()
        }
        binding.minusImageButton.setOnClickListener {

            if (quantity > 0) {
                quantity--

                addItemToCart1()
                updateQuantityText()
            }
            else if (quantity == 0) {
                quantity--
                updateQuantityText()


            }
        }


        binding.quantityText.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > debounceDuration) {
                lastClickTime = currentTime
                if (quantity > 0) {
                    quantity = 0 // Set quantity to 0 to remove the item
                    removeItemFromCart()
                } else {
                    quantity = 1 // Set quantity to 1 to add the item
                    addItemToCart()
                }

                binding.plusImageButton.visibility = View.VISIBLE
                binding.minusImageButton.visibility = View.VISIBLE
                updateQuantityText()
            }
        }
        observeFoodQuantityChanges()
    }

    private fun removeItemFromCart() {
        val database = FirebaseDatabase.getInstance().reference
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val foodName =
            binding.detailFoodNameTextView.text.toString() + binding.detailFoodNameTextView1.text.toString()

        if (userId != null && foodName.isNotEmpty()) {
            val cartItemsRef = database.child("user").child(userId).child("cartItems")

            cartItemsRef.orderByChild("foodName").equalTo(foodName)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Item in cart, remove it
                            for (cartSnapshot in dataSnapshot.children) {
                                cartSnapshot.ref.removeValue()
                                    .addOnSuccessListener {
                                        context?.let { it1 ->
                                            ToastHelper.showCustomToast(
                                                it1,
                                                "Item removed from cart successfully"
                                            )
                                        }
                                    }.addOnFailureListener {
                                        context?.let { it1 ->
                                            ToastHelper.showCustomToast(
                                                it1,
                                                "Failed to remove item"
                                            )
                                        }
                                    }
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
        }
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
                            updateQuantityText(0)
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

    private fun monitorCartItems() {
        val userId = auth.currentUser?.uid ?: return
        val cartItemsRef = database.child("user").child(userId).child("cartItems")

        cartItemsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                checkAndRemoveItemIfZeroQuantity(dataSnapshot)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                checkAndRemoveItemIfZeroQuantity(dataSnapshot)
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                // Handle child removed if necessary
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // Handle child moved if necessary
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Database error: ${databaseError.message}")
            }
        })
    }

    private fun checkAndRemoveItemIfZeroQuantity(dataSnapshot: DataSnapshot) {
        val foodQuantity = dataSnapshot.child("foodQuantity").getValue(Int::class.java) ?: return
        if (foodQuantity == 0) {
            dataSnapshot.ref.removeValue()
                .addOnSuccessListener {
                    context?.let { ToastHelper.showCustomToast(it, "Item removed from cart successfully") }
                }
                .addOnFailureListener {
                    context?.let { ToastHelper.showCustomToast(it, "Failed to remove item from cart") }
                }
        }
    }

    // Call this function to start monitoring the cart items
    private fun startMonitoringCart() {
        monitorCartItems()
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
                        updateQuantityText(0) // Default to 1 if removed
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
        if (quantity > 0) {
            binding.quantityText.text = quantity.toString()
        } else {
            binding.quantityText.text = "Add"
            binding.plusImageButton.visibility = View.GONE
            binding.minusImageButton.visibility = View.GONE
            this.quantity = 0 // Ensure quantity is set to 0
            removeItemFromCart() // Remove item from local cart
            removeFromFirebaseCart() // Remove item from Firebase
        }
        this.quantity = quantity // Update local quantity variable
    }


    private fun removeFromFirebaseCart() {
        val database = FirebaseDatabase.getInstance().reference
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val foodName =
            binding.detailFoodNameTextView.text.toString() + binding.detailFoodNameTextView1.text.toString()

        if (userId != null && foodName.isNotEmpty()) {
            val cartItemsRef = database.child("user").child(userId).child("cartItems")

            cartItemsRef.orderByChild("foodName").equalTo(foodName)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Item in cart, remove it
                            for (cartSnapshot in dataSnapshot.children) {
                                cartSnapshot.ref.removeValue()
                                    .addOnSuccessListener {
                                        context?.let { it1 ->
                                            ToastHelper.showCustomToast(
                                                it1,
                                                "Item removed from cart successfully"
                                            )
                                        }
                                    }.addOnFailureListener {
                                        context?.let { it1 ->
                                            ToastHelper.showCustomToast(
                                                it1,
                                                "Failed to remove item"
                                            )
                                        }
                                    }
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
        }

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
        if (quantity > 0) {
            binding.quantityText.text = quantity.toString()
        } else {
            binding.quantityText.text = "Add"
            binding.plusImageButton.visibility = View.GONE
            binding.minusImageButton.visibility = View.GONE
            this.quantity = 0 // Ensure quantity is set to 0
            removeItemFromCart() // Remove item from local cart
            removeFromFirebaseCart() // Remove item from Firebase
        }
        this.quantity = quantity
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
                        .setCancelable(false)
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
                         quantity = 0
                        updateQuantityText(0)
                        dialog.dismiss()
                    }
                    dialog.setOnKeyListener { _, keyCode, event ->
                        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                            ToastHelper.showCustomToast(context, "Please select Yes or No")
                            true
                        } else {
                            false
                        }
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
    private fun addItemToCart1() {
        val currentShopName = binding.shopname.text.toString()
        val cartItemsRef = currentUserID?.let {
            FirebaseDatabase.getInstance().reference.child("user").child(it).child("cartItems")
        }

        if (cartItemsRef != null) {
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
                            .setCancelable(false)
                            .create()

                        val positiveButton = customLayout.findViewById<AppCompatButton>(R.id.dialog_positive_button)
                        val negativeButton = customLayout.findViewById<AppCompatButton>(R.id.dialog_negative_button)

                        positiveButton.setOnClickListener {
                            cartItemsRef.removeValue().addOnSuccessListener {
                                addItemToCartWithoutCheck1()
                                dialog.dismiss()
                            }
                        }

                        negativeButton.setOnClickListener {
                            quantity = 0
                            updateQuantityText(0)
                            dialog.dismiss()
                        }
                        dialog.setOnKeyListener { _, keyCode, event ->
                            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                                ToastHelper.showCustomToast(context, "Please select Yes or No")
                                true
                            } else {
                                false
                            }
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
    }
    private fun addItemToCartWithoutCheck1() {
        val database = FirebaseDatabase.getInstance().reference
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val shopName = binding.shopname.text.toString()
        val foodName = binding.detailFoodNameTextView.text.toString() + binding.detailFoodNameTextView1.text.toString()
        val foodPrice = binding.textView21.text.toString().removePrefix("₹")
        val foodImage = binding.detailImageView.tag.toString()

        if (userId != null && foodName.isNotEmpty() && foodPrice.isNotEmpty() && foodImage.isNotEmpty()) {
            val cartItemsRef = database.child("user").child(userId).child("cartItems")

            cartItemsRef.orderByChild("foodName").equalTo(foodName)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (cartSnapshot in dataSnapshot.children) {
                            val currentQuantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java) ?: 1
                            val quantityText = binding.quantityText.text.toString()

// Check if quantityText is "Add" and foodQuantity is 0
                            if (quantityText == "Add" && currentQuantity <= 0) {
                                // Remove the item from Firebase
                                cartSnapshot.ref.removeValue()
                                    .addOnSuccessListener {
                                        context?.let { it1 ->
                                            ToastHelper.showCustomToast(it1, "Item removed from cart successfully")
                                        }
                                    }
                                    .addOnFailureListener {
                                        context?.let { it1 ->
                                            ToastHelper.showCustomToast(it1, "Failed to remove item from cart")
                                        }
                                    }
                            } else {
                                // Update the quantity or handle as needed
                                val newQuantity = currentQuantity - 1
                                cartSnapshot.ref.child("foodQuantity").setValue(newQuantity)
                                    .addOnSuccessListener {
                                        context?.let { it1 ->
                                            ToastHelper.showCustomToast(it1, "Item quantity updated in cart successfully")
                                        }
                                    }
                                    .addOnFailureListener {
                                        context?.let { it1 ->
                                            ToastHelper.showCustomToast(it1, "Failed to update item quantity")
                                        }
                                    }
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