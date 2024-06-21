package com.capztone.seafishfy.ui.activities

import android.content.ContentValues.TAG
import android.graphics.text.LineBreaker
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.capztone.seafishfy.databinding.ActivityDetailsBinding
import com.capztone.seafishfy.ui.activities.models.CartItems
import com.capztone.seafishfy.ui.activities.Utils.ToastHelper
import com.capztone.seafishfy.ui.activities.models.DiscountItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DetailsActivity : AppCompatActivity() {

   private lateinit var binding: ActivityDetailsBinding
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

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      binding = ActivityDetailsBinding.inflate(layoutInflater)
      setContentView(binding.root)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         binding.detailsShortDescriptionTextView.justificationMode =
            LineBreaker.JUSTIFICATION_MODE_INTER_WORD
      }

      auth = FirebaseAuth.getInstance()
      if (intent.hasExtra("MenuItemName")) {
         foodName = intent.getStringExtra("MenuItemName")
         foodPrice = intent.getStringExtra("MenuItemPrice")
         foodDescription = intent.getStringExtra("MenuItemDescription")
         foodImage = intent.getStringExtra("MenuItemImage")
         productQuantity = intent.getStringExtra("MenuQuantity")
      } else if (intent.hasExtra("DiscountItemName")) {
         foodNames = intent.getStringExtra("DiscountItemName")
         foodPrices = intent.getStringExtra("DiscountItemPrice")
         foodDescriptions = intent.getStringExtra("DiscountItemDescription")
         foodImages = intent.getStringExtra("DiscountItemImage")
         productQuantity= intent.getStringExtra("DiscountQuantity")
         discount=intent.getStringExtra("discounts")
      }

      with(binding) {
         if (intent.hasExtra("MenuItemName")) {
            val foodName = intent.getStringExtra("MenuItemName")
            val foodNameParts = foodName?.split("/") ?: listOf("", "") // Split by '/' if exists, or default to empty strings

            detailFoodNameTextView.text = foodNameParts.getOrNull(0) ?: ""
            detailFoodNameTextView1.text = foodNameParts.getOrNull(1) ?: "" // Assign second part to detailFoodNameTextView1
            detailsShortDescriptionTextView.text = intent.getStringExtra("MenuItemDescription")
            textView22.text = intent.getStringExtra("MenuQuantity")
            val price = intent.getStringExtra("MenuItemPrice")
            textView21.text = "Price : ₹$price"
            Glide.with(this@DetailsActivity)
               .load(Uri.parse(intent.getStringExtra("MenuItemImage")))
               .into(detailImageView)

            fetchCartQuantity(foodName) { quantity ->
               this@DetailsActivity.quantity = quantity
               updateQuantityText()
            }

            foodName?.let {
               val description = intent.getStringExtra("MenuItemDescription")
               if (description != null) {
                  fetchShopLocations { shopLocations ->
                     fetchItemPath(it, description, shopLocations) { path ->
                        shopname.text = path ?: "Item not found in any path"
                     }
                  }
               }
            }
         } else if (intent.hasExtra("DiscountItemName")) {
            val foodName = intent.getStringExtra("DiscountItemName")
            val foodNameParts = foodName?.split("/") ?: listOf("", "") // Split by '/' if exists, or default to empty strings

            detailFoodNameTextView.text = foodNameParts.getOrNull(0) ?: ""
            detailFoodNameTextView1.text = foodNameParts.getOrNull(1) ?: "" // Assign second part to detailFoodNameTextView1
            detailsShortDescriptionTextView.text = intent.getStringExtra("DiscountItemDescription")
             textView22.text = intent.getStringExtra("DiscountQuantity")

            val price = intent.getStringExtra("DiscountItemPrice")
            textView21.text = "Price : ₹$price"
            Glide.with(this@DetailsActivity)
               .load(Uri.parse(intent.getStringExtra("DiscountItemImage")))
               .into(detailImageView)

            fetchCartQuantity(foodName) { quantity ->
               this@DetailsActivity.quantity = quantity
               updateQuantityText()
            }

            foodName?.let {
               val description = intent.getStringExtra("DiscountItemDescription")
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

      binding.detailGoToBackImageButton.setOnClickListener {
         finish()
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
   }


   private fun fetchCartQuantity(foodName: String?, onQuantityFetched: (Int) -> Unit) {
      val userId = auth.currentUser?.uid ?: return
      val cartItemsRef =
         FirebaseDatabase.getInstance().reference.child("Home").child(userId).child("cartItems")

      cartItemsRef.orderByChild("foodName").equalTo(foodName)
         .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
               var quantity = 1
               if (dataSnapshot.exists()) {
                  for (cartSnapshot in dataSnapshot.children) {
                     quantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java) ?: 1
                     break
                  }
               }
               onQuantityFetched(quantity)
            }

            override fun onCancelled(databaseError: DatabaseError) {
               // Handle error
               onQuantityFetched(1)
            }
         })
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
         val childPaths = listOf("menu", "menu1", "menu2")
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
               AlertDialog.Builder(this@DetailsActivity)
                  .setTitle("Different Shop")
                  .setMessage("You have items from a different shop in your cart. Do you want to clear the cart and add this item?")
                  .setPositiveButton("Yes") { _, _ ->
                     cartItemsRef.removeValue().addOnSuccessListener {
                        addItemToCartWithoutCheck()
                     }
                  }
                  .setNegativeButton("No", null)
                  .show()
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
                        val currentQuantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java) ?: 1
                        val newQuantity = currentQuantity + quantity
                        cartSnapshot.ref.child("foodQuantity").setValue(newQuantity)
                           .addOnSuccessListener {
                              updateHomeCartQuantity(foodName!!,quantity)
                              ToastHelper.showCustomToast(this@DetailsActivity, "Item quantity updated in cart successfully 🥰")
                           }.addOnFailureListener {
                              ToastHelper.showCustomToast(this@DetailsActivity, "Failed to update item quantity 😒")
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
                     database.child("user").child(userId).child("cartItems").push().setValue(cartItem)
                        .addOnSuccessListener {
                           ToastHelper.showCustomToast(this@DetailsActivity, "Item added to cart successfully 🥰")
                        }.addOnFailureListener {
                           ToastHelper.showCustomToast(this@DetailsActivity, "Failed to add item 😒")
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
                        val currentQuantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java) ?: 1
                        val newQuantity = currentQuantity + quantity
                        cartSnapshot.ref.child("foodQuantity").setValue(newQuantity)
                           .addOnSuccessListener {
                              foodName?.let { it1 -> updateHomeCartQuantity(it1,quantity) }
                              ToastHelper.showCustomToast(
                                 this@DetailsActivity,
                                 "Discount item quantity updated in cart successfully 🥰"
                              )
                           }.addOnFailureListener {
                              ToastHelper.showCustomToast(
                                 this@DetailsActivity,
                                 "Failed to update discount item quantity 😒"
                              )
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
                           foodName?.let { it1 -> updateHomeCartQuantity(it1,quantity) }
                           ToastHelper.showCustomToast(
                              this@DetailsActivity,
                              "Discount item added to cart successfully 🥰"
                           )
                        }.addOnFailureListener {
                           ToastHelper.showCustomToast(
                              this@DetailsActivity,
                              "Failed to add discount item 😒"
                           )
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
         FirebaseDatabase.getInstance().reference.child("Home").child(userId).child("cartItems")

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