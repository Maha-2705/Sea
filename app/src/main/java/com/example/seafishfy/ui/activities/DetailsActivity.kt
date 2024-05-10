package com.example.seafishfy.ui.activities

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.seafishfy.databinding.ActivityDetailsBinding
import com.example.seafishfy.ui.activities.models.CartItems
import com.example.seafishfy.ui.activities.models.DiscountItem
import com.example.seafishfy.ui.activities.Utils.ToastHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class DetailsActivity : AppCompatActivity() {

   private lateinit var binding: ActivityDetailsBinding
   private var foodName: String? = null
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
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      binding = ActivityDetailsBinding.inflate(layoutInflater)
      setContentView(binding.root)

      // initialize Firabase Auth
      auth = FirebaseAuth.getInstance()
      if (intent.hasExtra("MenuItemName")) {
         foodName = intent.getStringExtra("MenuItemName")
         foodPrice = intent.getStringExtra("MenuItemPrice")
         foodDescription = intent.getStringExtra("MenuItemDescription")
         foodImage = intent.getStringExtra("MenuItemImage")


      } else if (intent.hasExtra("DiscountItemName")) {
         foodNames = intent.getStringExtra("DiscountItemName")
         foodPrices = intent.getStringExtra("DiscountItemPrice")
         foodDescriptions = intent.getStringExtra("DiscountItemDescription")
         foodImages = intent.getStringExtra("DiscountItemImage")
      }

      with(binding) {
         if (intent.hasExtra("MenuItemName")) {
            detailFoodNameTextView.text = intent.getStringExtra("MenuItemName")
            detailsShortDescriptionTextView.text = intent.getStringExtra("MenuItemDescription")

            val price = intent.getStringExtra("MenuItemPrice")
            textView21.text = "Price : ₹$price"
            Glide.with(this@DetailsActivity)
               .load(Uri.parse(intent.getStringExtra("MenuItemImage")))
               .into(detailImageView)

            val firebasePaths = listOf("Shop 1", "Shop 2", "Shop 3", "Shop 4", "Shop 5", "Shop 6")
            foodName?.let {
               val description = intent.getStringExtra("MenuItemDescription")
               if (description != null) {
                  fetchItemPath(it, description, firebasePaths) { path ->
                     // Update the TextView with the fetched path
                     shopname.text = path ?: "Item not found in any path"
                  }
               }
            }


         } else if (intent.hasExtra("DiscountItemName")) {
            detailFoodNameTextView.text = intent.getStringExtra("DiscountItemName")
            detailsShortDescriptionTextView.text = intent.getStringExtra("DiscountItemDescription")
            val price = intent.getStringExtra("DiscountItemPrice")
            textView21.text = "Price : ₹$price"
            Glide.with(this@DetailsActivity)
               .load(Uri.parse(intent.getStringExtra("DiscountItemImage")))
               .into(detailImageView)
            val firebasePaths1 = listOf("Shop 1", "Shop 2", "Shop 3", "Shop 4", "Shop 5", "Shop 6")
            foodNames?.let {
               val description = intent.getStringExtra("DiscountItemDescription")
               if (description != null) {
                  fetchItemPath1(it, description, firebasePaths1) { path ->
                     // Update the TextView with the fetched path
                     shopname.text = path ?: "Item not found in any path"
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

      // Decrease button click listener
      binding.minusImageButton.setOnClickListener {
         if (quantity > 1) {
            quantity--
            updateQuantityText()
         }
      }
      //
      binding.detailAddToCartButton.setOnClickListener {
         addItemToCart()
      }


   }

   private fun fetchItemPath(
      itemName: String,
      itemDescription: String,
      paths: List<String>,
      onComplete: (String?) -> Unit
   ) {
      val database = FirebaseDatabase.getInstance()

      // Iterate through each shop path
      for (shopPath in paths) {
         val shopReference = database.reference.child(shopPath)
         val childPaths = listOf("menu", "menu1", "menu2")
         val userId = FirebaseAuth.getInstance().currentUser?.uid

         userId?.let { uid ->

            // Iterate through each child path within the shop
            for (childPath in childPaths) {
               val childReference = shopReference.child(childPath)
               childReference.addListenerForSingleValueEvent(object : ValueEventListener {
                  override fun onDataChange(snapshot: DataSnapshot) {
                     // Check if the item exists in this child path under the shop
                     snapshot.children.forEach { shopSnapshot ->
                        if (shopSnapshot.child("foodName").value == itemName &&
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
         // If item not found in any path, onComplete will be invoked with null
         onComplete(null)
      }
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
         // Iterate through each path in the list
         paths.forEach { path ->
            val shopReference = database.reference.child(path)
            val childReference = shopReference.child("discount")

            childReference.addListenerForSingleValueEvent(object : ValueEventListener {
               override fun onDataChange(snapshot: DataSnapshot) {
                  // Check if the item exists in the "discount" node
                  snapshot.children.forEach { shopSnapshot ->
                     if (shopSnapshot.child("foodNames").value == itemName &&
                        shopSnapshot.child("foodDescriptions").value == itemDescription
                     ) {
                        // If item found, invoke onComplete with the provided path
                        onComplete(path)
                        return
                     }
                  }
               }

               override fun onCancelled(error: DatabaseError) {
                  // Handle error
                  onComplete(null) // Invoke onComplete with null on error
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
      val shopname = binding.shopname.text

      if (shopname != null) {
         if (foodName != null && foodPrice != null && foodDescription != null && foodImage != null) {
            val cartItemQuery = database.child("user").child(userId).child("cartItems")
               .orderByChild("foodName").equalTo(foodName)

            cartItemQuery.addListenerForSingleValueEvent(object : ValueEventListener {
               override fun onDataChange(snapshot: DataSnapshot) {
                  if (snapshot.exists()) {
                     // Product already exists in the cart
                     ToastHelper.showCustomToast(
                        this@DetailsActivity,
                        "This product is already in your cart"
                     )
                  } else {
                     // Create a CartItems object for regular items
                     val cartItem = CartItems(
                        shopname.toString(),
                        foodName!!,
                        foodPrice!!,
                        foodDescription!!,
                        foodImage!!,
                        quantity
                     )
                     // Save data to cart item to Firebase database
                     database.child("user").child(userId).child("cartItems").push()
                        .setValue(cartItem)
                        .addOnSuccessListener {
                           ToastHelper.showCustomToast(
                              this@DetailsActivity,
                              "Item added to cart successfully 🥰"
                           )
                        }.addOnFailureListener {
                           ToastHelper.showCustomToast(
                              this@DetailsActivity,
                              "Failed to add item 😒"
                           )
                        }
                  }
               }

               override fun onCancelled(error: DatabaseError) {
                  // Handle onCancelled
               }
            })
         }
      } else if (foodNames != null && foodPrices != null && foodDescriptions != null && foodImages != null) {
         val cartItemQuery = database.child("user").child(userId).child("cartItems")
            .orderByChild("foodNames").equalTo(foodNames)

         cartItemQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
               if (snapshot.exists()) {
                  // Discount product already exists in the cart
                  ToastHelper.showCustomToast(
                     this@DetailsActivity,
                     "This discount product is already in your cart"
                  )
               } else {
                  // Create a CartItems object for discount items
                  val cartItem = CartItems(
                     shopname.toString(),
                     foodNames!!,
                     foodPrices!!,
                     foodDescriptions!!,
                     foodImages!!,
                     quantity
                  )

                  // Save data to cart item to Firebase database
                  database.child("user").child(userId).child("cartItems").push().setValue(cartItem)
                     .addOnSuccessListener {
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
      } else {
         ToastHelper.showCustomToast(this, "Item details not found 😒")
      }
   }
}