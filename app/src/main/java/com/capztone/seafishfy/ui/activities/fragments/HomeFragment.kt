package com.capztone.seafishfy.ui.activities.fragments


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import com.google.firebase.storage.FirebaseStorage
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.RequiresApi

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import com.capztone.seafishfy.databinding.FragmentHomeBinding
import com.capztone.seafishfy.ui.activities.ContactusActivity
import com.capztone.seafishfy.ui.activities.LocationActivity
import com.capztone.seafishfy.ui.activities.models.DiscountItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import com.capztone.seafishfy.R
import com.google.android.gms.location.FusedLocationProviderClient
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capztone.seafishfy.ui.activities.adapters.ExploreShopAdapter
import com.capztone.seafishfy.ui.activities.adapters.HomeDiscountAdapter
import com.capztone.seafishfy.ui.activities.adapters.NearItemAdapter
import com.capztone.seafishfy.ui.activities.adapters.PreviousOrderAdapter
import com.capztone.seafishfy.ui.activities.models.MenuItem
import com.capztone.seafishfy.ui.activities.models.PreviousItem
import com.capztone.seafishfy.ui.activities.ViewModel.HomeViewModel
import com.capztone.seafishfy.ui.activities.adapters.DealItemAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), ExploreShopAdapter.OnItemClickListener {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databases: FirebaseDatabase
    private lateinit var viewModel: HomeViewModel
    private lateinit var discountItems: MutableList<DiscountItem>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var currentUserId: String
    private val menuItems = mutableListOf<MenuItem>()
    private lateinit var userId: String
    private val previousItem = mutableListOf<PreviousItem>()
    private lateinit var adapter: NearItemAdapter
    private lateinit var dealadapter: DealItemAdapter
    private lateinit var exploreShopAdapter: ExploreShopAdapter
    private lateinit var adapter1: PreviousOrderAdapter
    private val currentUserID: String? = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var sharedPreferences: SharedPreferences

    private val buyHistory: MutableList<PreviousItem> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        adapter1 = PreviousOrderAdapter(previousItem )
        binding.previousrRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = adapter1
        }

        observeViewModel()

        database = FirebaseDatabase.getInstance()
        databaseReference = database.getReference("Locations")
        databaseRef = FirebaseDatabase.getInstance().reference
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        databases = FirebaseDatabase.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        auth = FirebaseAuth.getInstance()


        sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)


        exploreShopAdapter = ExploreShopAdapter(listOf(), this)

        binding.Explorereclyview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exploreShopAdapter
        }

        binding.Nearitemrecycler.apply {
            adapter = NearItemAdapter(menuItems,requireContext())
            binding.Nearitemrecycler.layoutManager =
                GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
            // Update adapter with new data

        }


        val dealadapter = DealItemAdapter(menuItems, requireContext())
        binding.popularRecyclerView1.adapter = dealadapter
        binding.popularRecyclerView1.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)


        viewModel.menuItems.observe(viewLifecycleOwner) { items ->
            dealadapter.updateData(items)
        }
        viewModel.menuItems.observe(viewLifecycleOwner) { items ->
            adapter.updateData(items)
        }

        fetchUserShopNames()
        fetchUserShopName()

        fetchShopName()
        // Rest of your code...
        fetchBuyHistory()

        // Retrieve and display address and locality
        retrieveAddressAndLocality()
        setupRecyclerView()
        setupRecyclerView1()

        binding.dropdown.setOnClickListener {
            showPopupMenu(it)
        }

        binding.dotsMenu.setOnClickListener {
            showPopupMenus(it)
        }
        val adapter = HomeDiscountAdapter(requireContext())
        binding.discountrecycler.adapter = adapter
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.discountrecycler.layoutManager = layoutManager

        this@HomeFragment.database = FirebaseDatabase.getInstance()

        return binding.root
    }


    private fun fetchBuyHistory() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val userBuyHistoryRef = FirebaseDatabase.getInstance().reference.child("user").child(userId).child("BuyHistory")

        userBuyHistoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = mutableListOf<PreviousItem>()
                for (childSnapshot in snapshot.children) {
                    val foodNamesList = childSnapshot.child("foodNames").getValue(object : GenericTypeIndicator<ArrayList<String>>() {})
                    val foodPricesList = childSnapshot.child("foodPrices").getValue(object : GenericTypeIndicator<ArrayList<String>>() {})
                    val foodImagesList = childSnapshot.child("foodImage").getValue(object : GenericTypeIndicator<ArrayList<String>>() {})
                    val foodDescriptionList = childSnapshot.child("fooddescription").getValue(object : GenericTypeIndicator<ArrayList<String>>() {})

                    if (foodNamesList != null && foodPricesList != null && foodImagesList != null &&  foodDescriptionList != null) {
                        val foodName = if (foodNamesList.isNotEmpty()) foodNamesList[0] else ""
                        val foodPrice = if (foodPricesList.isNotEmpty()) foodPricesList[0] else ""
                        val foodImage = if (foodImagesList.isNotEmpty()) foodImagesList[0] else ""
                        val foodDescription = if (foodDescriptionList.isNotEmpty())  foodDescriptionList[0] else ""

                        val order = PreviousItem(foodName = foodName, foodPrice = foodPrice, foodImage = foodImage, foodDescription = foodDescription)
                        orders.add(order)
                    }
                }
                adapter1.updateData(orders)
                updateTextViewVisibility(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun updateTextViewVisibility(orders: List<PreviousItem>) {
        if (orders.isEmpty()) {
            binding.textVieww9.visibility = View.INVISIBLE

        } else {
            binding.textVieww9.visibility = View.VISIBLE
        }
    }


    private fun setupRecyclerView() {
        binding.previousrRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter =this@HomeFragment.adapter1
        }
    }

    private fun setupRecyclerView1() {
        adapter = NearItemAdapter(menuItems, requireContext())
        binding.Nearitemrecycler.apply {
            layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
            adapter = this@HomeFragment.adapter
        }
    }

    private fun fetchShopName() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val currentUserId = currentUser.uid
            val locationPath = "Locations/$currentUserId"
            Log.d("FirebaseDebug", "Fetching data from path: $locationPath")
            databaseRef.child(locationPath)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val shopNames = mutableListOf<String>()
                        Log.d("FirebaseDebug", "Data snapshot: ${dataSnapshot.value}")
                        val shopName = dataSnapshot.child("shopname").getValue(String::class.java)
                        Log.d("FirebaseDebug", "Found shop name: $shopName")
                        if (shopName != null) {
                            // Split the shop names by comma and trim any extra spaces
                            val individualShopNames = shopName.split(",").map { it.trim() }
                            shopNames.addAll(individualShopNames)
                        }
                        exploreShopAdapter.setShopList(shopNames)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("FirebaseError", "Error fetching data", databaseError.toException())
                    }
                })
        } else {
            Log.e("FirebaseError", "No authenticated user found.")
        }
    }


    override fun onItemClick(shopName: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid

        if (currentUserId != null) {
            val databaseRef =
                FirebaseDatabase.getInstance().getReference("Exploreshop/$currentUserId/ShopName")
            databaseRef.setValue(shopName)
                .addOnSuccessListener {
                    Log.d("FirebaseDebug", "Shop name $shopName stored successfully")
                    findNavController().navigate(R.id.action_homeFragment_to_shoponefragment)
                    // Navigate to next fragment or perform any other action if needed
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseError", "Error storing shop name: $e")
                }
        } else {
            Log.e("FirebaseError", "Current user ID is null")
        }
    }

    override fun onResume() {
        super.onResume()
        fetchOrders()
        fetchShopName()

    }

    private fun fetchOrders() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.fetchOrders()

        }
    }


    private fun fetchUserShopNames() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            databaseReference.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val shopNames = snapshot.child("shopname").getValue(String::class.java)
                        shopNames?.let { names ->
                            val shopNameList = names.split(",") // Split shop names by comma
                            shopNameList.forEach { shopName ->
                                fetchMenuItems(shopName.trim())
                                // Trim whitespace from shop name
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun fetchUserShopName() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            databaseReference.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val shopNames = snapshot.child("shopname").getValue(String::class.java)
                        shopNames?.let { names ->
                            val shopNameList = names.split(",") // Split shop names by comma
                            shopNameList.forEach { shopName ->
                                fetchMenuItem(shopName.trim())
                                // Trim whitespace from shop name
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }
    private fun fetchMenuItems(shopName: String) {
        val userId = auth.currentUser?.uid ?: return

        // Fetch user language first
        database.getReference("user").child(userId).child("language")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(languageSnapshot: DataSnapshot) {
                    val userLanguage = languageSnapshot.getValue(String::class.java)?.toLowerCase() ?: "english"

                    val menuReferences = listOf("menu", "menu1", "menu2")
                    menuReferences.forEach { menu ->
                        database.getReference(shopName).child(menu)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        for (itemSnapshot in snapshot.children) {
                                            val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                                            menuItem?.let {
                                                // Select the appropriate foodNames based on the user's language
                                                val foodNamesList = it.foodName ?: arrayListOf()
                                                val englishName = foodNamesList.getOrNull(0) ?: ""
                                                val languageSpecificName = when (userLanguage) {
                                                    "tamil" -> foodNamesList.getOrNull(1) ?: ""
                                                    "malayalam" -> foodNamesList.getOrNull(2) ?: ""
                                                    "telugu" -> foodNamesList.getOrNull(3) ?: ""
                                                    else -> englishName // Default to English
                                                }

                                                // Create a combined name with both English and language-specific names
                                                val combinedName = "$englishName / $languageSpecificName"

                                                // Add the combined name to the foodName list
                                                it.foodName = arrayListOf(combinedName)

                                                // Add the menuItem to the list
                                                menuItems.add(it)

                                            }
                                        }
                                        if (!::adapter.isInitialized) {
                                            adapter = NearItemAdapter(menuItems,requireContext())
                                            binding.Nearitemrecycler.adapter = adapter
                                        } else {
                                            adapter.notifyDataSetChanged()
                                        }
                                        adapter.updateMenuItems(menuItems)
                                        loadFavoriteItems()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    // Handle error
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun fetchMenuItem(shopName: String) {
        val userId = auth.currentUser?.uid ?: return
        val menuItems = mutableListOf<MenuItem>()
        database.getReference("user").child(userId).child("language")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(languageSnapshot: DataSnapshot) {
                    val userLanguage = languageSnapshot.getValue(String::class.java)?.toLowerCase() ?: "english"
                    val menuReferences = listOf("menu")
                    menuReferences.forEach { menu ->
                        database.getReference(shopName).child(menu)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        for (itemSnapshot in snapshot.children) {
                                            val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                                            menuItem?.let {
                                                val foodNamesList = it.foodName ?: arrayListOf()
                                                val englishName = foodNamesList.getOrNull(0) ?: ""
                                                val languageSpecificName = when (userLanguage) {
                                                    "tamil" -> foodNamesList.getOrNull(1) ?: ""
                                                    "malayalam" -> foodNamesList.getOrNull(2) ?: ""
                                                    "telugu" -> foodNamesList.getOrNull(3) ?: ""
                                                    else -> englishName
                                                }
                                                val combinedName = "$englishName / $languageSpecificName"
                                                it.foodName = arrayListOf(combinedName)
                                                menuItems.add(it)
                                            }
                                        }
                                        viewModel.setMenuItems(menuItems)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    // Handle error
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
    private fun loadFavoriteItems() {
        currentUserID?.let { userId ->
            val databaseRef = FirebaseDatabase.getInstance().getReference("Favourite").child(userId)

            databaseRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val favoriteItems = mutableMapOf<String, Boolean>()
                        val itemsToRemove = mutableListOf<DataSnapshot>()

                        snapshot.children.forEach { itemSnapshot ->
                            val favoriteItem = itemSnapshot.getValue(MenuItem::class.java)
                            favoriteItem?.let {
                                it.foodName?.forEach { foodName ->
                                    if (!it.favorite) {
                                        itemsToRemove.add(itemSnapshot)
                                    } else {
                                        favoriteItems[foodName] = it.favorite
                                    }
                                }
                            }
                        }

                        // Remove items from Firebase if they are not favorite
                        itemsToRemove.forEach { itemSnapshot ->
                            itemSnapshot.ref.removeValue()
                        }

                        updateMenuItemsWithFavorites(favoriteItems)
                    } else {
                        // Handle the case when there are no favorites
                        updateMenuItemsWithFavorites(emptyMap())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun updateMenuItemsWithFavorites(favoriteItems: Map<String, Boolean>) {
        menuItems.forEach { menuItem ->
            menuItem.foodName?.forEach { foodName ->
                menuItem.favorite = favoriteItems[foodName] ?: false
                // Assuming only one foodName should be true, break after setting favorite
                if (menuItem.favorite) return@forEach
            }
        }
        adapter.notifyDataSetChanged()
    }


    private fun observeViewModel() {
        viewModel.address.observe(viewLifecycleOwner) { address ->
            binding.tvAddress.text = address
        }

        viewModel.locality.observe(viewLifecycleOwner) { locality ->
            binding.tvLocality.text = locality
        }
        viewModel.latitude.observe(viewLifecycleOwner) { latitude ->
            binding.tvLatitude.text = latitude.toString()
        }
        viewModel.longitude.observe(viewLifecycleOwner) { longitude ->
            binding.tvLogitude.text = longitude.toString()
        }
    }


    private fun retrieveAddressAndLocality() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val userLocationRef = databaseReference.child(userId)
            userLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val address = snapshot.child("address").getValue(String::class.java)
                        val locality = snapshot.child("locality").getValue(String::class.java)
                        val latitude = snapshot.child("latitude").getValue(Double::class.java)
                        val longitude = snapshot.child("longitude").getValue(Double::class.java)



                        binding.tvAddress.text = address
                        binding.tvLocality.text = locality
                        binding.tvLatitude.text = latitude?.toString() ?: "N/A"
                        binding.tvLogitude.text = longitude?.toString() ?: "N/A"

                    } else {
                        // Handle case where data doesn't exist
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun showPopupMenus(view: View) {
        val popupMenus = PopupMenu(requireContext(), view)
        popupMenus.menuInflater.inflate(R.menu.option_menu, popupMenus.menu)

        // Set text color for the PopupMenu items
        val menus = popupMenus.menu

        popupMenus.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.about -> {
                    // Handle "Use Current Location" click
                    val intent = Intent(requireContext(), ContactusActivity::class.java)
                    startActivity(intent)
                    // Hide the dropdown icon after picking an address
                    true
                }
                // Add more saved address clicks as needed
                else -> false
            }
        }

        popupMenus.setOnDismissListener {
            // Dismiss the PopupMenu when it's dismissed
            popupMenus.dismiss()
        }

        popupMenus.show()
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.address, popupMenu.menu)

        // Set text color for the PopupMenu items
        val menu = popupMenu.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val spannableString = SpannableString(menuItem.title.toString())
            spannableString.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.navy)),
                0,
                spannableString.length,
                0
            )
            menuItem.title = spannableString
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.use_current_location -> {
                    // Handle "Use Current Location" click
                    val intent = Intent(requireContext(), LocationActivity::class.java)
                    startActivity(intent)
                    // Hide the dropdown icon after picking an address
                    true
                }
                // Add more saved address clicks as needed
                else -> false
            }
        }

        popupMenu.setOnDismissListener {
            // Dismiss the PopupMenu when it's dismissed
            popupMenu.dismiss()
        }

        popupMenu.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupImageSlider()

        binding.imageView1.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_FreshFishFragment)
        }

        binding.imageView2.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_DryFishFragment)
        }

        binding.imageView3.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_PicklesFragment)
        }
        binding.searchView1.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_productSearchFragment)
        }

        disableSearchViewInput(binding.searchView1)
    }

    private fun disableSearchViewInput(searchView: androidx.appcompat.widget.SearchView) {
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                searchView.clearFocus()
                findNavController().navigate(R.id.action_homeFragment_to_productSearchFragment)
            }
        }

    }

    private fun setupImageSlider() {
        val imageList = ArrayList<SlideModel>()
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("Banners")

        // List all images under "Banners" path
        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                listResult.items.forEach { item ->
                    // Get download URL for each image
                    item.downloadUrl.addOnSuccessListener { uri ->
                        // Add each image to the imageList
                        imageList.add(SlideModel(uri.toString(), scaleType = ScaleTypes.FIT))
                        // Set imageList to the image slider
                        binding.imageSlider.setImageList(imageList)
                    }.addOnFailureListener { exception ->
                        // Handle any errors during getting download URL

                    }
                }
            }
            .addOnFailureListener { exception ->
                // Handle any errors during listing images

            }

        val imageSlide = binding.imageSlider
        imageSlide.setItemClickListener(object : ItemClickListener {
            override fun doubleClick(position: Int) {
                // Double click listener implementation
            }

            override fun onItemSelected(position: Int) {
                // Check if the selected image is "ban"
            }
        })
    }
}